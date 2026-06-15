#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_DIR="${ROOT_DIR}/deploy/k8s"

failures=0

echo "Validating Kubernetes runtime security hygiene"

while IFS= read -r deployment; do
  rel="${deployment#${ROOT_DIR}/}"
  echo "Checking ${rel}"

  if ! grep -q "securityContext:" "${deployment}"; then
    echo "  ERROR: missing securityContext blocks"
    failures=$((failures + 1))
    continue
  fi

  if ! grep -q "runAsNonRoot: true" "${deployment}"; then
    echo "  ERROR: missing pod securityContext runAsNonRoot: true"
    failures=$((failures + 1))
  fi

  if ! grep -q "seccompProfile:" "${deployment}" || ! grep -q "type: RuntimeDefault" "${deployment}"; then
    echo "  ERROR: missing pod seccompProfile RuntimeDefault"
    failures=$((failures + 1))
  fi

  if ! grep -q "allowPrivilegeEscalation: false" "${deployment}"; then
    echo "  ERROR: missing container allowPrivilegeEscalation: false"
    failures=$((failures + 1))
  fi

  if ! grep -q "readOnlyRootFilesystem: true" "${deployment}"; then
    echo "  ERROR: missing container readOnlyRootFilesystem: true"
    failures=$((failures + 1))
  fi

  if ! grep -q "drop:" "${deployment}" || ! grep -q -- "- ALL" "${deployment}"; then
    echo "  ERROR: missing capabilities drop ALL"
    failures=$((failures + 1))
  fi
done < <(find "${DEPLOY_DIR}" -mindepth 2 -maxdepth 2 -name deployment.yaml | sort)

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: runtime security hygiene is complete."
