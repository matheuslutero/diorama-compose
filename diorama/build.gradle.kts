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

// Android-only: the override engine is built on Configuration/ContextThemeWrapper.
kotlin {
  androidLibrary {
    namespace = "io.github.matheuslutero.diorama"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()
  }

  sourceSets {
    commonMain.dependencies {
      api(project(":diorama-frame"))
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.ui)
      implementation(compose.material3)
    }
  }
}
