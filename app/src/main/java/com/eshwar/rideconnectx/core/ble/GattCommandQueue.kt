package com.eshwar.rideconnectx.core.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

sealed class GattCommand {
    @SuppressLint("MissingPermission")
    abstract fun execute(gatt: BluetoothGatt): Boolean

    class DiscoverServices : GattCommand() {
        @SuppressLint("MissingPermission")
        override fun execute(gatt: BluetoothGatt): Boolean = gatt.discoverServices()
    }

    class RequestMtu(val mtu: Int) : GattCommand() {
        @SuppressLint("MissingPermission")
        override fun execute(gatt: BluetoothGatt): Boolean = gatt.requestMtu(mtu)
    }

    class WriteDescriptor(val descriptor: BluetoothGattDescriptor, val value: ByteArray) : GattCommand() {
        @SuppressLint("MissingPermission")
        override fun execute(gatt: BluetoothGatt): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = value
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    class WriteCharacteristic(
        val characteristic: BluetoothGattCharacteristic,
        val value: ByteArray,
        val writeType: Int
    ) : GattCommand() {
        @SuppressLint("MissingPermission")
        override fun execute(gatt: BluetoothGatt): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(characteristic, value, writeType) == BluetoothStatusCodes.SUCCESS
            } else {
                characteristic.writeType = writeType
                @Suppress("DEPRECATION")
                characteristic.value = value
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            }
        }
    }
}

class GattCommandQueue {
    private val queue = ConcurrentLinkedQueue<GattCommand>()
    private var isProcessing = false
    private var startedAt = 0L

    private companion object {
        /** A completion callback that never arrives must not freeze the queue. */
        const val STUCK_MS = 1_000L
        const val NAVIGATION = 0x31.toByte()
    }

    @Synchronized
    fun addCommand(command: GattCommand, gatt: BluetoothGatt?) {
        if (gatt == null) {
            Log.e("GattCommandQueue", "GATT is null, clearing queue.")
            queue.clear()
            isProcessing = false
            return
        }
        // A queued navigation frame is out of date the moment a newer one exists.
        if (command is GattCommand.WriteCharacteristic && command.value.getOrNull(1) == NAVIGATION) {
            queue.removeIf { it is GattCommand.WriteCharacteristic && it.value.getOrNull(1) == NAVIGATION }
        }
        queue.add(command)
        if (isProcessing && System.currentTimeMillis() - startedAt > STUCK_MS) {
            Log.w("GattCommandQueue", "Previous command never completed - moving on")
            isProcessing = false
        }
        if (!isProcessing) {
            processNext(gatt)
        }
    }

    @Synchronized
    fun onCommandCompleted(gatt: BluetoothGatt?) {
        isProcessing = false
        if (gatt != null) {
            processNext(gatt)
        }
    }

    private fun processNext(gatt: BluetoothGatt) {
        val command = queue.poll()
        if (command != null) {
            isProcessing = true
            startedAt = System.currentTimeMillis()
            Log.d("GattCommandQueue", "Executing command: ${command.javaClass.simpleName}")
            val success = try {
                command.execute(gatt)
            } catch (e: Exception) {
                Log.e("GattCommandQueue", "Exception during command execution", e)
                false
            }
            if (!success) {
                Log.e("GattCommandQueue", "Failed to execute command: ${command.javaClass.simpleName}")
                onCommandCompleted(gatt)
            }
        } else {
            Log.d("GattCommandQueue", "Queue is empty.")
        }
    }

    @Synchronized
    fun clear() {
        queue.clear()
        isProcessing = false
    }
}
