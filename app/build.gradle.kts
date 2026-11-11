plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "ca.gbc.comp3074.sakugavault"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ca.gbc.comp3074.sakugavault"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.2-functional-prototype"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Where the SakugaVault API (the ASP.NET Core back end, started with `docker compose up`) runs.
        // 10.0.2.2 is how the Android emulator reaches the computer it is running on.
        // On a real phone, use the computer's Wi-Fi IP address instead and add it to res/xml/network_security_config.xml.
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.lifecycle.livedata)

    // Room: the on-phone database that holds the watchlist ("Title" and "Metadata" tables)
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)

    // Retrofit + OkHttp: calls to the SakugaVault API
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // Glide: loads poster and backdrop images
    implementation(libs.glide)

    // Media3 (ExoPlayer): plays the HLS and MP4 streams the back end returns, and the demo videos.
    // The OkHttp data source sends video requests through the app's OkHttp client and cookie jar.
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.datasource.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.rules)
}
