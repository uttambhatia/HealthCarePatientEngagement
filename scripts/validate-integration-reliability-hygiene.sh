#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
failures=0

echo "Validating integration reliability hygiene (retry and DLQ contracts)"

for app_file in "${ROOT_DIR}"/services/svc-*/src/main/resources/application.yml; do
  svc_name="$(basename "$(dirname "$(dirname "${app_file}")")")"

  if ! grep -Fq "retryAttempts:" "${app_file}"; then
    echo "FAIL: ${svc_name} missing platform.messaging.retryAttempts in application.yml" >&2
    failures=$((failures + 1))
  fi

  if ! grep -Fq "deadLetterQueue:" "${app_file}"; then
    echo "FAIL: ${svc_name} missing platform.messaging.deadLetterQueue in application.yml" >&2
    failures=$((failures + 1))
  fi
done

for adapter_file in "${ROOT_DIR}"/services/svc-*/src/main/java/com/healthcare/*/integration/*Adapter.java; do
  [[ -f "${adapter_file}" ]] || continue

  if ! grep -Fq '@Value("${platform.messaging.retryAttempts:3}")' "${adapter_file}"; then
    continue
  fi

  adapter_name="${adapter_file#${ROOT_DIR}/}"

  if ! grep -Fq 'for (int attempt = 1; attempt <= maxAttempts; attempt++)' "${adapter_file}"; then
    echo "FAIL: ${adapter_name} missing bounded retry loop using maxAttempts" >&2
    failures=$((failures + 1))
  fi

  if ! grep -Eq 'throw new [A-Za-z0-9_]+(Exception|Error)\(' "${adapter_file}"; then
    echo "FAIL: ${adapter_name} missing terminal exception throw after retry exhaustion" >&2
    failures=$((failures + 1))
  fi
done

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed: integration reliability hygiene has ${failures} issue(s)." >&2
  exit 1
fi

echo "Validation passed: integration reliability hygiene is complete."
