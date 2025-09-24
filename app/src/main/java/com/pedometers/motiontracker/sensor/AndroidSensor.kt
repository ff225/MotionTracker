package com.pedometers.motiontracker.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

abstract class AndroidSensor(
    private val context: Context,
    private val sensorFeature: String,
    sensorType: Int
) : MeasurableSensor(sensorType) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(sensorType)
    
    // Flag to prevent memory leaks
    private var isDestroyed = false

    override val doesSensorExist: Boolean
        get() = !isDestroyed && context.packageManager.hasSystemFeature(sensorFeature)

    override fun startListening() {
        if (isDestroyed) {
            Log.w("AndroidSensor", "Cannot start listening - sensor is destroyed")
            return
        }

        Log.d("AndroidSensor", "Checking if sensor exists: $doesSensorExist")

        if (doesSensorExist.not()) return

        Log.d("AndroidSensor", "Starting to listen to sensor: $sensorType")

        sensorManager.registerListener(
            sensorEventListener,
            sensor,
            SensorManager.SENSOR_DELAY_GAME // ~50Hz instead of ~5Hz for better data quality
        )
    }

    override fun stopListening() {
        if (isDestroyed) return
        
        if (doesSensorExist.not()) return

        try {
            sensorManager.unregisterListener(sensorEventListener)
            Log.d("AndroidSensor", "Stopped listening to sensor: $sensorType")
        } catch (e: Exception) {
            Log.e("AndroidSensor", "Error stopping sensor listener: ${e.message}")
        }
    }
    
    /**
     * Call this to clean up resources and prevent memory leaks
     */
    fun destroy() {
        stopListening()
        isDestroyed = true
        onSensorValuesChanged = null
        Log.d("AndroidSensor", "Sensor destroyed: $sensorType")
    }

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (isDestroyed) return
            if (doesSensorExist.not()) return
            if (event.sensor.type == sensorType) {
                try {
                    onSensorValuesChanged?.invoke(event.values.toList())
                } catch (e: Exception) {
                    Log.e("AndroidSensor", "Error processing sensor data: ${e.message}")
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            if (isDestroyed) return
            Log.d("AndroidSensor", "Sensor accuracy changed: $accuracy for sensor: $sensorType")
        }
    }

}