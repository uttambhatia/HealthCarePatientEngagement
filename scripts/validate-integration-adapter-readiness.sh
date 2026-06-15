#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

failures=0

check_file_contains() {
  local file="$1"
  local pattern="$2"
  local message="$3"

  if ! grep -Fq "$pattern" "$file"; then
    echo "FAIL: ${message}" >&2
    failures=$((failures + 1))
  fi
}

check_deployment_env_secret() {
  local deployment_file="$1"
  local env_name="$2"
  local secret_key="$3"

  grep -Fq "name: ${env_name}" "$deployment_file" && grep -Fq "key: ${secret_key}" "$deployment_file"
}

expect_deployment_wiring() {
  local deployment_file="$1"
  local env_name="$2"
  local secret_key="$3"

  if ! check_deployment_env_secret "$deployment_file" "$env_name" "$secret_key"; then
    echo "FAIL: Missing deployment wiring ${env_name} -> platform-secrets/${secret_key} in ${deployment_file#${ROOT_DIR}/}" >&2
    failures=$((failures + 1))
  fi
}

echo "Validating integration adapter readiness (FHIR + Service Bus + health exposure)"

platform_secrets_file="${ROOT_DIR}/deploy/k8s/platform-secrets.template.yaml"
check_file_contains "$platform_secrets_file" 'fhir-integration-base-url:' "platform-secrets template missing fhir-integration-base-url"
check_file_contains "$platform_secrets_file" 'service-bus-integration-base-url:' "platform-secrets template missing service-bus-integration-base-url"

# FHIR adapter services
for svc in svc-patient svc-careplan svc-medical-record; do
  app_file="${ROOT_DIR}/services/${svc}/src/main/resources/application.yml"
  deploy_file="${ROOT_DIR}/deploy/k8s/${svc}/deployment.yaml"

  check_file_contains "$app_file" 'include: health,info' "${svc} missing management health/info exposure"
  check_file_contains "$app_file" 'fhir:' "${svc} missing platform.integration.fhir section"
  check_file_contains "$app_file" 'base-url: ${FHIR_INTEGRATION_BASE_URL:' "${svc} missing FHIR_INTEGRATION_BASE_URL mapping in application.yml"
  check_file_contains "$app_file" 'path: ${FHIR_INTEGRATION_PATH:' "${svc} missing FHIR_INTEGRATION_PATH mapping in application.yml"

  expect_deployment_wiring "$deploy_file" "FHIR_INTEGRATION_BASE_URL" "fhir-integration-base-url"
done

# Service Bus adapter service
event_app_file="${ROOT_DIR}/services/svc-event-messaging/src/main/resources/application.yml"
event_deploy_file="${ROOT_DIR}/deploy/k8s/svc-event-messaging/deployment.yaml"

check_file_contains "$event_app_file" 'include: health,info' "svc-event-messaging missing management health/info exposure"
check_file_contains "$event_app_file" 'service-bus:' "svc-event-messaging missing platform.integration.service-bus section"
check_file_contains "$event_app_file" 'base-url: ${SERVICE_BUS_INTEGRATION_BASE_URL:' "svc-event-messaging missing SERVICE_BUS_INTEGRATION_BASE_URL mapping in application.yml"
check_file_contains "$event_app_file" 'path: ${SERVICE_BUS_INTEGRATION_PATH:' "svc-event-messaging missing SERVICE_BUS_INTEGRATION_PATH mapping in application.yml"

expect_deployment_wiring "$event_deploy_file" "SERVICE_BUS_INTEGRATION_BASE_URL" "service-bus-integration-base-url"

if [[ $failures -gt 0 ]]; then
  echo "Validation failed: integration adapter readiness has ${failures} issue(s)." >&2
  exit 1
fi

echo "Validation passed: integration adapter readiness is complete."
