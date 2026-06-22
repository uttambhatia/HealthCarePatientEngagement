#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APPSETTINGS_FILES=(
  "${ROOT_DIR}/deploy/appservice/frontend/dev.appsettings.template"
  "${ROOT_DIR}/deploy/appservice/frontend/prod.appsettings.template"
)

required_vars=(
  "UI_TRAFFIC_MANAGER_PROFILE_NAME"
  "UI_TRAFFIC_MANAGER_DNS_NAME"
  "UI_FRONT_DOOR_PRIMARY_HOSTNAME"
  "UI_FRONT_DOOR_SECONDARY_HOSTNAME"
  "UI_VITE_PUBLIC_BASE_URL"
)

failures=0

echo "Validating App Service edge routing configuration templates"

for appsettings in "${APPSETTINGS_FILES[@]}"; do
  rel_file="${appsettings#${ROOT_DIR}/}"
  echo "Checking ${rel_file}"

  if [[ ! -f "${appsettings}" ]]; then
    echo "  ERROR: missing file"
    failures=$((failures + 1))
    continue
  fi

  for var_name in "${required_vars[@]}"; do
    if ! grep -Eq "^${var_name}=" "${appsettings}"; then
      echo "  ERROR: missing ${var_name}"
      failures=$((failures + 1))
    fi
  done

  if ! grep -Eq '^UI_VITE_API_BASE_URL=https://.+' "${appsettings}"; then
    echo "  ERROR: UI_VITE_API_BASE_URL must be a non-empty HTTPS URL"
    failures=$((failures + 1))
  fi

done

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: App Service edge routing templates are complete."
