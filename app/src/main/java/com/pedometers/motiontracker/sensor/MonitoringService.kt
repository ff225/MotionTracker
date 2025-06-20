package com.pedometers.motiontracker.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.movesense.mds.Mds
import com.movesense.mds.MdsException
import com.movesense.mds.MdsNotificationListener
import com.movesense.mds.MdsSubscription
import com.pedometers.motiontracker.R
import com.pedometers.motiontracker.SendToFirebaseWorker
import com.pedometers.motiontracker.data.InfoDataClass
import com.pedometers.motiontracker.data.Movesense
import com.pedometers.motiontracker.data.Position
import com.pedometers.motiontracker.di.SensorModule
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : Service() {


    private val workManager = WorkManager.getInstance(application)

    private var mds: Mds? = null

    private var mdsSubAcc: MdsSubscription? = null

    @SensorModule.Accelerometer
    @Inject
    lateinit var accelerometer: MeasurableSensor

    @SensorModule.Gyroscope
    @Inject
    lateinit var gyroscope: MeasurableSensor

    @SensorModule.Magnetometer
    @Inject
    lateinit var magnetometerSensor: MeasurableSensor

    private lateinit var _accelerometerSensorValue: MutableList<List<Float>>
    private lateinit var _gyroscopeSensorValue: MutableList<List<Float>>
    private lateinit var _magnetometerSensorValue: MutableList<List<Float>>
    private lateinit var _timeStamp: MutableList<Long>

    private lateinit var _accelerometerSensorValueMovesense: MutableList<List<String>>
    private lateinit var _gyroscopeSensorValueMovesense: MutableList<List<String>>
    private lateinit var _magnetometerSensorValueMovesense: MutableList<List<String>>
    private lateinit var _timeStampMovesense: MutableList<Long>
    private lateinit var infoUser: InfoDataClass

    private var timestamp: Long = 0L


    private var fileWriter: FileWriter? = null

    private var file: File? = null
    private var fileMovesense: File? = null

    private val CHANNEL_ID = "MonitoringServiceChannel"

    override fun onCreate() {
        Log.d("MonitoringService", "Initializing MonitoringService")
        super.onCreate()
        _accelerometerSensorValue = mutableListOf(listOf())
        _gyroscopeSensorValue = mutableListOf(listOf())
        _magnetometerSensorValue = mutableListOf(listOf())
        _timeStamp = mutableListOf()

        _accelerometerSensorValueMovesense = mutableListOf(listOf())
        _gyroscopeSensorValueMovesense = mutableListOf(listOf())
        _magnetometerSensorValueMovesense = mutableListOf(listOf())
        _timeStampMovesense = mutableListOf()


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

        infoUser = InfoDataClass(
            intent.getStringExtra("sex") ?: "Male",
            intent.getIntExtra("age", 0),
            intent.getIntExtra("height", 0),
            intent.getIntExtra("weight", 0),
            intent.getStringExtra("position") ?: Position.SHOULDER.name,
            intent.getStringExtra("activityType") ?: "Slow walking",
            intent.getStringExtra("uuid") ?: ""
        )

        timestamp = intent.getLongExtra("timestamp", 0L)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitoring in progress")
            .setContentText("Collecting sensor data...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()


        startForeground(1, notification)
        // Imposta i listener per i sensori
        magnetometerSensor.setOnSensorValuesChangedListener {
            // Log.d("MonitoringService", "Sensor magnetometer value changed: $it")
            _magnetometerSensorValue.add(it)
        }
        gyroscope.setOnSensorValuesChangedListener {
            // Log.d("MonitoringService", "Sensor gyroscope value changed: $it")
            _gyroscopeSensorValue.add(it)
        }
        accelerometer.setOnSensorValuesChangedListener {
            //Log.d("MonitoringService", "Sensor accelerometer value changed: $it")
            _timeStamp.add(System.currentTimeMillis())
            _accelerometerSensorValue.add(it)
        }


        mdsSubAcc?.unsubscribe()

        if (Movesense.name != null) {
            mds = Mds.builder().build(this)
            mdsSubAcc = mds!!.subscribe(Mds.URI_EVENTLISTENER,
                """{"Uri": "${Movesense.name?.removePrefix("Movesense ")}/Meas/IMU9/${Movesense.frequencyHertz.value}"}""",
                object : MdsNotificationListener {
                    override fun onNotification(p0: String?) {
                        Log.d("MonitoringService", "$p0")
                        _accelerometerSensorValueMovesense.add(extractArray(p0!!, "ArrayAcc"))
                        _gyroscopeSensorValueMovesense.add(extractArray(p0, "ArrayGyro"))
                        _magnetometerSensorValueMovesense.add(extractArray(p0, "ArrayMagn"))
                        _timeStampMovesense.add(System.currentTimeMillis())
                    }

                    override fun onError(p0: MdsException?) {
                        println("Error: $p0")
                    }

                })

        }
        // Combinare i dati dei sensori e scrivere nel file
        /*
        CoroutineScope(Dispatchers.Main).launch {
            _accelerometerSensorValue.
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
         */

        return START_NOT_STICKY
    }


    override fun onDestroy() {
        // Ferma i sensori
        accelerometer.stopListening()
        gyroscope.stopListening()
        magnetometerSensor.stopListening()

        mdsSubAcc?.unsubscribe()


        CoroutineScope(Dispatchers.IO).launch {
            try {
                file = File(
                    getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "${infoUser.uuid}_${timestamp}_${infoUser.activityType}_${infoUser.position}_${infoUser.age}_${infoUser.sex}_${Build.MANUFACTURER}_${Build.MODEL}.csv"
                )
                fileWriter = FileWriter(file, true)
                fileWriter?.append("Timestamp, AccelerometerX, AccelerometerY, AccelerometerZ, GyroscopeX, GyroscopeY, GyroscopeZ, MagnetometerX, MagnetometerY, MagnetometerZ, Sex, Age, Height, Weight, Position, Activity\n")

                _timeStamp.forEachIndexed { idx, timestamp ->
                    try {
                        if (_accelerometerSensorValue[idx].isNotEmpty() && _gyroscopeSensorValue[idx].isNotEmpty() && _magnetometerSensorValue[idx].isNotEmpty()) {
                            fileWriter?.append(
                                "$timestamp,${_accelerometerSensorValue[idx][0]}, ${_accelerometerSensorValue[idx][1]}, ${_accelerometerSensorValue[idx][2]}, " +
                                        "${_gyroscopeSensorValue[idx][0]}, ${_gyroscopeSensorValue[idx][1]}, ${_gyroscopeSensorValue[idx][2]}, " +
                                        "${_magnetometerSensorValue[idx][0]}, ${_magnetometerSensorValue[idx][1]}, ${_magnetometerSensorValue[idx][2]},"
                                        + "${infoUser.sex}, ${infoUser.age}, ${infoUser.height}, ${infoUser.weight}, ${infoUser.position}, ${infoUser.activityType} \n"
                            )
                        }
                    } catch (e: IndexOutOfBoundsException) {
                        e.printStackTrace()
                    }
                }
                fileWriter?.close()

                if (Movesense.name != null) {
                    fileMovesense = File(
                        getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                        "${infoUser.uuid}_${timestamp}_${infoUser.activityType}_MOVESENSE_${Movesense.position}_${infoUser.age}_${infoUser.sex}.csv"
                    )

                    fileWriter = FileWriter(fileMovesense, true)
                    fileWriter?.append("Timestamp, AccelerometerX, AccelerometerY, AccelerometerZ, GyroscopeX, GyroscopeY, GyroscopeZ, MagnetometerX, MagnetometerY, MagnetometerZ, Sex, Age, Height, Weight, Position, Activity\n")

                    _timeStampMovesense.forEachIndexed { idx, timestamp ->
                        try {
                            if (_accelerometerSensorValueMovesense[idx].isNotEmpty() && _gyroscopeSensorValueMovesense[idx].isNotEmpty() && _magnetometerSensorValueMovesense[idx].isNotEmpty()) {
                                fileWriter?.append(
                                    "$timestamp,${_accelerometerSensorValueMovesense[idx][0]}, ${_accelerometerSensorValueMovesense[idx][1]}, ${_accelerometerSensorValueMovesense[idx][2]}, " +
                                            "${_gyroscopeSensorValueMovesense[idx][0]}, ${_gyroscopeSensorValueMovesense[idx][1]}, ${_gyroscopeSensorValueMovesense[idx][2]}, " +
                                            "${_magnetometerSensorValueMovesense[idx][0]}, ${_magnetometerSensorValueMovesense[idx][1]}, ${_magnetometerSensorValueMovesense[idx][2]},"
                                            + "${infoUser.sex}, ${infoUser.age}, ${infoUser.height}, ${infoUser.weight}, ${Movesense.position}, ${infoUser.activityType} \n"
                                )
                            }
                        } catch (e: IndexOutOfBoundsException) {
                            e.printStackTrace()
                        }
                    }
                }

            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                fileWriter?.close()
                _accelerometerSensorValue.clear()
                _gyroscopeSensorValue.clear()
                _magnetometerSensorValue.clear()
                _timeStamp.clear()

                _accelerometerSensorValueMovesense.clear()
                _gyroscopeSensorValueMovesense.clear()
                _magnetometerSensorValueMovesense.clear()
                _timeStampMovesense.clear()

            }
            val workRequestSensor = OneTimeWorkRequestBuilder<SendToFirebaseWorker>()
                .setInputData(workDataOf("file" to file!!.absolutePath))
                .setConstraints(
                    Constraints(
                        requiredNetworkType = NetworkType.CONNECTED
                    )
                )
                .build()


            workManager.beginUniqueWork(
                "SendToFirebaseWorker",
                ExistingWorkPolicy.APPEND,
                workRequestSensor
            ).enqueue()

            if (Movesense.name != null) {
                val workRequestMovesense = OneTimeWorkRequestBuilder<SendToFirebaseWorker>()
                    .setInputData(workDataOf("file" to fileMovesense!!.absolutePath))
                    .setConstraints(
                        Constraints(
                            requiredNetworkType = NetworkType.CONNECTED
                        )
                    )
                    .build()

                workManager.beginUniqueWork(
                    "SendToFirebaseWorker",
                    ExistingWorkPolicy.APPEND,
                    workRequestMovesense
                ).enqueue()
            }


        }

        stopSelf()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    fun extractArray(json: String, key: String): List<String> {
        val startIndex = json.indexOf(key)
        if (startIndex == -1) return emptyList()  // Se non trovato, restituisce una lista vuota

        val arrayStart = json.indexOf("[", startIndex)
        val arrayEnd = json.indexOf("]", arrayStart)
        val arrayString = json.substring(arrayStart + 1, arrayEnd)  // Estrarre l'array come stringa

        // Dividere l'array per estrarre i valori x, y, z e concatenarli in una singola lista
        return arrayString.split("},").flatMap { element ->
            val x = extractValue(element, "x")
            val y = extractValue(element, "y")
            val z = extractValue(element, "z")
            listOf(x, y, z)  // Restituisce i valori x, y, z in una lista unica
        }
    }

    // Funzione per estrarre i valori x, y, z da ogni stringa nell'array
    private fun extractValue(element: String, axis: String): String {
        val axisIndex = element.indexOf("\"$axis\":")
        if (axisIndex == -1) return "0.0"

        val start = element.indexOf(":", axisIndex) + 1
        val end = element.indexOf(",", start).takeIf { it != -1 } ?: element.length
        return element.substring(start, end).replace("}", "").trim()
    }
}