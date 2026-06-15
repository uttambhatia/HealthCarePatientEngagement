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

echo "Validating Kubernetes resilience hygiene (HPA + PDB) in ${DEPLOY_DIR}"

for service in "${critical_services[@]}"; do
  deployment="${DEPLOY_DIR}/${service}/deployment.yaml"
  hpa="${DEPLOY_DIR}/${service}/hpa.yaml"
  pdb="${DEPLOY_DIR}/${service}/pdb.yaml"

  echo "Checking deploy/k8s/${service}"

  if [[ ! -f "${deployment}" ]]; then
    echo "  ERROR: missing deployment manifest"
    failures=$((failures + 1))
    continue
  fi

  if [[ ! -f "${hpa}" ]]; then
    echo "  ERROR: missing hpa.yaml"
    failures=$((failures + 1))
  else
    if ! grep -q "kind: HorizontalPodAutoscaler" "${hpa}"; then
      echo "  ERROR: hpa kind missing"
      failures=$((failures + 1))
    fi
    if ! grep -q "name: ${service}" "${hpa}"; then
      echo "  ERROR: hpa target name mismatch"
      failures=$((failures + 1))
    fi
    if ! grep -q "minReplicas:" "${hpa}" || ! grep -q "maxReplicas:" "${hpa}"; then
      echo "  ERROR: hpa replica bounds missing"
      failures=$((failures + 1))
    fi
    if ! grep -q "name: cpu" "${hpa}"; then
      echo "  ERROR: hpa cpu metric missing"
      failures=$((failures + 1))
    fi
  fi

  if [[ ! -f "${pdb}" ]]; then
    echo "  ERROR: missing pdb.yaml"
    failures=$((failures + 1))
  else
    if ! grep -q "kind: PodDisruptionBudget" "${pdb}"; then
      echo "  ERROR: pdb kind missing"
      failures=$((failures + 1))
    fi
    if ! grep -q "minAvailable:" "${pdb}"; then
      echo "  ERROR: pdb minAvailable missing"
      failures=$((failures + 1))
    fi
    if ! grep -q "app: ${service}" "${pdb}"; then
      echo "  ERROR: pdb selector mismatch"
      failures=$((failures + 1))
    fi
  fi
done

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: resilience hygiene is complete for critical services."
