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

    // Synchronized sensor data collection
    private data class SensorSample(
        val timestamp: Long,
        val accelerometer: List<Float>? = null,
        val gyroscope: List<Float>? = null,
        val magnetometer: List<Float>? = null
    )
    
    private val sensorSamples = mutableListOf<SensorSample>()
    private var lastSampleTime = 0L
    private val sampleInterval = 20L // ~50Hz (20ms between samples)
    
    // Latest sensor values for interpolation
    private data class LatestSensorValues(
        var accelerometer: List<Float>? = null,
        var gyroscope: List<Float>? = null,
        var magnetometer: List<Float>? = null,
        var lastAccUpdate: Long = 0L,
        var lastGyroUpdate: Long = 0L,
        var lastMagUpdate: Long = 0L
    )
    
    private val latestValues = LatestSensorValues()

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
        
        // Initialize lists properly without empty elements
        _accelerometerSensorValue = mutableListOf()
        _gyroscopeSensorValue = mutableListOf()
        _magnetometerSensorValue = mutableListOf()
        _timeStamp = mutableListOf()

        _accelerometerSensorValueMovesense = mutableListOf()
        _gyroscopeSensorValueMovesense = mutableListOf()
        _magnetometerSensorValueMovesense = mutableListOf()
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
        
        // Improved synchronized sensor data collection system
        // Set up sensor listeners with better synchronization
        accelerometer.setOnSensorValuesChangedListener { values ->
            synchronized(this) {
                latestValues.accelerometer = values
                latestValues.lastAccUpdate = System.currentTimeMillis()
                checkAndStoreSampleImproved()
            }
        }
        
        gyroscope.setOnSensorValuesChangedListener { values ->
            synchronized(this) {
                latestValues.gyroscope = values
                latestValues.lastGyroUpdate = System.currentTimeMillis()
                checkAndStoreSampleImproved()
            }
        }
        
        magnetometerSensor.setOnSensorValuesChangedListener { values ->
            synchronized(this) {
                latestValues.magnetometer = values
                latestValues.lastMagUpdate = System.currentTimeMillis()
                checkAndStoreSampleImproved()
            }
        }

        mdsSubAcc?.unsubscribe()

        if (Movesense.name != null) {
            mds = Mds.builder().build(this)
            mdsSubAcc = mds!!.subscribe(Mds.URI_EVENTLISTENER,
                """{"Uri": "${Movesense.name?.removePrefix("Movesense ")}/Meas/IMU9/${Movesense.frequencyHertz.value}"}""",
                object : MdsNotificationListener {
                    override fun onNotification(p0: String?) {
                        Log.d("MonitoringService", "Movesense data: $p0")
                        
                        try {
                            val accData = extractArray(p0!!, "ArrayAcc")
                            val gyroData = extractArray(p0, "ArrayGyro")
                            val magData = extractArray(p0, "ArrayMagn")
                            
                            // Add data with proper error checking
                            if (accData.isNotEmpty() && gyroData.isNotEmpty() && magData.isNotEmpty()) {
                                synchronized(this@MonitoringService) {
                                    _accelerometerSensorValueMovesense.add(accData)
                                    _gyroscopeSensorValueMovesense.add(gyroData)
                                    _magnetometerSensorValueMovesense.add(magData)
                                    _timeStampMovesense.add(System.currentTimeMillis())
                                }
                                Log.d("MonitoringService", "Stored Movesense sample: acc=${accData.size}, gyro=${gyroData.size}, mag=${magData.size}")
                            } else {
                                Log.w("MonitoringService", "Incomplete Movesense data - skipping")
                            }
                        } catch (e: Exception) {
                            Log.e("MonitoringService", "Error processing Movesense data: ${e.message}")
                        }
                    }

                    override fun onError(p0: MdsException?) {
                        Log.e("MonitoringService", "Movesense error: $p0")
                    }
                })

        }

        return START_NOT_STICKY
    }

    /**
     * Improved synchronization - stores samples at regular intervals using latest available data
     */
    private fun checkAndStoreSampleImproved() {
        val currentTime = System.currentTimeMillis()
        
        // Store sample at regular intervals, using latest available sensor data
        if (currentTime - lastSampleTime >= sampleInterval) {
            
            // Use latest available data from each sensor (even if slightly old)
            val acc = latestValues.accelerometer
            val gyro = latestValues.gyroscope
            val mag = latestValues.magnetometer
            
            // Only store if we have at least SOME data (prefer partial data over no data)
            if (acc != null || gyro != null || mag != null) {
                
                // Use previous values if current sensor hasn't updated recently
                val finalAcc = acc ?: (if (_accelerometerSensorValue.isNotEmpty()) _accelerometerSensorValue.last() else listOf(0f, 0f, 0f))
                val finalGyro = gyro ?: (if (_gyroscopeSensorValue.isNotEmpty()) _gyroscopeSensorValue.last() else listOf(0f, 0f, 0f))
                val finalMag = mag ?: (if (_magnetometerSensorValue.isNotEmpty()) _magnetometerSensorValue.last() else listOf(0f, 0f, 0f))
                
                // Store synchronized data
                _accelerometerSensorValue.add(finalAcc)
                _gyroscopeSensorValue.add(finalGyro)
                _magnetometerSensorValue.add(finalMag)
                _timeStamp.add(currentTime)
                
                lastSampleTime = currentTime
                
                Log.d("MonitoringService", "Stored sample at $currentTime (acc=${acc != null}, gyro=${gyro != null}, mag=${mag != null})")
            }
        }
    }


    override fun onDestroy() {
        Log.d("MonitoringService", "Service being destroyed - cleaning up resources")
        
        // Stop and destroy sensors to prevent memory leaks
        try {
            accelerometer.stopListening()
            gyroscope.stopListening()
            magnetometerSensor.stopListening()
            
            // If sensors are AndroidSensor instances, call destroy method
            if (accelerometer is com.pedometers.motiontracker.sensor.AndroidSensor) {
                (accelerometer as? Any)?.let { sensor ->
                    sensor.javaClass.getMethod("destroy").invoke(sensor)
                }
            }
            // Similar for gyroscope and magnetometer
            
        } catch (e: Exception) {
            Log.e("MonitoringService", "Error destroying sensors: ${e.message}")
        }

        // Unsubscribe from Movesense
        try {
            mdsSubAcc?.unsubscribe()
            mds = null
        } catch (e: Exception) {
            Log.e("MonitoringService", "Error unsubscribing from Movesense: ${e.message}")
        }


        CoroutineScope(Dispatchers.IO).launch {
            try {
                file = File(
                    getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "${infoUser.uuid}_${timestamp}_${infoUser.activityType}_${infoUser.position}_${infoUser.age}_${infoUser.sex}_${Build.MANUFACTURER}_${Build.MODEL}.csv"
                )
                fileWriter = FileWriter(file, true)
                fileWriter?.append("Timestamp, AccelerometerX, AccelerometerY, AccelerometerZ, GyroscopeX, GyroscopeY, GyroscopeZ, MagnetometerX, MagnetometerY, MagnetometerZ, Sex, Age, Height, Weight, Position, Activity\n")

                // Safe data writing with size validation
                val dataSize = minOf(
                    _timeStamp.size,
                    _accelerometerSensorValue.size, 
                    _gyroscopeSensorValue.size,
                    _magnetometerSensorValue.size
                )
                
                Log.d("MonitoringService", "Writing $dataSize synchronized samples to file")

                for (idx in 0 until dataSize) {
                    try {
                        val timestamp = _timeStamp[idx]
                        val acc = _accelerometerSensorValue[idx]
                        val gyro = _gyroscopeSensorValue[idx]  
                        val mag = _magnetometerSensorValue[idx]
                        
                        if (acc.size >= 3 && gyro.size >= 3 && mag.size >= 3) {
                            fileWriter?.append(
                                "$timestamp,${acc[0]},${acc[1]},${acc[2]}," +
                                        "${gyro[0]},${gyro[1]},${gyro[2]}," +
                                        "${mag[0]},${mag[1]},${mag[2]}," +
                                        "${infoUser.sex},${infoUser.age},${infoUser.height},${infoUser.weight},${infoUser.position},${infoUser.activityType}\n"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("MonitoringService", "Error writing sample $idx: ${e.message}")
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

                    // Safe Movesense data writing
                    val movesenseDataSize = minOf(
                        _timeStampMovesense.size,
                        _accelerometerSensorValueMovesense.size,
                        _gyroscopeSensorValueMovesense.size, 
                        _magnetometerSensorValueMovesense.size
                    )
                    
                    Log.d("MonitoringService", "Writing $movesenseDataSize Movesense samples to file")

                    for (idx in 0 until movesenseDataSize) {
                        try {
                            val timestamp = _timeStampMovesense[idx]
                            val acc = _accelerometerSensorValueMovesense[idx]
                            val gyro = _gyroscopeSensorValueMovesense[idx]
                            val mag = _magnetometerSensorValueMovesense[idx]
                            
                            if (acc.size >= 3 && gyro.size >= 3 && mag.size >= 3) {
                                fileWriter?.append(
                                    "$timestamp,${acc[0]},${acc[1]},${acc[2]}," +
                                            "${gyro[0]},${gyro[1]},${gyro[2]}," +
                                            "${mag[0]},${mag[1]},${mag[2]}," +
                                            "${infoUser.sex},${infoUser.age},${infoUser.height},${infoUser.weight},${Movesense.position},${infoUser.activityType}\n"
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("MonitoringService", "Error writing Movesense sample $idx: ${e.message}")
                        }
                    }
                }

            } catch (e: IOException) {
                Log.e("MonitoringService", "Error writing sensor data: ${e.message}")
                e.printStackTrace()
            } finally {
                fileWriter?.close()
                
                // Clean up data structures
                synchronized(this) {
                    _accelerometerSensorValue.clear()
                    _gyroscopeSensorValue.clear()
                    _magnetometerSensorValue.clear()
                    _timeStamp.clear()

                    _accelerometerSensorValueMovesense.clear()
                    _gyroscopeSensorValueMovesense.clear()
                    _magnetometerSensorValueMovesense.clear()
                    _timeStampMovesense.clear()
                }
                
                Log.d("MonitoringService", "Data cleanup completed")
                
                // Safe WorkManager scheduling - only if files were created successfully
                try {
                    if (file != null && file!!.exists()) {
                        val workRequestSensor = OneTimeWorkRequestBuilder<SendToFirebaseWorker>()
                            .setInputData(workDataOf("file" to file!!.absolutePath))
                            .setConstraints(
                                Constraints(
                                    requiredNetworkType = NetworkType.CONNECTED
                                )
                            )
                            .build()

                        workManager.beginUniqueWork(
                            "SendToFirebaseWorker_${System.currentTimeMillis()}",
                            ExistingWorkPolicy.APPEND,
                            workRequestSensor
                        ).enqueue()
                        
                        Log.d("MonitoringService", "Scheduled upload for sensor data file")
                    }

                    if (Movesense.name != null && fileMovesense != null && fileMovesense!!.exists()) {
                        val workRequestMovesense = OneTimeWorkRequestBuilder<SendToFirebaseWorker>()
                            .setInputData(workDataOf("file" to fileMovesense!!.absolutePath))
                            .setConstraints(
                                Constraints(
                                    requiredNetworkType = NetworkType.CONNECTED
                                )
                            )
                            .build()

                        workManager.beginUniqueWork(
                            "SendToFirebaseWorker_Movesense_${System.currentTimeMillis()}",
                            ExistingWorkPolicy.APPEND,
                            workRequestMovesense
                        ).enqueue()
                        
                        Log.d("MonitoringService", "Scheduled upload for Movesense data file")
                    }
                } catch (e: Exception) {
                    Log.e("MonitoringService", "Error scheduling file uploads: ${e.message}")
                }
            }
        }

        stopSelf()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    /**
     * Extract array data from Movesense JSON with improved error handling
     */
    fun extractArray(json: String, key: String): List<String> {
        try {
            val startIndex = json.indexOf(key)
            if (startIndex == -1) {
                Log.w("MonitoringService", "Key '$key' not found in JSON")
                return emptyList()
            }

            val arrayStart = json.indexOf("[", startIndex)
            val arrayEnd = json.indexOf("]", arrayStart)
            
            if (arrayStart == -1 || arrayEnd == -1 || arrayEnd <= arrayStart) {
                Log.w("MonitoringService", "Invalid array format for key '$key'")
                return emptyList()
            }
            
            val arrayString = json.substring(arrayStart + 1, arrayEnd)

            // Handle empty array
            if (arrayString.trim().isEmpty()) {
                Log.w("MonitoringService", "Empty array for key '$key'")
                return emptyList()
            }

            // Process array elements
            return arrayString.split("},").flatMap { element ->
                val x = extractValue(element, "x")
                val y = extractValue(element, "y") 
                val z = extractValue(element, "z")
                
                // Validate extracted values
                if (x != "0.0" || y != "0.0" || z != "0.0") {
                    listOf(x, y, z)
                } else {
                    Log.w("MonitoringService", "All zero values extracted from element: $element")
                    listOf(x, y, z) // Keep even zero values, they might be valid
                }
            }
        } catch (e: Exception) {
            Log.e("MonitoringService", "Error extracting array '$key': ${e.message}")
            return emptyList()
        }
    }

    /**
     * Extract individual values with improved error handling
     */
    private fun extractValue(element: String, axis: String): String {
        try {
            val axisIndex = element.indexOf("\"$axis\":")
            if (axisIndex == -1) {
                Log.w("MonitoringService", "Axis '$axis' not found in element")
                return "0.0"
            }

            val start = element.indexOf(":", axisIndex) + 1
            val end = element.indexOf(",", start).takeIf { it != -1 } ?: element.length
            
            if (end <= start) {
                Log.w("MonitoringService", "Invalid value format for axis '$axis'")
                return "0.0"
            }
            
            val value = element.substring(start, end).replace("}", "").trim()
            
            // Validate numeric value
            value.toDoubleOrNull() ?: run {
                Log.w("MonitoringService", "Non-numeric value for axis '$axis': '$value'")
                return "0.0"
            }
            
            return value
        } catch (e: Exception) {
            Log.e("MonitoringService", "Error extracting value for axis '$axis': ${e.message}")
            return "0.0"
        }
    }
}