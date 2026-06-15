#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DRILL_DIR="${ROOT_DIR}/docs/dr/evidence/drills"
BACKUP_DIR="${ROOT_DIR}/docs/dr/evidence/backups"

current_year="$(date -u +%Y)"
current_month="$(date -u +%m)"
current_year_month="${current_year}-${current_month}"

# Quarter windows: Q1=01-03, Q2=04-06, Q3=07-09, Q4=10-12
case "${current_month}" in
  01|02|03) quarter_months='01|02|03' ;;
  04|05|06) quarter_months='04|05|06' ;;
  07|08|09) quarter_months='07|08|09' ;;
  10|11|12) quarter_months='10|11|12' ;;
  *)
    echo "ERROR: invalid month ${current_month}"
    exit 1
    ;;
esac

failures=0

echo "Validating DR evidence freshness"

echo "Checking monthly backup evidence for ${current_year_month}"
if [[ ! -d "${BACKUP_DIR}" ]]; then
  echo "  ERROR: missing ${BACKUP_DIR#${ROOT_DIR}/}"
  failures=$((failures + 1))
elif ! find "${BACKUP_DIR}" -maxdepth 1 -type f -name "${current_year_month}_Backup_Verification_Log.md" | grep -q .; then
  echo "  ERROR: missing monthly backup evidence file ${current_year_month}_Backup_Verification_Log.md"
  failures=$((failures + 1))
fi

echo "Checking quarterly drill evidence for ${current_year} (months ${quarter_months})"
if [[ ! -d "${DRILL_DIR}" ]]; then
  echo "  ERROR: missing ${DRILL_DIR#${ROOT_DIR}/}"
  failures=$((failures + 1))
elif ! find "${DRILL_DIR}" -maxdepth 1 -type f -regextype posix-extended -regex ".*/${current_year}-(${quarter_months})_DR_Drill_Report\.md" | grep -q .; then
  echo "  ERROR: missing quarterly drill evidence file for current quarter"
  failures=$((failures + 1))
fi

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: DR evidence is fresh for current period."
