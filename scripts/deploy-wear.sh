#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SERIAL="${1:-${ANDROID_SERIAL:-}}"
ADB=(adb)
if [[ -n "$SERIAL" ]]; then
	ADB+=(-s "$SERIAL")
fi

PACKAGE="com.ziacik.cookcue"
ACTIVITY="com.ziacik.cookcue.wear.MainActivity"

bash ./gradlew :wear:assembleDebug

# Remove the pre-sync prototype package if it is still installed.
"${ADB[@]}" shell pm uninstall com.ziacik.cookcue.wear >/dev/null 2>&1 || true

"${ADB[@]}" install -r wear/build/outputs/apk/debug/wear-debug.apk
"${ADB[@]}" shell am start -n "$PACKAGE/$ACTIVITY"
