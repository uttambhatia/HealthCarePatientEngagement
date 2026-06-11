# Azure Deployment Minimum Plan (Infra + Services)

Date: 2026-06-06
Purpose: Provide the minimum executable deployment plan for Azure planning and non-production rollout.

## 1) Scope and intent

This plan is intentionally minimal:
- Establish Azure infrastructure required to host the platform.
- Deploy all backend services and gateway to AKS.
- Deploy frontend UI to Azure App Service.
- Validate with existing guardrails and smoke checks.

This plan does not assert production readiness by itself. Production go-live still depends on closing mandatory backlog gaps in `docs/Architecture_Alignment_Remediation_Backlog.md`.

## 2) Minimum target architecture

- Compute/orchestration: AKS
- Traffic entry: Ingress -> API Gateway service
- Public API path: APIM -> API Gateway -> services
- Frontend hosting: Azure App Service (React SPA build artifact)
- Data: Azure SQL (shared contract currently expected by manifests)
- Secrets/config: Key Vault + Kubernetes secret sync path already assumed by manifests
- Messaging: Service Bus / Event Hubs endpoints configured via environment variables
- Identity: Entra issuer/audience/JWK values provided via runtime config
- Observability endpoint: OTLP endpoint configured via secret

## 3) Environment model (minimum)

- `dev` (first rollout)
- `qa` (optional but recommended before prod)
- `prod` (only after backlog gate closure)

Use separate Azure resource groups and secret scopes per environment.

## 4) Minimum prerequisite checklist

Before deploying services:
- Azure subscription, resource groups, and AKS cluster are provisioned.
- DNS + ingress hostnames are available.
- Key Vault/secret source has required values for k8s secret materialization.
- Container images are built and pushed to the target registry.
- `deploy/k8s/platform-secrets.template.yaml` has been translated into real environment secrets.

## 5) Deployment order (minimum)

1. Cluster-level prerequisites
- Namespace(s)
- Ingress controller (if not pre-installed)
- Any CSI/secret provider components required by your secret model

2. Shared configuration/secrets
- Apply environment-specific secrets/config first.

3. Core ingress path
- Deploy API gateway manifests from `deploy/k8s/api-gateway/`.

4. Domain services
- Deploy each service under `deploy/k8s/svc-*/`.

5. Frontend UI
- Deploy frontend build artifact to Azure App Service.
- Configure frontend `VITE_API_BASE_URL` to APIM hostname (not direct AKS ingress/service URL).

6. Optional resilience/network controls
- Apply network policy, HPA, and PDB manifests where present.

## 6) Minimum validation gates after deployment

Run from repository root:

```bash
bash scripts/run-infra-dr-guardrails.sh
```

Then run backend tests in CI (or controlled environment):

```bash
mvn test
```

Minimum success criteria:
- Guardrail script exits successfully.
- Key API paths are reachable through gateway.
- Frontend app loads and browser API traffic targets APIM hostname.
- No critical crash-loop/backoff pods in AKS.

## 7) Service rollout strategy (minimum risk)

- Use staged rollout: gateway -> core clinical services -> remaining adapters.
- Verify health/readiness per service before moving to next.
- Keep rollback simple: re-apply last known good image tags/manifests.

## 8) Production go-live gate (minimum policy)

Do not mark prod ready until P0 mandatory items are closed per:
- `docs/Architecture_Alignment_Remediation_Backlog.md`

At minimum, explicitly re-check:
- Real JWT/OIDC validation behavior
- Durable persistence expectations
- Real integration adapter behavior
- OpenAPI/runtime parity guardrails

## 9) Immediate next actions

1. Create environment-specific values matrix (dev/qa/prod) for all required secrets and endpoints.
2. Execute first `dev` dry-run deployment with this order.
3. Capture outputs/issues and map them back to backlog items.

## 10) Dev and prod execution artifacts

Use these environment templates and checklist:
- `deploy/k8s/env/dev/platform-secrets.dev.template.yaml`
- `deploy/k8s/env/prod/platform-secrets.prod.template.yaml`
- `deploy/k8s/env/dev/dev.env.template`
- `deploy/k8s/env/prod/prod.env.template`
- `deploy/k8s/env/render-platform-secret.ps1`
- `deploy/k8s/env/dev/apply-dev.ps1`
- `deploy/k8s/env/prod/apply-prod.ps1`
- `docs/Azure_Env_Dev_Prod_Checklist.md`
- `.github/workflows/frontend-appservice.yml`
- `deploy/appservice/frontend/dev.appsettings.template`
- `deploy/appservice/frontend/prod.appsettings.template`

Minimum execution commands:

```powershell
# dev
.\deploy\k8s\env\dev\apply-dev.ps1 -Namespace healthcare-dev -Preflight
.\deploy\k8s\env\dev\apply-dev.ps1 -Namespace healthcare-dev

# prod
.\deploy\k8s\env\prod\apply-prod.ps1 -Namespace healthcare-prod -Preflight
.\deploy\k8s\env\prod\apply-prod.ps1 -Namespace healthcare-prod -AcknowledgeProd
```
