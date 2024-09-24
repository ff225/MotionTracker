package com.pedometers.motiontracker

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedometers.motiontracker.data.ActivityType
import com.pedometers.motiontracker.data.Position
import com.pedometers.motiontracker.data.Sex
import com.pedometers.motiontracker.sensor.MeasurableSensor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @SensorModule.Accelerometer private val accelerometer: MeasurableSensor,
    @SensorModule.Gyroscope private val gyroscope: MeasurableSensor,
    @SensorModule.Magnetometer private val magnetometerSensor: MeasurableSensor
) : ViewModel() {


    private lateinit var _accelerometerSensorValue: MutableStateFlow<List<Float>>
    private lateinit var _gyroscopeSensorValue: MutableStateFlow<List<Float>>
    private lateinit var _magnetometerSensorValue: MutableStateFlow<List<Float>>

    private val _combinedSensorValues =
        MutableLiveData<Triple<List<Float>, List<Float>, List<Float>>>()
    val combinedSensorValues: LiveData<Triple<List<Float>, List<Float>, List<Float>>> get() = _combinedSensorValues


    init {
        Log.d("MainViewModel", "Initializing MainViewModel")
        resetMutableStateFlow()
    }

    private fun resetMutableStateFlow() {
        _accelerometerSensorValue = MutableStateFlow(listOf())
        _gyroscopeSensorValue = MutableStateFlow(listOf())
        _magnetometerSensorValue = MutableStateFlow(listOf())
        _combinedSensorValues.value = Triple(listOf(), listOf(), listOf())
    }


    fun startListening() {
        magnetometerSensor.setOnSensorValuesChangedListener {
            Log.d(
                "MainViewModel",
                "Sensor magnetometer value changed: $it"
            )
            _magnetometerSensorValue.value = it
        }
        gyroscope.setOnSensorValuesChangedListener {
            Log.d("MainViewModel", "Sensor gyroscope value changed: $it")
            _gyroscopeSensorValue.value = it
        }
        accelerometer.setOnSensorValuesChangedListener {
            Log.d("MainViewModel", "Sensor accelerometer value changed: $it")
            _accelerometerSensorValue.value = it
        }

        accelerometer.startListening()
        gyroscope.startListening()
        magnetometerSensor.startListening()
        combineFlow()
        Log.d("MainViewModel", "Listening to sensor")
    }

    private fun combineFlow() {
        viewModelScope.launch {
            _accelerometerSensorValue.combine(_gyroscopeSensorValue) { acc, gyro ->
                Pair(acc, gyro)
            }.zip(_magnetometerSensorValue) { accGyro, mag ->
                Triple(accGyro.first, accGyro.second, mag)
            }.collect {
                Log.d("MainViewModel", "Combined flow: $it")
                _combinedSensorValues.value = it
            }
        }
    }

    fun stopListening() {
        accelerometer.stopListening()
        gyroscope.stopListening()
        magnetometerSensor.stopListening()
        resetMutableStateFlow()
        Log.d("MainViewModel", "Stopped listening to sensor")
    }


    fun updateUiState(uiState: UiState) {
        this.uiState =
            uiState.copy(isValid = uiState.ageError == null && uiState.heightError == null && uiState.weightError == null)
    }


    var uiState by mutableStateOf(UiState())
        private set

}

data class UiState(
    var sex: Sex = Sex.MALE,
    var age: String = "",
    var height: String = "",
    var weight: String = "",
    var position: Position = Position.FOREARM,
    var activityType: ActivityType = ActivityType.SLOW_WALKING,
    val accelerometerValues: String = "",
    val gyroscopeValues: String = "",
    val magnetometerValues: String = "",
    var ageError: String? = null,
    var heightError: String? = null,
    var weightError: String? = null,
    var isValid: Boolean = ageError != null && heightError != null && weightError != null,

    )