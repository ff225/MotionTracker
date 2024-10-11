package com.pedometers.motiontracker

import android.Manifest
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedometers.motiontracker.bluetooth.AndroidBluetoothController
import com.pedometers.motiontracker.bluetooth.BluetoothDeviceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject


@HiltViewModel
class MovesenseViewModel @Inject constructor(
    private val bluetoothController: AndroidBluetoothController,
) : ViewModel() {
    // MutableStateFlow to manage Bluetooth UI state
    private val _state = MutableStateFlow(BluetoothUiState())

    /**
     * Publicly exposed StateFlow of the Bluetooth UI state.
     * Combines the state from Bluetooth controller and internal state to provide a complete UI state.
     */
    val state = combine(
        bluetoothController.scannedDevices,
        _state
    ) { scannedDevices, state ->
        state.copy(
            scannedDevices = scannedDevices,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        _state.value
    )

    /**
     * Starts Bluetooth device scanning.
     */
    fun startScan() = bluetoothController.startDiscovery()

    /**
     * Stops Bluetooth device scanning.
     */
    fun stopScan() = bluetoothController.stopDiscovery()

    /**
     * Updates the UI state to reflect whether Bluetooth scanning is in progress.
     *
     * @param isScanning Boolean indicating whether Bluetooth scanning is in progress.
     */
    fun updateUi(isScanning: Boolean = false, isConnected: Boolean = false) {
        _state.update {
            it.copy(isScanning = isScanning, isConnected = isConnected)
        }
    }

    /**
     * Checks whether the app has necessary Bluetooth permissions.
     *
     * @return True if the app has necessary Bluetooth permissions, false otherwise.
     */
    fun hasPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        bluetoothController.hasPermission(Manifest.permission.BLUETOOTH_SCAN)
    } else {
        bluetoothController.hasPermission(Manifest.permission.BLUETOOTH) && bluetoothController.hasPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) && bluetoothController.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }


}

/**
 * Data class representing the UI state of Bluetooth device scanning.
 *
 * @param scannedDevices List of scanned Bluetooth devices.
 * @param isScanning Boolean indicating whether Bluetooth scanning is in progress.
 */
data class BluetoothUiState(
    val scannedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val isScanning: Boolean = false,
    val isConnected: Boolean = false

)