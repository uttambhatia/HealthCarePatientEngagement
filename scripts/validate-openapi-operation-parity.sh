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

echo "Validating OpenAPI operation and parameter parity"

contract_ops_file="$(mktemp)"
contract_params_file="$(mktemp)"
implemented_ops_file="$(mktemp)"
implemented_params_file="$(mktemp)"
missing_ops_file="$(mktemp)"
missing_params_file="$(mktemp)"
trap 'rm -f "${contract_ops_file}" "${contract_params_file}" "${implemented_ops_file}" "${implemented_params_file}" "${missing_ops_file}" "${missing_params_file}"' EXIT

awk '
  function emit_param() {
    if (in_parameters && current_method != "" && current_path != "" && param_in != "" && param_name != "") {
      method = toupper(current_method)
      print method "|" current_path "|" param_name "|" param_in
    }
    param_in = ""
    param_name = ""
  }

  {
    line = $0
    gsub(/\r/, "", line)

    if (line ~ /^paths:/) {
      in_paths = 1
      next
    }

    if (in_paths && line ~ /^[^[:space:]]/) {
      emit_param()
      in_paths = 0
      in_parameters = 0
    }

    if (!in_paths) {
      next
    }

    if (line ~ /^  \//) {
      emit_param()
      in_parameters = 0
      current_method = ""
      current_path = line
      sub(/^  /, "", current_path)
      sub(/:$/, "", current_path)
      next
    }

    if (line ~ /^    (get|post|put|delete|patch):/) {
      emit_param()
      in_parameters = 0
      current_method = line
      sub(/^    /, "", current_method)
      sub(/:$/, "", current_method)
      print "OP|" toupper(current_method) "|" current_path
      next
    }

    if (line ~ /^      parameters:/) {
      emit_param()
      in_parameters = 1
      next
    }

    if (in_parameters && line ~ /^      [a-zA-Z]/ && line !~ /^      parameters:/) {
      emit_param()
      in_parameters = 0
      next
    }

    if (!in_parameters) {
      next
    }

    if (line ~ /^        - in: (path|query)$/) {
      emit_param()
      split(line, parts, ": ")
      param_in = parts[2]
      next
    }

    if (line ~ /^          name: /) {
      split(line, parts, ": ")
      param_name = parts[2]
      next
    }
  }

  END {
    emit_param()
  }
' "${CONTRACT_FILE}" | while IFS= read -r record; do
  if [[ "${record}" == OP\|* ]]; then
    echo "${record#OP|}" >> "${contract_ops_file}"
  else
    echo "${record}" >> "${contract_params_file}"
  fi
done

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
    awk '
      function normalize_path(path) {
        gsub(/\r/, "", path)
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", path)
        if (path == "") {
          return ""
        }
        if (substr(path, 1, 1) != "/") {
          path = "/" path
        }
        gsub(/\/\/+/, "/", path)
        return path
      }

      function emit_endpoint(method, mapping) {
        full = "/api" base_path mapping
        gsub(/\/\/+/, "/", full)
        print "OP|" method "|" full
        current_method = method
        current_path = full
        has_endpoint = 1
      }

      {
        line = $0
        gsub(/\r/, "", line)

        if (base_path == "" && match(line, /@RequestMapping\("[^"]+"\)/)) {
          base_path = line
          sub(/^.*@RequestMapping\("/, "", base_path)
          sub(/"\).*/, "", base_path)
          base_path = normalize_path(base_path)
          next
        }

        if (match(line, /@(Get|Post|Put|Delete|Patch)Mapping\(/)) {
          mapping = ""
          if (match(line, /"[^"]+"/)) {
            mapping = substr(line, RSTART + 1, RLENGTH - 2)
          }
          mapping = normalize_path(mapping)

          method = line
          sub(/^.*@/, "", method)
          sub(/Mapping\(.*/, "", method)
          method = toupper(method)

          emit_endpoint(method, mapping)
          next
        }

        if (has_endpoint == 1 && match(line, /@PathVariable\("[^"]+"\)/)) {
          param_name = substr(line, RSTART, RLENGTH)
          sub(/^.*@PathVariable\("/, "", param_name)
          sub(/"\).*/, "", param_name)
          print "PARAM|" current_method "|" current_path "|" param_name "|path"
          next
        }

        if (has_endpoint == 1 && match(line, /@RequestParam\("[^"]+"\)/)) {
          param_name = substr(line, RSTART, RLENGTH)
          sub(/^.*@RequestParam\("/, "", param_name)
          sub(/"\).*/, "", param_name)
          print "PARAM|" current_method "|" current_path "|" param_name "|query"
          next
        }
      }
    ' "${controller_file}" | while IFS= read -r record; do
      if [[ "${record}" == OP\|* ]]; then
        echo "${record#OP|}" >> "${implemented_ops_file}"
      else
        echo "${record#PARAM|}" >> "${implemented_params_file}"
      fi
    done
  done < <(find "${controller_root}" -type f -name '*Controller.java' -print0)
done

sort -u "${contract_ops_file}" -o "${contract_ops_file}"
sort -u "${contract_params_file}" -o "${contract_params_file}"
sort -u "${implemented_ops_file}" -o "${implemented_ops_file}"
sort -u "${implemented_params_file}" -o "${implemented_params_file}"

while IFS= read -r implemented_op; do
  if [[ -z "${implemented_op}" ]]; then
    continue
  fi

  op_method="${implemented_op%%|*}"
  op_path="${implemented_op#*|}"
  contract_path="${op_path}"
  if [[ "${contract_path}" == /api/* ]]; then
    contract_path="/${contract_path#/api/}"
  fi

  if ! grep -Fxq "${op_method}|${contract_path}" "${contract_ops_file}"; then
    echo "${op_method} ${op_path}" >> "${missing_ops_file}"
  fi
done < "${implemented_ops_file}"

while IFS= read -r implemented_param; do
  if [[ -z "${implemented_param}" ]]; then
    continue
  fi

  param_method="${implemented_param%%|*}"
  rest="${implemented_param#*|}"
  param_path="${rest%%|*}"
  rest2="${rest#*|}"
  param_name="${rest2%%|*}"
  param_in="${rest2##*|}"

  contract_path="${param_path}"
  if [[ "${contract_path}" == /api/* ]]; then
    contract_path="/${contract_path#/api/}"
  fi

  if ! grep -Fxq "${param_method}|${contract_path}|${param_name}|${param_in}" "${contract_params_file}"; then
    echo "${param_method} ${param_path} missing ${param_in} parameter '${param_name}' in OpenAPI" >> "${missing_params_file}"
  fi
done < "${implemented_params_file}"

failed=0
if [[ -s "${missing_ops_file}" ]]; then
  echo "Validation failed: implemented operations missing from OpenAPI contract:" >&2
  cat "${missing_ops_file}" >&2
  failed=1
fi

if [[ -s "${missing_params_file}" ]]; then
  if [[ ${failed} -eq 1 ]]; then
    echo "" >&2
  fi
  echo "Validation failed: implemented path/query parameters missing from OpenAPI contract:" >&2
  cat "${missing_params_file}" >&2
  failed=1
fi

if [[ ${failed} -eq 1 ]]; then
  exit 1
fi

echo "Validation passed: OpenAPI operation and parameter parity is intact."
