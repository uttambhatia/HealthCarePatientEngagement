#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT_DIR}"

echo "Running integration adapter contract tests"

mvn -pl services/svc-patient -am -Dtest=PatientFhirAdapterContractTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl services/svc-event-messaging -am -Dtest=ServiceBusAdapterContractTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl services/svc-medical-record -am -Dtest=FhirAdapterContractTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl services/svc-careplan -am -Dtest=CarePlanFhirAdapterContractTest -Dsurefire.failIfNoSpecifiedTests=false test

echo "Integration adapter contract tests passed."
