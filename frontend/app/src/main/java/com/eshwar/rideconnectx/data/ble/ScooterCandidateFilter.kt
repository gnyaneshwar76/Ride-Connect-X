package com.eshwar.rideconnectx.data.ble

import android.util.Log

object ScooterCandidateFilter {
    private const val TAG = "RCX-BLE"

    fun isSuzukiScooterCandidate(name: String?, address: String, savedAddress: String?): Boolean {
        if (address.equals(savedAddress, ignoreCase = true)) {
            Log.d(TAG, "Candidate found (Saved Address Match): $address")
            return true
        }

        val deviceName = name ?: return false
        val isCandidate = deviceName.startsWith("SAS", ignoreCase = true) || 
                          deviceName.contains("SUZUKI", ignoreCase = true)
        
        if (isCandidate) {
            Log.d(TAG, "Candidate scooter found: Name=$deviceName, Address=$address")
        } else {
            // Logging filtered out devices for debug transparency
            // Log.v(TAG, "Non-candidate device filtered out: $deviceName ($address)")
        }
        
        return isCandidate
    }
}
