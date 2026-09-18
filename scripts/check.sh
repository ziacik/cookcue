#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

bash ./gradlew 	:core:testDebugUnitTest 	:mobile:assembleDebug 	:wear:assembleDebug
