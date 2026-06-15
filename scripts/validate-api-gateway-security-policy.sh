#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SECURITY_CONFIG="${ROOT_DIR}/services/api-gateway/src/main/java/com/healthcare/gateway/security/SecurityConfig.java"

if [[ ! -f "${SECURITY_CONFIG}" ]]; then
  echo "Gateway security config not found at ${SECURITY_CONFIG}" >&2
  exit 1
fi

echo "Validating API gateway security policy"

required_fragments=(
  '.pathMatchers("/actuator/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()'
  '.pathMatchers("/bff/**").hasAnyRole(HUMAN_ROLES)'
  '.pathMatchers("/api/patients", "/api/patients/**").hasAnyRole(HUMAN_ROLES)'
  '.pathMatchers("/api/consents", "/api/consents/**").hasAnyRole(HUMAN_ROLES)'
  '.pathMatchers("/api/appointments", "/api/appointments/**").hasAnyRole(HUMAN_ROLES)'
  '.pathMatchers("/api/careplans", "/api/careplans/**").hasAnyRole(HUMAN_ROLES)'
  '.pathMatchers("/api/medical-records", "/api/medical-records/**").hasAnyRole(HUMAN_ROLES)'
  '.pathMatchers("/api/notifications", "/api/notifications/**").hasAnyRole("PATIENT", "DOCTOR", "COORDINATOR", "ADMIN", "SYSTEM_INTEGRATION")'
  '.pathMatchers("/api/telemetry", "/api/telemetry/**").hasAnyRole("PATIENT", "DOCTOR", "COORDINATOR", "ADMIN", "SYSTEM_INTEGRATION")'
  '.pathMatchers("/api/alerts", "/api/alerts/**").hasAnyRole("PATIENT", "DOCTOR", "COORDINATOR", "ADMIN", "SYSTEM_INTEGRATION")'
  '.pathMatchers("/api/devices/events", "/api/devices/events/**").hasAnyRole("SYSTEM_INTEGRATION", "DEVICE_IDENTITY", "ADMIN")'
  '.pathMatchers("/api/identity/assertions", "/api/identity/assertions/**").hasAnyRole("SYSTEM_INTEGRATION", "ADMIN")'
  '.pathMatchers("/api/servicebus/messages", "/api/servicebus/messages/**").hasAnyRole("SYSTEM_INTEGRATION", "COORDINATOR", "ADMIN")'
  '.pathMatchers("/api/**").hasAnyRole(CLINICAL_ROLES)'
  '.anyExchange().authenticated()'
  'configuration.setExposedHeaders(List.of("X-Correlation-Id"));'
)

failed=0
for expected in "${required_fragments[@]}"; do
  if ! grep -Fq "${expected}" "${SECURITY_CONFIG}"; then
    echo "Missing gateway security policy fragment: ${expected}" >&2
    failed=1
  fi
done

if [[ ${failed} -eq 1 ]]; then
  exit 1
fi

echo "Validation passed: API gateway security policy covers route-level authorization and correlation header exposure."
