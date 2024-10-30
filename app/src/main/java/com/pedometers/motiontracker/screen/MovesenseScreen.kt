package com.pedometers.motiontracker.screen

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.movesense.mds.Mds
import com.movesense.mds.MdsConnectionListener
import com.movesense.mds.MdsException
import com.pedometers.motiontracker.bluetooth.BluetoothDeviceInfo
import com.pedometers.motiontracker.data.FrequencyHertz
import com.pedometers.motiontracker.data.Movesense
import com.pedometers.motiontracker.data.PositionWearable
import com.pedometers.motiontracker.navigation.NavigationDestination

object MovesenseScreen : NavigationDestination {
    override val route: String = "movesense"
    override val titleRes: String = "Movesense"
}


private val TAG = "MovesenseScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovesenseScreen(
    navigateUp: () -> Unit
) {

    // Verificare la presenza dei permessi necessari per la ricerca del dispositivo.

    val viewModel: MovesenseViewModel = hiltViewModel()
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var canEnableBl by remember { mutableStateOf(false) }
    var canEnableGeo by remember { mutableStateOf(false) }
    var isPermissionPermanentlyDenied by remember { mutableStateOf(false) }
    var isLocationEnabled by remember {
        mutableStateOf(
            isLocationEnabled(
                context
            )
        )
    }

    val mds by lazy {
        Mds.builder().build(context)
    }

    val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    val bluetoothAdapter by lazy {
        bluetoothManager.adapter
    }

    var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter.isEnabled) }

    val state = viewModel.state.collectAsState()

    val enableBL =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {}


    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        isPermissionPermanentlyDenied = perms.any {
            !it.value && !ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity,
                it.key
            )
        }
        showDialog = isPermissionPermanentlyDenied
        Log.d(TAG, "onRequestPermissionsResult: $perms")
        canEnableBl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms[Manifest.permission.BLUETOOTH_SCAN] == true
        } else
            perms[Manifest.permission.BLUETOOTH] == true


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            canEnableGeo =
                perms[Manifest.permission.ACCESS_FINE_LOCATION] == true && perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            isLocationEnabled = isLocationEnabled(context)

            if (canEnableGeo && !isLocationEnabled)
                enableBL.launch(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )

        }

        if (canEnableBl && !isBluetoothEnabled)
            enableBL.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            )
    }

    fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canEnableBl = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
            showDialog = !(canEnableBl)
            isBluetoothEnabled = bluetoothAdapter.isEnabled
        } else {
            canEnableBl = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
            canEnableGeo = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
            showDialog = !(canEnableBl && canEnableGeo)
            isBluetoothEnabled = bluetoothAdapter.isEnabled
            isLocationEnabled = isLocationEnabled(context)

        }

    }


    LaunchedEffect(Unit) {
        checkPermissions()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else
            launcher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
    }

    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {
            MotionTrackerAppBar(
                title = MovesenseScreen.titleRes,
                navigateUp = navigateUp,
                canNavigateBack = true
            )
        }) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (state.value.isScanning)
                Loading()
            else if (state.value.isConnected || Movesense.isConnected)
                MovesenseBody(
                    hasPermission = viewModel.hasPermission(),
                    onDisconnect = {
                        Log.d(TAG, "onDisconnect")
                        mds.disconnect(Movesense.macAddr!!)
                        Movesense.reset()
                        viewModel.updateUi(isConnected = false)
                    },
                    onForget = {
                        Log.d(TAG, "onForget")
                        mds.disconnect(Movesense.macAddr!!)
                        Movesense.reset()
                        viewModel.updateUi(isConnected = false)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            else
                DeviceScreen(
                    state = state.value,
                    startScan = {
                        Log.d(TAG, "startScan")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (canEnableBl)
                                viewModel.startScan()
                            else
                                showDialog = true
                        } else {
                            isLocationEnabled = isLocationEnabled(context)
                            if (canEnableGeo && canEnableBl && isLocationEnabled)
                                viewModel.startScan()
                            else if (canEnableGeo && !isLocationEnabled) {
                                Toast.makeText(
                                    context,
                                    "Attiva la localizzazione per cercare i dispositivi Movesense.",
                                    Toast.LENGTH_LONG
                                ).show()
                                enableBL.launch(
                                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                )
                            } else
                                showDialog = true
                        }
                    },
                    stopScan = viewModel::stopScan,
                    onClick = { device ->
                        Log.d(TAG, "onClick: ${device.name}")
                        viewModel.updateUi(true)
                        mds.connect(device.address, object : MdsConnectionListener {
                            override fun onConnect(p0: String?) {
                                Log.d(TAG, "onConnect: $p0")

                                viewModel.stopScan()
                            }

                            override fun onConnectionComplete(p0: String?, p1: String?) {
                                Log.d(TAG, "onConnectionComplete: $p0, $p1")
                                viewModel.updateUi(false, true)
                                Movesense.init(device.name!!, device.address, true)
                                viewModel.stopScan()

                            }

                            override fun onError(p0: MdsException?) {
                                Log.e(TAG, "onError: ${p0?.message}")
                                viewModel.updateUi()
                                viewModel.stopScan()
                            }

                            override fun onDisconnect(p0: String?) {
                                Log.d(TAG, "onDisconnect: $p0")
                                viewModel.stopScan()
                                Movesense.reset()
                                viewModel.updateUi()
                            }


                        })

                    },
                    Modifier.padding(innerPadding)
                )
        }

    }
}

/**
 * Composable function representing the device screen displaying the list of scanned Bluetooth devices.
 *
 * @param state BluetoothUiState holding the state of scanned devices.
 * @param startScan Lambda function to start scanning for devices.
 * @param stopScan Lambda function to stop scanning for devices.
 * @param onClick Lambda function triggered on clicking a device.
 * @param modifier Modifier for applying layout attributes.
 */
@Composable
fun DeviceScreen(
    state: BluetoothUiState,
    startScan: () -> Unit,
    stopScan: () -> Unit,
    onClick: (BluetoothDeviceInfo) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(modifier.fillMaxSize()) {
        BluetoothDeviceList(
            scannedDevices = state.scannedDevices,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            Arrangement.SpaceAround
        ) {
            Button(onClick = startScan) {
                Text(text = "Start scan")
            }
            Button(onClick = stopScan) {
                Text(text = "Stop scan")
            }
        }
    }
}

/**
 * Composable function representing the list of scanned Bluetooth devices.
 *
 * @param scannedDevices List of scanned Bluetooth devices.
 * @param onClick Lambda function triggered on clicking a device.
 * @param modifier Modifier for applying layout attributes.
 */
@Composable
fun BluetoothDeviceList(
    scannedDevices: List<BluetoothDeviceInfo>,
    onClick: (BluetoothDeviceInfo) -> Unit,
    modifier: Modifier = Modifier
) {

    var isEnabled by remember {
        mutableStateOf(true)
    }
    LazyColumn(modifier = modifier) {
        items(scannedDevices) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = it.name ?: "No Name"
                    )
                    Text(
                        text = it.address
                    )
                }
                Button(onClick = {
                    isEnabled = !isEnabled
                    onClick(it)
                }, enabled = isEnabled) {
                    Text(
                        text = "Connetti"
                    )
                }
            }
        }
    }
}


@Composable
fun Loading() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp)
        )
        Text(
            text = "Operazione in corso",
            modifier = Modifier.padding(8.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovesenseBody(
    //state: State<MovesenseUiState>,
    hasPermission: Boolean = false,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
    modifier: Modifier
) {

    var expandedPosition by remember { mutableStateOf(false) }
    var expandedFrequencyHertz by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onDisconnect,
            enabled = Movesense.isConnected && hasPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Disconnetti")
        }
        OutlinedButton(
            onClick = onForget,
            enabled = Movesense.isConnected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Dimentica il dispositivo")
        }
        ExposedDropdownMenuBox(
            expanded = expandedPosition,
            onExpandedChange = { expandedPosition = it },
            modifier = Modifier.padding(8.dp)
        ) {
            TextField(
                value = Movesense.position?.name ?: PositionWearable.WRIST.name,
                onValueChange = {

                },
                label = { Text("Posizione del wearable") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPosition) },
                modifier = Modifier
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expandedPosition,
                onDismissRequest = { expandedPosition = false },
            ) {

                for (position in PositionWearable.entries) {
                    DropdownMenuItem(
                        text = { Text(position.name) },
                        onClick = {
                            Movesense.setPosition(position)
                            expandedPosition = false
                        })
                }
            }

        }
        ExposedDropdownMenuBox(
            expanded = expandedFrequencyHertz,
            onExpandedChange = { expandedFrequencyHertz = it },
            modifier = Modifier.padding(8.dp)
        ) {
            TextField(
                value = Movesense.frequencyHertz.name,
                onValueChange = {

                },
                label = { Text("Frequenza (Hz)") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrequencyHertz) },
                modifier = Modifier
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expandedFrequencyHertz,
                onDismissRequest = { expandedFrequencyHertz = false },
            ) {

                for (frequency in FrequencyHertz.entries) {
                    DropdownMenuItem(
                        text = { Text(frequency.name) },
                        onClick = {
                            Movesense.setFrequency(frequency)
                            expandedFrequencyHertz = false
                        })
                }
            }

        }
    }
}


/**
 * Function to check if location services are enabled.
 *
 * @param context The Context.
 * @return Boolean indicating if location services are enabled.
 */
fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}