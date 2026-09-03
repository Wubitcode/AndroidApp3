plugins {
    alias(libs.plugins.android.application)

    /*
     * Enables Kotlin Symbol Processing for Room.
     *
     * KSP processes Room annotations such as @Entity, @Dao, and @Database
     * and generates the required database implementation during compilation.
     */
    alias(libs.plugins.ksp)
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

    // Core Android libraries used throughout the Treasure Hunt application.
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    /*
     * Provides the interactive Toronto map used by the Treasure Hunt.
     */
    implementation(libs.maplibre.android)

    /*
     * Provides Room's runtime database APIs for locally storing
     * Treasure Hunt location and completion data.
     */
    implementation(libs.androidx.room.runtime)

    /*
     * Provides Kotlin coroutine extensions for asynchronous Room
     * database operations.
     */
    implementation(libs.androidx.room.ktx)

    /*
     * Uses Kotlin Symbol Processing to generate Room database code
     * from the application's annotations.
     */
    ksp(libs.androidx.room.compiler)

    // Unit and Android instrumentation testing libraries.
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}