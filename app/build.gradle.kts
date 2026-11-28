import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

fun loadProperties(name: String) = Properties().apply {
    val file = rootProject.file(name)
    if (file.exists()) file.inputStream().use(::load)
}

/**
 * Supabase project shared with the web app (habbit-tracker `.env`:
 * VITE_SUPABASE_CO / VITE_SUPABASE_ANON). Lives in local.properties, never in Git.
 */
val localProperties = loadProperties("local.properties")

/**
 * Sync is off by default: the Supabase tables do not exist yet (see
 * docs/supabase-schema.sql), so every build keeps habits on the phone only.
 * `-Psync=true` bakes the Supabase URL/key from local.properties in.
 */
val syncEnabled = providers.gradleProperty("sync").orNull == "true"
fun supabase(key: String) = if (syncEnabled) localProperties.getProperty(key, "") else ""

/**
 * Release signing material, outside the repo (`keystore.properties` + `.jks` are
 * gitignored). Without it the release APK is built unsigned rather than signed
 * with a throwaway key: a different key cannot update an installed app.
 */
val keystoreProperties = loadProperties("keystore.properties")

fun String.asBuildConfigString() = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "dev.sito8943.focushabit"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sito8943.focushabit"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.2.1"

        buildConfigField(
            "String",
            "SUPABASE_URL",
            supabase("supabase.url").asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            supabase("supabase.anonKey").asBuildConfigString(),
        )
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Lives next to the release install instead of replacing it.
            applicationIdSuffix = ".debug"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            // Licence texts, Kotlin/coroutines debug metadata and version files:
            // nothing reads them at runtime.
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/*.kotlin_module",
                "/META-INF/**/LICENSE*",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
            )
        }
    }

    // Sideloaded APK, not Play: no need for the encrypted dependency report.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}

/**
 * `./gradlew dist`: signed, minified release APK copied to
 * dist/FocusHabit-<version>.apk (dist/ and *.apk are gitignored), like SitoWallet.
 */
val distApkName = "FocusHabit-${android.defaultConfig.versionName}.apk"

tasks.register<Copy>("dist") {
    dependsOn("assembleRelease")
    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    into(rootProject.layout.projectDirectory.dir("dist"))
    rename("app-release.apk", distApkName) // regex form: a lambda would capture the script (config cache)
}
