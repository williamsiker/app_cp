
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    kotlin("plugin.serialization") version "1.9.23"
    id("com.google.devtools.ksp")
    id("androidx.room")
    //id("org.mozilla.rust-android-gradle.rust-android")
}

/*apply(plugin = "org.mozilla.rust-android-gradle.rust-android")
cargo {
    module  = "src/main/rust"       // directory of Cargo.toml
    libname = "rust"          // Cargo.toml's [package] name.
    targets = listOf("arm", "arm64", "x86", "x86_64")  // abis
}*/

val buildRustLib by tasks.registering(Exec::class) {
    workingDir = file("D:\\Android\\Lancelot\\rust-lib") // ajusta si tu path cambia
    commandLine = listOf("powershell", "-ExecutionPolicy", "Bypass", "-File", "build.ps1")
    onlyIf { file("D:\\Android\\Lancelot\\rust-lib\\build.ps1").exists() }
}

tasks.named("preBuild") {
    dependsOn(buildRustLib)
}

android {
    namespace = "com.example.lancelot"
    compileSdk = 35
    //ndkVersion = "29.0.13113456 rc1" //slows the build :'=/
    defaultConfig {
        applicationId = "com.example.lancelot"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions{
            annotationProcessorOptions{
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        /* freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn"
        ) */
    }
    buildFeatures {
        compose = true
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // project
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.webkit)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    //implementation(libs.jna)
    implementation(libs.kotlinx.coroutines.core)
}