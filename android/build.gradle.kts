plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val gdxVersion = "1.13.1"
val abis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
val natives: Configuration by configurations.creating

// Keystore rilis opsional (diisi lewat environment / GitHub Secrets).
val ksFile = System.getenv("KEYSTORE_FILE")
val hasKeystore = !ksFile.isNullOrBlank() && file(ksFile).exists()

android {
    namespace = "com.jasonhideseek"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jasonhideseek.game"
        minSdk = 24
        targetSdk = 35
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "0.1.0"
    }

    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = file(ksFile!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasKeystore) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
    }

    sourceSets["main"].apply {
        assets.srcDirs("../assets")
        jniLibs.srcDir(layout.buildDirectory.dir("natives"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        jniLibs.useLegacyPackaging = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    abis.forEach { abi ->
        natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-$abi")
    }
}

// Ekstrak .so native libGDX ke build/natives/<abi>/ lalu masukkan ke jniLibs.
val copyNativeTasks = abis.map { abi ->
    tasks.register<Copy>("copyNatives_$abi") {
        from({
            natives.files
                .filter { it.name.endsWith("natives-$abi.jar") }
                .map { zipTree(it) }
        })
        include("*.so")
        into(layout.buildDirectory.dir("natives/$abi"))
    }
}

tasks.matching { it.name.matches(Regex("merge.*JniLibFolders")) }.configureEach {
    dependsOn(copyNativeTasks)
}
