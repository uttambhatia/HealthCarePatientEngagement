#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
combined_script="${ROOT_DIR}/scripts/switch-dr-control-plane.ps1"
workflow_file="${ROOT_DIR}/.github/workflows/dr-failover-smoke.yml"
runbook_file="${ROOT_DIR}/docs/Disaster_Recovery_Runbook.md"

failures=0

echo "Validating combined DR control-plane switch integration"

if [[ ! -f "${combined_script}" ]]; then
  echo "ERROR: missing scripts/switch-dr-control-plane.ps1"
  failures=$((failures + 1))
else
  if ! grep -q 'PrecheckOnly' "${combined_script}"; then
    echo "ERROR: combined switch script must support -PrecheckOnly mode"
    failures=$((failures + 1))
  fi
fi

if [[ -f "${workflow_file}" ]]; then
  if ! grep -q 'applyCombinedControlPlaneSwitch' "${workflow_file}"; then
    echo "ERROR: workflow must include applyCombinedControlPlaneSwitch input"
    failures=$((failures + 1))
  fi
  if ! grep -q 'switch-dr-control-plane.ps1' "${workflow_file}"; then
    echo "ERROR: workflow must invoke switch-dr-control-plane.ps1"
    failures=$((failures + 1))
  fi
else
  echo "ERROR: missing .github/workflows/dr-failover-smoke.yml"
  failures=$((failures + 1))
fi

if [[ -f "${runbook_file}" ]]; then
  if ! grep -q 'switch-dr-control-plane.ps1' "${runbook_file}"; then
    echo "ERROR: DR runbook must reference switch-dr-control-plane.ps1"
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

echo "Validation passed: combined DR control-plane switch is present and integrated."
