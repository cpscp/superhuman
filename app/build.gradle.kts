plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "pt.carlos.painel"
    compileSdk = 35

    defaultConfig {
        applicationId = "pt.carlos.painel"
        minSdk = 33          // Lenovo Tab M10 Plus Gen 3 corre Android 13
        targetSdk = 35
        versionCode = 21
        versionName = "0.21"
    }

    signingConfigs {
        create("painel") {
            // Chave fixa: cada versão instala por cima da anterior sem perder a configuração.
            storeFile = file("painel.jks")
            storePassword = "painel2026"
            keyAlias = "painel"
            keyPassword = "painel2026"
        }
    }

    buildTypes {
        release {
            // R8 + recursos encolhidos: APK mais pequeno e código otimizado (requisito N1).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("painel")
        }
        debug {
            signingConfig = signingConfigs.getByName("painel")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // O SDK do Google Home foi compilado com um Kotlin mais recente.
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }
    buildFeatures {
        compose = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    // O SDK do Google Home precisa da versão mais recente das corrotinas (Mutex.unlock$default).
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.+")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.+")
    // Google Home APIs (Casa)
    implementation("com.google.android.gms:play-services-home:17.1.0")
    implementation("com.google.android.gms:play-services-home-types:17.1.0")
}
