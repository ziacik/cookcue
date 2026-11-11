# CookCue

CookCue is an Android + Wear OS cooking assistant that treats a recipe as an executable plan instead of a static list of steps.

The scheduler understands dependencies, limited resources and work that can happen in parallel. For example, two knife-prep tasks compete for the cook, while a pot can keep simmering in the background while you prepare something else.

## Modules

- `core` — recipe model, resource-aware scheduler, cook-speed profile, bean-soup scenario and unit tests
- `mobile` — phone timeline UI
- `wear` — glanceable current-task UI
- `scripts` — local checks and ADB deploy helpers

## Build

```bash
bash ./scripts/check.sh
```

## Release

Pushing a semantic-version tag (for example, `v0.9.0`) runs [Release CI](.github/workflows/release.yml). It tests and lints the project, builds **signed** phone and Wear OS APKs, verifies their signatures, and publishes both files in [GitHub Releases](https://github.com/ziacik/cookcue/releases). No release is published if building, signing, or verification fails.

Before the first release, generate a permanent signing key (or reuse the existing production key):

```bash
keytool -genkeypair -v -keystore cookcue-release.jks -alias cookcue -keyalg RSA -keysize 3072 -validity 10000
base64 -w0 cookcue-release.jks
```

Set these repository **Settings → Secrets and variables → Actions → Repository secrets**:

- `ANDROID_RELEASE_KEYSTORE_BASE64` — complete Base64 output of the keystore file
- `ANDROID_RELEASE_STORE_PASSWORD` — keystore password
- `ANDROID_RELEASE_KEY_ALIAS` — signing key alias (for example, `cookcue`)
- `ANDROID_RELEASE_KEY_PASSWORD` — key password

Keep a secure backup of the original keystore and passwords; losing the signing key prevents updating already-installed releases signed with it. **Never commit the keystore or passwords.** Both APKs use the same release key.

Create a release after merging the desired changes to `master`:

```bash
git checkout master
git pull --ff-only
git tag v0.9.0
git push origin v0.9.0
```

The tag must be `vMAJOR.MINOR.PATCH`. Both release APKs receive the tag's version name; their Android version code is derived as `MAJOR × 1,000,000 + MINOR × 1,000 + PATCH` (minor and patch each below 1000). For example, `v0.9.0` produces version code `9000`. Local debug builds keep the module versions defined in their Gradle files. Do not reuse an already-published tag.

## Deploy

```bash
bash ./scripts/deploy-mobile.sh
bash ./scripts/deploy-wear.sh
```

Pass an ADB serial as the first argument if multiple devices are connected.

## Rules

- primary development branch: `master`
- tabs in Kotlin, Gradle Kotlin DSL and shell scripts
- YAML uses spaces because YAML itself forbids tab indentation
