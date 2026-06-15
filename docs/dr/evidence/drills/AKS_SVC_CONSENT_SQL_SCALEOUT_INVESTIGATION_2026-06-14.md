# AKS Consent Service Scale-Out SQL Auth Investigation

Date: 2026-06-14
Environment: aks-hpe-devx / namespace healthcare-dev
Service: svc-consent

## Executive summary
- Production path is currently stabilized at single replica and is healthy.
- End-to-end UI consent save is working.
- Scale-out to new consent pods intermittently fails during startup with SQL authentication error 18456 for user hpesqladmin.
- Password drift was ruled out by hash comparison (Kubernetes secret, dev env value, and manually set server password all matched).
- Queue processor credential errors are a separate noise path and not the SQL startup blocker.

## Final runtime state at capture
- HPA: min 1 / max 1 / replicas 1
- Deployment svc-consent: READY 1/1, AVAILABLE 1/1
- Running pod: svc-consent-57dffcf58d-fqgfk (stable)
- Failed canary ReplicaSet: svc-consent-5457f44b8c scaled to 0

## Reproduced failure evidence
During attempted rollout/scale-out, newly created pods failed startup and entered crash-loop behavior with SQL auth errors:
- Pod examples:
  - svc-consent-5457f44b8c-n5fj8
  - svc-consent-5457f44b8c-rv7w4
- Error signature:
  - com.microsoft.sqlserver.jdbc.SQLServerException: Login failed for user 'hpesqladmin'.
- Captured SQL client connection IDs from failing startup logs:
  - f030c25a-2910-461e-8057-8bc94167560a
  - b873f0b8-ac04-475b-9e42-9588eb302f8d

## Drift checks performed
Password parity check (hash only) showed no mismatch:
- SECRET_HASH = c5b0bfc2e6c1bf9897f0b04841ce2acecd6d114de05311419ff05dc490f3370b
- DEVENV_HASH = c5b0bfc2e6c1bf9897f0b04841ce2acecd6d114de05311419ff05dc490f3370b
- TARGET_HASH = c5b0bfc2e6c1bf9897f0b04841ce2acecd6d114de05311419ff05dc490f3370b

## Cloud network and policy context captured
Azure SQL firewall rules:
- AllowAzureServices: 0.0.0.0 - 0.0.0.0

AKS outbound metadata:
- outboundType: loadBalancer
- effective outbound public IP resource:
  - /subscriptions/4116346b-5ded-4f3b-8387-f8d055802adc/resourceGroups/MC_rg-azuser7080_mml.local-1nLQA_aks-hpe-devx_centralindia/providers/Microsoft.Network/publicIPAddresses/e3368db7-0020-4937-977d-2a9d1340ac30
- node resource group:
  - MC_rg-azuser7080_mml.local-1nLQA_aks-hpe-devx_centralindia

## Additional observed noise (non-blocking to current single-replica runtime)
On old revision, logs repeatedly showed Service Bus credential failures from ConsentQueueProcessor:
- EnvironmentCredential authentication unavailable
- Managed Identity authentication is not available
This noise persisted in logs but was not the direct cause of SQL startup failure in failing canary pods.

## Actions executed to stabilize production
- Re-applied HPA cap to single replica.
- Rolled back svc-consent deployment to known-good ReplicaSet.
- Confirmed single healthy pod serving traffic.
- Verified UI consent save success after stabilization.

## Recommended DBA / cloud escalation packet
Please correlate SQL-side auth telemetry at the exact startup windows using the client connection IDs listed above, including:
- SQL login failure reason detail for each ClientConnectionId
- Server principal state for hpesqladmin (disable/lockout/password policy transitions)
- Any conditional access, policy, or backend auth throttling events near those timestamps
- Correlation with AKS node egress path and transient platform events

## Proposed next controlled test (after DBA feedback)
1. Temporarily scale svc-consent to 2 while collecting:
   - New pod name, node, and exact startup timestamps
   - ClientConnectionId values from JDBC errors
2. Pull SQL audit records for those IDs and timestamps.
3. If SQL-side cause is identified and corrected, raise HPA max above 1 gradually and observe restart/error rates.
