plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ncmdump.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ncmdump.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.2.1"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Don't compress frontend assets
    androidResources {
        noCompress += listOf("html", "js", "css", "map")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.webkit:webkit:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // MP3 ID3 tag library
    implementation("com.mpatric:mp3agic:0.9.1")
    // JSON parsing
    implementation("org.json:json:20231013")
    // DocumentFile for SAF
    implementation("androidx.documentfile:documentfile:1.0.1")
}
