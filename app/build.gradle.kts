plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.kgjr.uno"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kgjr.uno"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    aaptOptions {
        noCompress("tflite")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // TensorFlow Lite dependencies
    implementation("org.tensorflow:tensorflow-lite:2.11.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.11.0")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.11.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.2")

    // Camera
    implementation("androidx.camera:camera-core:1.2.3")
    implementation("androidx.camera:camera-camera2:1.2.3")
    implementation("androidx.camera:camera-lifecycle:1.2.3")
    implementation("androidx.camera:camera-view:1.2.3")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // coroutines for parallel computation
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
}