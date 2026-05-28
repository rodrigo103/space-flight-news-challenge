plugins {
  id("jacoco")
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.hilt)
  alias(libs.plugins.ksp)
  alias(libs.plugins.detekt)
  alias(libs.plugins.sonarcloud)
  alias(libs.plugins.google.gms.google.services)
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
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    property("sonar.coverage.jacoco.xmlReportPaths",
      "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
    )
  }
}

tasks.register<JacocoReport>("jacocoTestReport") {
  dependsOn("testDebugUnitTest")

  reports {
    xml.required = true
    html.required = true
  }

  val debugClassesDir = "${project.layout.buildDirectory.get()}/intermediates/classes/debug/transformDebugClassesWithAsm/dirs"
  val mainSrc = "${project.projectDir}/src/main/java"

  sourceDirectories.setFrom(files(mainSrc))
  classDirectories.setFrom(
    fileTree(debugClassesDir) {
      exclude(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/*Test*.*",
        "**/*Preview*.*",
        "**/*_Factory*",
        "**/*_HiltModules*",
        "**/Hilt_*",
        "**/dagger/**",
        "**/Dagger*.*",
        "android/**",
      )
    }
  )
  executionData.setFrom(files("${project.layout.buildDirectory.get()}/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"))
}

dependencies {
  implementation(project(":domain"))
  implementation(libs.firebase.analytics)
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
