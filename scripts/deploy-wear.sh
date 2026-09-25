#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SERIAL="${1:-${ANDROID_SERIAL:-}}"

DEVICE_SERIALS=()
DEVICE_LABELS=()

load_connected_devices() {
	while IFS= read -r line; do
		[[ -z "$line" || "$line" == "List of devices attached" ]] && continue

		if [[ "$line" =~ ^(.*[^[:space:]])[[:space:]]+device([[:space:]].*)?$ ]]; then
			local serial="${BASH_REMATCH[1]}"
			local details="${BASH_REMATCH[2]:-}"

			DEVICE_SERIALS+=("$serial")

			local model=""
			local product=""
			if [[ "$details" =~ model:([^[:space:]]+) ]]; then
				model="${BASH_REMATCH[1]}"
			fi
			if [[ "$details" =~ product:([^[:space:]]+) ]]; then
				product="${BASH_REMATCH[1]}"
			fi

			local label=""
			if [[ -n "$model" ]]; then
				label+="model:$model"
			fi
			if [[ -n "$product" ]]; then
				[[ -n "$label" ]] && label+="  "
				label+="product:$product"
			fi
			[[ -n "$label" ]] && label+="  "
			label+="$serial"

			DEVICE_LABELS+=("$label")
		fi
	done < <(adb devices -l)
}

if [[ -z "$SERIAL" ]]; then
	load_connected_devices

	case "${#DEVICE_SERIALS[@]}" in
		0)
			echo "No usable ADB device found." >&2
			exit 1
			;;
		1)
			SERIAL="${DEVICE_SERIALS[0]}"
			;;
		*)
			echo "Multiple ADB targets found:"
			PS3="Select target: "
			select LABEL in "${DEVICE_LABELS[@]}"; do
				if [[ -n "$LABEL" ]]; then
					SERIAL="${DEVICE_SERIALS[REPLY - 1]}"
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
