#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATEWAY_CONFIG="${ROOT_DIR}/services/api-gateway/src/main/java/com/healthcare/gateway/config/GatewayRoutesConfig.java"

if [[ ! -f "${GATEWAY_CONFIG}" ]]; then
  echo "Gateway route config not found at ${GATEWAY_CONFIG}" >&2
  exit 1
fi

echo "Validating API gateway route coverage"

required_routes=(
  'route("patients"'
  'route("appointments"'
  'route("careplans"'
  'route("consents"'
  'route("medical-records"'
  'route("notifications"'
  'route("telemetry"'
  'route("device-events"'
  'route("alerts"'
  'route("identity-assertions"'
  'route("servicebus-messages"'
)

required_paths=(
  'path("/api/patients", "/api/patients/**")'
  'path("/api/appointments", "/api/appointments/**")'
  'path("/api/careplans", "/api/careplans/**")'
  'path("/api/consents", "/api/consents/**")'
  'path("/api/medical-records", "/api/medical-records/**")'
  'path("/api/notifications", "/api/notifications/**")'
  'path("/api/telemetry", "/api/telemetry/**")'
  'path("/api/devices/events", "/api/devices/events/**")'
  'path("/api/alerts", "/api/alerts/**")'
  'path("/api/identity/assertions", "/api/identity/assertions/**")'
  'path("/api/servicebus/messages", "/api/servicebus/messages/**")'
)

required_uris=(
  'uri("http://localhost:8081")'
  'uri("http://localhost:8082")'
  'uri("http://localhost:8083")'
  'uri("http://localhost:8084")'
  'uri("http://localhost:8085")'
  'uri("http://localhost:8086")'
  'uri("http://localhost:8087")'
  'uri("http://localhost:8088")'
  'uri("http://localhost:8089")'
  'uri("http://localhost:8090")'
  'uri("http://localhost:8091")'
)

failed=0

for expected in "${required_routes[@]}"; do
  if ! grep -Fq "${expected}" "${GATEWAY_CONFIG}"; then
    echo "Missing gateway route declaration: ${expected}" >&2
    failed=1
  fi
done

for expected in "${required_paths[@]}"; do
  if ! grep -Fq "${expected}" "${GATEWAY_CONFIG}"; then
    echo "Missing gateway path mapping: ${expected}" >&2
    failed=1
  fi
done

for expected in "${required_uris[@]}"; do
  if ! grep -Fq "${expected}" "${GATEWAY_CONFIG}"; then
    echo "Missing gateway upstream URI: ${expected}" >&2
    failed=1
  fi
done

if [[ ${failed} -eq 1 ]]; then
  exit 1
fi

echo "Validation passed: API gateway route coverage includes all platform service routes."
