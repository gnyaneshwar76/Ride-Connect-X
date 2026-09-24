package com.eshwar.rideconnectx.core.util

import android.Manifest
import android.os.Build

object BlePermissionHandler {
    /**
     * Returns a comprehensive list of all permissions required for 
     * BLE scanning, connection, and foreground service status.
     */
    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        
        // 1. Bluetooth & Scan Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires specific BLE permissions
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Legacy Bluetooth permissions
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        // 2. Location Permission (Mandatory for BLE on most Android versions)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        // 3. Notification Permission (Mandatory for Foreground Services on Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.toTypedArray()
    }
}
