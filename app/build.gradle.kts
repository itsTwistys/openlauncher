import java.net.URI
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace  = "com.openlauncher.app"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId  = "com.openlauncher.app"
        manifestPlaceholders["appLabel"] = "Open Launcher"
        minSdk         = 21
        targetSdk      = 36
        versionCode    = 22
        versionName    = "0.0.21-preview"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".preview"
            manifestPlaceholders["appLabel"] = "Open Launcher Preview"
        }
        release {
            applicationIdSuffix = ".preview"
            manifestPlaceholders["appLabel"] = "Open Launcher Preview"
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            it.jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED", "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.security=ALL-UNNAMED", "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    implementation("androidx.webkit:webkit:1.12.1")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Network — weather
    implementation("com.squareup.retrofit2:retrofit:2.12.0")
    implementation("com.squareup.retrofit2:converter-gson:2.12.0")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // JSON serialization
    implementation("com.google.code.gson:gson:2.13.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

// Package a pinned, verified renderer locally; the installed app loads no CDN scripts.
val prepareVectorAssets = tasks.register("prepareVectorAssets") {
    val manifest = rootProject.file("scripts/maplibre-assets.json")
    inputs.file(manifest)
    val assetDir = file("src/main/assets/map")
    outputs.files(file("src/main/assets/map/maplibre-gl.js"), file("src/main/assets/map/maplibre-gl.css"))
    doLast {
        val spec = groovy.json.JsonSlurper().parse(manifest) as Map<*, *>
        val version = spec["version"] as String
        val files = spec["files"] as Map<*, *>
        fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it) }
        for ((name, expected) in files) {
            val target = assetDir.resolve(name as String)
            if (target.exists() && digest(target.readBytes()) == expected) continue
            val connection = URI("https://unpkg.com/maplibre-gl@$version/dist/$name").toURL().openConnection()
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            val bytes = connection.getInputStream().use { it.readBytes() }
            check(digest(bytes) == expected) { "MapLibre asset checksum mismatch: $name" }
            target.writeBytes(bytes)
        }
    }
}
tasks.named("preBuild") { dependsOn(prepareVectorAssets) }
