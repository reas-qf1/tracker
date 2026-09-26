import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.*

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    target {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    dependencies {
        implementation(projects.composeApp)
        implementation(projects.shared)
        implementation(libs.androidx.core)
        implementation(libs.androidx.lifecycle.runtime)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.activity)
        implementation(libs.androidx.work.runtime)
        implementation(libs.snitcher)
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.android)
        implementation(libs.logging)
        implementation(libs.androidx.datastore.core)
        implementation(libs.androidx.datastore.preferences.core)
        implementation(libs.compose.foundation)
        implementation(libs.androidx.room3.runtime)
        implementation(libs.androidx.paging.common)
        // testImplementation(libs.junit)
        // androidTestImplementation(libs.androidx.espresso.core)
        // androidTestImplementation(project.dependencies.platform(libs.androidx.compose.bom))
        // androidTestImplementation(libs.androidx.compose.ui.test.junit4)
        // debugImplementation(libs.androidx.compose.ui.tooling)
        // debugImplementation(libs.androidx.compose.ui.test.manifest)
    }
}

android {
    namespace = "com.reas.tracker2"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.reas.tracker2"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("release-signing.properties")
            val keystoreProperties = Properties()
            keystoreProperties.load(FileInputStream(keystorePropertiesFile))

            storeFile = rootProject.file(keystoreProperties["storeFile"].toString())
            storePassword = keystoreProperties["storePassword"].toString()
            keyAlias = keystoreProperties["keyAlias"].toString()
            keyPassword = keystoreProperties["keyPassword"].toString()
        }
    }
    buildTypes {
        getByName("release") {
            // isShrinkResources = true
            // isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}
