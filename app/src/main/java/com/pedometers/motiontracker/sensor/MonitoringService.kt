package com.pedometers.motiontracker.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.pedometers.motiontracker.R
import com.pedometers.motiontracker.SendToFirebaseWorker
import com.pedometers.motiontracker.SensorModule
import com.pedometers.motiontracker.data.InfoDataClass
import com.pedometers.motiontracker.data.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.UUID

class MonitoringService : Service() {


    private val workManager = WorkManager.getInstance(application)

    private val accelerometer: MeasurableSensor by lazy {
        SensorModule.provideAccelerometerSensor(application)
    }
    private val gyroscope: MeasurableSensor by lazy {
        SensorModule.provideGyroscopeSensor(application)
    }
    private val magnetometerSensor: MeasurableSensor by lazy {
        SensorModule.provideMagnetometerSensor(application)
    }
    private lateinit var _accelerometerSensorValue: MutableStateFlow<List<Float>>
    private lateinit var _gyroscopeSensorValue: MutableStateFlow<List<Float>>
    private lateinit var _magnetometerSensorValue: MutableStateFlow<List<Float>>

    private val _combinedSensorValues =
        MutableLiveData<Triple<List<Float>, List<Float>, List<Float>>>()

    private var fileWriter: FileWriter? = null

    private var file: File? = null

    private val CHANNEL_ID = "MonitoringServiceChannel"

    override fun onCreate() {
        Log.d("MonitoringService", "Initializing MonitoringService")
        _accelerometerSensorValue = MutableStateFlow(listOf())
        _gyroscopeSensorValue = MutableStateFlow(listOf())
        _magnetometerSensorValue = MutableStateFlow(listOf())
        _combinedSensorValues.value = Triple(listOf(), listOf(), listOf())

        accelerometer.startListening()
        gyroscope.startListening()
        magnetometerSensor.startListening()

        createNotificationChannel()
    }

    private fun createNotificationChannel() {

        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Monitoring Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val infoUser = InfoDataClass(
            intent.getStringExtra("sex") ?: "Male",
            intent.getIntExtra("age", 0),
            intent.getIntExtra("height", 0),
            intent.getIntExtra("weight", 0),
            intent.getStringExtra("position") ?: Position.FOREARM.name
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitoring in progress")
            .setContentText("Collecting sensor data...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        // Imposta la scrittura del file nel percorso dei documenti
        file = File(
            getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "${UUID.randomUUID()}_${infoUser.position}_${infoUser.age}_${infoUser.sex}.csv"
        )

        // Prova ad aprire il file per la scrittura (append)
        try {
            fileWriter = FileWriter(file, true)
            fileWriter?.append("Timestamp, AccelerometerX, AccelerometerY, AccelerometerZ, GyroscopeX, GyroscopeY, GyroscopeZ, MagnetometerX, MagnetometerY, MagnetometerZ, Sex, Age, Height, Weight, Position\n")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        startForeground(1, notification)
        // Imposta i listener per i sensori
        magnetometerSensor.setOnSensorValuesChangedListener {
            Log.d("MonitoringService", "Sensor magnetometer value changed: $it")
            _magnetometerSensorValue.value = it
        }
        gyroscope.setOnSensorValuesChangedListener {
            Log.d("MonitoringService", "Sensor gyroscope value changed: $it")
            _gyroscopeSensorValue.value = it
        }
        accelerometer.setOnSensorValuesChangedListener {
            Log.d("MonitoringService", "Sensor accelerometer value changed: $it")
            _accelerometerSensorValue.value = it
        }

        // Combinare i dati dei sensori e scrivere nel file
        CoroutineScope(Dispatchers.Main).launch {
            _accelerometerSensorValue.combine(_gyroscopeSensorValue) { acc, gyro ->
                Pair(acc, gyro)
            }.zip(_magnetometerSensorValue) { accGyro, mag ->
                Triple(accGyro.first, accGyro.second, mag)
            }.collect {
                Log.d("MonitoringService", "Combined flow: $it")
                _combinedSensorValues.value = it

                // Scrivi i dati combinati nel file
                withContext(Dispatchers.IO) {
                    try {
                        if (it.first.isNotEmpty() && it.second.isNotEmpty() && it.third.isNotEmpty()) {
                            fileWriter?.append(
                                "${System.currentTimeMillis()},${it.first[0]}, ${it.first[1]}, ${it.first[2]}, " +
                                        "${it.second[0]}, ${it.second[1]}, ${it.second[2]}, " +
                                        "${it.third[0]}, ${it.third[1]}, ${it.third[2]},"
                                        + "${infoUser.sex}, ${infoUser.age}, ${infoUser.height}, ${infoUser.weight}, ${infoUser.position} \n"
                            )
                            fileWriter?.flush()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // Ferma i sensori
        accelerometer.stopListening()
        gyroscope.stopListening()
        magnetometerSensor.stopListening()

        // SEND to Firebase

        CoroutineScope(Dispatchers.IO).launch {
            try {
                fileWriter?.close()
                val workRequest = OneTimeWorkRequestBuilder<SendToFirebaseWorker>()
                    .setInputData(workDataOf("file" to file!!.absolutePath))
                    .setConstraints(
                        Constraints(
                            requiredNetworkType = androidx.work.NetworkType.CONNECTED
                        )
                    )
                    .build()

                workManager.beginUniqueWork(
                    "SendToFirebaseWorker",
                    ExistingWorkPolicy.APPEND,
                    workRequest
                ).enqueue()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        stopSelf()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}
