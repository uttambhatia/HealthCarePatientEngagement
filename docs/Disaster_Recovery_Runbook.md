# Disaster Recovery Runbook

Date: 2026-06-05
Scope: Azure SQL single database, Service Bus/Event Hubs messaging, AKS workloads, and platform configuration artifacts.

## Objectives

- Define repeatable backup, replication, and synchronization controls.
- Provide failover/failback execution steps with ownership and evidence requirements.
- Align DR drills with backlog requirement K0-2.

## DR Matrix

| Component | Strategy | Target |
|---|---|---|
| Azure SQL single database | Automated backup + geo-replication | RPO <= 15 min, RTO <= 60 min |
| Service Bus / Event Hubs | Geo-disaster recovery alias + replay strategy | RPO <= 15 min, RTO <= 60 min |
| AKS workloads | Multi-zone node pools + redeploy from manifests | RTO <= 60 min |
| Platform secrets/config | Versioned secret rotation and restore procedures | RPO <= 15 min |

## Backup and Replication Baseline

1. Azure SQL
- Enable automatic backups and long-term retention according to compliance profile.
- Enable geo-replication to paired region.
- Verify backup retention and replication status daily.

2. Service Bus and Event Hubs
- Enable geo-DR alias for namespaces.
- Maintain message replay/reconciliation capability for in-flight events.
- Validate alias failover procedure during drills.

3. AKS and Manifests
- Keep deployment manifests and validation scripts under source control.
- Validate deterministic redeploy in secondary region using the same manifest set.

4. Secrets and Identity
- Keep secret material in Azure Key Vault with soft delete and purge protection enabled.
- Keep managed identity assignments codified and auditable.

## Failover Procedure (Primary -> Secondary)

1. Declare incident and capture start timestamp.
2. Freeze non-essential deployments in primary region.
3. Promote Azure SQL secondary replica.
4. Execute Service Bus/Event Hubs namespace failover using geo-DR alias.
5. Deploy or scale AKS workloads in secondary region using repository manifests.
6. Update DNS or traffic manager routing to secondary region ingress.
7. Run smoke checks for gateway, auth, patient, appointment, and telemetry endpoints.
8. Record achieved RTO/RPO and unresolved gaps.

## Failback Procedure (Secondary -> Primary)

1. Re-establish healthy primary region dependencies.
2. Reconfigure replication direction if needed.
3. Shift traffic gradually from secondary to primary.
4. Confirm data synchronization integrity and event pipeline health.
5. Close incident with postmortem and remediation actions.

## Drill Cadence and Evidence

- Cadence: quarterly regional failover drill, monthly backup restore verification.
- Required evidence per drill:
  - Incident timeline
  - RTO/RPO measurements
  - Validation command output and endpoint checks
  - Action items with owners and due dates
- Templates:
  - `docs/dr/DR_Drill_Report_Template.md`
  - `docs/dr/DR_Backup_Verification_Log_Template.md`
- Evidence storage:
  - `docs/dr/evidence/drills/`
  - `docs/dr/evidence/backups/`

## Readiness Checklist

- [ ] Azure SQL backup and geo-replication configured and monitored.
- [ ] Service Bus/Event Hubs geo-DR alias configured.
- [ ] AKS redeploy validated in secondary region.
- [ ] Key Vault soft delete and purge protection enabled.
- [ ] DR drill executed in last quarter with evidence stored.
