package com.pedometers.motiontracker.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionTrackerAppBar(
    title: String,
    canNavigateBack: Boolean,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigateUp: () -> Unit = {},
    navigateToInfo: () -> Unit = {},
    navigateToUpdateID: () -> Unit = {},
    navigateToMovesense: () -> Unit = {}

) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        actions = {
            if (!canNavigateBack)
                Row {
                    IconButton(onClick = navigateToInfo) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = navigateToUpdateID) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = navigateToMovesense) {
                        Icon(
                            imageVector = Icons.Filled.Build,
                            contentDescription = null
                        )
                    }
                }

        },
        navigationIcon = {
            if (canNavigateBack)
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }

        }
    )
}