#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_DIR="${ROOT_DIR}/deploy/k8s"

failures=0

extract_first_match() {
  local pattern="$1"
  local file="$2"
  grep -m1 -E "${pattern}" "${file}" | sed -E "s/.*${pattern}.*/\1/"
}

echo "Validating Kubernetes NetworkPolicy hygiene in ${DEPLOY_DIR}"

while IFS= read -r deployment; do
  service_dir="$(dirname "${deployment}")"
  networkpolicy="${service_dir}/networkpolicy.yaml"

  rel_deployment="${deployment#${ROOT_DIR}/}"
  rel_networkpolicy="${networkpolicy#${ROOT_DIR}/}"
  echo "Checking ${rel_deployment}"

  if [[ ! -f "${networkpolicy}" ]]; then
    echo "  ERROR: missing ${rel_networkpolicy}"
    failures=$((failures + 1))
    continue
  fi

  app_name="$(extract_first_match 'app:[[:space:]]*([^[:space:]]+)' "${deployment}")"
  container_port="$(extract_first_match 'containerPort:[[:space:]]*([0-9]+)' "${deployment}")"

  if ! grep -q "kind: NetworkPolicy" "${networkpolicy}"; then
    echo "  ERROR: networkpolicy kind missing"
    failures=$((failures + 1))
  fi

  if ! grep -q "policyTypes:" "${networkpolicy}" || ! grep -q -- "- Ingress" "${networkpolicy}"; then
    echo "  ERROR: policyTypes must include Ingress"
    failures=$((failures + 1))
  fi

  if [[ -n "${app_name}" ]] && ! grep -q "app: ${app_name}" "${networkpolicy}"; then
    echo "  ERROR: podSelector app mismatch (expected ${app_name})"
    failures=$((failures + 1))
  fi

  if [[ -n "${container_port}" ]] && ! grep -q "port: ${container_port}" "${networkpolicy}"; then
    echo "  ERROR: ingress port mismatch (expected ${container_port})"
    failures=$((failures + 1))
  fi

  if [[ "${app_name}" != "api-gateway" ]]; then
    if ! grep -q "app: api-gateway" "${networkpolicy}"; then
      echo "  ERROR: backend policy must allow source app api-gateway"
      failures=$((failures + 1))
    fi
  fi
done < <(find "${DEPLOY_DIR}" -mindepth 2 -maxdepth 2 -name deployment.yaml | sort)

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: Kubernetes NetworkPolicy hygiene is complete."
