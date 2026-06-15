#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_DIR="${ROOT_DIR}/deploy/k8s"

failures=0

echo "Validating Kubernetes deployment baseline in ${DEPLOY_DIR}"

while IFS= read -r deployment; do
  echo "Checking ${deployment#${ROOT_DIR}/}"

  if ! grep -q "readinessProbe:" "${deployment}"; then
    echo "  ERROR: missing readinessProbe"
    failures=$((failures + 1))
  fi

  if ! grep -q "livenessProbe:" "${deployment}"; then
    echo "  ERROR: missing livenessProbe"
    failures=$((failures + 1))
  fi

  if ! grep -q "path: /actuator/health" "${deployment}"; then
    echo "  ERROR: missing /actuator/health probe path"
    failures=$((failures + 1))
  fi

  if ! grep -q "resources:" "${deployment}"; then
    echo "  ERROR: missing resources block"
    failures=$((failures + 1))
  fi

  if ! grep -q "requests:" "${deployment}"; then
    echo "  ERROR: missing resources.requests"
    failures=$((failures + 1))
  fi

  if ! grep -q "limits:" "${deployment}"; then
    echo "  ERROR: missing resources.limits"
    failures=$((failures + 1))
  fi

  if ! grep -q "cpu:" "${deployment}"; then
    echo "  ERROR: missing cpu resource values"
    failures=$((failures + 1))
  fi

  if ! grep -q "memory:" "${deployment}"; then
    echo "  ERROR: missing memory resource values"
    failures=$((failures + 1))
  fi
done < <(find "${DEPLOY_DIR}" -mindepth 2 -maxdepth 2 -name deployment.yaml | sort)

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: Kubernetes deployment baseline is complete."
