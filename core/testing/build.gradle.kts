plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.notification.timer.core.testing"
    compileSdk = 37

    defaultConfig {
        minSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.junit)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.turbine)
}
