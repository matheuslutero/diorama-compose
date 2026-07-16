# Publishing

`:diorama` and `:diorama-frame` publish to Maven Central through the
[vanniktech maven-publish](https://github.com/vanniktech/gradle-maven-publish-plugin) plugin;
`:sample` is not published.

```
io.github.matheuslutero:diorama:<version>
io.github.matheuslutero:diorama-frame:<version>
```

## Cutting a release

The version is the git tag, not a committed value (`VERSION_NAME` in `gradle.properties` is only a
local default). To release:

1. Move the `Unreleased` entries in `CHANGELOG.md` under the new version.
2. Tag and push:

   ```bash
   git tag 0.2.0 && git push origin 0.2.0
   ```

`.github/workflows/release.yml` fires on the tag, takes the version from it, and uploads to the
Central Portal. The upload waits for a manual release in the Portal; set `publishToMavenCentral(true)`
in the module `build.gradle.kts` to release automatically (a Central release is permanent, so manual
review is the safer default).

## Required Actions secrets

The workflow reads these repository secrets (Settings → Secrets and variables → Actions) and maps
them to the env vars the publish plugin expects.

| Secret | What it is |
|---|---|
| `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD` | A Central Portal user token ([central.sonatype.com](https://central.sonatype.com) → Account → Generate User Token). |
| `SIGNING_KEY`, `SIGNING_PASSWORD` | An armored GPG private key and its passphrase, with the public key on a keyserver. See the plugin's [signing docs](https://vanniktech.github.io/gradle-maven-publish-plugin/central/#in-memory-gpg-key). |

Central verifies the `io.github.matheuslutero` namespace through the GitHub account, so no domain is
needed. Do not change the group id after the first release: a published coordinate is permanent.

## Consuming

```kotlin
implementation("io.github.matheuslutero:diorama:<version>")
```

`:diorama` pulls in `:diorama-frame` transitively.
