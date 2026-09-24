package com.eshwar.rideconnectx.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.eshwar.rideconnectx.core.di.ServiceEntryPoint
import com.eshwar.rideconnectx.data.nav.MapsNotificationParser
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Debug-only hook for driving the navigation pipeline without Google Maps.
 *
 * It exists because the real path can only be exercised on a phone, on a live
 * route, next to a running scooter — and everything upstream of the BLE write
 * deserves to be verified before then. It feeds text through the *same*
 * [MapsNotificationParser] and the same relay that the real listener uses, so a
 * pass here means the parse → relay → UI chain is sound; only the notification
 * delivery itself is stubbed.
 *
 * Lives in `src/debug`, so it is not compiled into a release build at all.
 *
 *     adb shell am broadcast -a com.eshwar.rideconnectx.TEST_NAV \
 *       -e title "Turn left onto MG Road" -e text "400 m · 12 min"
 *
 *     adb shell am broadcast -a com.eshwar.rideconnectx.TEST_NAV_STOP
 */
class NavTestReceiver : BroadcastReceiver() {

    private companion object {
        const val TAG = "RCX-Nav"
        const val ACTION_MANEUVER = "com.eshwar.rideconnectx.TEST_NAV"
        const val ACTION_STOP = "com.eshwar.rideconnectx.TEST_NAV_STOP"

        /** Sends one raw cluster maneuver code — see [sendRawCode]. */
        const val ACTION_CODE = "com.eshwar.rideconnectx.TEST_CODE"

        /**
         * Forces the heartbeat's battery field to a given value, so the scale
         * the cluster expects can be found by looking at the dashboard.
         *
         *     adb shell "am broadcast -n <pkg>/<this> \
         *       -a com.eshwar.rideconnectx.TEST_BATT --ei val 3"
         *
         * `--ei val -1` hands the field back to the real battery level.
         */
        const val ACTION_BATTERY = "com.eshwar.rideconnectx.TEST_BATT"

        /** Same idea for the signal field — see [ACTION_BATTERY]. */
        const val ACTION_SIGNAL = "com.eshwar.rideconnectx.TEST_SIG"

        /**
         * Sends ONE heartbeat with the alert flags forced to given characters.
         *
         * Exists to settle a contradiction in the knowledge base: the JADX
         * decompile says 78 (N) means an alert is PRESENT, while a note written
         * from a packet capture says Y is the alert. We ship Y, and on
         * 18 August the cluster ignored a correctly formed Y missed-call flag,
         * which points at the decompile being the right reading.
         *
         * Usage: broadcast TEST_FLAGS with --es msg N --es call N
         */
        const val ACTION_FLAGS = "com.eshwar.rideconnectx.TEST_FLAGS"

        /**
         * Fingerprints every manoeuvre arrow in Maps' catalogue into the ride
         * log. One shot; needs no route and no movement.
         */
        const val ACTION_ARROWS = "com.eshwar.rideconnectx.TEST_ARROWS"

        /**
         * Switches how kilometres are written into the distance fields, so the
         * encoding can be settled at the scooter without a rebuild.
         * `--ei style 0|1|2` - see ProtocolEngine.kmStyle.
         */
        const val ACTION_KMSTYLE = "com.eshwar.rideconnectx.TEST_KMSTYLE"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val relay = EntryPointAccessors
            .fromApplication(context.applicationContext, ServiceEntryPoint::class.java)
            .navigationRelay()

        when (intent.action) {
            ACTION_KMSTYLE -> {
                val style = intent.getIntExtra("style", 0)
                com.eshwar.rideconnectx.domain.ProtocolEngine.kmStyle = style
                android.util.Log.d("RCX-BLE", "kmStyle set to $style")
                return
            }
            ACTION_ARROWS -> {
                com.eshwar.rideconnectx.data.nav.MapsNotificationListener
                    .instance?.dumpArrowCatalog()
                return
            }
            ACTION_STOP -> {
                Log.d(TAG, "[TEST] stop")
                relay.stop()
            }

            ACTION_BATTERY -> {
                val value = intent.getIntExtra("val", -1)
                val status = EntryPointAccessors
                    .fromApplication(context.applicationContext, ServiceEntryPoint::class.java)
                    .phoneStatusProvider()
                status.batteryOverride = value.takeIf { it >= 0 }
                Log.d(TAG, "[SWEEP] battery field forced to ${status.batteryOverride ?: "real"}")
            }

            ACTION_SIGNAL -> {
                val value = intent.getIntExtra("val", -1)
                val status = EntryPointAccessors
                    .fromApplication(context.applicationContext, ServiceEntryPoint::class.java)
                    .phoneStatusProvider()
                status.signalOverride = value.takeIf { it >= 0 }
                Log.d(TAG, "[SWEEP] signal field forced to ${status.signalOverride ?: "real"}")
            }

            ACTION_FLAGS -> {
                val msg = intent.getStringExtra("msg")?.firstOrNull() ?: 78.toChar()
                val call = intent.getStringExtra("call")?.firstOrNull() ?: 78.toChar()
                sendForcedFlags(context, msg, call)
            }

            ACTION_CODE -> {
                val code = intent.getIntExtra("code", -1)
                val distance = intent.getIntExtra("dist", 200)
                if (code < 0) {
                    Log.w(TAG, "[SWEEP] no code given")
                    return
                }
                sendRawCode(context, code, distance)
            }

            ACTION_MANEUVER -> {
                val title = intent.getStringExtra("title").orEmpty()
                val text = intent.getStringExtra("text").orEmpty()

                val maneuver = MapsNotificationParser.parse(title, text)
                if (maneuver == null) {
                    Log.w(TAG, "[TEST] not a navigation notification: '$title' / '$text'")
                    return
                }

                Log.d(TAG, "[TEST] ${maneuver.instruction} | ${maneuver.distanceToTurn} | id=${maneuver.maneuverId}")
                scope.launch { relay.onManeuver(maneuver) }
            }
        }
    }

    /**
     * Puts one raw maneuver code on the wire so a human can read what the
     * cluster actually draws for it.
     *
     * The translation from Mappls maneuver id to cluster code is recovered from
     * the official app, but *what each cluster code renders* is not written
     * anywhere — it lives in the dashboard firmware. Guessing it is what made
     * the turn arrows wrong. This exists so the mapping can be established by
     * observation instead:
     *
     *     adb shell "am broadcast -n <pkg>/<this>      *       -a com.eshwar.rideconnectx.TEST_CODE --ei code 40 --ei dist 200"
     */
    private fun sendRawCode(context: Context, code: Int, distanceMetres: Int) {
        val ble = EntryPointAccessors
            .fromApplication(context.applicationContext, ServiceEntryPoint::class.java)
            .bleRepository()

        val clock = java.text.SimpleDateFormat("hhmma", java.util.Locale.US)
            .format(java.util.Date()).uppercase()

        val packet = com.eshwar.rideconnectx.domain.ProtocolEngine.buildNavigationPacket(
            clusterCode = code,
            distanceMetres = distanceMetres,
            clock = clock,
        )

        scope.launch {
            val sent = ble.sendPacket(packet).first()
            Log.i(TAG, "[SWEEP] code=$code dist=${distanceMetres}m sent=$sent")
        }
    }

    /**
     * One heartbeat with bytes 14 and 15 forced, bypassing ClusterAlerts.
     *
     * Everything else in the frame is real, so polarity is the only variable.
     */
    private fun sendForcedFlags(context: Context, msgFlag: Char, callFlag: Char) {
        val entry = EntryPointAccessors
            .fromApplication(context.applicationContext, ServiceEntryPoint::class.java)
        val ble = entry.bleRepository()
        val phone = entry.phoneStatusProvider()

        val packet = com.eshwar.rideconnectx.domain.ProtocolEngine.buildHeartbeatPacket(
            batteryBucket = phone.batteryBucket(),
            isCharging = phone.isCharging(),
            signalBars = phone.signalBars(),
            clockHHmmss = phone.clockHHmmss(),
            notificationPending = false,
            missedCallPending = false,
        )
        packet[14] = msgFlag.code.toByte()
        packet[15] = callFlag.code.toByte()
        packet[28] = com.eshwar.rideconnectx.domain.ProtocolEngine.calculateChecksum(packet)

        scope.launch {
            val sent = ble.sendPacket(packet).first()
            Log.i(TAG, "[FLAGS] msg=$msgFlag call=$callFlag sent=$sent")
        }
    }
}
