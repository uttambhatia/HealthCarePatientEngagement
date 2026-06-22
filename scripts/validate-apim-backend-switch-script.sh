#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
script_file="${ROOT_DIR}/scripts/switch-apim-active-backend.ps1"
workflow_file="${ROOT_DIR}/.github/workflows/dr-failover-smoke.yml"
runbook_file="${ROOT_DIR}/docs/Disaster_Recovery_Runbook.md"

failures=0

echo "Validating APIM backend switch script integration"

if [[ ! -f "${script_file}" ]]; then
  echo "ERROR: missing scripts/switch-apim-active-backend.ps1"
  failures=$((failures + 1))
fi

if [[ -f "${workflow_file}" ]]; then
  if ! grep -q 'switch-apim-active-backend.ps1' "${workflow_file}"; then
    echo "ERROR: dr-failover-smoke workflow must invoke switch-apim-active-backend.ps1"
    failures=$((failures + 1))
  fi
else
  echo "ERROR: missing .github/workflows/dr-failover-smoke.yml"
  failures=$((failures + 1))
fi

if [[ -f "${runbook_file}" ]]; then
  if ! grep -q 'switch-apim-active-backend.ps1' "${runbook_file}"; then
    echo "ERROR: DR runbook must reference switch-apim-active-backend.ps1"
    failures=$((failures + 1))
  fi
else
  echo "ERROR: missing docs/Disaster_Recovery_Runbook.md"
  failures=$((failures + 1))
fi

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: APIM backend switch script is present and integrated."
