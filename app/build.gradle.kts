import java.util.Properties

// ── Version ───────────────────────────────────────────────────────────────────
// Edit app/version.properties to change the version, or run:
//   ./gradlew bumpPatch   →  1.1.5  →  1.1.6
//   ./gradlew bumpMinor   →  1.1.5  →  1.2.0
//   ./gradlew bumpMajor   →  1.1.5  →  2.0.0
val versionProps = Properties().apply {
    file("version.properties").inputStream().use { load(it) }
}
val appVersionName: String = versionProps.getProperty("VERSION_NAME")
val appVersionCode: Int    = versionProps.getProperty("VERSION_CODE").toInt()

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.leeam.cryptowidget"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.leeam.cryptowidget"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        create("release") {
            storeFile   = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias    = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }
}

// ── APK output naming ─────────────────────────────────────────────────────────
// Release APK → Coinflow-1.1.5.apk  (in app/build/outputs/apk/release/)
// Uses a proper Gradle task (not doLast) so the action is configuration-cache safe.
abstract class RenameReleaseApkTask : DefaultTask() {
    @get:InputDirectory
    abstract val apkDir: DirectoryProperty

    @get:Input
    abstract val versionName: Property<String>

    @TaskAction
    fun rename() {
        val dir = apkDir.get().asFile
        dir.listFiles { _, n -> n.endsWith(".apk") }
            ?.forEach { apk -> apk.renameTo(File(dir, "Coinflow-${versionName.get()}.apk")) }
    }
}

val renameReleaseApk by tasks.registering(RenameReleaseApkTask::class) {
    val packageTask = tasks.named<com.android.build.gradle.tasks.PackageApplication>("packageRelease")
    apkDir.set(packageTask.flatMap { it.outputDirectory })
    versionName.set(appVersionName)
}

afterEvaluate {
    tasks.named("assembleRelease") {
        finalizedBy(renameReleaseApk)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    // Hilt
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    implementation("androidx.hilt:hilt-work:1.3.0")
    ksp("androidx.hilt:hilt-compiler:1.3.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Room
    implementation("androidx.room:room-runtime:2.7.0-alpha11")
    implementation("androidx.room:room-ktx:2.7.0-alpha11")
    ksp("androidx.room:room-compiler:2.7.0-alpha11")

    // Coroutines + Lifecycle
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Material
    implementation("com.google.android.material:material:1.12.0")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}

// ── Version bump tasks ────────────────────────────────────────────────────────
// These edit version.properties in place; run before assembleRelease.
// Uses a typed DefaultTask (not doLast closures) so the configuration cache
// can serialize all state without capturing the build script object.
abstract class BumpVersionTask : DefaultTask() {
    @get:Input
    abstract val currentVersionName: Property<String>

    @get:Input
    abstract val component: Property<String>   // "MAJOR", "MINOR", or "PATCH"

    @get:Internal
    abstract val versionPropsFile: RegularFileProperty

    @TaskAction
    fun bump() {
        val ver   = currentVersionName.get()
        val parts = ver.split(".").map { it.toInt() }.toMutableList()
        when (component.get()) {
            "MAJOR" -> { parts[0]++; parts[1] = 0; parts[2] = 0 }
            "MINOR" -> { parts[1]++;               parts[2] = 0 }
            else    ->                             { parts[2]++ }  // PATCH
        }
        val newName = parts.joinToString(".")
        val newCode = parts[0] * 10000 + parts[1] * 100 + parts[2]
        val props = Properties()
        props.setProperty("VERSION_NAME", newName)
        props.setProperty("VERSION_CODE", newCode.toString())
        versionPropsFile.get().asFile.outputStream().use { props.store(it, null) }
        println("╔══════════════════════════════╗")
        println("║  Version: $ver → $newName")
        println("║  Code:    → $newCode")
        println("╚══════════════════════════════╝")
        println("Run ./gradlew assembleRelease to build Coinflow-${newName}.apk")
    }
}

tasks.register("bumpPatch", BumpVersionTask::class) {
    group = "versioning"
    description = "Increment patch version: 1.1.5 → 1.1.6"
    currentVersionName.set(appVersionName)
    component.set("PATCH")
    versionPropsFile.set(file("version.properties"))
}

tasks.register("bumpMinor", BumpVersionTask::class) {
    group = "versioning"
    description = "Increment minor version: 1.1.5 → 1.2.0"
    currentVersionName.set(appVersionName)
    component.set("MINOR")
    versionPropsFile.set(file("version.properties"))
}

tasks.register("bumpMajor", BumpVersionTask::class) {
    group = "versioning"
    description = "Increment major version: 1.1.5 → 2.0.0"
    currentVersionName.set(appVersionName)
    component.set("MAJOR")
    versionPropsFile.set(file("version.properties"))
}
