#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNBOOK="${ROOT_DIR}/docs/Disaster_Recovery_Runbook.md"
DR_DRILL_TEMPLATE="${ROOT_DIR}/docs/dr/DR_Drill_Report_Template.md"
DR_BACKUP_TEMPLATE="${ROOT_DIR}/docs/dr/DR_Backup_Verification_Log_Template.md"
DRILL_EVIDENCE_DIR="${ROOT_DIR}/docs/dr/evidence/drills"
BACKUP_EVIDENCE_DIR="${ROOT_DIR}/docs/dr/evidence/backups"

required_sections=(
  "## DR Matrix"
  "## Backup and Replication Baseline"
  "## Failover Procedure (Primary -> Secondary)"
  "## Failback Procedure (Secondary -> Primary)"
  "## Drill Cadence and Evidence"
  "## Readiness Checklist"
)

failures=0

echo "Validating DR readiness artifacts"

if [[ ! -f "${RUNBOOK}" ]]; then
  echo "ERROR: missing ${RUNBOOK#${ROOT_DIR}/}"
  exit 1
fi

echo "Checking ${RUNBOOK#${ROOT_DIR}/}"
for section in "${required_sections[@]}"; do
  if ! grep -qF "${section}" "${RUNBOOK}"; then
    echo "  ERROR: missing section ${section}"
    failures=$((failures + 1))
  fi
done

if [[ ! -f "${DR_DRILL_TEMPLATE}" ]]; then
  echo "ERROR: missing ${DR_DRILL_TEMPLATE#${ROOT_DIR}/}"
  failures=$((failures + 1))
else
  echo "Checking ${DR_DRILL_TEMPLATE#${ROOT_DIR}/}"
  if ! grep -qF "## RTO/RPO Measurements" "${DR_DRILL_TEMPLATE}"; then
    echo "  ERROR: drill template missing RTO/RPO section"
    failures=$((failures + 1))
  fi
fi

if [[ ! -f "${DR_BACKUP_TEMPLATE}" ]]; then
  echo "ERROR: missing ${DR_BACKUP_TEMPLATE#${ROOT_DIR}/}"
  failures=$((failures + 1))
else
  echo "Checking ${DR_BACKUP_TEMPLATE#${ROOT_DIR}/}"
  if ! grep -qF "## Verification Records" "${DR_BACKUP_TEMPLATE}"; then
    echo "  ERROR: backup verification template missing verification records section"
    failures=$((failures + 1))
  fi
fi

if [[ ! -d "${DRILL_EVIDENCE_DIR}" ]]; then
  echo "ERROR: missing ${DRILL_EVIDENCE_DIR#${ROOT_DIR}/}"
  failures=$((failures + 1))
else
  echo "Checking ${DRILL_EVIDENCE_DIR#${ROOT_DIR}/}"
  drill_sample="$(find "${DRILL_EVIDENCE_DIR}" -maxdepth 1 -type f -name '*_DR_Drill_Report.md' | head -n 1)"
  if [[ -z "${drill_sample}" ]]; then
    echo "  ERROR: no drill evidence files found"
    failures=$((failures + 1))
  elif ! grep -qF "## RTO/RPO Measurements" "${drill_sample}"; then
    echo "  ERROR: drill evidence file missing RTO/RPO section"
    failures=$((failures + 1))
  fi
fi

if [[ ! -d "${BACKUP_EVIDENCE_DIR}" ]]; then
  echo "ERROR: missing ${BACKUP_EVIDENCE_DIR#${ROOT_DIR}/}"
  failures=$((failures + 1))
else
  echo "Checking ${BACKUP_EVIDENCE_DIR#${ROOT_DIR}/}"
  backup_sample="$(find "${BACKUP_EVIDENCE_DIR}" -maxdepth 1 -type f -name '*_Backup_Verification_Log.md' | head -n 1)"
  if [[ -z "${backup_sample}" ]]; then
    echo "  ERROR: no backup verification evidence files found"
    failures=$((failures + 1))
  elif ! grep -qF "## Verification Records" "${backup_sample}"; then
    echo "  ERROR: backup evidence file missing verification records section"
    failures=$((failures + 1))
  fi
fi

if [[ ${failures} -gt 0 ]]; then
  echo "Validation failed with ${failures} issue(s)."
  exit 1
fi

echo "Validation passed: DR readiness artifacts are complete."
