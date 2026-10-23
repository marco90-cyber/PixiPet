import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
}

val appVersion: String = Regex("APP_VERSION = \"([0-9.]+)\"")
    .find(file("../shared/src/main/kotlin/com/marco/pixipet/Version.kt").readText())!!.groupValues[1]

kotlin {
    jvmToolchain(17)
    sourceSets {
        getByName("main") {
            kotlin.srcDir("../shared/src/main/kotlin")
        }
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation("org.json:json:20240303")
}

compose.desktop {
    application {
        mainClass = "com.marco.pixipet.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "PixiPet"
            packageVersion = appVersion
            includeAllModules = true
        }
    }
}
