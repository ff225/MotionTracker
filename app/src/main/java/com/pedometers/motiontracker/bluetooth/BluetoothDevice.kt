package com.pedometers.motiontracker.bluetooth

import android.annotation.SuppressLint

/**
 * Type alias for BluetoothDeviceInfo representing a Bluetooth device.
 */
typealias BluetoothDeviceInfo = BluetoothDevice

/**
 * Data class representing Bluetooth device information.
 *
 * @property name The name of the Bluetooth device.
 * @property address The MAC address of the Bluetooth device.
 */
data class BluetoothDevice(
    val name: String?,
    val address: String
)

/**
 * Extension function to convert android.bluetooth.BluetoothDevice to BluetoothDeviceInfo.
 *
 * @return BluetoothDeviceInfo instance containing the name and address of the Bluetooth device.
 */

@SuppressLint("MissingPermission")
fun android.bluetooth.BluetoothDevice.toBluetoothDeviceInfo(): BluetoothDeviceInfo {
    return BluetoothDevice(
        name = if (name == null) "NoName" else name,
        address = address
    )

}
