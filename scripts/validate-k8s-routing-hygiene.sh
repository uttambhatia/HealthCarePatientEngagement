#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_DIR="${ROOT_DIR}/deploy/k8s"

failures=0

echo "Validating Kubernetes service/ingress routing hygiene in ${DEPLOY_DIR}"

extract_first_match() {
  local pattern="$1"
  local file="$2"
  grep -m1 -E "${pattern}" "${file}" | sed -E "s/.*${pattern}.*/\1/"
}

while IFS= read -r deployment; do
  service_file="$(dirname "${deployment}")/service.yaml"
  rel_deploy="${deployment#${ROOT_DIR}/}"
  rel_service="${service_file#${ROOT_DIR}/}"

  echo "Checking ${rel_deploy}"

  if [[ ! -f "${service_file}" ]]; then
    echo "  ERROR: missing companion service manifest ${rel_service}"
    failures=$((failures + 1))
    continue
  fi

  deploy_app="$(extract_first_match 'app:[[:space:]]*([^[:space:]]+)' "${deployment}")"
  service_name="$(extract_first_match 'name:[[:space:]]*([^[:space:]]+)' "${service_file}")"
  service_selector="$(extract_first_match 'app:[[:space:]]*([^[:space:]]+)' "${service_file}")"
  container_port="$(extract_first_match 'containerPort:[[:space:]]*([0-9]+)' "${deployment}")"
  target_port="$(extract_first_match 'targetPort:[[:space:]]*([0-9]+)' "${service_file}")"
  service_port="$(extract_first_match 'port:[[:space:]]*([0-9]+)' "${service_file}")"

  if [[ -z "${deploy_app}" || -z "${service_selector}" ]]; then
    echo "  ERROR: missing deployment/service app labels"
    failures=$((failures + 1))
  elif [[ "${deploy_app}" != "${service_selector}" ]]; then
    echo "  ERROR: selector mismatch (deployment app=${deploy_app}, service selector=${service_selector})"
    failures=$((failures + 1))
  fi

  if [[ -z "${container_port}" || -z "${target_port}" ]]; then
    echo "  ERROR: missing containerPort/targetPort"
    failures=$((failures + 1))
  elif [[ "${container_port}" != "${target_port}" ]]; then
    echo "  ERROR: port mismatch (containerPort=${container_port}, targetPort=${target_port})"
    failures=$((failures + 1))
  fi

  if [[ -z "${service_name}" ]]; then
    echo "  ERROR: missing service metadata.name"
    failures=$((failures + 1))
  fi

  if [[ -z "${service_port}" ]]; then
    echo "  ERROR: missing service spec.ports[].port"
    failures=$((failures + 1))
  fi
done < <(find "${DEPLOY_DIR}" -mindepth 2 -maxdepth 2 -name deployment.yaml | sort)

ingress_file="${DEPLOY_DIR}/api-gateway/ingress.yaml"
service_file="${DEPLOY_DIR}/api-gateway/service.yaml"
if [[ ! -f "${ingress_file}" ]]; then
  echo "ERROR: missing ${ingress_file#${ROOT_DIR}/}"
  failures=$((failures + 1))
else
  echo "Checking ${ingress_file#${ROOT_DIR}/}"
  ingress_service="$(extract_first_match 'name:[[:space:]]*([^[:space:]]+)' "${ingress_file}")"
  ingress_service_port="$(extract_first_match 'number:[[:space:]]*([0-9]+)' "${ingress_file}")"
  gateway_service_name="$(extract_first_match 'name:[[:space:]]*([^[:space:]]+)' "${service_file}")"
  gateway_service_port="$(extract_first_match 'port:[[:space:]]*([0-9]+)' "${service_file}")"

  if [[ "${ingress_service}" != "${gateway_service_name}" ]]; then
    echo "  ERROR: ingress backend service mismatch (${ingress_service} vs ${gateway_service_name})"
    failures=$((failures + 1))
  fi

  if [[ "${ingress_service_port}" != "${gateway_service_port}" ]]; then
    echo "  ERROR: ingress backend port mismatch (${ingress_service_port} vs ${gateway_service_port})"
    failures=$((failures + 1))
  fi

  if ! grep -q 'pathType:[[:space:]]*Prefix' "${ingress_file}"; then
    echo "  ERROR: ingress pathType Prefix is required"
    failures=$((failures + 1))
  fi
fi

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: service and ingress routing hygiene is complete."
