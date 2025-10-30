plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.todolist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.todolist"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Room
    implementation(libs.room.runtime)
    implementation(libs.androidx.appcompat)
    annotationProcessor(libs.room.compiler)

    // Room KTX – do LiveData
    implementation(libs.room.ktx)

    // LiveData / ViewModel
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    implementation (libs.androidx.activity.v180)
    implementation (libs.androidx.fragment)
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}