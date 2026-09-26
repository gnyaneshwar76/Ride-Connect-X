import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
}

/*
 * The google-services plugin hard-fails if google-services.json is absent, which
 * would block every build until Firebase is registered. Applying it conditionally
 * keeps the project buildable now and switches Firebase on automatically the
 * moment the file is dropped into app/.
 */
val firebaseConfig = file("google-services.json")
if (firebaseConfig.exists()) {
    apply(plugin = "com.google.gms.google-services")
    logger.lifecycle("Firebase: google-services.json found — Firebase enabled.")
} else {
    logger.lifecycle("Firebase: google-services.json missing — running without Firebase.")
}

/*
 * Release signing, same conditional trick as Firebase above. The keystore and its
 * passwords live in keystore.properties, which git ignores and nobody but the
 * rider ever sees. Until that file exists, release builds stay unsigned and the
 * build still passes. Expected keys: storeFile, storePassword, keyAlias, keyPassword.
 */
val keystoreProps = rootProject.file("keystore.properties")
    .takeIf { it.exists() }
    ?.let { f -> Properties().apply { f.inputStream().use { load(it) } } }

android {
    namespace = "com.eshwar.rideconnectx"
    compileSdk = 35

    defaultConfig {
        // Must match the package registered in Firebase; the Kotlin namespace
        // above is separate and does not need to change.
        applicationId = "com.gnyaneshwar.rideconnectx"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            // R8 on: shrinks, and strips android.util.Log via proguard-rules.pro.
            // Without it every Log.d survived into release — BLE packets in hex,
            // caller names, message senders and the Firebase uid, all readable
            // over ADB by anyone holding the phone.
            //
            // NOT YET SMOKE-TESTED ON HARDWARE. Obfuscation breaks at runtime,
            // not at compile time. Install a release build and exercise sign-in,
            // the BLE link and a real turn before trusting it.
            isMinifyEnabled = true
            isShrinkResources = true

            // An unsigned release APK is not installable and cannot be uploaded
            // to Play, but the build used to succeed anyway and say nothing —
            // you only found out at the store. Fail loudly instead.
            if (keystoreProps == null) {
                tasks.matching { it.name == "packageRelease" }.configureEach {
                    doFirst {
                        throw GradleException(
                            "keystore.properties is missing, so this release would be UNSIGNED " +
                                "and cannot be installed or uploaded to Play. Create it at the " +
                                "project root with storeFile, storePassword, keyAlias, keyPassword. " +
                                "To build an unsigned APK on purpose, use assembleDebug."
                        )
                    }
                }
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        // Screen 23 (About) reads the version and build number from BuildConfig
        // rather than hardcoding them, so they can never drift out of date.
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

hilt {
    enableAggregatingTask = true
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Daily service-reminder check while the app is closed (N9).
    implementation(libs.androidx.work.runtime.ktx)

    // Navigation & Hilt
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Room — ride history for Statistics
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Firebase — Auth (email/password, Google, phone) and Firestore
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    // App Check — proves to Firebase that a request came from THIS app, not from
    // a script someone built after pulling google-services.json out of the APK.
    // Rules stop cross-user reads; they do not stop quota abuse. This does.
    implementation(libs.firebase.appcheck)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.coroutines.play.services)

    // The in-app "Turn on location?" dialog. Without it the only route is
    // the system Location settings page, which the rider has to navigate.
    implementation(libs.gms.location)

    // Per-app language. AppCompatDelegate.setApplicationLocales is the only
    // API that works on both API 33+ (where it delegates to LocaleManager) and
    // below (where it stores the choice and recreates). Nothing else in the app
    // uses appcompat — no AppCompatActivity, no appcompat theme.
    implementation(libs.androidx.appcompat)

    // Real backdrop blur. Compose has no built-in way to blur what is
    // *behind* a surface — Modifier.blur blurs the surface's own content,
    // which erases the text inside it. Haze captures the background into a
    // layer and applies RenderEffect to that, which is the frosted look.
    implementation(libs.haze)

    // Credential Manager, for Google Sign-In
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
