#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_DIR="${ROOT_DIR}/deploy/k8s"

# api-gateway has no datasource and is intentionally excluded.
database_services=(
  "svc-patient:PATIENT"
  "svc-appointment:APPOINTMENT"
  "svc-careplan:CAREPLAN"
  "svc-consent:CONSENT"
  "svc-medical-record:MEDICAL_RECORD"
  "svc-notification:NOTIFICATION"
  "svc-telemetry:TELEMETRY"
  "svc-device-ingestion:DEVICE_INGESTION"
  "svc-alert-management:ALERT_MANAGEMENT"
  "svc-identity-adapter:IDENTITY_ADAPTER"
  "svc-event-messaging:EVENT_MESSAGING"
)

required_secret_keys=(
  "azure-sql-jdbc-url"
  "azure-sql-username"
  "azure-sql-password"
  "azure-managed-identity-client-id"
)

failures=0

echo "Validating Azure SQL + managed identity environment wiring in ${DEPLOY_DIR}"

for entry in "${database_services[@]}"; do
  service_dir="${entry%%:*}"
  prefix="${entry##*:}"
  deployment="${DEPLOY_DIR}/${service_dir}/deployment.yaml"

  echo "Checking ${deployment#${ROOT_DIR}/}"

  if [[ ! -f "${deployment}" ]]; then
    echo "  ERROR: missing deployment manifest"
    failures=$((failures + 1))
    continue
  fi

  for env_name in "${prefix}_DB_URL" "${prefix}_DB_USERNAME" "${prefix}_DB_PASSWORD" "AZURE_CLIENT_ID"; do
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

echo "Validation passed: Azure SQL + managed identity wiring is complete."
