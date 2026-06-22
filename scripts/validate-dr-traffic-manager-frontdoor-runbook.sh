#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLAN_FILE="${ROOT_DIR}/docs/Azure_Deployment_Minimum_Plan.md"
RUNBOOK_FILE="${ROOT_DIR}/docs/Disaster_Recovery_Runbook.md"

failures=0

echo "Validating runbook and deployment plan coverage for Traffic Manager + Front Door DR model"

if [[ ! -f "${PLAN_FILE}" ]]; then
  echo "ERROR: missing ${PLAN_FILE#${ROOT_DIR}/}"
  failures=$((failures + 1))
else
  if ! grep -qi 'traffic manager' "${PLAN_FILE}"; then
    echo "ERROR: ${PLAN_FILE#${ROOT_DIR}/} must mention Traffic Manager"
    failures=$((failures + 1))
  fi
  if ! grep -qi 'front door' "${PLAN_FILE}"; then
    echo "ERROR: ${PLAN_FILE#${ROOT_DIR}/} must mention Front Door"
    failures=$((failures + 1))
  fi
  if ! grep -qi 'hub' "${PLAN_FILE}" || ! grep -qi 'spoke' "${PLAN_FILE}"; then
    echo "ERROR: ${PLAN_FILE#${ROOT_DIR}/} must describe hub-spoke network model"
    failures=$((failures + 1))
  fi
fi

if [[ ! -f "${RUNBOOK_FILE}" ]]; then
  echo "ERROR: missing ${RUNBOOK_FILE#${ROOT_DIR}/}"
  failures=$((failures + 1))
else
  if ! grep -qi 'traffic manager' "${RUNBOOK_FILE}"; then
    echo "ERROR: ${RUNBOOK_FILE#${ROOT_DIR}/} must include Traffic Manager failover steps"
    failures=$((failures + 1))
  fi
  if ! grep -qi 'front door' "${RUNBOOK_FILE}"; then
    echo "ERROR: ${RUNBOOK_FILE#${ROOT_DIR}/} must include Front Door validation steps"
    failures=$((failures + 1))
  fi
fi

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: runbook and plan include Traffic Manager + Front Door DR coverage."
