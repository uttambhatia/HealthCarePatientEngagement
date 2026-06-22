#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILES=(
  "${ROOT_DIR}/deploy/k8s/env/dev/dev.env.template"
  "${ROOT_DIR}/deploy/k8s/env/prod/prod.env.template"
)

required_vars=(
  "PRIMARY_REGION"
  "SECONDARY_REGION"
  "HUB_VNET_NAME"
  "PRIMARY_VNET_NAME"
  "SECONDARY_VNET_NAME"
  "TRAFFIC_MANAGER_PROFILE_NAME"
  "TRAFFIC_MANAGER_DNS_NAME"
  "PRIMARY_FRONT_DOOR_PROFILE_NAME"
  "PRIMARY_FRONT_DOOR_ENDPOINT_HOST"
  "SECONDARY_FRONT_DOOR_PROFILE_NAME"
  "SECONDARY_FRONT_DOOR_ENDPOINT_HOST"
  "PRIMARY_AKS_CLUSTER_NAME"
  "SECONDARY_AKS_CLUSTER_NAME"
  "PRIMARY_APP_SERVICE_NAME"
  "SECONDARY_APP_SERVICE_NAME"
)

failures=0

echo "Validating hub-spoke and DR environment schema"

for env_file in "${ENV_FILES[@]}"; do
  rel_file="${env_file#${ROOT_DIR}/}"
  echo "Checking ${rel_file}"

  if [[ ! -f "${env_file}" ]]; then
    echo "  ERROR: missing file"
    failures=$((failures + 1))
    continue
  fi

  for var_name in "${required_vars[@]}"; do
    if ! grep -Eq "^${var_name}=" "${env_file}"; then
      echo "  ERROR: missing ${var_name}"
      failures=$((failures + 1))
    fi
  done

  primary_region=$(grep -E '^PRIMARY_REGION=' "${env_file}" | head -n1 | cut -d'=' -f2-)
  secondary_region=$(grep -E '^SECONDARY_REGION=' "${env_file}" | head -n1 | cut -d'=' -f2-)

  if [[ -n "${primary_region}" && -n "${secondary_region}" ]]; then
    if [[ "${primary_region}" == "${secondary_region}" ]]; then
      if [[ "${primary_region}" != *"<"* && "${primary_region}" != *">"* ]]; then
        echo "  ERROR: PRIMARY_REGION and SECONDARY_REGION should differ"
        failures=$((failures + 1))
      fi
    fi
  fi
done

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: hub-spoke environment schema is present."
