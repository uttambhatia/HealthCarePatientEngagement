#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTRACT_FILE="${ROOT_DIR}/contracts/care-coordination-platform-openapi.yaml"

echo "Validating correlation-id contract and runtime propagation"

required_contract_fragments=(
  'CorrelationIdHeader:'
  'name: X-Correlation-Id'
  'in: header'
  'description: Correlation identifier propagated across the platform.'
  'CorrelationIdResponseHeader:'
  "\$ref: '#/components/headers/CorrelationIdResponseHeader'"
)

required_response_components=(
  'BadRequestResponse:'
  'NotFoundResponse:'
  'ConflictResponse:'
  'InternalServerErrorResponse:'
)

required_path_item_fragments=(
  '/patients:'
  '/patients/{id}:'
  '/appointments/available-slots:'
  '/appointments:'
  '/appointments/{id}:'
  '/appointments/{id}/teleconsult/start:'
  '/appointments/{id}/teleconsult/join:'
  '/appointments/{id}/teleconsult/complete:'
  '/careplans:'
  '/careplans/{id}:'
  '/careplans/responsibility/{patientId}:'
  '/consents:'
  '/consents/{id}:'
  '/consents/history:'
  '/consents/check-access:'
  '/notifications:'
  '/notifications/{id}:'
  '/servicebus/messages:'
  '/servicebus/messages/{id}:'
  '/alerts:'
  '/alerts/{id}:'
  '/telemetry/by-patient/{patientId}:'
  '/telemetry/metric-types:'
  '/telemetry:'
  '/telemetry/{id}:'
  '/medical-records:'
  '/medical-records/{id}:'
  '/devices/events:'
  '/devices/events/{id}:'
  '/identity/assertions:'
  '/identity/assertions/{id}:'
)

filter_fragments=(
  'getHeader("X-Correlation-Id")'
  'addHeader("X-Correlation-Id", correlationId)'
)

web_filter_fragments=(
  'getHeaders().add("X-Correlation-Id", correlationId)'
  'mutate().header("X-Correlation-Id", correlationId)'
)

adapter_fragments=(
  'header("X-Correlation-Id", correlationId)'
)

gateway_security_fragments=(
  'configuration.setExposedHeaders(List.of("X-Correlation-Id"));'
)

failed=0

if [[ ! -f "${CONTRACT_FILE}" ]]; then
  echo "OpenAPI contract not found at ${CONTRACT_FILE}" >&2
  exit 1
fi

for expected in "${required_contract_fragments[@]}"; do
  if ! grep -Fq "${expected}" "${CONTRACT_FILE}"; then
    echo "Missing OpenAPI correlation-id contract fragment: ${expected}" >&2
    failed=1
  fi
done

for expected in "${required_response_components[@]}"; do
  if ! grep -Fq "${expected}" "${CONTRACT_FILE}"; then
    echo "Missing OpenAPI standard error response component: ${expected}" >&2
    failed=1
  fi
done

for expected in "${required_path_item_fragments[@]}"; do
  if ! grep -Fq "${expected}" "${CONTRACT_FILE}"; then
    echo "Missing OpenAPI path item for correlation-id contract coverage: ${expected}" >&2
    failed=1
  fi
done

path_item_count=$(grep -E '^[[:space:]]{2}/' "${CONTRACT_FILE}" | wc -l | tr -d '[:space:]')
header_ref_count=$(grep -F -- "- \$ref: '#/components/parameters/CorrelationIdHeader'" "${CONTRACT_FILE}" | wc -l | tr -d '[:space:]')

if [[ "${path_item_count}" != "${header_ref_count}" ]]; then
  echo "Expected every path item to reference CorrelationIdHeader (${path_item_count} paths, ${header_ref_count} references)." >&2
  failed=1
fi

success_response_count=$(grep -E "^[[:space:]]*'20[01]':" "${CONTRACT_FILE}" | wc -l | tr -d '[:space:]')
total_response_header_ref_count=$(grep -F "\$ref: '#/components/headers/CorrelationIdResponseHeader'" "${CONTRACT_FILE}" | wc -l | tr -d '[:space:]')
expected_response_header_ref_count=$((success_response_count + 4))

if [[ "${total_response_header_ref_count}" != "${expected_response_header_ref_count}" ]]; then
  echo "Expected CorrelationIdResponseHeader refs to cover all 2xx responses and 4 standard error responses (${expected_response_header_ref_count} expected, ${total_response_header_ref_count} found)." >&2
  failed=1
fi

while IFS= read -r file; do
  rel_file="${file#${ROOT_DIR}/}"

  case "${rel_file}" in
    *"/utils/CorrelationIdFilter.java"|*"/utils/CorrelationIdWebFilter.java")
      if [[ "${rel_file}" == *"CorrelationIdWebFilter.java" ]]; then
        for expected in "${web_filter_fragments[@]}"; do
          if ! grep -Fq "${expected}" "${file}"; then
            echo "Missing runtime correlation-id propagation fragment in ${rel_file}: ${expected}" >&2
            failed=1
          fi
        done
      else
        for expected in "${filter_fragments[@]}"; do
          if ! grep -Fq "${expected}" "${file}"; then
            echo "Missing runtime correlation-id propagation fragment in ${rel_file}: ${expected}" >&2
            failed=1
          fi
        done
      fi
      ;;
    *"/integration/"*"Adapter.java")
      for expected in "${adapter_fragments[@]}"; do
        if ! grep -Fq "${expected}" "${file}"; then
          echo "Missing runtime correlation-id propagation fragment in ${rel_file}: ${expected}" >&2
          failed=1
        fi
      done
      ;;
    "services/api-gateway/src/main/java/com/healthcare/gateway/security/SecurityConfig.java")
      for expected in "${gateway_security_fragments[@]}"; do
        if ! grep -Fq "${expected}" "${file}"; then
          echo "Missing runtime correlation-id propagation fragment in ${rel_file}: ${expected}" >&2
          failed=1
        fi
      done
      ;;
  esac
done < <(find "${ROOT_DIR}/services" -type f \( \
  -path '*/utils/CorrelationIdFilter.java' -o \
  -path '*/utils/CorrelationIdWebFilter.java' -o \
  -path '*/integration/*Adapter.java' -o \
  -path '*/api-gateway/src/main/java/com/healthcare/gateway/security/SecurityConfig.java' \
\) | sort)

if [[ ${failed} -eq 1 ]]; then
  exit 1
fi

echo "Validation passed: correlation-id contract and runtime propagation are aligned."
