package com.eshwar.rideconnectx.data.repository

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.eshwar.rideconnectx.data.ble.BleForegroundService
import com.eshwar.rideconnectx.data.ble.BleScannerImpl
import com.eshwar.rideconnectx.data.ble.ScooterCandidateFilter
import com.eshwar.rideconnectx.data.local.ServicePreferencesStore.Companion.MAX_PLAUSIBLE_ODO_KM
import com.eshwar.rideconnectx.data.local.SessionDataStore
import com.eshwar.rideconnectx.domain.model.BleDevice
import com.eshwar.rideconnectx.domain.model.OnboardingStatus
import com.eshwar.rideconnectx.domain.model.ScooterTelemetry
import com.eshwar.rideconnectx.domain.repository.BlePacket
import com.eshwar.rideconnectx.domain.repository.BleRepository
import com.eshwar.rideconnectx.domain.repository.ConnectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: BleScannerImpl,
    private val sessionDataStore: SessionDataStore,
    private val userPreferences: com.eshwar.rideconnectx.data.local.UserPreferencesStore,
    private val phoneStatus: com.eshwar.rideconnectx.data.ble.PhoneStatusProvider,
    private val appSettings: com.eshwar.rideconnectx.data.local.AppSettingsStore,
) : BleRepository {
    private val TAG = "RCX-BLE"
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var bleService: BleForegroundService? = null
    private val _isServiceBound = MutableStateFlow(false)
    
    /**
     * True once the rider has explicitly disconnected or forgotten the vehicle.
     *
     * Without it the watcher below would treat a deliberate disconnect as a
     * dropout and reconnect within seconds, making the Disconnect button look
     * broken.
     */
    @Volatile private var userDisconnected = false

    /**
     * Keeps trying to bring the link back while the app is alive.
     *
     * **This did not exist.** `reconnectLastDevice()` was called from exactly one
     * place - `MainActivity.onCreate` when `savedInstanceState == null` - so it
     * only ever fired on a *cold app launch*. Switching the scooter off and on
     * with the app open left it disconnected forever, and the only way back was
     * to kill and reopen the app. The rider hit this on 18 August and reported
     * it as 'not reconnecting even though I opened app and kept there'.
     *
     * It also masked a second problem: with no link there is no heartbeat, so
     * the message and missed-call lamps could never light either.
     *
     * Backs off from 3s to 30s so a scooter that is simply out of range does not
     * sit there scanning and draining the phone.
     */
    private fun startReconnectWatcher() {
        repositoryScope.launch {
            var delayMs = RECONNECT_MIN_MS
            while (true) {
                kotlinx.coroutines.delay(delayMs)

                val connected = _connectionState.value is ConnectionState.Connected
                if (connected) {
                    delayMs = RECONNECT_MIN_MS
                    continue
                }
                if (userDisconnected) continue
                if (!appSettings.settings.first().autoConnect) continue

                val address = sessionDataStore.lastDeviceAddress.first()
                if (address.isNullOrBlank()) continue

                Log.d(TAG, "Reconnect watcher: retrying $address (next in ${delayMs}ms)")
                connect(address).collect()

                // Widen the gap only while it keeps failing.
                delayMs = (delayMs * 2).coerceAtMost(RECONNECT_MAX_MS)
            }
        }
    }

    /**
     * Pushes an alert to the cluster the moment it arrives.
     *
     * The lamp used to ride only on the 5s heartbeat, so a message landing just
     * after a tick waited up to five seconds. The rider's objection, 18 August,
     * is the right one: a lamp that appears seconds late - or a lamp with no
     * sender name - makes them take the phone out mid-road to see who it is,
     * which is the hazard the cluster is supposed to remove.
     *
     * Two packets go out, in this order:
     *  1. `0x06`, carrying the app, the sender and the message. This is what
     *     puts a NAME on the dashboard. `buildNotificationPacket` had existed
     *     since the protocol work and was never called from anywhere, so the
     *     name had genuinely never been transmitted.
     *  2. `0x33`, the heartbeat, so the lamp lights in the same instant rather
     *     than on the next tick.
     *
     * End to end this is a BLE write, so tens of milliseconds - bounded by the
     * radio, not by our polling.
     */
    private fun startAlertPusher() {
        repositoryScope.launch {
            com.eshwar.rideconnectx.data.nav.ClusterAlerts.alerts.collect { alert ->
                if (_connectionState.value !is ConnectionState.Connected) {
                    Log.d(TAG, "Alert while disconnected, nothing to push: ${alert.title}")
                    return@collect
                }

                val named = com.eshwar.rideconnectx.domain.ProtocolEngine.buildNotificationPacket(
                    appIdentifier = alert.appIdentifier,
                    title = alert.title,
                    message = alert.text,
                )
                val namedSent = sendPacket(named).first()

                com.eshwar.rideconnectx.data.nav.ClusterAlerts.expireStale()
                val beat = com.eshwar.rideconnectx.domain.ProtocolEngine.buildHeartbeatPacket(
                    batteryBucket = phoneStatus.batteryBucket(),
                    isCharging = phoneStatus.isCharging(),
                    signalBars = phoneStatus.signalBars(),
                    clockHHmmss = phoneStatus.clockHHmmss(),
                    notificationPending =
                        com.eshwar.rideconnectx.data.nav.ClusterAlerts.notificationPending.value,
                    missedCallPending =
                        com.eshwar.rideconnectx.data.nav.ClusterAlerts.missedCallPending.value,
                )
                val beatSent = sendPacket(beat).first()

                Log.d(
                    TAG,
                    "Alert pushed: who='${alert.title}' call=${alert.isCall} " +
                        "0x06=$namedSent 0x33=$beatSent",
                )
            }
        }
    }

    init {
        // Last known readings first, so the dashboard has something true to show
        // before (or without) a live frame. Marked valid because these ARE real
        // readings from this vehicle - just not necessarily current.
        repositoryScope.launch {
            sessionDataStore.cachedTelemetry.first()?.let { c ->
                if (_telemetry.value.odometerKm == 0) {
                    _telemetry.value = ScooterTelemetry(
                        odometerKm = c.odometerKm,
                        tripAKm = c.tripAKm,
                        tripBKm = c.tripBKm,
                        fuelSegments = c.fuelSegments,
                        fuelLevel = c.fuelSegments / 5f,
                        isValid = true,
                    )
                }
            }
        }
        startReconnectWatcher()
        startAlertPusher()
        repositoryScope.launch {
            observeNotifications().collect { packet ->
                val telemetry = com.eshwar.rideconnectx.domain.ProtocolEngine.parseTelemetry(packet.data)
                telemetry?.let {
                    _telemetry.value = it
                    // Persisted so the dashboard survives a dropout and a restart.
                    //
                    // The frame's checksum (byte 28) is still unverified, so a
                    // garbled reading can land here. The service odometer is
                    // guarded by `isPlausibleOdometer`, but this cache is what
                    // the dashboard card actually shows and it had no guard at
                    // all — which is how a 6,001,923 km reading stayed on screen
                    // against a real 2,248. Don't persist a reading the service
                    // store would refuse; the live value is still shown.
                    if (it.odometerKm > MAX_PLAUSIBLE_ODO_KM) {
                        Log.w(TAG, "Not caching implausible odometer ${it.odometerKm} km")
                        return@let
                    }
                    sessionDataStore.cacheTelemetry(
                        odometerKm = it.odometerKm,
                        tripAKm = it.tripAKm,
                        tripBKm = it.tripBKm,
                        fuelSegments = it.fuelSegments,
                    )
                }
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BleForegroundService.LocalBinder
            bleService = binder.getService()
            _isServiceBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleService = null
            _isServiceBound.value = false
        }
    }

    /**
     * True when the app holds the Bluetooth permissions the platform demands
     * before a `connectedDevice` foreground service may run. Starting the
     * service without them is an immediate SecurityException.
     */
    private val hasBluetoothPermission: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            ).all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun ensureServiceStarted(): Boolean {
        if (!hasBluetoothPermission) {
            Log.w(TAG, "Bluetooth permission not granted — not starting BLE service")
            return false
        }
        if (!_isServiceBound.value) {
            Intent(context, BleForegroundService::class.java).also { intent ->
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                context.startForegroundService(intent)
            }
        }
        return true
    }

    private val _telemetry = MutableStateFlow(ScooterTelemetry())
    override val telemetry: Flow<ScooterTelemetry> = _telemetry.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)

    /** Single source of truth — see [BleRepository.connectionState]. */
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override val onboardingStatus: Flow<OnboardingStatus> = sessionDataStore.onboardingStatus.map {
        try { OnboardingStatus.valueOf(it) } catch (e: Exception) { OnboardingStatus.NOT_STARTED }
    }

    override suspend fun updateOnboardingStatus(status: OnboardingStatus) {
        sessionDataStore.saveOnboardingStatus(status.name)
    }

    override fun scanDevices(): Flow<List<BleDevice>> {
        if (!ensureServiceStarted()) return flowOf(emptyList())
        return scanner.scanDevices()
    }

    override fun stopScan() {
        scanner.stopScan()
    }

    /**
     * Emits the live connection state for [address].
     *
     * Subscription happens *before* the connect is issued, and the service's
     * replay cache is cleared first — otherwise the `replay = 1` SharedFlow
     * hands the new subscriber the previous session's DISCONNECTED and the UI
     * concludes the connection failed before it has even been attempted.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun connect(address: String): Flow<ConnectionState> {
        Log.d(TAG, "Connect requested: $address")
        if (!ensureServiceStarted()) {
            return flowOf(ConnectionState.Failed("Bluetooth permission is required to connect."))
                .onEach { _connectionState.value = it }
        }

        return _isServiceBound.filter { it }.flatMapLatest {
            val service = bleService
                ?: return@flatMapLatest flowOf(ConnectionState.Failed("Service unavailable"))

            val name = service.getDeviceByAddress(address)?.name ?: "Suzuki Scooter"
            val savedAddress = sessionDataStore.lastDeviceAddress.first()

            if (!ScooterCandidateFilter.isSuzukiScooterCandidate(name, address, savedAddress)) {
                Log.d(TAG, "Unsupported device rejected: $address ($name)")
                return@flatMapLatest flowOf(ConnectionState.UnsupportedDevice)
            }

            service.resetConnectionState()

            service.connectionState
                .onStart {
                    // Runs once the collector is attached, so no state is missed.
                    Log.d(TAG, "Subscribed; issuing connect for $address")
                    service.connect(address)
                }
                .map { state ->
                    when (state) {
                        android.bluetooth.BluetoothProfile.STATE_CONNECTED -> {
                            Log.d(TAG, "GATT connected: $address")
                            // Cancel the previous wait before starting a new
                            // one. Without this, a peer that never resolved
                            // 0xFEFB left a coroutine parked on `servicesReady`
                            // forever — and the reconnect watcher retries every
                            // 3-30 s, so they accumulated. Worse, `servicesReady`
                            // is a SharedFlow: when the *real* scooter later
                            // connected and emitted, every parked collector woke
                            // and wrote ITS OWN stale address into the session,
                            // re-poisoning the very value this check protects.
                            sessionSaveJob?.cancel()
                            sessionSaveJob = repositoryScope.launch {
                                // Not trusted until the cluster identifies
                                // itself. A remembered address is not proof of
                                // the vehicle, and saving the peer here is what
                                // made the *next* silent auto-reconnect treat an
                                // impostor as the scooter. `servicesReady` only
                                // fires once the 0xFEFB write characteristic
                                // resolved.
                                bleService?.servicesReady?.first()
                                // Prefer the name the service resolved from the
                                // connected device: on auto-reconnect there is
                                // no cached scan result, so [name] above is the
                                // placeholder. Storing that loses the model,
                                // and `configureForDevice` reads the checksum
                                // branch out of it — right for the Access only
                                // by luck, wrong for an e-ACCESS or TFT.
                                val resolved = service.resolvedDeviceName
                                    ?: sessionDataStore.lastDeviceName.first()
                                    ?: name
                                sessionDataStore.saveSession(address, resolved)
                            }
                            // Status first: it is a single packet, and starting
                            // it after the ten profile repeats left the cluster
                            // without battery or signal for ten seconds.
                            startHeartbeat()
                            // The cluster's WELCOME line comes from the profile
                            // packet, so it goes out as part of connecting.
                            sendProfile()
                            ConnectionState.Connected(address, name)
                        }
                        android.bluetooth.BluetoothProfile.STATE_CONNECTING ->
                            ConnectionState.Connecting(address, name)
                        android.bluetooth.BluetoothProfile.STATE_DISCONNECTED ->
                            ConnectionState.Disconnected("Scooter disconnected")
                        else -> ConnectionState.Failed("Connection failed")
                    }
                }
        }
            // Publishing here rather than in each caller is what keeps every
            // screen agreeing about whether the vehicle is connected. Sitting
            // outside flatMapLatest means the early rejections above are
            // published too.
            .onEach { _connectionState.value = it }
    }

    /**
     * Reconnects to the last vehicle. Called on app start; a failure is silent
     * because the rider did not ask for anything — if the bike is off or out of
     * range the pairing screen is still there.
     */
    override fun reconnectLastDevice() {
        userDisconnected = false
        repositoryScope.launch {
            val address = sessionDataStore.lastDeviceAddress.first()
            if (address.isNullOrBlank()) {
                Log.d(TAG, "No previously paired vehicle — nothing to reconnect")
                return@launch
            }
            if (_connectionState.value is ConnectionState.Connected) return@launch

            Log.d(TAG, "Auto-reconnecting to $address")
            connect(address).collect()
        }
    }

    override fun disconnect() {
        Log.d(TAG, "User disconnect requested")
        // Stops the reconnect watcher from immediately undoing this.
        userDisconnected = true
        bleService?.disconnect()
        _connectionState.value = ConnectionState.Disconnected("Disconnected")
    }

    /**
     * Full teardown: drops the GATT link, unbinds and stops the service.
     *
     * Called when the app is cleared from recents. Until there is an explicit
     * "allow background activity" setting, closing the app means closing the
     * connection — the scooter should not keep showing the Bluetooth glyph.
     */
    override fun shutdown() {
        Log.d(TAG, "Shutdown requested — dropping link and stopping service")
        profileJob?.cancel()
        heartbeatJob?.cancel()
        bleService?.disconnect()
        scanner.stopScan()

        if (_isServiceBound.value) {
            runCatching { context.unbindService(serviceConnection) }
            _isServiceBound.value = false
        }
        bleService = null

        runCatching {
            context.stopService(Intent(context, BleForegroundService::class.java))
        }

        _connectionState.value = ConnectionState.Idle
        // Deliberately NOT cleared. Wiping it here is what made every reading
        // vanish the moment the rider disconnected, replacing an odometer they
        // had just read with a dash. The values stay as last-known.
    }

    /**
     * Writes [packet] to the vehicle.
     *
     * Reports what actually happened: false when there is no link or the write
     * characteristic has not been resolved, so the Navigation screen says
     * "not relaying" instead of claiming a delivery that never left the phone.
     */
    override fun sendPacket(packet: ByteArray): Flow<Boolean> {
        val service = bleService
        if (service == null || !service.isReadyToWrite) {
            Log.d(TAG, "sendPacket(${packet.size} bytes) — no writable link")
            return flowOf(false)
        }
        return flowOf(service.writePacket(packet))
    }

    /**
     * Sends the rider's name so the cluster can greet them, mirroring what the
     * official app does on connect.
     */
    /** The pending "save this peer once it proves itself" wait. One at a time. */
    private var sessionSaveJob: kotlinx.coroutines.Job? = null

    private var profileJob: kotlinx.coroutines.Job? = null

    /**
     * The official app does not send the profile once on connect — it sends it
     * from a repeating timer (`C4956y.run()`). A single write on connect may
     * simply be missed, which is the most likely reason the cluster never
     * printed WELCOME. This mirrors the repeat.
     */
    // Synchronized: every connect() collector calls this on STATE_CONNECTED,
    // often at the same instant. Unsynchronized, two callers each cancel the
    // same old job and each launch a new one, and the loser is orphaned for good.
    @Synchronized
    private fun sendProfile() {
        profileJob?.cancel()
        profileJob = repositoryScope.launch {
            // Waits for service discovery rather than firing on STATE_CONNECTED:
            // the write characteristic does not exist yet at that point, which
            // the first hardware run proved by reporting sent=false.
            bleService?.servicesReady?.first()

            val clusterName = sessionDataStore.lastDeviceName.first()

            // The checksum branch depends on the vehicle model, which the
            // official app reads out of the advertised name.
            com.eshwar.rideconnectx.domain.ProtocolEngine.configureForDevice(clusterName)

            // Byte 27 answers "is this the same cluster as last time?". Read the
            // previous value *before* overwriting it, or the comparison is
            // always true and the greeting never changes.
            val previousCluster = sessionDataStore.previousClusterName.first()
            val flag = com.eshwar.rideconnectx.domain.ProtocolEngine.profileFlagFor(
                deviceName = clusterName,
                previousDeviceName = previousCluster,
            )
            clusterName?.takeIf { it.isNotBlank() }?.let { sessionDataStore.rememberCluster(it) }

            val name = userPreferences.riderName.first().ifBlank { return@launch }
            val packet = com.eshwar.rideconnectx.domain.ProtocolEngine.buildProfilePacket(name, flag)

            repeat(PROFILE_REPEATS) { attempt ->
                val sent = sendPacket(packet).first()
                Log.d(
                    TAG,
                    "Profile '$name' flag='$flag' (prev=$previousCluster now=$clusterName) " +
                        "attempt ${attempt + 1}/$PROFILE_REPEATS sent=$sent",
                )
                // Delay *after* the write, and skip it on the final pass, so the
                // greeting burst finishes as early as it can.
                if (attempt < PROFILE_REPEATS - 1) kotlinx.coroutines.delay(PROFILE_INTERVAL_MS)
            }
        }
    }

    private var heartbeatJob: kotlinx.coroutines.Job? = null

    /**
     * Sends the 0x33 status packet on a repeating timer for as long as the link
     * is up — phone battery, signal bars, the clock, and the message and
     * missed-call indicators.
     *
     * The official app does the same from `C4956y.run()`. It has to repeat:
     * the cluster shows the *last* status it received, so a single write would
     * leave a stale battery reading on the dashboard for the whole ride.
     */
    // Synchronized for the same reason as [sendProfile]. Without it the 11 Sep
    // 2026 log showed seven heartbeat loops alive at once — every beat written
    // seven times in the same millisecond from seven threads.
    @Synchronized
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        com.eshwar.rideconnectx.data.nav.ClusterAlerts.reset()
        heartbeatJob = repositoryScope.launch {
            bleService?.servicesReady?.first()

            while (kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
                // Drop any lamp whose 30s hold has elapsed, then read. The
                // lamp used to be cleared after a single heartbeat had carried
                // it, so it could be lit for under a second - no use to someone
                // riding. ClusterAlerts now expires by wall clock instead.
                com.eshwar.rideconnectx.data.nav.ClusterAlerts.expireStale()
                val notification =
                    com.eshwar.rideconnectx.data.nav.ClusterAlerts.notificationPending.value
                val missedCall =
                    com.eshwar.rideconnectx.data.nav.ClusterAlerts.missedCallPending.value

                val packet = com.eshwar.rideconnectx.domain.ProtocolEngine.buildHeartbeatPacket(
                    batteryBucket = phoneStatus.batteryBucket(),
                    isCharging = phoneStatus.isCharging(),
                    signalBars = phoneStatus.signalBars(),
                    clockHHmmss = phoneStatus.clockHHmmss(),
                    notificationPending = notification,
                    missedCallPending = missedCall,
                )

                val sent = sendPacket(packet).first()
                if (!sent) {
                    Log.d(TAG, "Heartbeat not sent — link gone, stopping")
                    return@launch
                }

                // Deliberately NOT cleared here. Clearing on the first
                // successful heartbeat is what made the lamp flash and vanish.
                // The hold is owned by ClusterAlerts and expires on a clock, so
                // a failed write cannot swallow the alert either.

                kotlinx.coroutines.delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private companion object {
        /** Matches the official app's ~1 s status cadence. */
        const val HEARTBEAT_INTERVAL_MS = 5000L
        /** First reconnect attempt after a dropout. */
        private const val RECONNECT_MIN_MS = 3_000L
        
        /** Ceiling for the reconnect backoff - a scooter out of range must not
         *  keep the phone scanning at full rate. */
        private const val RECONNECT_MAX_MS = 30_000L
        /** Matches the official app's ~200 ms cadence for repeated writes. */
        const val PROFILE_INTERVAL_MS = 1000L
        const val PROFILE_REPEATS = 10
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeNotifications(): Flow<BlePacket> {
        return _isServiceBound.filter { it }.flatMapLatest {
            bleService?.notifications?.map { (uuid, data) ->
                BlePacket(uuid, data)
            } ?: emptyFlow()
        }
    }
}
