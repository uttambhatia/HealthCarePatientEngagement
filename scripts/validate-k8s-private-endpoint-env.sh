#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_DIR="${ROOT_DIR}/deploy/k8s"

service_dirs=(
  "svc-patient"
  "svc-appointment"
  "svc-careplan"
  "svc-consent"
  "svc-medical-record"
  "svc-notification"
  "svc-telemetry"
  "svc-device-ingestion"
  "svc-alert-management"
  "svc-identity-adapter"
  "svc-event-messaging"
)

required_env_names=(
  "SERVICEBUS_NAMESPACE"
  "EVENTHUB_NAMESPACE"
  "KEY_VAULT_URL"
)

required_secret_keys=(
  "servicebus-namespace"
  "eventhub-namespace"
  "key-vault-url"
)

failures=0

echo "Validating private-endpoint dependency environment wiring in ${DEPLOY_DIR}"

for service_dir in "${service_dirs[@]}"; do
  deployment="${DEPLOY_DIR}/${service_dir}/deployment.yaml"
  echo "Checking ${deployment#${ROOT_DIR}/}"

  if [[ ! -f "${deployment}" ]]; then
    echo "  ERROR: missing deployment manifest"
    failures=$((failures + 1))
    continue
  fi

  for env_name in "${required_env_names[@]}"; do
    if ! grep -q "name: ${env_name}" "${deployment}"; then
      echo "  ERROR: missing env var ${env_name}"
      failures=$((failures + 1))
    fi
  done

  for secret_key in "${required_secret_keys[@]}"; do
    if ! grep -q "key: ${secret_key}" "${deployment}"; then
      echo "  ERROR: missing secret key reference ${secret_key}"
      failures=$((failures + 1))
    fi
  done
done

SECRET_TEMPLATE="${DEPLOY_DIR}/platform-secrets.template.yaml"
if [[ ! -f "${SECRET_TEMPLATE}" ]]; then
  echo "ERROR: missing ${SECRET_TEMPLATE#${ROOT_DIR}/}"
  failures=$((failures + 1))
else
  echo "Checking ${SECRET_TEMPLATE#${ROOT_DIR}/}"
  for secret_key in "${required_secret_keys[@]}"; do
    if ! grep -q "${secret_key}:" "${SECRET_TEMPLATE}"; then
      echo "  ERROR: secret template missing ${secret_key}"
      failures=$((failures + 1))
    fi
  done
fi

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: private-endpoint dependency wiring is complete."
