package com.pedometers.motiontracker.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pedometers.motiontracker.BuildConfig
import com.pedometers.motiontracker.PreferencesManager
import com.pedometers.motiontracker.navigation.NavigationDestination
import java.util.UUID


object UpdateIDScreen : NavigationDestination {
    override val route: String = "updateID"
    override val titleRes: String = "Aggiorna ID"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateIDScreen(
    navigateUp: () -> Unit
) {

    val context = LocalContext.current
    var showDialog by remember {
        mutableStateOf(true)
    }
    var password by remember {
        mutableStateOf("")
    }
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        MotionTrackerAppBar(
            title = UpdateIDScreen.titleRes, canNavigateBack = true, navigateUp = navigateUp
        )
    }) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (showDialog) AlertDialog(onDismissRequest = {}, dismissButton = {
                TextButton(onClick = navigateUp) {
                    Text(text = "Annulla")
                }
            }, confirmButton = {
                TextButton(onClick = {
                    if (password == BuildConfig.PASSWORD) showDialog = false
                    else {
                        password = ""
                        Toast.makeText(context, "Password errata", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(text = "Conferma")
                }

            }, title = { Text(text = "Inserisci password") }, text = {
                TextField(
                    value = password,
                    onValueChange = { pass ->
                        password = pass
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Password
                    ),
                    modifier = Modifier.padding(8.dp)
                )
            }, properties = DialogProperties(
                dismissOnBackPress = false, dismissOnClickOutside = false
            )
            )

            val preferencesManager = PreferencesManager(context)

            var uuid by remember {
                mutableStateOf(preferencesManager.getUUID())
            }
            Text(text = "ID: ${uuid ?: "Non disponibile"}")

            Button(onClick = {
                preferencesManager.saveUUID(UUID.randomUUID().toString())
                uuid = preferencesManager.getUUID()
            }) {
                Text(text = "Aggiorna ID")
            }

        }
    }
}