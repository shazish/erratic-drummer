// buildscript block (optional, as plugins block is preferred)
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath(libs.gradle)
    }
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "my.proj"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "my.proj"
        minSdk = 30
        targetSdk = 36
        versionCode = 13
        versionName = "1.3.0.0705"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }
    
    buildFeatures {
        prefab = true // Enable Prefab
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
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
    ndkVersion = "27.0.12077973"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.fragment) // Ensure this matches the key in libs.versions.toml
    implementation(libs.material)
    implementation(libs.oboe)
    // implementation("com.github.pdrogfer:MidiDroid:1.1") // Commented out due to dependency issues
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
}