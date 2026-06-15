#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTRACT_FILE="${ROOT_DIR}/contracts/care-coordination-platform-openapi.yaml"

if [[ ! -f "${CONTRACT_FILE}" ]]; then
  echo "OpenAPI contract not found at ${CONTRACT_FILE}" >&2
  exit 1
fi

if ! grep -q '^paths:' "${CONTRACT_FILE}"; then
  echo "OpenAPI contract missing paths section." >&2
  exit 1
fi

echo "Validating OpenAPI coverage against implemented controller mappings"

contract_paths_file="$(mktemp)"
implemented_paths_file="$(mktemp)"
missing_paths_file="$(mktemp)"
trap 'rm -f "${contract_paths_file}" "${implemented_paths_file}" "${missing_paths_file}"' EXIT

# Collect all contract paths (top-level YAML keys under paths).
awk '
  /^paths:/ { in_paths=1; next }
  in_paths && /^[^[:space:]]/ { in_paths=0 }
  in_paths && /^  \// {
    path=$1
    gsub(/\r/, "", path)
    sub(/:$/, "", path)
    print path
  }
' "${CONTRACT_FILE}" | sort -u > "${contract_paths_file}"

service_controllers=(
  "${ROOT_DIR}/services/svc-patient/src/main/java"
  "${ROOT_DIR}/services/svc-appointment/src/main/java"
  "${ROOT_DIR}/services/svc-careplan/src/main/java"
  "${ROOT_DIR}/services/svc-consent/src/main/java"
  "${ROOT_DIR}/services/svc-medical-record/src/main/java"
  "${ROOT_DIR}/services/svc-notification/src/main/java"
  "${ROOT_DIR}/services/svc-telemetry/src/main/java"
  "${ROOT_DIR}/services/svc-device-ingestion/src/main/java"
  "${ROOT_DIR}/services/svc-alert-management/src/main/java"
  "${ROOT_DIR}/services/svc-identity-adapter/src/main/java"
  "${ROOT_DIR}/services/svc-event-messaging/src/main/java"
)

for controller_root in "${service_controllers[@]}"; do
  if [[ ! -d "${controller_root}" ]]; then
    continue
  fi

  while IFS= read -r -d '' controller_file; do
    base_path="$(sed -nE 's/^[[:space:]]*@RequestMapping\("([^"]+)"\).*/\1/p' "${controller_file}" | head -n1 | tr -d '\r')"
    if [[ -z "${base_path}" ]]; then
      continue
    fi

    normalized_base="/api${base_path}"
    normalized_base="${normalized_base//\/\//\/}"
    echo "${normalized_base}" >> "${implemented_paths_file}"

    while IFS= read -r method_subpath; do
      method_subpath="${method_subpath//$'\r'/}"
      full_path="${normalized_base}${method_subpath}"
      full_path="${full_path//\/\//\/}"
      echo "${full_path}" >> "${implemented_paths_file}"
    done < <(
      sed -nE 's/^[[:space:]]*@(Get|Post|Put|Delete|Patch)Mapping\("([^"]+)"\).*/\2/p' "${controller_file}"
    )
  done < <(find "${controller_root}" -type f -name '*Controller.java' -print0)
done

sort -u "${implemented_paths_file}" -o "${implemented_paths_file}"

while IFS= read -r implemented_path; do
  implemented_path="${implemented_path//$'\r'/}"
  if [[ -z "${implemented_path}" ]]; then
    continue
  fi

  contract_match_path="${implemented_path}"
  if [[ "${contract_match_path}" == /api/* ]]; then
    contract_match_path="/${contract_match_path#/api/}"
  fi

  if grep -Fxq "${contract_match_path}" "${contract_paths_file}"; then
    continue
  fi

  echo "${implemented_path}" >> "${missing_paths_file}"
done < "${implemented_paths_file}"

if [[ -s "${missing_paths_file}" ]]; then
  echo "Validation failed: implemented controller paths missing from OpenAPI contract:" >&2
  cat "${missing_paths_file}" >&2
  exit 1
fi

echo "Validation passed: OpenAPI contract covers implemented controller paths."
