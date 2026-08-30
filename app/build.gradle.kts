plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.wubitcode.androidapp3"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.wubitcode.androidapp3"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    // Provides the interactive map used to display Toronto treasure locations.
// MapLibre is used as an open-source mapping solution that does not require
// Google Maps billing credentials for this assignment.
    implementation(libs.maplibre.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}