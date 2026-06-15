#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_FILE="${1:-${ROOT_DIR}/guardrail-report.txt}"

checks=(
  "validate-k8s-security-env.sh"
  "validate-k8s-deployment-baseline.sh"
  "validate-k8s-routing-hygiene.sh"
  "validate-k8s-networkpolicy-hygiene.sh"
  "validate-k8s-resilience-hygiene.sh"
  "validate-k8s-scheduling-hygiene.sh"
  "validate-k8s-observability-env.sh"
  "validate-k8s-runtime-security-hygiene.sh"
  "validate-k8s-rollout-hygiene.sh"
  "validate-k8s-azure-sql-env.sh"
  "validate-k8s-private-endpoint-env.sh"
  "validate-integration-adapter-readiness.sh"
  "validate-integration-reliability-hygiene.sh"
  "validate-integration-adapter-contract-tests.sh"
  "validate-integration-correlation-id-propagation.sh"
  "validate-api-gateway-route-coverage.sh"
  "validate-api-gateway-security-policy.sh"
  "validate-correlation-id-contract.sh"
  "validate-openapi-controller-coverage.sh"
  "validate-openapi-operation-parity.sh"
  "validate-dr-readiness-artifacts.sh"
  "validate-dr-evidence-freshness.sh"
)

failures=0
mkdir -p "$(dirname "${REPORT_FILE}")"
: > "${REPORT_FILE}"

echo "Running infrastructure and DR guardrails" | tee -a "${REPORT_FILE}"

for check in "${checks[@]}"; do
  echo "" | tee -a "${REPORT_FILE}"
  echo "==> ${check}" | tee -a "${REPORT_FILE}"
  if bash "${ROOT_DIR}/scripts/${check}" >> "${REPORT_FILE}" 2>&1; then
    echo "PASS: ${check}" | tee -a "${REPORT_FILE}"
  else
    echo "FAIL: ${check}" | tee -a "${REPORT_FILE}"
    failures=$((failures + 1))
  fi
done

echo "" | tee -a "${REPORT_FILE}"
if [[ ${failures} -gt 0 ]]; then
  echo "Guardrail run failed with ${failures} failing check(s)." | tee -a "${REPORT_FILE}"
  exit 1
fi

echo "Guardrail run passed." | tee -a "${REPORT_FILE}"
