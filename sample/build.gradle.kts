plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
}

// A plain Android app rather than KMP: AGP 9 carries Kotlin built in, and the sample only ever
// needs to run on Android.
android {
  namespace = "dev.lutero.diorama.sample"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "dev.lutero.diorama.sample"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = 1
    versionName = "0.1.0"
  }

  buildFeatures { compose = true }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  implementation(project(":diorama"))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.adaptive)
  implementation(libs.androidx.activity.compose)
}
