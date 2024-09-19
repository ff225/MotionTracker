package com.pedometers.motiontracker.sensor

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor

data class AccelerometerSensor(
    val context: Context,
) : AndroidSensor(
    context,
    PackageManager.FEATURE_SENSOR_ACCELEROMETER,
    Sensor.TYPE_ACCELEROMETER
)

data class GyroscopeSensor(
    val context: Context,
) : AndroidSensor(
    context,
    PackageManager.FEATURE_SENSOR_GYROSCOPE,
    Sensor.TYPE_GYROSCOPE
)

data class MagnetometerSensor(
    val context: Context,
) : AndroidSensor(
    context,
    PackageManager.FEATURE_SENSOR_COMPASS,
    Sensor.TYPE_MAGNETIC_FIELD
)