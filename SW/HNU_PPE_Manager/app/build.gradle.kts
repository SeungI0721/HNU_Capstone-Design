// 관리자 Android 앱 모듈의 빌드 옵션과 의존성을 정의하는 파일
plugins {
    alias(libs.plugins.android.application)
}

fun configValue(name: String): String {
    return providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orElse("")
        .get()
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

android {
    namespace = "com.example.hnu_ppe_manager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.hnu_ppe_manager"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "FIREBASE_DATABASE_URL", "\"${configValue("HNU_PPE_FIREBASE_DATABASE_URL")}\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${configValue("HNU_PPE_FIREBASE_API_KEY")}\"")
        buildConfigField("String", "FIREBASE_APPLICATION_ID", "\"${configValue("HNU_PPE_FIREBASE_APPLICATION_ID")}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${configValue("HNU_PPE_FIREBASE_PROJECT_ID")}\"")
    }

    buildFeatures {
        buildConfig = true
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
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-database")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
