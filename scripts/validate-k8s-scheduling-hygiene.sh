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

echo "Validating Kubernetes scheduling hygiene (anti-affinity + spread constraints)"

for service in "${critical_services[@]}"; do
  deployment="${DEPLOY_DIR}/${service}/deployment.yaml"
  echo "Checking deploy/k8s/${service}/deployment.yaml"

  if [[ ! -f "${deployment}" ]]; then
    echo "  ERROR: missing deployment manifest"
    failures=$((failures + 1))
    continue
  fi

  if ! grep -q "podAntiAffinity:" "${deployment}"; then
    echo "  ERROR: missing podAntiAffinity"
    failures=$((failures + 1))
  fi

  if ! grep -q "requiredDuringSchedulingIgnoredDuringExecution:" "${deployment}"; then
    echo "  ERROR: missing required podAntiAffinity rule"
    failures=$((failures + 1))
  fi

  if ! grep -q "topologyKey: kubernetes.io/hostname" "${deployment}"; then
    echo "  ERROR: missing node-level anti-affinity topology key"
    failures=$((failures + 1))
  fi

  if ! grep -q "topologySpreadConstraints:" "${deployment}"; then
    echo "  ERROR: missing topologySpreadConstraints"
    failures=$((failures + 1))
  fi

  if ! grep -q "topologyKey: topology.kubernetes.io/zone" "${deployment}"; then
    echo "  ERROR: missing zone topology spread key"
    failures=$((failures + 1))
  fi

  if ! grep -q "whenUnsatisfiable: ScheduleAnyway" "${deployment}"; then
    echo "  ERROR: missing spread unsatisfiable behavior"
    failures=$((failures + 1))
  fi

  if ! grep -q "app: ${service}" "${deployment}"; then
    echo "  ERROR: scheduling selectors do not reference service label ${service}"
    failures=$((failures + 1))
  fi
done

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: scheduling hygiene is complete for critical services."
