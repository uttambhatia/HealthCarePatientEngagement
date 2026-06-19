# Azure Entra OAuth2 Configuration Contract

Date: 2026-06-05
Scope: Runtime property contract for gateway and all backend services when OAuth2/JWT security is enabled.

## Required Environment Variables

| Variable | Required when `PLATFORM_SECURITY_ENABLED=true` | Purpose |
|---|---|---|
| `PLATFORM_SECURITY_ENABLED` | Yes | Enables OAuth2 resource-server enforcement and authorization checks. |
| `OAUTH2_ISSUER` | Yes | Expected token issuer (`iss`) used for JWT issuer validation. |
| `OAUTH2_AUDIENCE` | Yes | Expected token audience (`aud`) used for JWT audience validation. |
| `OAUTH2_JWK_SET_URI` | Recommended | JWK endpoint used to verify token signatures. |

## Startup Behavior
- If `PLATFORM_SECURITY_ENABLED=false` (default), services run with local permissive security profile.
- If `PLATFORM_SECURITY_ENABLED=true`, startup fails fast when `OAUTH2_ISSUER` or `OAUTH2_AUDIENCE` is missing.
- Signature validation uses `OAUTH2_JWK_SET_URI`.

## Service Coverage

The following service templates include this property contract in `application.yml`:

- `services/api-gateway`
- `services/svc-patient`
- `services/svc-appointment`
- `services/svc-careplan`
- `services/svc-consent`
- `services/svc-medical-record`
- `services/svc-notification`
- `services/svc-telemetry`
- `services/svc-device-ingestion`
- `services/svc-alert-management`
- `services/svc-identity-adapter`
- `services/svc-event-messaging`

## Azure Deployment Notes

- Use private endpoints for downstream dependencies (Azure SQL, Service Bus, Event Hubs, Key Vault) and run services in private subnets.
- Use managed identity for service-to-service and platform access; avoid embedding secrets in manifests.
- Keep issuer/audience values environment-specific (dev/test/prod) and managed through secure configuration pipelines.
- Validate private endpoint dependency env wiring in CI via `bash scripts/validate-k8s-private-endpoint-env.sh`.
- Validate integration adapter readiness in CI via `bash scripts/validate-integration-adapter-readiness.sh`.
- Validate integration reliability hygiene in CI via `bash scripts/validate-integration-reliability-hygiene.sh`.
- Validate integration adapter contract tests in CI via `bash scripts/validate-integration-adapter-contract-tests.sh`.
- Maintain DR runbook and readiness artifact checks via `bash scripts/validate-dr-readiness-artifacts.sh`.
- Validate observability endpoint wiring in CI via `bash scripts/validate-k8s-observability-env.sh`.

## Kubernetes Wiring

- All service deployments under `deploy/k8s/*/deployment.yaml` now inject:
  - `PLATFORM_SECURITY_ENABLED=true`
  - `OAUTH2_ISSUER` from secret key `oauth2-issuer`
  - `OAUTH2_AUDIENCE` from secret key `oauth2-audience`
  - `OAUTH2_JWK_SET_URI` from secret key `oauth2-jwk-set-uri`
- Seed secret template: `deploy/k8s/platform-secrets.template.yaml`

## Azure SQL Single-Database Wiring

- All DB-backed service deployments now reference the same secret-backed JDBC endpoint key:
  - `azure-sql-jdbc-url`
  - `azure-sql-username`
  - `azure-sql-password`
- Managed identity client id is injected via:
  - `AZURE_CLIENT_ID` from secret key `azure-managed-identity-client-id`
- This supports the approved single database model by mapping each service-specific `*_DB_URL` env var to the same shared Azure SQL JDBC URL.
- CI validation command:
  - `bash scripts/validate-k8s-azure-sql-env.sh`

## Integration Endpoint Secret Keys

- `fhir-integration-base-url`
- `service-bus-integration-base-url`

## Role-Based User Sign-In Implementation (Entra)

This platform expects role-based JWT claims for human users and maps them directly to Spring Security authorities.

### Expected Role Claim Values

The backend and frontend expect these exact role values in JWT `roles` claim:

- `PATIENT`
- `DOCTOR`
- `COORDINATOR`
- `ADMIN`

Do not rename these role values in Entra (for example, do not replace `COORDINATOR` with `CARE_COORDINATOR`).

### Group-Based Assignment Model

Recommended Entra security groups:

- `HCPE-PATIENT`
- `HCPE-DOCTOR`
- `HCPE-COORDINATOR`
- `HCPE-ADMIN`

Create app roles with the same values (`PATIENT`, `DOCTOR`, `COORDINATOR`, `ADMIN`) on the protected API app registration and assign each group to its matching app role.

### Frontend OIDC Scope Contract

Frontend must request a scope that yields an API access token containing app-role claims.

Required shape for `VITE_OIDC_SCOPE`:

- `openid profile api://<tenant-id>/hpe-devx-api/access_as_user`

Notes:

- If only `openid profile email` is requested, role claims for API authorization may not be present in the access token.
- Frontend role derivation should use the access token claims for role and ID token claims for display identity.

### Verify Current Users Per Role Group

Use this command to list user principal names assigned to each role group:

```powershell
$roles = @("PATIENT","DOCTOR","COORDINATOR","ADMIN")
foreach ($r in $roles) {
  $g = "HCPE-$r"
  Write-Host "`n=== $g ==="
  az ad group member list --group $g --query "[].userPrincipalName" -o tsv
}
```

### B2B Guest Onboarding Automation

Use the scripted batch workflow to onboard guests into existing role groups without changing role claim values.

Input template:

- `scripts/b2b-guests.template.csv`

Automation script:

- `scripts/onboard-b2b-guests.ps1`

Safe validation-only check (no Azure calls):

```powershell
.\scripts\onboard-b2b-guests.ps1 -CsvPath .\scripts\b2b-guests.template.csv -ValidateOnly
```

Dry-run check (Azure read-only flow, no invitations or assignments):

```powershell
.\scripts\onboard-b2b-guests.ps1 -CsvPath .\scripts\b2b-guests.template.csv
```

Live execution (invites guest users and assigns to `HCPE-*` groups):

```powershell
.\scripts\onboard-b2b-guests.ps1 -CsvPath .\scripts\b2b-guests.csv -Execute
```

Notes:

- Supported roles in CSV are `patient`, `doctor`, `care-coordinator` (maps to `COORDINATOR`), and `admin`.
- `admin` and `care-coordinator` require approver metadata unless `-AllowPrivilegedWithoutApprover` is set.
- The script writes a timestamped report CSV to the workspace root unless `-ReportPath` is provided.

### Approved Patient Automation

Approved patient onboarding now follows the same role-group model, but it is triggered automatically when a patient record is approved.

Required deployment values for the patient provisioning worker:

- `ENTRA_API_APP_ID`
- `ENTRA_GRAPH_BASE_URL`
- `ENTRA_PATIENT_GROUP_NAME`
- `ENTRA_PATIENT_ROLE_VALUE`
- `ENTRA_INVITE_REDIRECT_URL`

For dev bootstrap, `deploy/k8s/env/dev/bootstrap-dev-azure.ps1` writes these values into `dev.env` and the rendered platform secret. When Entra app registration is enabled, the script derives `ENTRA_API_APP_ID` from the created API app registration.

The approved-patient worker resolves or invites the user, adds them to `HCPE-PATIENT`, and ensures the matching API app role is assigned so the access token includes `PATIENT` in the `roles` claim.

### Admin Consent

After wiring API delegated permission into the SPA app registration, grant tenant admin consent for the SPA app permissions before validating secure sign-in flows.
