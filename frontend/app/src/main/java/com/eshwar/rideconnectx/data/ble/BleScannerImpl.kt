package com.eshwar.rideconnectx.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.eshwar.rideconnectx.domain.model.BleDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import com.eshwar.rideconnectx.data.local.SessionDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@Singleton
class BleScannerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDataStore: SessionDataStore
) {
    private val TAG = "RCX-BLE"
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bleScanner = bluetoothManager.adapter?.bluetoothLeScanner
    
    private val scannerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun scanDevices(): Flow<List<BleDevice>> = callbackFlow {
        Log.d(TAG, "Scan started")
        val discoveredDevices = mutableMapOf<String, BleDevice>()
        val savedAddress = sessionDataStore.lastDeviceAddress.first()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val address = result.device.address
                val name = result.device.name
                
                if (ScooterCandidateFilter.isSuzukiScooterCandidate(name, address, savedAddress)) {
                    val device = BleDevice(
                        name = name,
                        address = address,
                        rssi = result.rssi,
                        lastSeen = System.currentTimeMillis()
                    )
                    discoveredDevices[address] = device
                    trySend(discoveredDevices.values.toList().sortedByDescending { it.rssi })
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed: $errorCode")
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bleScanner?.startScan(null, settings, callback)
        
        // 30 Second Timeout
        val timeoutJob = launch {
            delay(30000)
            Log.d(TAG, "Scan timeout fired")
            close()
        }

        awaitClose {
            Log.d(TAG, "Scan stopped")
            timeoutJob.cancel()
            bleScanner?.stopScan(callback)
        }
    }

    fun stopScan() {
        Log.d(TAG, "Manual scan stop requested")
    }
}
