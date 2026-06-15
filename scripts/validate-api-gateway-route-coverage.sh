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

required_uri_bindings=(
  'uri(patientsUri)'
  'uri(appointmentsUri)'
  'uri(careplansUri)'
  'uri(consentsUri)'
  'uri(medicalRecordsUri)'
  'uri(notificationsUri)'
  'uri(telemetryUri)'
  'uri(deviceEventsUri)'
  'uri(alertsUri)'
  'uri(identityAssertionsUri)'
  'uri(servicebusMessagesUri)'
)

required_uri_properties=(
  '@Value("${platform.routes.patients-uri:http://svc-patient:80}")'
  '@Value("${platform.routes.appointments-uri:http://svc-appointment:80}")'
  '@Value("${platform.routes.careplans-uri:http://svc-careplan:80}")'
  '@Value("${platform.routes.consents-uri:http://svc-consent:80}")'
  '@Value("${platform.routes.medical-records-uri:http://svc-medical-record:80}")'
  '@Value("${platform.routes.notifications-uri:http://svc-notification:80}")'
  '@Value("${platform.routes.telemetry-uri:http://svc-telemetry:80}")'
  '@Value("${platform.routes.device-events-uri:http://svc-device-ingestion:80}")'
  '@Value("${platform.routes.alerts-uri:http://svc-alert-management:80}")'
  '@Value("${platform.routes.identity-assertions-uri:http://svc-identity-adapter:80}")'
  '@Value("${platform.routes.servicebus-messages-uri:http://svc-event-messaging:80}")'
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

for expected in "${required_uri_bindings[@]}"; do
  if ! grep -Fq "${expected}" "${GATEWAY_CONFIG}"; then
    echo "Missing gateway upstream route binding: ${expected}" >&2
    failed=1
  fi
done

for expected in "${required_uri_properties[@]}"; do
  if ! grep -Fq "${expected}" "${GATEWAY_CONFIG}"; then
    echo "Missing gateway route property default: ${expected}" >&2
    failed=1
  fi
done

if [[ ${failed} -eq 1 ]]; then
  exit 1
fi

echo "Validation passed: API gateway route coverage includes all platform service routes."
