#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_DIR="${ROOT_DIR}/deploy/k8s"

critical_services=(
  "api-gateway"
  "svc-patient"
  "svc-appointment"
  "svc-careplan"
  "svc-consent"
  "svc-medical-record"
  "svc-telemetry"
)

failures=0

echo "Validating Kubernetes rollout hygiene (strategy + startupProbe)"

for service in "${critical_services[@]}"; do
  deployment="${DEPLOY_DIR}/${service}/deployment.yaml"
  echo "Checking deploy/k8s/${service}/deployment.yaml"

  if [[ ! -f "${deployment}" ]]; then
    echo "  ERROR: missing deployment manifest"
    failures=$((failures + 1))
    continue
  fi

  if ! grep -q "strategy:" "${deployment}" || ! grep -q "type: RollingUpdate" "${deployment}"; then
    echo "  ERROR: missing rolling update strategy"
    failures=$((failures + 1))
  fi

  if ! grep -q "maxUnavailable: 0" "${deployment}"; then
    echo "  ERROR: missing maxUnavailable: 0"
    failures=$((failures + 1))
  fi

  if ! grep -q "maxSurge: 1" "${deployment}"; then
    echo "  ERROR: missing maxSurge: 1"
    failures=$((failures + 1))
  fi

  if ! grep -q "startupProbe:" "${deployment}"; then
    echo "  ERROR: missing startupProbe"
    failures=$((failures + 1))
  fi

  if ! grep -q "failureThreshold: 30" "${deployment}"; then
    echo "  ERROR: missing startupProbe failureThreshold: 30"
    failures=$((failures + 1))
  fi

  if ! grep -q "periodSeconds: 10" "${deployment}"; then
    echo "  ERROR: missing startupProbe periodSeconds: 10"
    failures=$((failures + 1))
  fi

  if ! grep -q "path: /actuator/health" "${deployment}"; then
    echo "  ERROR: startup/readiness/liveness health path missing"
    failures=$((failures + 1))
  fi
done

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: rollout hygiene is complete for critical services."
