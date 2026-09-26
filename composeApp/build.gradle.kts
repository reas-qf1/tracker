import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.*

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hotReload)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.build.config)
}

kotlin {
    androidLibrary {
        namespace = "com.reas.tracker2.android"
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        androidResources {
            enable = true
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.ui.graphics)
            implementation(libs.compose.adaptive)
            implementation(libs.compose.material3.adaptive.navigation.suite)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.androidx.datastore.core)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.androidx.paging.compose)
            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.room3.paging)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.landscapist.image)
            implementation(libs.landscapist.palette)
            implementation(libs.landscapist.animation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.logging)
            implementation(libs.colormath)
            implementation(libs.colormath.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.koin.logger.slf4j)
            implementation(libs.logback)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.snitcher)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspJvm", libs.androidx.room3.compiler)
}

compose.desktop {
    application {
        mainClass = "com.reas.tracker2.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.reas.tracker2"
            packageVersion = "1.0.0"
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

buildConfig {
    useKotlinOutput { topLevelConstants = true }
    packageName("com.reas.tracker2.buildConfig")

    buildConfigField("IS_DEBUG", true)

    buildConfigField("IS_ANDROID", expect(false))
    buildConfigField("IS_DESKTOP", expect(false))
    buildConfigField("IS_WINDOWS", expect(false))
    buildConfigField("IS_LINUX", expect(false))
    sourceSets.named("androidMain") {
        buildConfigField("IS_ANDROID", true)
    }
    sourceSets.named("jvmMain") {
        buildConfigField("IS_DESKTOP", true)
        if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
            buildConfigField("IS_WINDOWS", true)
        } else {
            buildConfigField("IS_LINUX", true)
        }
    }

    val localProperties =
        Properties().apply {
            load(rootProject.file("local.properties").inputStream())
        }

    fun secret(name: String, property: String) {
        val value = localProperties.getProperty(property)
        buildConfigField("String?", name, if (value != null) "\"$value\"" else "null")
    }

    secret("LASTFM_API_KEY", "lastfm.key")
}
