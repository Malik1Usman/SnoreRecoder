plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.portfolio.snorerecoder"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.portfolio.snorerecoder"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf(
                "armeabi-v7a",
                "arm64-v8a",
                "x86",
                "x86_64"
            )
        }
    }

    ndkVersion = "28.0.12433566"

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
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

    androidResources {
        noCompress += "tflite"
    }

    packaging {

        jniLibs {

            // IMPORTANT
            useLegacyPackaging = false

            // REQUIRED FOR 16 KB
            keepDebugSymbols += setOf(
                "**/*.so"
            )
        }

        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }

    splits {
        abi {
            isEnable = true

            reset()

            include(
                "armeabi-v7a",
                "arm64-v8a",
                "x86",
                "x86_64"
            )

            isUniversalApk = true
        }
    }
}

configurations.all {

    resolutionStrategy {

        // REMOVE OLD/CONFLICTING TF LITE API
        force("com.google.ai.edge.litert:litert-api:1.1.0")
    }

    exclude(
        group = "org.tensorflow",
        module = "tensorflow-lite-api"
    )
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.cardview)

    // UPDATED
    implementation("org.tensorflow:tensorflow-lite:2.17.0")

    implementation(
        "org.tensorflow:tensorflow-lite-support:0.4.4"
    )

    // UPDATED
    implementation(
        "com.google.ai.edge.litert:litert-api:1.1.0"
    )

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)

    androidTestImplementation(
        libs.androidx.espresso.core
    )
}