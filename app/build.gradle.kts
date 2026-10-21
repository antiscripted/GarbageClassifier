plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.garbageclassifier"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.garbageclassifier"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        create("newModel") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug"
            versionNameSuffix = "-new-model"
        }
    }
    testBuildType = if (providers.gradleProperty("newModel").isPresent) "newModel" else "debug"
    sourceSets {
        getByName("debug").java.srcDir("src/legacy/kotlin")
        getByName("release").java.srcDir("src/legacy/kotlin")
        getByName("newModel").assets.srcDir(rootProject.file("tools/models"))
        if (providers.gradleProperty("newModel").isPresent) {
            getByName("androidTest").java.srcDir("src/newModelTest/kotlin")
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
        viewBinding = true
    }
    androidResources {
        noCompress += listOf("tflite", "onnx")
    }
    lint {
        abortOnError = true
    }
}

androidComponents {
    onVariants(selector().withBuildType("newModel")) { variant ->
        variant.packaging.jniLibs.useLegacyPackaging.set(true)
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.ai.edge.litert:litert:1.4.1")
    add("newModelImplementation", files(rootProject.file("tools/android-runtime/onnxruntime-android-1.22.0.aar")))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(files(rootProject.file("tools/android-runtime/onnxruntime-android-1.22.0.aar")))
}
