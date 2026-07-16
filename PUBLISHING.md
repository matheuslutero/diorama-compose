# Publishing

Diorama publishes `:diorama` and `:diorama-frame` to Maven Central through the
[vanniktech maven-publish](https://github.com/vanniktech/gradle-maven-publish-plugin) plugin.
`:sample` is never published.

Coordinates come from `gradle.properties`:

```
io.github.matheuslutero:diorama:0.1.0
io.github.matheuslutero:diorama-frame:0.1.0
```

## One-time setup

### 1. Namespace

`io.github.matheuslutero` is verified on the [Central Portal](https://central.sonatype.com) through
the GitHub account, so it needs no domain. Do not change the group id after the first release: a
published coordinate is permanent, and moving it forces every consumer to update their dependency.

### 2. Central Portal token

Create a user token at central.sonatype.com → Account → Generate User Token. Put it in
`~/.gradle/gradle.properties` (never in the repo):

```
mavenCentralUsername=<token-username>
mavenCentralPassword=<token-password>
```

### 3. GPG signing key

Central requires every artifact to be signed.

```bash
gpg --gen-key                                    # once
gpg --armor --export-secret-keys <KEY_ID>        # copy the block
```

Add to `~/.gradle/gradle.properties`:

```
signingInMemoryKey=<the ascii-armored secret key, newlines as \n>
signingInMemoryKeyPassword=<key passphrase>
```

Also publish the public key so Central can verify it:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
```

## Release

The version comes from the git tag, not from `gradle.properties`. `VERSION_NAME=0.1.0` there is only
a local default; the release workflow overrides it from the tag through
`ORG_GRADLE_PROJECT_VERSION_NAME`.

### Through CI (the normal path)

The credentials above live as GitHub Actions secrets, not in `~/.gradle`:

```
ORG_GRADLE_PROJECT_mavenCentralUsername
ORG_GRADLE_PROJECT_mavenCentralPassword
ORG_GRADLE_PROJECT_signingInMemoryKey
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword
```

Then move the `Unreleased` entries in `CHANGELOG.md` under the new version, and tag:

```bash
git tag 0.1.0 && git push origin 0.1.0
```

`.github/workflows/release.yml` fires on the tag, sets the version from it, and uploads to the
Central Portal. The upload waits for a manual release in the Portal UI; switch the module config to
`publishToMavenCentral(true)` to release automatically (a Central release cannot be undone, so manual
review is the safer default for now).

### Locally

Configuration cache is on, so publishing needs it off:

```bash
./gradlew publishToMavenCentral --no-configuration-cache -PVERSION_NAME=0.1.0
```

A `-SNAPSHOT` version publishes to the snapshots repository with no signing or release step, useful
for testing a consumer against an unreleased build.

## Consuming

```kotlin
dependencies {
  implementation("io.github.matheuslutero:diorama:0.1.0")
}
```

`:diorama` pulls in `:diorama-frame` transitively.
