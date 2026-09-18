#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SERIAL="${1:-${ANDROID_SERIAL:-}}"
ADB=(adb)
if [[ -n "$SERIAL" ]]; then
	ADB+=(-s "$SERIAL")
fi

bash ./gradlew :wear:assembleDebug
"${ADB[@]}" install -r wear/build/outputs/apk/debug/wear-debug.apk
"${ADB[@]}" shell am start -n com.ziacik.cookcue.wear/.MainActivity
