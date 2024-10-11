package com.pedometers.motiontracker.di
import android.app.Application
import com.pedometers.motiontracker.sensor.AccelerometerSensor
import com.pedometers.motiontracker.sensor.GyroscopeSensor
import com.pedometers.motiontracker.sensor.MagnetometerSensor
import com.pedometers.motiontracker.sensor.MeasurableSensor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import javax.inject.Qualifier


@Module
@InstallIn(ServiceComponent::class)
object SensorModule {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class Accelerometer

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class Gyroscope

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class Magnetometer

    @Provides
    @Accelerometer
    fun provideAccelerometerSensor(app: Application): MeasurableSensor {
        return AccelerometerSensor(app)
    }

    @Provides
    @Gyroscope
    fun provideGyroscopeSensor(app: Application): MeasurableSensor {
        return GyroscopeSensor(app)
    }

    @Provides
    @Magnetometer
    fun provideMagnetometerSensor(app: Application): MeasurableSensor {
        return MagnetometerSensor(app)
    }
}