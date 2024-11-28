package com.pedometers.motiontracker.screen

import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.pedometers.motiontracker.PreferencesManager
import com.pedometers.motiontracker.SendToFirebaseWorker
import com.pedometers.motiontracker.data.ActivityType
import com.pedometers.motiontracker.data.FirebaseRealTimeDatabase
import com.pedometers.motiontracker.data.Movesense
import com.pedometers.motiontracker.data.Position
import com.pedometers.motiontracker.data.Sex
import com.pedometers.motiontracker.navigation.NavigationDestination
import com.pedometers.motiontracker.sensor.MonitoringService
import java.io.File
import java.io.IOException
import java.util.UUID


object HomeScreen : NavigationDestination {
    override val route: String = "home"
    override val titleRes: String = "Motion Tracker"
}


enum class Timer(var value: Long) {
    TWO_MIN(120000L),
    FIVE_MIN(300000L),
    NONE(0L)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToInfo: () -> Unit,
    navigateToUpdateID: () -> Unit,
    navigateToMovesense: () -> Unit,
) {


    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel<MainViewModel>()
    var permission by remember { mutableStateOf(false) }
    var isEnabled by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var timestamp by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }
    val preferencesManager = PreferencesManager(context)

    val uuid = preferencesManager.getUUID() ?: UUID.randomUUID().toString().also {
        Log.d("HomeScreen", "new UUID: $it")
        preferencesManager.saveUUID(it)
    }

    var numSteps by remember { mutableStateOf("") }

    var buttonEnabled by remember {
        mutableStateOf(false)
    }

    var expandedTimer by remember { mutableStateOf(false) }
    val timerValue by remember {
        mutableStateOf(Timer.NONE)
    }
    var remainingTime by remember { mutableLongStateOf(Timer.NONE.value) }

    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            permission = isGranted
        }

    when {
        ContextCompat.checkSelfPermission(
            LocalContext.current,
            android.Manifest.permission.ACTIVITY_RECOGNITION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
            permission = true
        }

        else -> {
            LaunchedEffect(Unit) {
                launcher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
            }

        }
    }
    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {
            MotionTrackerAppBar(
                title = HomeScreen.titleRes,
                canNavigateBack = false,
                navigateToInfo = navigateToInfo,
                navigateToUpdateID = navigateToUpdateID,
                navigateToMovesense = navigateToMovesense
            )
        }) { innerPadding ->

        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AddUserInfo(
                Modifier,
                viewModel::updateUiState,
                viewModel.uiState,
                isEnabled
            )
            /*                        Spacer(modifier = Modifier.padding(8.dp))
                                    ShowSensorValue(
                                        sensorType = "Accelerometer",
                                        values = if (accelerometerValues == "") "No accelerometer value" else accelerometerValues,
                                        modifier = Modifier
                                    )
                                    Spacer(modifier = Modifier.padding(8.dp))
                                    ShowSensorValue(
                                        sensorType = "GyroScope",
                                        values = if (gyroscopeValues == "") "No gyroscope value" else gyroscopeValues,
                                        modifier = Modifier
                                    )
                                    Spacer(modifier = Modifier.padding(8.dp))
                                    ShowSensorValue(
                                        sensorType = "Magnetometer",
                                        values = if (magnetometerValues == "") "No magnetometer value" else magnetometerValues,
                                        modifier = Modifier
                                    )
             */
            /*ExposedDropdownMenuBox(
                expanded = expandedTimer,
                onExpandedChange = { if (isEnabled) expandedTimer = !expandedTimer },
                modifier = Modifier.padding(8.dp)
            ) {
                TextField(
                    enabled = isEnabled,
                    value = timerValue.name,
                    onValueChange = {},
                    label = { Text("Timer") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTimer) },
                    modifier = Modifier
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedTimer,
                    onDismissRequest = { expandedTimer = false }) {
                    for (time in Timer.entries) {
                        DropdownMenuItem(
                            text = { Text(text = (time.name)) },
                            onClick = {
                                timerValue = time
                                remainingTime = time.value
                                expandedTimer = false
                            })
                    }
                }
            }*/
            Spacer(modifier = Modifier.padding(8.dp))
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = {
                        if (timerValue.value == 0L) {
                            context.stopService(
                                Intent(
                                    context,
                                    MonitoringService::class.java
                                )
                            )
                            //showDialog = false
                        }
                    },
                    dismissButton = {
                        /*TextButton(
                            onClick = {
                                context.stopService(
                                    Intent(
                                        context,
                                        MonitoringService::class.java
                                    )
                                )

                                showDialog = false
                            },
                        ) {
                            Text(text = "Annulla")
                        }*/
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (timerValue.value == 0L) {
                                    context.stopService(
                                        Intent(
                                            context,
                                            MonitoringService::class.java
                                        )
                                    )
                                    //showDialog = false
                                }
                                Log.d("HomeScreen", "numSteps: $numSteps")

                                /*FirebaseRealTimeDatabase.instance.child(uuid)
                                    .child(timestamp.toString()).setValue(
                                        mapOf(
                                            "ID" to timestamp,
                                            "numSteps" to numSteps
                                        )
                                    )
*/
                                showDialog = false
                                showConfirmationDialog = true
                            },
                            enabled = buttonEnabled && numSteps.isNotEmpty()
                        ) {
                            Text(text = "Interrompi registrazione")
                        }
                    },
                    title = {
                        Text(
                            text = "Recording in progress\n" /*+ if (remainingTime == 0L) {
                                //"You can stop logging anytime\n"
                            } else {
                                "Tempo : ${remainingTime / 1000} secondi\n"
                            }*/ + "Effettua 50 passi"
                        )
                    },
                    text = {
                        TextField(
                            value = numSteps,
                            onValueChange = { steps ->
                                numSteps = steps
                            },
                            label = { Text("Inserisci il numero di passi:") },
                            enabled = buttonEnabled,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.padding(8.dp)
                        )
                    },
                    properties = DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    )
                )
            }

            if (showConfirmationDialog) {
                val file = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "${uuid}_${timestamp}_${viewModel.uiState.activityType}_${viewModel.uiState.position}_${viewModel.uiState.age}_${viewModel.uiState.sex}_${Build.MANUFACTURER}_${Build.MODEL}.csv"
                )
                val fileMovesense = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "${uuid}_${timestamp}_${viewModel.uiState.activityType}_${viewModel.uiState.position}_${viewModel.uiState.age}_${viewModel.uiState}.csv"
                )
                AlertDialog(
                    onDismissRequest = {},
                    dismissButton = {
                        TextButton(
                            onClick = {
                                try {
                                    file.delete()
                                    if (Movesense.name != null) fileMovesense.delete()
                                } catch (e: IOException) {
                                    Log.e("HomeScreen", "Error deleting file", e)
                                }
                                showConfirmationDialog = false
                            },
                        ) {
                            Text(text = "Annulla")
                        }

                    },
                    confirmButton = {
                        // "${infoUser.uuid}_${timestamp}_${infoUser.activityType}_${infoUser.position}_${infoUser.age}_${infoUser.sex}_${Build.MANUFACTURER}_${Build.MODEL}.csv"
                        TextButton(
                            onClick = {
                                val workRequestSensor =
                                    OneTimeWorkRequestBuilder<SendToFirebaseWorker>()
                                        .setInputData(workDataOf("file" to file.absolutePath))
                                        .setConstraints(
                                            Constraints(
                                                requiredNetworkType = NetworkType.CONNECTED
                                            )
                                        )
                                        .build()


                                WorkManager.getInstance(context).beginUniqueWork(
                                    "SendToFirebaseWorker",
                                    ExistingWorkPolicy.APPEND,
                                    workRequestSensor
                                ).enqueue()


                                if (Movesense.name != null) {
                                    val workRequestMovesense =
                                        OneTimeWorkRequestBuilder<SendToFirebaseWorker>()
                                            .setInputData(workDataOf("file" to fileMovesense.absolutePath))
                                            .setConstraints(
                                                Constraints(
                                                    requiredNetworkType = NetworkType.CONNECTED
                                                )
                                            )
                                            .build()

                                    WorkManager.getInstance(context).beginUniqueWork(
                                        "SendToFirebaseWorker",
                                        ExistingWorkPolicy.APPEND,
                                        workRequestMovesense
                                    ).enqueue()
                                }

                                FirebaseRealTimeDatabase.instance.child(uuid)
                                    .child(timestamp.toString()).setValue(
                                        mapOf(
                                            "ID" to timestamp,
                                            "numSteps" to numSteps
                                        )
                                    )
                                showConfirmationDialog = false
                            },
                        ) {
                            Text(text = "Salva")
                        }

                    },
                    title = {
                        Text(
                            text = "Vuoi salvare la registrazione?"
                        )
                    },
                    properties = DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    )
                )
            }
            Row {
                StartStopListeningButton(
                    isEnabled = viewModel.uiState.isValid,
                    startListening = {
                        timestamp = System.currentTimeMillis()
                        numSteps = ""
                        buttonEnabled = true
                        isEnabled = viewModel.uiState.isValid
                        showDialog = true
                        ContextCompat.startForegroundService(
                            context,
                            Intent(
                                context,
                                MonitoringService::class.java
                            ).putExtra("age", viewModel.uiState.age.toInt())
                                .putExtra("sex", viewModel.uiState.sex.name)
                                .putExtra("height", viewModel.uiState.height.toInt())
                                .putExtra(
                                    "weight", viewModel.uiState.weight.toInt()
                                )
                                .putExtra("position", viewModel.uiState.position.name)
                                .putExtra("activityType", viewModel.uiState.activityType.name)
                                .putExtra("uuid", uuid)
                                .putExtra("timestamp", timestamp)
                        )

                        if (timerValue != Timer.NONE) {
                            buttonEnabled = false
                            val timer =
                                object : CountDownTimer(timerValue.value, 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                        remainingTime = millisUntilFinished
                                    }

                                    override fun onFinish() {
                                        //showDialog = false
                                        buttonEnabled = true
                                        context.stopService(
                                            Intent(
                                                context,
                                                MonitoringService::class.java
                                            )
                                        )
                                    }
                                }
                            timer.start()
                        }
                    },
                    /*
                                        stopListening = {
                                            showDialog = false
                                            context.stopService(
                                                Intent(
                                                    context,
                                                    MonitoringService::class.java
                                                )
                                            )
                                            //viewModel.stopListening()
                                            //isEnabled = true
                                        }*/
                )
                Spacer(modifier = Modifier.padding(8.dp))
                IconButton(onClick = {
                    viewModel.updateUiState(UiState())
                    numSteps = ""
                    Movesense.reset()
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
            }


        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserInfo(
    modifier: Modifier = Modifier,
    updateUiState: (UiState) -> Unit,
    uiState: UiState,
    isEnabled: Boolean
) {
    var expandedSex by remember { mutableStateOf(false) }
    var expandedPosition by remember { mutableStateOf(false) }
    var expandedActivity by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        TextField(
            enabled = isEnabled,
            value = uiState.age,
            onValueChange = {
                val age = it.toIntOrNull()
                updateUiState(
                    uiState.copy(
                        age = it, ageError = if (age == null || age <= 0 || age > 120) {
                            "Età non valida"
                        } else null
                    )
                )
            },
            label = { Text(uiState.ageError ?: "Età") },
            singleLine = true,
            modifier = Modifier.padding(8.dp),
            isError = uiState.ageError != null,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
        TextField(
            enabled = isEnabled,
            value = uiState.height,
            onValueChange = {
                val height = it.toIntOrNull()
                updateUiState(
                    uiState.copy(
                        height = it,
                        heightError = if (height == null || (height <= 0 || height > 250)) {
                            "Altezza non valida"
                        } else null
                    )
                )
            },
            label = { Text(uiState.heightError ?: "Altezza in cm") },
            singleLine = true,
            isError = uiState.heightError != null,
            modifier = Modifier.padding(8.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
        TextField(
            enabled = isEnabled,
            value = uiState.weight,
            onValueChange = {
                val weight = it.toIntOrNull()
                updateUiState(
                    uiState.copy(
                        weight = it,
                        weightError = if (weight == null || weight <= 0 || weight > 200) {
                            "Peso non valido"
                        } else null
                    )
                )
            },
            label = { Text(uiState.weightError ?: "Peso in kg") },
            singleLine = true,
            modifier = Modifier.padding(8.dp),
            isError = uiState.weightError != null,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
        ExposedDropdownMenuBox(
            expanded = expandedSex,
            onExpandedChange = { if (isEnabled) expandedSex = !expandedSex },
            modifier = Modifier.padding(8.dp)
        ) {
            TextField(
                enabled = isEnabled,
                value = uiState.sex.name,
                onValueChange = {},
                label = { Text("Sesso") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSex) },
                modifier = Modifier
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expandedSex,
                onDismissRequest = { expandedSex = false }) {
                for (sex in Sex.entries) {
                    DropdownMenuItem(text = { Text(text = (sex.name)) }, onClick = {
                        updateUiState(uiState.copy(sex = sex))
                        expandedSex = false
                    })
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = expandedPosition,
            onExpandedChange = { if (isEnabled) expandedPosition = it },
            modifier = Modifier.padding(8.dp)
        ) {
            TextField(
                enabled = isEnabled,
                value = uiState.position.name,
                onValueChange = {},
                label = { Text("Posizione del telefono") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPosition) },
                modifier = Modifier
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expandedPosition,
                onDismissRequest = { expandedPosition = false },
            ) {

                for (position in Position.entries) {
                    DropdownMenuItem(
                        text = { Text(position.name) },
                        onClick = {
                            updateUiState(uiState.copy(position = position))
                            expandedPosition = false
                        })
                }
            }

        }

        ExposedDropdownMenuBox(
            expanded = expandedActivity,
            onExpandedChange = { if (isEnabled) expandedActivity = it },
            Modifier.padding(8.dp)
        ) {
            TextField(
                enabled = isEnabled,
                value = uiState.activityType.name,
                onValueChange = {},
                label = { Text("Tipo di attività") },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedActivity)
                },
                modifier = Modifier
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expandedActivity,
                onDismissRequest = { expandedActivity = false }) {
                for (activity in ActivityType.entries) {
                    DropdownMenuItem(text = { Text(activity.name) }, onClick = {
                        updateUiState(uiState.copy(activityType = activity))
                        expandedActivity = false
                    })
                }
            }
        }
    }
}

@Composable
fun StartStopListeningButton(
    isEnabled: Boolean,
    startListening: () -> Unit,
    //stopListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    //var isListening by remember { mutableStateOf(false) }

    Button(
        enabled = isEnabled,
        onClick = startListening
        /*{
            /*if (isListening) {
                stopListening()
            } else */

            //isListening = !isListening
        }*/,
        modifier = modifier
    ) {
        Text("Start Listening")
    }
}

/*
@Composable
fun TimerDialog(remainingTime: Long, onDismissRequest: () -> Unit = {}) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            if (remainingTime == 0L)
                TextButton(onClick = onDismissRequest) {
                    Text(text = "Cancel")
                }
        },
        confirmButton = {
        },
        title = { Text(text = "Recording in progress") },
        text = {

        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}
 */