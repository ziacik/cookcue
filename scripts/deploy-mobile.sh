#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SERIAL="${1:-${ANDROID_SERIAL:-}}"

if [[ -z "$SERIAL" ]]; then
	mapfile -t DEVICES < <(adb devices | awk 'NR > 1 && $2 == "device" {print $1}')

	case "${#DEVICES[@]}" in
		0)
			echo "No usable ADB device found." >&2
			exit 1
			;;
		1)
			SERIAL="${DEVICES[0]}"
			;;
		*)
			echo "Multiple ADB targets found:"
			adb devices -l | awk 'NR > 1 && $2 == "device"'
			echo
			PS3="Select target: "
			select SERIAL in "${DEVICES[@]}"; do
				if [[ -n "$SERIAL" ]]; then
					break
				fi
				echo "Invalid selection." >&2
			done
			;;
	esac
fi

ADB=(adb -s "$SERIAL")

PACKAGE="com.ziacik.cookcue"
ACTIVITY="com.ziacik.cookcue.mobile.MainActivity"

bash ./gradlew :mobile:assembleDebug
"${ADB[@]}" install -r mobile/build/outputs/apk/debug/mobile-debug.apk
"${ADB[@]}" shell am start -n "$PACKAGE/$ACTIVITY"
