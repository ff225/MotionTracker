package com.pedometers.motiontracker.di

import android.content.Context
import com.pedometers.motiontracker.bluetooth.AndroidBluetoothController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object BluetoothModule {

    @Provides
    fun provideBluetoothController(context: Context): AndroidBluetoothController {
        return AndroidBluetoothController(context)
    }
}