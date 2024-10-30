package com.pedometers.motiontracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pedometers.motiontracker.screen.HomeScreen
import com.pedometers.motiontracker.screen.HowToUse
import com.pedometers.motiontracker.screen.InfoScreen
import com.pedometers.motiontracker.screen.MovesenseScreen
import com.pedometers.motiontracker.screen.UpdateIDScreen
import com.pedometers.motiontracker.ui.theme.MotionTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MotionTrackerTheme {
                val navController = rememberNavController()

                val context = LocalContext.current
                val preferencesManager = PreferencesManager(context)
                LaunchedEffect(Unit) {
                    if (preferencesManager.isFirstLaunch()) {
                        navController.navigate("how_to_use")
                    } else {
                        navController.navigate(HomeScreen.route)
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "how_to_use",
                    modifier = Modifier,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                ) {


                    composable("how_to_use") {
                        HowToUseScreen(navController, preferencesManager)
                    }
                    composable(route = HomeScreen.route) {
                        HomeScreen(
                            navigateToInfo = {
                                navController.navigate(InfoScreen.route)
                            },
                            navigateToUpdateID = {
                                navController.navigate(UpdateIDScreen.route)
                            },
                            navigateToMovesense = {
                                navController.navigate(MovesenseScreen.route)
                            }

                        )
                    }

                    composable(route = InfoScreen.route) {
                        InfoScreen(
                            navigateUp = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(route = UpdateIDScreen.route) {
                        UpdateIDScreen(
                            navigateUp = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(route = MovesenseScreen.route) {
                        MovesenseScreen(
                            navigateUp = {
                                navController.popBackStack()
                            }
                        )
                    }

                }
            }
        }
    }
}


@Composable
fun HowToUseScreen(navController: NavController, preferencesManager: PreferencesManager) {
    Scaffold { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            HowToUse()
            Spacer(modifier = Modifier.padding(16.dp))
            Button(
                onClick = {
                    preferencesManager.setFirstLaunch()
                    navController.navigate(HomeScreen.route)
                }
            ) {
                Text("Inizia a usare l'app")
            }
        }
    }
}