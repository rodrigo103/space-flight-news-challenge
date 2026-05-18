plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.hilt)
  alias(libs.plugins.ksp)
  alias(libs.plugins.detekt)
  alias(libs.plugins.sonarcloud)
}

android {
    namespace = "com.example.myandroidapp"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.myandroidapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
      buildConfig = false
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

detekt {
  config.setFrom(files("../config/detekt/detekt.yml"))
  baseline = file("detekt-baseline.xml")
  buildUponDefaultConfig = true
  allRules = false
}

sonarqube {
  properties {
    property("sonar.projectKey", "rodrigo103_proyecto-android")
    property("sonar.organization", "rodrigo103")
    property("sonar.host.url", "https://sonarcloud.io")
    property("sonar.sources", "src/main")
    property("sonar.tests", "src/test, src/androidTest")
    property("sonar.java.binaries", "build/intermediates/classes/debug")
    property("sonar.java.test.binaries", "build/intermediates/classes/test/debug")
    property("sonar.android.lint.report", "build/reports/lint-results-debug.xml")
  }
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
  testImplementation(libs.mockk)
  testImplementation(libs.mockwebserver)
  testImplementation(libs.turbine)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation.compose)

  // Retrofit + OkHttp
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlinx.serialization)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging)

  // Coil
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)

  // Kotlinx Serialization
  implementation(libs.kotlinx.serialization.json)

  // Material Icons
  implementation(libs.androidx.compose.material.icons)

  // Hilt
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.hilt.navigation.compose)

  // LeakCanary
  debugImplementation(libs.leakcanary)

  // Timber
  implementation(libs.timber)

  // SplashScreen
  implementation(libs.androidx.core.splashscreen)

  // Lottie
  implementation(libs.lottie.compose)

  // DataStore Preferences
  implementation(libs.datastore.preferences)

  // Room
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  implementation(libs.room.paging)
  ksp(libs.room.compiler)

  // Paging 3
  implementation(libs.paging.runtime)
  implementation(libs.paging.compose)
  implementation(libs.paging.common)

  // Detekt Compose rules
  detektPlugins("io.nlopez.compose.rules:detekt:${libs.versions.detektComposeRules.get()}")
}
