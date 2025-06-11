plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "jp.ac.gifu_u.info.ishida.mylastapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "jp.ac.gifu_u.info.ishida.mylastapplication"
        minSdk = 24
        targetSdk = 35
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation ("com.google.android.gms:play-services-maps:18.2.0") // Google Maps SDK
    implementation ("com.google.android.gms:play-services-location:21.2.") // FusedLocationProviderClientで既に使用

    implementation("androidx.fragment:fragment-ktx:1.8.0")
}