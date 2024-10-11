package com.pedometers.motiontracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

                NavHost(
                    navController = navController,
                    startDestination = HomeScreen.route,
                    modifier = Modifier,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                ) {

                    composable(route = HomeScreen.route) {
                        HomeScreen(
                            navigateToMovesense = {
                                navController.navigate(MovesenseScreen.route)
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



