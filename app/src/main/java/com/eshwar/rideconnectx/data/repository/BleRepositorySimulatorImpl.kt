package com.eshwar.rideconnectx.data.repository

import com.eshwar.rideconnectx.domain.model.BleDevice
import com.eshwar.rideconnectx.domain.model.OnboardingStatus
import com.eshwar.rideconnectx.domain.model.ScooterTelemetry
import com.eshwar.rideconnectx.domain.repository.BlePacket
import com.eshwar.rideconnectx.domain.repository.BleRepository
import com.eshwar.rideconnectx.domain.repository.ConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleRepositorySimulatorImpl @Inject constructor() : BleRepository {

    private val _onboardingStatus = MutableStateFlow(OnboardingStatus.NOT_STARTED)
    override val onboardingStatus: Flow<OnboardingStatus> = _onboardingStatus.asStateFlow()

    private val _telemetry = MutableStateFlow(ScooterTelemetry())
    override val telemetry: Flow<ScooterTelemetry> = _telemetry.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override fun reconnectLastDevice() = Unit

    override fun shutdown() {
        _connectionState.value = ConnectionState.Idle
    }

    override fun scanDevices(): Flow<List<BleDevice>> = flow {
        val mockDevices = listOf(
            BleDevice("SAS210217219 (virtual)", "00:11:22:33:44:55", -55),
        )
        while (true) {
            emit(mockDevices)
            delay(2000)
        }
    }

    override fun stopScan() {}

    override fun connect(address: String): Flow<ConnectionState> = flow {
        emit(ConnectionState.Connecting(address, "Simulated Scooter"))
        delay(1500)
        emit(ConnectionState.Connected(address, "Simulated Scooter"))
        // Example readings so the dashboard has something to show.
        _telemetry.value = ScooterTelemetry(
            odometerKm = 12846, tripAKm = 42.6f, tripBKm = 318.2f, fuelSegments = 4, isValid = true,
        )
        // A short simulated ride: Trip A climbs 1.5 km over ~15 s, so ride
        // recording (Statistics) can be checked on the emulator.
        repeat(15) {
            delay(1000)
            val t = _telemetry.value
            _telemetry.value = t.copy(tripAKm = t.tripAKm + 0.1f, tripBKm = t.tripBKm + 0.1f)
        }
    }.onEach { _connectionState.value = it }

    override fun disconnect() {
        _telemetry.value = ScooterTelemetry()
        _connectionState.value = ConnectionState.Disconnected("Disconnected")
    }

    override fun sendPacket(packet: ByteArray): Flow<Boolean> = flow {
        emit(true)
    }

    override fun observeNotifications(): Flow<BlePacket> = flow {
        // Telemetry parsing suspended
    }

    override suspend fun updateOnboardingStatus(status: OnboardingStatus) {
        _onboardingStatus.value = status
    }
}
