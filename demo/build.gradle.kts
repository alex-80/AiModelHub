plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val basePackage = project.property("BASE_PACKAGE") as String
val compileSdkVer = (project.property("COMPILE_SDK") as String).toInt()
val minSdkVer = (project.property("MIN_SDK") as String).toInt()
val targetSdkVer = (project.property("TARGET_SDK") as String).toInt()
val appVersionCode = (project.property("VERSION_CODE") as String).toInt()
val appVersionName = project.property("VERSION_NAME") as String

android {
    namespace = "$basePackage.demo"
    compileSdk = compileSdkVer

    defaultConfig {
        applicationId = "$basePackage.demo"
        minSdk = minSdkVer
        targetSdk = targetSdkVer
        versionCode = appVersionCode
        versionName = appVersionName
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
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":sdk"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material.icon.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.ui.tooling)
}
