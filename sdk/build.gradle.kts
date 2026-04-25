plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

val basePackage = project.property("BASE_PACKAGE") as String
val compileSdkVer = (project.property("COMPILE_SDK") as String).toInt()
val minSdkVer = (project.property("MIN_SDK") as String).toInt()

android {
    namespace = "$basePackage.sdk"
    compileSdk = compileSdkVer

    defaultConfig {
        minSdk = minSdkVer
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
        aidl = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = project.properties["BASE_PACKAGE"] as String
                artifactId = "sdk"
                version = project.properties["VERSION_NAME"] as String
            }
        }
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/alex-80/AiModelHub")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                    password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
