plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kmp.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.maven.publish)
}

mavenPublishing {
  publishToMavenCentral()
  signAllPublications()
}

// Nothing here may touch android.content.Context; this module stays portable.
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
