import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.pedometers.motiontracker"
    compileSdk = 34
    // Allow buildConfig to load properties from local.properties file
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.pedometers.motiontracker"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val localProperties = project.rootProject.file("local.properties")
        val properties = Properties()
        properties.load(localProperties.inputStream())
        buildConfigField("String", "FIREBASE_URL", "\"${properties.getProperty("FIREBASE_URL")}\"")
        buildConfigField("String", "PASSWORD", "\"${properties.getProperty("PASSWORD")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.7"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.firebase.common.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(files("libs/mdslib-3.15.0(1)-release.aar"))
    implementation("com.polidea.rxandroidble2:rxandroidble:1.10.2")

    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    implementation("io.reactivex.rxjava2:rxjava:2.2.8")



    kapt(libs.hilt.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    implementation(libs.firebase.bom)
    implementation(libs.firebase.storage)
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
}