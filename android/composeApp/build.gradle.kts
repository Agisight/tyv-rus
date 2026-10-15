import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(compose.components.resources)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.core.ktx)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.testExt.junit)
            implementation("androidx.test:runner:1.7.0")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(compose.materialIconsExtended)

            implementation(libs.androidx.core.ktx)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(compose.components.resources)
        }
    }
}

android {
    namespace = "ru.tuvlin.tyv_rus_android"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.tyv.sostuk"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 7
        versionName = "2.1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    val localSigning = Properties().apply {
        val settingsFile = rootProject.file("signing.properties")
        if (settingsFile.exists()) settingsFile.inputStream().use { load(it) }
    }
    fun signingValue(name: String): String? =
        (project.findProperty(name) as String?)
            ?: System.getenv(name)
            ?: localSigning.getProperty(name)

    val signingNames = listOf(
        "UPLOAD_STORE_FILE", "UPLOAD_STORE_PASSWORD", "UPLOAD_KEY_ALIAS", "UPLOAD_KEY_PASSWORD"
    )
    val signingValues = signingNames.map(::signingValue)
    require(signingValues.all { it == null } || signingValues.all { !it.isNullOrBlank() }) {
        "Provide all UPLOAD_* signing settings or leave all of them unset."
    }
    val hasUploadSigning = signingValues.all { !it.isNullOrBlank() }

    if (hasUploadSigning) {
        signingConfigs.create("upload") {
            storeFile = file(signingValues[0]!!)
            storePassword = signingValues[1]
            keyAlias = signingValues[2]
            keyPassword = signingValues[3]
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasUploadSigning) signingConfig = signingConfigs.getByName("upload")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    bundle { storeArchive { } }

    ndkVersion = "26.3.11579264"
    buildToolsVersion = "36.0.0"
}

compose {
    resources {
        packageOfResClass = "ru.tuvlin.tyv_rus_android"
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
