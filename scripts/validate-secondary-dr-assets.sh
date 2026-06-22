#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

required_files=(
  "deploy/k8s/env/secondary/apply-secondary.ps1"
  "deploy/k8s/env/secondary/secondary.env.template"
  "deploy/k8s/env/secondary/platform-secrets.secondary.template.yaml"
)

required_env_keys=(
  "NAMESPACE"
  "KUBE_CONTEXT"
  "PRIMARY_REGION"
  "SECONDARY_REGION"
  "TRAFFIC_MANAGER_PROFILE_NAME"
  "TRAFFIC_MANAGER_DNS_NAME"
  "PRIMARY_FRONT_DOOR_ENDPOINT_HOST"
  "SECONDARY_FRONT_DOOR_ENDPOINT_HOST"
  "PRIMARY_AKS_CLUSTER_NAME"
  "SECONDARY_AKS_CLUSTER_NAME"
  "SERVICEBUS_NAMESPACE"
  "EVENTHUB_NAMESPACE"
  "KEY_VAULT_URL"
  "OAUTH2_ISSUER"
  "OAUTH2_AUDIENCE"
  "OAUTH2_JWK_SET_URI"
  "AZURE_SQL_JDBC_URL"
)

required_secret_keys=(
  "servicebus-namespace"
  "eventhub-namespace"
  "key-vault-url"
  "oauth2-issuer"
  "oauth2-audience"
  "oauth2-jwk-set-uri"
  "azure-sql-jdbc-url"
)

failures=0

echo "Validating secondary-region DR deployment assets"

for file in "${required_files[@]}"; do
  if [[ ! -f "${ROOT_DIR}/${file}" ]]; then
    echo "ERROR: missing ${file}"
    failures=$((failures + 1))
  fi
done

env_file="${ROOT_DIR}/deploy/k8s/env/secondary/secondary.env.template"
if [[ -f "${env_file}" ]]; then
  for key in "${required_env_keys[@]}"; do
    if ! grep -Eq "^${key}=" "${env_file}"; then
      echo "ERROR: secondary env template missing ${key}"
      failures=$((failures + 1))
    fi
  done
fi

secret_file="${ROOT_DIR}/deploy/k8s/env/secondary/platform-secrets.secondary.template.yaml"
if [[ -f "${secret_file}" ]]; then
  for key in "${required_secret_keys[@]}"; do
    if ! grep -q "${key}:" "${secret_file}"; then
      echo "ERROR: secondary secret template missing key ${key}"
      failures=$((failures + 1))
    fi
  done
fi

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: secondary-region DR assets are present and complete."
