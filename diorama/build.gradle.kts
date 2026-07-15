plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kmp.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

// Android-only for now: the configuration-override engine is built on Configuration and
// ContextThemeWrapper, which have no counterpart on other targets. Other platforms plug in later
// through an expect/actual seam in commonMain — density, layout direction and window size are all
// expressible everywhere; only the Context/Configuration half is Android-specific.
kotlin {
  androidLibrary {
    namespace = "dev.lutero.diorama"
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
