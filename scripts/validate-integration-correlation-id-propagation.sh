#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Validating integration adapter correlation-id propagation contract tests"

test_files=(
  "${ROOT_DIR}/services/svc-patient/src/test/java/com/healthcare/patient/integration/PatientFhirAdapterContractTest.java"
  "${ROOT_DIR}/services/svc-event-messaging/src/test/java/com/healthcare/eventmessaging/integration/ServiceBusAdapterContractTest.java"
  "${ROOT_DIR}/services/svc-medical-record/src/test/java/com/healthcare/medicalrecord/integration/FhirAdapterContractTest.java"
  "${ROOT_DIR}/services/svc-careplan/src/test/java/com/healthcare/careplan/integration/CarePlanFhirAdapterContractTest.java"
)

failed=0
for file in "${test_files[@]}"; do
  if [[ ! -f "${file}" ]]; then
    echo "Missing required contract test file: ${file}" >&2
    failed=1
    continue
  fi

  if ! grep -Fq 'import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;' "${file}"; then
    echo "Missing header matcher import in ${file}" >&2
    failed=1
  fi

  if ! grep -Fq '.andExpect(header("X-Correlation-Id", "corr-123"))' "${file}"; then
    echo "Missing X-Correlation-Id assertion in ${file}" >&2
    failed=1
  fi
done

if [[ ${failed} -eq 1 ]]; then
  exit 1
fi

echo "Validation passed: integration adapter contract tests enforce X-Correlation-Id propagation."
