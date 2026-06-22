#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
workflow_file="${ROOT_DIR}/.github/workflows/dr-failover-smoke.yml"
runbook_file="${ROOT_DIR}/docs/Disaster_Recovery_Runbook.md"

failures=0

echo "Validating DR failover smoke workflow wiring"

if [[ ! -f "${workflow_file}" ]]; then
  echo "ERROR: missing .github/workflows/dr-failover-smoke.yml"
  failures=$((failures + 1))
else
  if ! grep -q 'workflow_dispatch' "${workflow_file}"; then
    echo "ERROR: dr-failover-smoke workflow must be manually dispatchable"
    failures=$((failures + 1))
  fi
  if ! grep -q 'switch-traffic-manager-priority.ps1' "${workflow_file}"; then
    echo "ERROR: workflow must invoke switch-traffic-manager-priority.ps1"
    failures=$((failures + 1))
  fi
  if ! grep -q 'switch-apim-active-backend.ps1' "${workflow_file}"; then
    echo "ERROR: workflow must invoke switch-apim-active-backend.ps1"
    failures=$((failures + 1))
  fi
  if ! grep -q 'applyApimBackendSwitch' "${workflow_file}"; then
    echo "ERROR: workflow must include applyApimBackendSwitch input"
    failures=$((failures + 1))
  fi
  if ! grep -q 'applyCombinedControlPlaneSwitch' "${workflow_file}"; then
    echo "ERROR: workflow must include applyCombinedControlPlaneSwitch input"
    failures=$((failures + 1))
  fi
  if ! grep -q 'apimServiceName' "${workflow_file}"; then
    echo "ERROR: workflow must include apimServiceName input"
    failures=$((failures + 1))
  fi
  if ! grep -q 'switch-dr-control-plane.ps1' "${workflow_file}"; then
    echo "ERROR: workflow must invoke switch-dr-control-plane.ps1"
    failures=$((failures + 1))
  fi
  if ! grep -q 'Precheck DR control plane (combined)' "${workflow_file}"; then
    echo "ERROR: workflow must include combined precheck step before switching"
    failures=$((failures + 1))
  fi
  if ! grep -q 'PrecheckOnly' "${workflow_file}"; then
    echo "ERROR: workflow combined precheck must call switch-dr-control-plane.ps1 with -PrecheckOnly"
    failures=$((failures + 1))
  fi
  if ! grep -q 'az sql failover-group show' "${workflow_file}"; then
    echo "ERROR: workflow must include SQL failover group status check"
    failures=$((failures + 1))
  fi
  if ! grep -q 'az servicebus georecovery-alias show' "${workflow_file}"; then
    echo "ERROR: workflow must include Service Bus Geo-DR alias status check"
    failures=$((failures + 1))
  fi
  if ! grep -q 'Validate synthetic UI and API probes through Traffic Manager' "${workflow_file}"; then
    echo "ERROR: workflow must include synthetic UI/API probe validation"
    failures=$((failures + 1))
  fi
  if ! grep -q 'oidcWellKnownUrl' "${workflow_file}"; then
    echo "ERROR: workflow must include oidcWellKnownUrl input"
    failures=$((failures + 1))
  fi
  if ! grep -q 'Validate OIDC discovery endpoint' "${workflow_file}"; then
    echo "ERROR: workflow must validate OIDC discovery endpoint"
    failures=$((failures + 1))
  fi
  if ! grep -q 'businessEndpointPath' "${workflow_file}"; then
    echo "ERROR: workflow must include businessEndpointPath input"
    failures=$((failures + 1))
  fi
  if ! grep -q 'Validate business endpoint probe through Traffic Manager' "${workflow_file}"; then
    echo "ERROR: workflow must include business endpoint probe step"
    failures=$((failures + 1))
  fi
fi

if [[ ! -f "${runbook_file}" ]]; then
  echo "ERROR: missing docs/Disaster_Recovery_Runbook.md"
  failures=$((failures + 1))
else
  if ! grep -q 'dr-failover-smoke.yml' "${runbook_file}"; then
    echo "ERROR: runbook must reference dr-failover-smoke workflow"
    failures=$((failures + 1))
  fi
fi

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: DR failover smoke workflow is present and documented."
