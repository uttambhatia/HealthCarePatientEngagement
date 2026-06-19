# ACS Adapter Deployment and Verification Report

Date: 2026-06-17
Environment: AKS cluster `aks-hpe-devx`, namespace `healthcare-dev`

## Summary

The internal ACS adapter scaffold in identity-adapter was deployed and verified in Azure Kubernetes Service.

Final status:
- Deployment: successful
- Runtime wiring to adapter URL: successful
- Contract verification:
  - POST /acs/notifications -> HTTP 202
  - POST /acs/teleconsult/sessions -> HTTP 200 with response payload

## Code and Configuration Changes

### Identity Adapter Service
- Added ACS integration controller:
  - `POST /acs/notifications`
  - `POST /acs/teleconsult/sessions`
- Added request/response DTOs for ACS adapter contract.
- Added service logic for:
  - accepting notification dispatch requests
  - provisioning deterministic teleconsult session IDs and join URLs
- Updated security rules to allow unauthenticated access for `/acs/**` and `/error`.
- Added ACS teleconsult URL generation config in application properties.

### Kubernetes and Environment Templates
- Updated platform secret templates to route ACS calls internally to identity-adapter:
  - `acs-integration-base-url=http://svc-identity-adapter`
  - `teleconsult-acs-integration-base-url=http://svc-identity-adapter`
- Updated dev bootstrap defaults to the same internal service URL values.

## Deployment Actions Performed

1. Verified Docker daemon availability and authenticated to GHCR.
2. Built and pushed identity-adapter image:
   - `ghcr.io/uttambhatia/healthcarepatientengagement/svc-identity-adapter:latest`
   - digest: `sha256:20b0498b40fe53e03ad5942e63ffc08552bc8d725781e21895bfbacf9106e294`
3. Restarted identity-adapter deployment and waited for rollout completion.
4. Verified platform secret values in AKS secret `platform-secrets`.
5. Verified runtime env vars in dependent services:
   - notification service uses `ACS_INTEGRATION_BASE_URL=http://svc-identity-adapter`
   - appointment service uses `TELECONSULT_ACS_INTEGRATION_BASE_URL=http://svc-identity-adapter`
6. Verified endpoint behavior via local port-forward and HTTP requests.

## Verification Evidence

### Deployment Readiness
- `svc-identity-adapter`: READY 1/1, UPDATED 1, AVAILABLE 1
- `svc-notification`: READY 1/1, UPDATED 1, AVAILABLE 1
- `svc-appointment`: READY 3/3, UPDATED 3, AVAILABLE 3

### Secret Values
- `acs-integration-base-url=http://svc-identity-adapter`
- `teleconsult-acs-integration-base-url=http://svc-identity-adapter`

### Contract Test Results

Request: `POST /acs/notifications`
- Response: HTTP 202 Accepted

Request: `POST /acs/teleconsult/sessions`
- Response: HTTP 200 OK
- Body:

```json
{
  "sessionId": "9414ee5d-f319-356c-aa08-bc5d5557506b",
  "doctorJoinUrl": "https://teleconsult.healthcare.local/session/9414ee5d-f319-356c-aa08-bc5d5557506b?role=DOCTOR",
  "patientJoinUrl": "https://teleconsult.healthcare.local/session/9414ee5d-f319-356c-aa08-bc5d5557506b?role=PATIENT"
}
```

## Notes

- Earlier 401 responses were from old pods before the final image rollout and from malformed JSON during shell-escaped in-cluster curl attempts.
- Final verification used port-forward plus PowerShell HTTP requests to avoid quoting issues and confirm real endpoint behavior.
