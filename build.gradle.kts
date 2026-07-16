plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.kmp.library) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.compose.multiplatform) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.maven.publish) apply false
  alias(libs.plugins.ktlint) apply false
}

// The version lives in version.txt, which release-please bumps. The publish plugin falls back to
// this when VERSION_NAME is unset; the release workflow still overrides it from the git tag.
val libraryVersion = rootDir.resolve("version.txt").readText().trim()

allprojects {
  version = libraryVersion
}

subprojects {
  apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
