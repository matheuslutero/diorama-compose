plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kmp.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

// Geometry and device catalog only. Nothing here may touch android.content.Context — that is what
// keeps this module portable, and separately consumable the way device_frame is in Flutter.
kotlin {
  androidLibrary {
    namespace = "dev.lutero.diorama.frame"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()
  }
  jvm()
  // No iosX64: Compose Multiplatform dropped the Intel simulator target.
  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.ui)
    }
  }
}
