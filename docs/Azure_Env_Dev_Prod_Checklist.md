# Azure Environment Checklist (Dev and Prod)

Date: 2026-06-06
Scope: Minimum required values and gates for dev and prod environment rollout.

## 1) Environment inventory

| Item | Dev | Prod |
|---|---|---|
| Azure subscription | <dev-subscription-id> | <prod-subscription-id> |
| Resource group | <rg-healthcare-dev> | <rg-healthcare-prod> |
| AKS cluster | <aks-healthcare-dev> | <aks-healthcare-prod> |
| Kubernetes namespace | healthcare-dev | healthcare-prod |
| Container registry | <acr-dev>.azurecr.io | <acr-prod>.azurecr.io |
| Ingress host | <dev-api-hostname> | <prod-api-hostname> |
| API Management host | <dev-apim-hostname> | <prod-apim-hostname> |
| UI App Service | <dev-ui-appservice-name> | <prod-ui-appservice-name> |
| UI host | <dev-ui-hostname> | <prod-ui-hostname> |

## 2) Required secret values

Use these templates:
- deploy/k8s/env/dev/platform-secrets.dev.template.yaml
- deploy/k8s/env/prod/platform-secrets.prod.template.yaml
- deploy/k8s/env/dev/dev.env.template
- deploy/k8s/env/prod/prod.env.template

Optional helper to render secrets from env files:
- deploy/k8s/env/render-platform-secret.ps1

Required value groups:
- Identity: oauth2-issuer, oauth2-audience, oauth2-jwk-set-uri
- Data: azure-sql-jdbc-url, azure-sql-username, azure-sql-password
- Messaging: servicebus-namespace, eventhub-namespace
- Integration: fhir-integration-base-url, service-bus-integration-base-url
- Platform: key-vault-url, otel-otlp-endpoint, azure-managed-identity-client-id

Teleconsult real ACS token mode additionally needs:
- `ACS_IDENTITY_CONNECTION_STRING` in env files (renders to secret key `acs-identity-connection-string`)
- `TELECONSULT_JOIN_BASE_URL` set to a reachable public teleconsult host (environment-specific; no `<...>` placeholders)
- An Azure Communication Services resource in the same environment/subscription

Frontend App Service build-time settings (GitHub repository variables):
- `UI_APP_SERVICE_NAME`
- `UI_VITE_API_BASE_URL` (must point to APIM host)
- `UI_VITE_OIDC_CLIENT_ID`
- `UI_VITE_OIDC_AUTHORIZATION_ENDPOINT`
- `UI_VITE_OIDC_TOKEN_ENDPOINT`
- `UI_VITE_OIDC_LOGOUT_ENDPOINT`
- `UI_VITE_OIDC_REDIRECT_URI`
- `UI_VITE_OIDC_SCOPE`
- `UI_VITE_EVENTS_WS_URL` (optional)
- `UI_VITE_EVENTS_SSE_URL` (optional)

Frontend deployment workflow credentials (GitHub secrets):
- `AZURE_CLIENT_ID`
- `AZURE_TENANT_ID`
- `AZURE_SUBSCRIPTION_ID`

## 3) Dev rollout (minimum)

0. (Optional) Bootstrap minimum Azure dev infrastructure and generate env values:

```powershell
.\deploy\k8s\env\dev\bootstrap-dev-azure.ps1 -TeleconsultJoinBaseUrl "https://teleconsult-dev.contoso.com/session"
```

This provisions the minimum Azure dependencies for the repo's AKS manifests,
creates or reuses the `healthcare-dev` namespace, writes `deploy/k8s/env/dev/dev.env`,
and renders `deploy/k8s/env/dev/platform-secrets.dev.generated.yaml`.

When ACS infra is not skipped, the bootstrap script also creates/reuses an Azure Communication Services resource,
retrieves its connection string, and writes `ACS_IDENTITY_CONNECTION_STRING` into `dev.env`.

1. Fill secret template values for dev.
	 - or copy `deploy/k8s/env/dev/dev.env.template` to `deploy/k8s/env/dev/dev.env` and fill values.
2. (Optional) Render secret manifest from env file:

```powershell
.\deploy\k8s\env\render-platform-secret.ps1 `
	-EnvFile .\deploy\k8s\env\dev\dev.env `
	-OutputFile .\deploy\k8s\env\dev\platform-secrets.dev.generated.yaml `
	-Namespace healthcare-dev
```

Use the generated file with `kubectl apply -f` before service rollout.
3. Run preflight checks (recommended before apply):

```powershell
.\deploy\k8s\env\dev\apply-dev.ps1 -Namespace healthcare-dev -EnvFile .\deploy\k8s\env\dev\dev.env -Preflight
```

4. Apply secrets and manifests (recommended one-command path):
2. Apply secrets and manifests (recommended one-command path):

```powershell
.\deploy\k8s\env\dev\apply-dev.ps1 -Namespace healthcare-dev -EnvFile .\deploy\k8s\env\dev\dev.env
```

4.1 If images are private in GitHub Container Registry (GHCR), configure pull secret:

```powershell
.\deploy\k8s\env\dev\setup-ghcr-pull-secret.ps1 -Namespace healthcare-dev
```

This prompts for GHCR username and PAT (read:packages), creates/updates
`ghcr-pull`, patches the default service account, and restarts deployments.

Use the GitHub owner that matches the published image namespace, for example
`uttambhatia` when images are pushed to `ghcr.io/uttambhatia/...`.

5. Manual equivalent:

```bash
kubectl apply -f deploy/k8s/env/dev/platform-secrets.dev.template.yaml
kubectl apply -f deploy/k8s/api-gateway/
kubectl apply -f deploy/k8s/svc-patient/
kubectl apply -f deploy/k8s/svc-appointment/
kubectl apply -f deploy/k8s/svc-careplan/
kubectl apply -f deploy/k8s/svc-consent/
kubectl apply -f deploy/k8s/svc-medical-record/
kubectl apply -f deploy/k8s/svc-notification/
kubectl apply -f deploy/k8s/svc-telemetry/
kubectl apply -f deploy/k8s/svc-device-ingestion/
kubectl apply -f deploy/k8s/svc-alert-management/
kubectl apply -f deploy/k8s/svc-identity-adapter/
kubectl apply -f deploy/k8s/svc-event-messaging/
```

6. Run minimum validation (if script was executed with SkipGuardrails):

```bash
bash scripts/run-infra-dr-guardrails.sh
```

7. Deploy frontend UI to App Service:

```powershell
# Option A: run manual dispatch from Actions UI
# Workflow: .github/workflows/frontend-appservice.yml

# Option B: push frontend changes to main after vars/secrets are configured
```

8. Dev post-deploy UI checks:
- UI loads from App Service hostname over HTTPS.
- Browser calls only APIM hostname for `/api/*` routes.
- API path remains APIM -> API Gateway -> services.

## 4) Prod rollout (minimum)

1. Confirm P0 go-live gate decision from Architecture_Alignment_Remediation_Backlog.
2. Fill prod template values and verify private endpoints/network policy assumptions.
	 - or copy `deploy/k8s/env/prod/prod.env.template` to `deploy/k8s/env/prod/prod.env` and fill values.
3. (Optional) Render secret manifest from env file:

```powershell
.\deploy\k8s\env\render-platform-secret.ps1 `
	-EnvFile .\deploy\k8s\env\prod\prod.env `
	-OutputFile .\deploy\k8s\env\prod\platform-secrets.prod.generated.yaml `
	-Namespace healthcare-prod
```

Use the generated file with `kubectl apply -f` before service rollout.
4. Run preflight checks (recommended before apply):

```powershell
.\deploy\k8s\env\prod\apply-prod.ps1 -Namespace healthcare-prod -Preflight
```

5. Apply secrets and manifests (recommended one-command path):

```powershell
.\deploy\k8s\env\prod\apply-prod.ps1 -Namespace healthcare-prod -AcknowledgeProd
```

6. Execute post-deploy validation (if script was executed with SkipGuardrails):

```bash
bash scripts/run-infra-dr-guardrails.sh
```

7. Execute controlled smoke path through gateway before traffic cutover.

8. Deploy frontend UI to prod App Service using environment-scoped approvals.

9. Prod post-deploy UI checks:
- UI host is reachable and TLS-valid.
- Browser API calls target prod APIM host only.
- OIDC redirect URI matches prod UI host.

## 5) Rollback minimum

- Re-apply last known-good image tags/manifests.
- Verify service readiness and ingress path.
- Re-run guardrail script to confirm baseline.
