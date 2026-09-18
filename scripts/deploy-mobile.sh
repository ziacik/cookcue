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
ACTIVITY="com.ziacik.cookcue.mobile.MainActivity"

bash ./gradlew :mobile:assembleDebug
"${ADB[@]}" install -r mobile/build/outputs/apk/debug/mobile-debug.apk
"${ADB[@]}" shell am start -n "$PACKAGE/$ACTIVITY"
