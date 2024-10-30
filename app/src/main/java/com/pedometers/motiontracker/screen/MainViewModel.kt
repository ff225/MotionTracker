package com.pedometers.motiontracker.screen

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pedometers.motiontracker.data.ActivityType
import com.pedometers.motiontracker.data.Position
import com.pedometers.motiontracker.data.Sex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {


    private lateinit var _accelerometerSensorValue: MutableStateFlow<List<Float>>
    private lateinit var _gyroscopeSensorValue: MutableStateFlow<List<Float>>
    private lateinit var _magnetometerSensorValue: MutableStateFlow<List<Float>>

    private val _combinedSensorValues =
        MutableLiveData<Triple<List<Float>, List<Float>, List<Float>>>()


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

    /*
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
    */

    fun updateUiState(uiState: UiState) {
        this.uiState = uiState.copy(
            isValid = uiState.age.isNotEmpty() && uiState.height.isNotEmpty() && uiState.weight.isNotEmpty()
        )
    }


    var uiState by mutableStateOf(UiState())
        private set

}

data class UiState(
    var sex: Sex = Sex.MALE,
    var age: String = "",
    var height: String = "",
    var weight: String = "",
    var position: Position = Position.POCKET,
    var activityType: ActivityType = ActivityType.PLAIN_WALKING,
    val accelerometerValues: String = "",
    val gyroscopeValues: String = "",
    val magnetometerValues: String = "",
    var ageError: String? = null,
    var heightError: String? = null,
    var weightError: String? = null,
    var isValid: Boolean = false

)