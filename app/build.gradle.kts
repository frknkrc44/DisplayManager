import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "org.blinksd.dispmgr"
    compileSdk = 36

    buildFeatures {
        aidl = true
    }

    defaultConfig {
        applicationId = namespace
        minSdk = 29
        targetSdk = compileSdk
        versionCode = 2
        versionName = "1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file(keystoreProperties["KEYFILE"] as String)
            keyAlias = keystoreProperties["KEYALIAS"] as String
            keyPassword = keystoreProperties["KEYPASS"] as String
            storePassword = keystoreProperties["STOREPASS"] as String
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.libsu.core)
    implementation(libs.libsu.service)

    compileOnly(project(":hidden"))

    implementation(libs.lsposed.hiddenapibypass)
}

android.applicationVariants.all {
    outputs.all {
        (this as BaseVariantOutputImpl).apply {
            outputFileName = "${rootProject.name.replace(" ", "_")}-v${versionName}-${buildType.name}.apk"
        }
    }
}
