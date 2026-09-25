#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SERIAL="${1:-${ANDROID_SERIAL:-}}"

list_connected_devices() {
	while IFS=$'\t' read -r serial state; do
		if [[ "$state" == "device" ]]; then
			printf '%s\n' "$serial"
		fi
	done < <(adb devices)
}

print_connected_devices() {
	while IFS=$'\t' read -r serial details; do
		if [[ "$details" == device* ]]; then
			printf '%s\t%s\n' "$serial" "$details"
		fi
	done < <(adb devices -l)
}

if [[ -z "$SERIAL" ]]; then
	mapfile -t DEVICES < <(list_connected_devices)

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
			print_connected_devices
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
ACTIVITY="com.ziacik.cookcue.wear.MainActivity"

bash ./gradlew :wear:assembleDebug

"${ADB[@]}" install -r wear/build/outputs/apk/debug/wear-debug.apk
"${ADB[@]}" shell am start -n "$PACKAGE/$ACTIVITY"
