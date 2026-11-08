#!/usr/bin/env bash
set -euo pipefail
exec python3 "$(dirname "$0")/trace_driver.py" "${1:-emulator-5556}"
