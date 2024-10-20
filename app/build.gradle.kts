plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.glancewords"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.glancewords"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isDebuggable = false
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // Enable for debugging
        // freeCompilerArgs = listOf("-Xdebug")
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
    lint {
        checkReleaseBuilds = false
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.example.words.database")
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
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material)
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.preview)
    debugImplementation(libs.androidx.glance.appwidget.preview)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.work)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.google.api.client)
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.google.api.services.sheets)
    implementation(libs.okio)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.sqldelight.driver.android)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.sqldelight.primitive.adapters)
    testImplementation(libs.sqldelight.driver.jvm)
    testImplementation(libs.okio.fakefilesystem)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}