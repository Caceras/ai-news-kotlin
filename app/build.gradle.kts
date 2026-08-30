import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Play Store signing material is supplied locally and is never committed.
 * Absent the file, only the `sideload` and `debug` variants can be assembled.
 */
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use(keystoreProperties::load)
}

/** Fails the build loudly rather than silently shipping a wrong version. */
fun requiredProperty(name: String): String = checkNotNull(project.findProperty(name) as String?) {
    "Missing required Gradle property '$name'. It is declared in gradle.properties."
}

/**
 * Version identity lives in `gradle.properties` so the release workflow can
 * read exactly the same values when it writes the update manifest.
 */
val appVersionName = requiredProperty("aibrief.versionName")
val versionCodeBase = requiredProperty("aibrief.versionCodeBase").toInt()

/**
 * Where the installed app looks to discover newer direct-install builds.
 * `releases/latest` always resolves to the most recent published release, so
 * this URL never has to change as builds come and go.
 */
val updateManifestUrl =
    "https://github.com/Caceras/ai-news-kotlin/releases/latest/download/update.json"

/**
 * Continuous integration passes `-PbuildNumber=<n>` so every published build
 * carries a strictly increasing `versionCode`. Android refuses to install an
 * APK whose `versionCode` is not greater than the installed one, so this is
 * what makes the direct-install update loop work at all.
 */
val buildNumber = (project.findProperty("buildNumber") as String?)?.toIntOrNull() ?: 0


plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.caceras.aibrief"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.caceras.aibrief"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCodeBase + buildNumber
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("int", "BUILD_NUMBER", "$buildNumber")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        /**
         * Signs the direct-install builds distributed through GitHub Releases.
         *
         * These credentials are deliberately public: Android only allows an
         * update to install over an existing app when both are signed by the
         * same key, so the test channel needs a key that is stable across CI
         * runners. It carries no trust — the Play Store upload key is a
         * separate, private key configured through `keystore.properties`.
         */
        create("sideload") {
            storeFile = rootProject.file("signing/sideload.jks")
            storePassword = "sideload"
            keyAlias = "sideload"
            keyPassword = "sideload"
        }

        create("release") {
            val storePath = keystoreProperties.getProperty("storeFile")
            if (storePath != null) {
                storeFile = rootProject.file(storePath)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "SELF_UPDATE_ENABLED", "false")
            buildConfigField("String", "UPDATE_MANIFEST_URL", "\"\"")
        }

        /** The Google Play artifact. Carries no self-update code path. */
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("boolean", "SELF_UPDATE_ENABLED", "false")
            buildConfigField("String", "UPDATE_MANIFEST_URL", "\"\"")
        }

        /**
         * The build installed directly on a phone for testing.
         *
         * Identical to the Play artifact in optimisation and debuggability so
         * that what gets tested matches what ships, but signed with the shared
         * sideload key and carrying the in-app updater. The
         * `REQUEST_INSTALL_PACKAGES` permission the updater needs lives in
         * `src/sideload/AndroidManifest.xml`, keeping it out of the Play build.
         */
        create("sideload") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("sideload")
            matchingFallbacks += listOf("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("boolean", "SELF_UPDATE_ENABLED", "true")
            buildConfigField("String", "UPDATE_MANIFEST_URL", "\"$updateManifestUrl\"")
        }
    }

    lint {
        // Correctness problems block the pipeline; "a newer dependency exists"
        // and similar advisory findings are reported without failing a build.
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

/**
 * Kotlin otherwise targets whichever JDK happens to run Gradle, which disagrees
 * with the Java target above on any JDK newer than 17 and fails the build. CI
 * pins JDK 17 and so never saw it; pinning the target keeps the build correct
 * on a contributor's machine regardless of their installed JDK.
 */
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // FileProvider hands the downloaded APK to the system package installer.
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")

    testImplementation("junit:junit:4.13.2")
    // Android stubs org.json in unit tests; this supplies a real implementation.
    testImplementation("org.json:json:20250107")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
