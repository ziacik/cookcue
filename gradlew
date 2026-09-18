#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="9.6.0"
GRADLE_SHA256="bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01"
CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/cookcue-wrapper"
ZIP="$CACHE_DIR/gradle-$GRADLE_VERSION-bin.zip"
HOME_DIR="$CACHE_DIR/gradle-$GRADLE_VERSION"

if [[ ! -x "$HOME_DIR/bin/gradle" ]]; then
	mkdir -p "$CACHE_DIR"
	if [[ ! -f "$ZIP" ]]; then
		URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
		if command -v curl >/dev/null 2>&1; then
			curl -fL "$URL" -o "$ZIP"
		elif command -v wget >/dev/null 2>&1; then
			wget -O "$ZIP" "$URL"
		else
			echo "CookCue needs curl or wget to bootstrap Gradle." >&2
			exit 1
		fi
	fi

	ACTUAL_SHA256="$(sha256sum "$ZIP" | awk '{print $1}')"
	if [[ "$ACTUAL_SHA256" != "$GRADLE_SHA256" ]]; then
		echo "Gradle archive checksum mismatch." >&2
		rm -f "$ZIP"
		exit 1
	fi

	rm -rf "$HOME_DIR"
	unzip -q "$ZIP" -d "$CACHE_DIR"
fi

exec "$HOME_DIR/bin/gradle" "$@"
