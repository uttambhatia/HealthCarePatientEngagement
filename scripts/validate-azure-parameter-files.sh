#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

parameter_files=(
  "deploy/azure/parameters/dev.parameters.json"
  "deploy/azure/parameters/prod.parameters.json"
  "deploy/azure/parameters/secondary.parameters.json"
)

required_keys=(
  "location"
  "secondaryLocation"
  "environment"
  "hubVnetName"
  "primaryVnetName"
  "secondaryVnetName"
  "trafficManagerProfileName"
  "trafficManagerDnsLabel"
  "primaryFrontDoorProfileName"
  "secondaryFrontDoorProfileName"
  "apimServiceName"
  "oauthOpenIdConfigUrl"
  "oauthAudience"
  "apimApiName"
  "primaryApiBackendUrl"
  "secondaryApiBackendUrl"
)

failures=0

echo "Validating environment-specific Azure parameter files"

for rel_file in "${parameter_files[@]}"; do
  file="${ROOT_DIR}/${rel_file}"
  if [[ ! -f "${file}" ]]; then
    echo "ERROR: missing ${rel_file}"
    failures=$((failures + 1))
    continue
  fi

  for key in "${required_keys[@]}"; do
    if ! grep -q "\"${key}\"" "${file}"; then
      echo "ERROR: ${rel_file} missing parameter ${key}"
      failures=$((failures + 1))
    fi
  done
done

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: Azure parameter files are present and complete."
