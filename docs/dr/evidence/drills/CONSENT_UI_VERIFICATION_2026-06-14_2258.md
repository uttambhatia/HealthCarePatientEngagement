# Consent UI Verification and Backend Logs

Timestamp (UTC): 2026-06-14T16:58:40Z
Scope: UI consent submit + backend runtime/log verification

## UI verification result
- Submit action from production UI completed successfully.
- UI status message: Consent preference updated successfully.

## Cluster runtime snapshot
From live kubectl checks at verification time:

```text
HPA svc-consent: min=1 max=1 replicas=1
Deployment svc-consent: READY 1/1, AVAILABLE 1/1
Pod svc-consent-57dffcf58d-fqgfk: Running, 0 restarts
Pod api-gateway-5cb78ccc47-dqxh4: Running, 0 restarts
```

## Filtered backend log findings
Time window checked: last 15 minutes from verification command.

### api-gateway
- No 502/503, timeout, or consent-route exception lines found by filter:
  - 502
  - 503
  - Connection refused
  - timed out
  - ERROR
  - Exception
  - /api/consents

### svc-consent
- No SQL startup failure signature observed in this verification window:
  - Login failed for user
  - SQLServerException
- Repeated non-request-path errors still present from queue processor credential chain:
  - ConsentQueueProcessor error
  - EnvironmentCredential authentication unavailable
  - Managed Identity authentication is not available
  - Azure CLI / Azure Developer CLI unavailable

These queue-processor errors are noisy but did not block the verified UI consent save in this run.

## Operator conclusion
- The specific user-reported UI error Backend service is unavailable. Try again shortly was not reproducible in this verification run.
- Current live path (UI -> api-gateway -> svc-consent) was healthy at capture time.
- If the issue recurs, correlate by exact user timestamp and capture:
  - gateway logs around /api/consents
  - consent pod logs for same minute
  - pod readiness transitions/events
