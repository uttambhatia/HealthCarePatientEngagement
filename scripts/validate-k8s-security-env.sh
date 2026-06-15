#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_DIR="${ROOT_DIR}/deploy/k8s"

required_env_names=(
  "PLATFORM_SECURITY_ENABLED"
  "OAUTH2_ISSUER"
  "OAUTH2_AUDIENCE"
  "OAUTH2_JWK_SET_URI"
)

required_secret_keys=(
  "oauth2-issuer"
  "oauth2-audience"
  "oauth2-jwk-set-uri"
)

failures=0

echo "Validating Kubernetes OAuth2 env wiring in ${DEPLOY_DIR}"

while IFS= read -r deployment; do
  echo "Checking ${deployment#${ROOT_DIR}/}"

  for env_name in "${required_env_names[@]}"; do
    if ! grep -q "name: ${env_name}" "${deployment}"; then
      echo "  ERROR: missing env var ${env_name}"
      failures=$((failures + 1))
    fi
  done

  if ! grep -q 'name: PLATFORM_SECURITY_ENABLED' "${deployment}" || ! grep -q 'value: "true"' "${deployment}"; then
    echo "  ERROR: PLATFORM_SECURITY_ENABLED must be explicitly set to true"
    failures=$((failures + 1))
  fi

  for secret_key in "${required_secret_keys[@]}"; do
    if ! grep -q "key: ${secret_key}" "${deployment}"; then
      echo "  ERROR: missing secret key reference ${secret_key}"
      failures=$((failures + 1))
    fi
  done
done < <(find "${DEPLOY_DIR}" -mindepth 2 -maxdepth 2 -name deployment.yaml | sort)

SECRET_TEMPLATE="${DEPLOY_DIR}/platform-secrets.template.yaml"
if [[ ! -f "${SECRET_TEMPLATE}" ]]; then
  echo "ERROR: missing ${SECRET_TEMPLATE#${ROOT_DIR}/}"
  failures=$((failures + 1))
else
  echo "Checking ${SECRET_TEMPLATE#${ROOT_DIR}/}"
  for secret_key in "${required_secret_keys[@]}"; do
    if ! grep -q "${secret_key}:" "${SECRET_TEMPLATE}"; then
      echo "  ERROR: secret template missing ${secret_key}"
      failures=$((failures + 1))
    fi
  done
fi

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: Kubernetes OAuth2 env wiring is complete."
