#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNBOOK_FILE="${ROOT_DIR}/docs/Disaster_Recovery_Runbook.md"

required_scripts=(
  "scripts/setup-sql-failover-group.ps1"
  "scripts/setup-servicebus-geodr-alias.ps1"
  "scripts/switch-traffic-manager-priority.ps1"
  "scripts/switch-apim-active-backend.ps1"
  "scripts/switch-dr-control-plane.ps1"
)

failures=0

echo "Validating DR automation scripts and runbook references"

for script_path in "${required_scripts[@]}"; do
  absolute_path="${ROOT_DIR}/${script_path}"
  if [[ ! -f "${absolute_path}" ]]; then
    echo "ERROR: missing ${script_path}"
    failures=$((failures + 1))
  fi
done

if [[ ! -f "${RUNBOOK_FILE}" ]]; then
  echo "ERROR: missing docs/Disaster_Recovery_Runbook.md"
  failures=$((failures + 1))
else
  if ! grep -q 'setup-sql-failover-group.ps1' "${RUNBOOK_FILE}"; then
    echo "ERROR: runbook must reference setup-sql-failover-group.ps1"
    failures=$((failures + 1))
  fi
  if ! grep -q 'setup-servicebus-geodr-alias.ps1' "${RUNBOOK_FILE}"; then
    echo "ERROR: runbook must reference setup-servicebus-geodr-alias.ps1"
    failures=$((failures + 1))
  fi
  if ! grep -q 'switch-traffic-manager-priority.ps1' "${RUNBOOK_FILE}"; then
    echo "ERROR: runbook must reference switch-traffic-manager-priority.ps1"
    failures=$((failures + 1))
  fi
  if ! grep -q 'switch-apim-active-backend.ps1' "${RUNBOOK_FILE}"; then
    echo "ERROR: runbook must reference switch-apim-active-backend.ps1"
    failures=$((failures + 1))
  fi
  if ! grep -q 'switch-dr-control-plane.ps1' "${RUNBOOK_FILE}"; then
    echo "ERROR: runbook must reference switch-dr-control-plane.ps1"
    failures=$((failures + 1))
  fi
fi

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: DR automation scripts are present and documented."
