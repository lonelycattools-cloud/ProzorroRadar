plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "ua.gov.prozorro.radar"
  compileSdk = 36
  defaultConfig {
    applicationId = "ua.gov.prozorro.radar"
    minSdk = 26
    targetSdk = 36
    versionCode = 14
    versionName = "1.2.1"
  }

  signingConfigs {
    create("release") {
      storeFile = file("${rootProject.projectDir}/keystore/release.jks")
      storePassword = "prozorro_release_2026"
      keyAlias = "prozorro_release"
      keyPassword = "prozorro_release_2026"
    }
  }

  buildTypes {
    release {
      signingConfig = signingConfigs.getByName("release")
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // WorkManager, OkHttp, Serialization, Icons
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.okhttp)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.androidx.compose.material.icons.extended)
}

val rootDirPath = rootDir.absolutePath
val buildDirPath = layout.buildDirectory.asFile.get().absolutePath

tasks.matching { it.name == "assembleDebug" }.configureEach {
  doLast {
    val src = File(buildDirPath, "outputs/apk/debug/app-debug.apk")
    if (src.exists()) {
      val dest = File(rootDirPath, "ProzorroRadar.apk")
      src.copyTo(dest, overwrite = true)
      println("Скопійовано Debug APK в корінь проєкту: ${dest.absolutePath}")
    }
  }
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
  doLast {
    val releaseApkDir = File(buildDirPath, "outputs/apk/release")
    val src = if (releaseApkDir.exists()) releaseApkDir.walkTopDown().firstOrNull { it.extension == "apk" } else null
    if (src != null && src.exists()) {
      val dest = File(rootDirPath, "ProzorroRadar_release.apk")
      src.copyTo(dest, overwrite = true)
      println("Скопійовано Release APK в корінь проєкту: ${dest.absolutePath}")
    }
  }
}

tasks.matching { it.name == "bundleRelease" }.configureEach {
  doLast {
    val releaseBundleDir = File(buildDirPath, "outputs/bundle/release")
    val src = if (releaseBundleDir.exists()) releaseBundleDir.walkTopDown().firstOrNull { it.extension == "aab" } else null
    if (src != null && src.exists()) {
      val dest = File(rootDirPath, "ProzorroRadar_release.aab")
      src.copyTo(dest, overwrite = true)
      println("Скопійовано Release AAB в корінь проєкту: ${dest.absolutePath}")
    }
  }
}
