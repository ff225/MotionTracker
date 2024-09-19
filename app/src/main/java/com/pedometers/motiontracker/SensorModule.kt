package com.pedometers.motiontracker
import android.app.Application
import com.pedometers.motiontracker.sensor.AccelerometerSensor
import com.pedometers.motiontracker.sensor.GyroscopeSensor
import com.pedometers.motiontracker.sensor.MagnetometerSensor
import com.pedometers.motiontracker.sensor.MeasurableSensor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
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
    @Singleton
    @Accelerometer
    fun provideAccelerometerSensor(app: Application): MeasurableSensor {
        return AccelerometerSensor(app)
    }

    @Provides
    @Singleton
    @Gyroscope
    fun provideGyroscopeSensor(app: Application): MeasurableSensor {
        return GyroscopeSensor(app)
    }

    @Provides
    @Singleton
    @Magnetometer
    fun provideMagnetometerSensor(app: Application): MeasurableSensor {
        return MagnetometerSensor(app)
    }
}