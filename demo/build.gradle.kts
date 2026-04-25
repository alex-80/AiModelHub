plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val basePackage = project.property("BASE_PACKAGE") as String
val compileSdkVer = (project.property("COMPILE_SDK") as String).toInt()
val minSdkVer = (project.property("MIN_SDK") as String).toInt()
val targetSdkVer = (project.property("TARGET_SDK") as String).toInt()
val versionCode = (project.property("VERSION_CODE") as String).toInt()
val versionName = project.property("VERSION_NAME") as String

android {
    namespace = "$basePackage.demo"
    compileSdk = compileSdkVer

    defaultConfig {
        applicationId = "$basePackage.demo"
        minSdk = minSdkVer
        targetSdk = targetSdkVer
        this.versionCode = versionCode
        this.versionName = versionName
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.ui.tooling)
}
