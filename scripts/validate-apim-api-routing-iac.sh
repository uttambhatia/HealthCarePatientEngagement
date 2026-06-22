#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
main_bicep="${ROOT_DIR}/deploy/azure/main.bicep"
module_bicep="${ROOT_DIR}/deploy/azure/11-apim-api-routing.bicep"
policy_xml="${ROOT_DIR}/deploy/azure/apim/api-routing-policy.xml"

failures=0

echo "Validating APIM API routing IaC wiring"

if [[ ! -f "${module_bicep}" ]]; then
  echo "ERROR: missing deploy/azure/11-apim-api-routing.bicep"
  failures=$((failures + 1))
fi

if [[ ! -f "${policy_xml}" ]]; then
  echo "ERROR: missing deploy/azure/apim/api-routing-policy.xml"
  failures=$((failures + 1))
fi

if [[ -f "${main_bicep}" ]]; then
  if ! grep -q "module apimApiRouting './11-apim-api-routing.bicep'" "${main_bicep}"; then
    echo "ERROR: main.bicep must include APIM API routing module"
    failures=$((failures + 1))
  fi
else
  echo "ERROR: missing deploy/azure/main.bicep"
  failures=$((failures + 1))
fi

if [[ -f "${module_bicep}" ]]; then
  if ! grep -q "backend-primary-appgw" "${module_bicep}"; then
    echo "ERROR: APIM API routing module must define primary backend"
    failures=$((failures + 1))
  fi
  if ! grep -q "backend-secondary-appgw" "${module_bicep}"; then
    echo "ERROR: APIM API routing module must define secondary backend"
    failures=$((failures + 1))
  fi
fi

if [[ -f "${policy_xml}" ]]; then
  if ! grep -q '<set-backend-service backend-id="backend-primary-appgw"' "${policy_xml}"; then
    echo "ERROR: routing policy must route to primary backend"
    failures=$((failures + 1))
  fi
  if ! grep -q '<set-backend-service backend-id="backend-secondary-appgw"' "${policy_xml}"; then
    echo "ERROR: routing policy must route to secondary backend"
    failures=$((failures + 1))
  fi
fi

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: APIM API routing IaC is present and wired."
