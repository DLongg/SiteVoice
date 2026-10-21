import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.sitevoice.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sitevoice.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { localProps.load(it) }
        }
        val defaultClassifyApiKey = localProps.getProperty("CLASSIFY_API_KEY", "")
        val defaultClassifyBaseUrl = localProps.getProperty("CLASSIFY_BASE_URL", "https://api.vilao.ai/v1")
        val defaultClassifyModel = localProps.getProperty("CLASSIFY_MODEL", "acc/gemini-3.8-flash")

        buildConfigField("String", "DEFAULT_CLASSIFY_API_KEY", "\"$defaultClassifyApiKey\"")
        buildConfigField("String", "DEFAULT_CLASSIFY_BASE_URL", "\"$defaultClassifyBaseUrl\"")
        buildConfigField("String", "DEFAULT_CLASSIFY_MODEL", "\"$defaultClassifyModel\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Dependencies kiến trúc Task 1.1.3
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.okhttp)
    implementation(libs.play.services.auth)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    debugImplementation(libs.androidx.ui.tooling)
}
