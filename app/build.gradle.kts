plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "pt.carlos.partidas"
    compileSdk = 35

    defaultConfig {
        applicationId = "pt.carlos.partidas"
        minSdk = 31
        targetSdk = 35
        versionCode = 8
        versionName = "1.8"
    }

    signingConfigs {
        create("partidas") {
            // Chave fixa: as atualizações instalam por cima sem desinstalar.
            storeFile = file("partidas.jks")
            storePassword = "partidas2026"
            keyAlias = "partidas"
            keyPassword = "partidas2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("partidas")
        }
        debug {
            signingConfig = signingConfigs.getByName("partidas")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
