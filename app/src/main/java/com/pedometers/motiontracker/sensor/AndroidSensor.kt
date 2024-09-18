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

    override val doesSensorExist: Boolean
        get() = context.packageManager.hasSystemFeature(sensorFeature)

    override fun startListening() {

        Log.d("AndroidSensor", "Checking if sensor exists: $doesSensorExist")

        if (doesSensorExist.not()) return

        Log.d("AndroidSensor", "Starting to listen to sensor: $sensorType")

        sensorManager.registerListener(
            sensorEventListener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun stopListening() {
        if (doesSensorExist.not()) return

        sensorManager.unregisterListener(sensorEventListener)
    }

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (doesSensorExist.not()) return
            if (event.sensor.type == sensorType)
                onSensorValuesChanged?.invoke(event.values.toList())
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Do nothing
        }
    }

}