# Deployment Record: Governance Workflow + Decision Audit + Coordinator UI

Date: 2026-06-17
Environment: AKS namespace `healthcare-dev` + App Service `healthcarepatientengagement`

## Scope

1. Add governance workflow for patient registration review.
2. Persist decision audit metadata for approve/reject actions.
3. Add coordinator UI support for registration review actions.
4. Deploy backend and frontend changes.
5. Verify runtime behavior.

## Code Changes

### Backend (`svc-patient`)

- Added persisted `decisionAudit` field through:
  - domain record (`PatientProfile`)
  - JPA entity (`PatientProfileEntity`)
  - repository mapper (`PatientJpaRepositoryAdapter`)
  - response DTO (`PatientResponse`)
- Updated approval/rejection service contract to include actor identity.
- Captured actor in controller from authenticated principal/JWT claims.
- On approve/reject, saved compact audit value in format:
  - `ACTION|<UTC_INSTANT>|<ACTOR>|<CORRELATION_ID>`
- Kept anonymous `POST /patients` open.
- Kept approve/reject/resend endpoints coordinator-restricted.

### Frontend

- Added `decisionAudit?: string` to patient response type.
- Coordinator registration review table now shows Decision audit column.
- Build prepared with `VITE_API_BASE_URL=/api` for App Service runtime proxying.

## Deployment Actions

### Backend

- Built and pushed image:
  - `ghcr.io/uttambhatia/healthcarepatientengagement/svc-patient:decisionaudit-20260617-2146`
  - digest: `sha256:95d436dc0fb67272ddba52af37f87002dc71bbb24b615f863a08b5e63e7c5e33`
- Updated deployment image to immutable digest and completed rollout.
- Final running image:
  - `ghcr.io/uttambhatia/healthcarepatientengagement/svc-patient@sha256:95d436dc0fb67272ddba52af37f87002dc71bbb24b615f863a08b5e63e7c5e33`

### Frontend

- OneDeploy (`az webapp deploy`) failed (HTTP 400).
- Switched to Kudu ZipDeploy (same pattern as workflow publish deploy).
- Deployment completed successfully with `Push-Deployer`.

## Verification Results

### Backend API behavior (live via AKS port-forward)

1. Anonymous registration works:
   - `POST /patients` returns `201` envelope with:
     - `status: PENDING_VERIFICATION`
     - `decisionAudit: null`
2. Coordinator-only endpoints are protected for anonymous access:
   - `PATCH /patients/{id}/approval/approve` -> `401`
   - `PATCH /patients/{id}/approval/reject` -> `401`
   - `POST /patients/{id}/notifications/resend` -> `401`

### Frontend behavior

1. App Service root reachable (`200`).
2. New JS bundle served (`assets/index-BR1NzqI7.js`).
3. API route from frontend host still enforces auth (`/api/patients` -> `401`).

## Open Operational Note

- `svc-patient` and `svc-identity-adapter` logs still show Azure Service Bus `DefaultAzureCredential` unavailability in cluster runtime.
- This is pre-existing infra/auth configuration drift and is independent of the governance workflow code deployment.

## Follow-up Execution (Option 2 then Option 1)

### Option 2: Service Bus identity/auth hardening attempt

1. Added default service account annotations:
  - `azure.workload.identity/client-id`
  - `azure.workload.identity/tenant-id`
2. Added pod-template label on both deployments:
  - `azure.workload.identity/use=true`
3. Added env vars on both deployments:
  - `AZURE_TENANT_ID`
  - `AZURE_FEDERATED_TOKEN_FILE=/var/run/secrets/azure/tokens/azure-identity-token`
4. Added projected service account token volume and mount under `/var/run/secrets/azure/tokens`.
5. Confirmed token file link exists in both pods.

Observed result:

- Authentication errors persist with messages including:
  - `Workload Identity authentication is not available`
  - `Managed Identity authentication is not available`

Interpretation:

- Deployment-level wiring is present, but cluster/platform side identity plumbing is still not functional for these workloads.

### Option 1: Authenticated coordinator E2E verification

1. Obtained backend token using current Azure login and cluster audience.
2. Decoded token claims.
3. Role claim present: `PATIENT`.
4. Attempted protected approve path with this token.

Observed result:

- Approve endpoint returned `403`, which is expected for a non-coordinator token.

Interpretation:

- Coordinator-protected workflow cannot be fully E2E verified without a token that carries `COORDINATOR` (or equivalent authorized role).

## Summary

Governance workflow and decision audit enhancements were implemented, deployed, and verified in runtime for anonymous registration and coordinator endpoint authorization boundaries. Frontend deployment succeeded using Kudu ZipDeploy after OneDeploy failure.
