plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.application)
    alias(libs.plugins.ksp)
}

val basePackage = project.property("BASE_PACKAGE") as String
val compileSdkVer = (project.property("COMPILE_SDK") as String).toInt()
val minSdkVer = (project.property("MIN_SDK") as String).toInt()
val targetSdkVer = (project.property("TARGET_SDK") as String).toInt()
val versionCode = (project.property("VERSION_CODE") as String).toInt()
val versionName = project.property("VERSION_NAME") as String

android {
    namespace = basePackage
    compileSdk = compileSdkVer

    defaultConfig {
        applicationId = basePackage
        minSdk = minSdkVer
        targetSdk = targetSdkVer
        this.versionCode = versionCode
        this.versionName = versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keyStorePath = System.getenv("SIGNING_KEY_STORE_PATH")
            if (keyStorePath != null) {
                storeFile = rootProject.file(keyStorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
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
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        aidl = true
    }
}

dependencies {
    implementation(project(":sdk"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.material.icon.extended)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.com.google.code.gson)
    implementation(libs.litertlm)
    ksp(libs.hilt.android.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
