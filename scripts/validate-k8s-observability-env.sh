#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_DIR="${ROOT_DIR}/deploy/k8s"

required_services=(
  "api-gateway"
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

failures=0

echo "Validating Kubernetes observability env wiring"

for service in "${required_services[@]}"; do
  deployment="${DEPLOY_DIR}/${service}/deployment.yaml"
  echo "Checking deploy/k8s/${service}/deployment.yaml"

  if [[ ! -f "${deployment}" ]]; then
    echo "  ERROR: missing deployment manifest"
    failures=$((failures + 1))
    continue
  fi

  if ! grep -q "name: OTEL_EXPORTER_OTLP_ENDPOINT" "${deployment}"; then
    echo "  ERROR: missing OTEL_EXPORTER_OTLP_ENDPOINT"
    failures=$((failures + 1))
  fi

  if ! grep -q "key: otel-otlp-endpoint" "${deployment}"; then
    echo "  ERROR: missing otel-otlp-endpoint secret key reference"
    failures=$((failures + 1))
  fi
done

secret_template="${DEPLOY_DIR}/platform-secrets.template.yaml"
if [[ ! -f "${secret_template}" ]]; then
  echo "ERROR: missing deploy/k8s/platform-secrets.template.yaml"
  failures=$((failures + 1))
elif ! grep -q "otel-otlp-endpoint:" "${secret_template}"; then
  echo "ERROR: platform-secrets template missing otel-otlp-endpoint"
  failures=$((failures + 1))
fi

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: observability env wiring is complete."
