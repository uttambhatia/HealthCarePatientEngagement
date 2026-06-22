#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
main_bicep="${ROOT_DIR}/deploy/azure/main.bicep"
apim_bicep="${ROOT_DIR}/deploy/azure/10-apim.bicep"
policy_xml="${ROOT_DIR}/deploy/azure/apim/global-policy.xml"

failures=0

echo "Validating APIM hardening IaC wiring"

if [[ ! -f "${apim_bicep}" ]]; then
  echo "ERROR: missing deploy/azure/10-apim.bicep"
  failures=$((failures + 1))
fi

if [[ ! -f "${policy_xml}" ]]; then
  echo "ERROR: missing deploy/azure/apim/global-policy.xml"
  failures=$((failures + 1))
fi

if [[ -f "${main_bicep}" ]]; then
  if ! grep -q "module apim './10-apim.bicep'" "${main_bicep}"; then
    echo "ERROR: main.bicep must include module apim './10-apim.bicep'"
    failures=$((failures + 1))
  fi
else
  echo "ERROR: missing deploy/azure/main.bicep"
  failures=$((failures + 1))
fi

if [[ -f "${apim_bicep}" ]]; then
  if ! grep -q "loadTextContent('./apim/global-policy.xml')" "${apim_bicep}"; then
    echo "ERROR: APIM module must load global policy XML"
    failures=$((failures + 1))
  fi
fi

if [[ -f "${policy_xml}" ]]; then
  if ! grep -q '<validate-jwt' "${policy_xml}"; then
    echo "ERROR: APIM global policy must include validate-jwt"
    failures=$((failures + 1))
  fi
  if ! grep -q '<rate-limit-by-key' "${policy_xml}"; then
    echo "ERROR: APIM global policy must include rate-limit-by-key"
    failures=$((failures + 1))
  fi
fi

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: APIM hardening IaC is present and wired."
