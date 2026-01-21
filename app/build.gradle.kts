plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.justself.klique"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.justself.klique"
        minSdk = 28
        targetSdk = 35
        versionCode = 20
        versionName = "2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "base_url", "http://10.0.2.2:8080/")
            resValue("string", "websocket_url", "ws://10.0.2.2:3030/")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "base_url", "https://api.kliquesocial.com/")
            resValue("string", "websocket_url", "wss://websocket.kliquesocial.com")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.21"
    }
    lint {
        disable += "ConfigurationScreenWidthHeight"
    }
//    defaultConfig {
//        ndk {
//            abiFilters += "arm64-v8a"
//            abiFilters += "armeabi-v7a"
//        }
//    }
//    externalNativeBuild {
//        cmake {
//            path = file("src/main/CMakeLists.txt")
//        }
//    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.car.ui.lib)
    implementation(libs.core.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.app.update.ktx)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.swiperefreshlayout)
    implementation(libs.retrofit)
    implementation(libs.retrofitGson)
    implementation(libs.okhttp)
    implementation(libs.dexter)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.composeUi)
    implementation(libs.coroutines.guava)
    implementation(libs.guava)
//    implementation(libs.composeMaterialIconsExtended)
//    implementation(libs.composeMaterial3)
    implementation(libs.materialComponents)
//    implementation(libs.androidx.compose.ui)
//    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.compose.runtime)
    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.livedata)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutinesTest)
    testImplementation(libs.archCoreTesting)
    implementation(libs.coil.compose)
    implementation(libs.picasso)
    implementation(libs.java.websocket)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.material.icons.extended)

    // Material-3 extended icons (latest stable 1.3.2)
    implementation(libs.material3)
//    implementation(libs.compose.foundation)
//    implementation(libs.compose.foundation.layout)
    implementation(libs.emoji.compat)
    implementation(libs.emoji.compat.bundled)
    ksp(libs.room.compiler)
    implementation (libs.emoji2)
    implementation (libs.emoji2.views)
    implementation (libs.emoji2.views.helper)
    implementation (libs.emoji2.emojipicker)
    implementation(libs.libphonenumber)
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.ui)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.datastore.preferences)
    implementation("org.jetbrains:annotations:23.0.0")
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains:annotations:23.0.0")
        eachDependency {
            if (requested.group == "com.intellij" && requested.name == "annotations") {
                useVersion("12.0")
            }
        }
    }
}
