plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.marco.pixipet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.marco.pixipet"
        minSdk = 26
        targetSdk = 34
        val ver = Regex("APP_VERSION = \"([0-9.]+)\"")
            .find(file("../shared/src/main/kotlin/com/marco/pixipet/Version.kt").readText())!!.groupValues[1]
        val p = ver.split(".").map { it.toInt() }
        versionCode = p[0] * 10000 + p[1] * 100 + p[2]
        versionName = ver
    }

    // La chiave di firma e' privata: arriva da variabili d'ambiente (segreti di GitHub), mai dal repository.
    val ksPath = System.getenv("PIXIPET_KEYSTORE")
    signingConfigs {
        if (ksPath != null) {
            create("pixipet") {
                storeFile = file(ksPath)
                storePassword = System.getenv("PIXIPET_KEYSTORE_PASSWORD")
                keyAlias = "pixipet"
                keyPassword = System.getenv("PIXIPET_KEYSTORE_PASSWORD")
            }
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDir("../shared/src/main/kotlin")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("pixipet") ?: signingConfigs.getByName("debug")
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
