# HealthCarePatientEngagement

Production-ready scaffolding for a **Healthcare Patient Engagement – Care Coordination Platform** aligned to:

- Azure cloud-native microservices on AKS
- Spring Boot + Spring Cloud backend services
- React frontend with role-aware modules
- Event-driven integration through Azure Service Bus/Event Hub abstractions
- Zero-trust security, correlation IDs, and OpenAPI-first APIs

## Repository structure

```text
.
├── .github/workflows/ci.yml
├── contracts/care-coordination-platform-openapi.yaml
├── deploy/k8s/
├── frontend/
├── platform-common/
└── services/
    ├── api-gateway/
    ├── svc-alert-management/
    ├── svc-appointment/
    ├── svc-careplan/
    ├── svc-consent/
    ├── svc-device-ingestion/
    ├── svc-event-messaging/
    ├── svc-identity-adapter/
    ├── svc-medical-record/
    ├── svc-notification/
    ├── svc-patient/
    └── svc-telemetry/
```

## Backend services

Each service follows the requested clean-architecture-style package layout:

- `controller`
- `service`
- `domain`
- `repository`
- `dto`
- `event`
- `integration`
- `config`
- `security`
- `exception`
- `utils`

Implemented services:

1. Patient Service (`svc-patient`)
2. Appointment Service (`svc-appointment`)
3. Care Plan Service (`svc-careplan`)
4. Consent Service (`svc-consent`)
5. Medical Record Service (`svc-medical-record`)
6. Notification Service (`svc-notification`)
7. Telemetry Service (`svc-telemetry`)
8. Device Ingestion Service (`svc-device-ingestion`)
9. Alert Management Service (`svc-alert-management`)
10. Identity Adapter Service (`svc-identity-adapter`)
11. Event Messaging Service (`svc-event-messaging`)
12. API Gateway / BFF (`api-gateway`)

## Key platform capabilities

- OpenAPI contracts for core APIs under `contracts/`
- Saga-oriented orchestration hooks in patient, appointment and care plan services
- Azure client scaffolding for Key Vault, Service Bus and Event Hub in `platform-common`
- Correlation ID propagation and centralized exception payloads
- OAuth2 resource server configuration with role-based access enforcement
- Dockerfiles and AKS-ready Kubernetes manifests per service
- React modules for authentication, patient dashboards, appointments, care plans, notifications, teleconsultation, IoT monitoring, and admin operations

## Local validation

Backend:

```bash
bash scripts/run-infra-dr-guardrails.sh
mvn test
```

`scripts/validate-correlation-id-contract.sh` now enforces reusable request-header coverage on all path items plus response-header coverage for all success (`200`/`201`) and standard error responses.

Scheduled enforcement:

- Monthly and on-demand workflow: `.github/workflows/infra-dr-guardrails.yml`
- On every run, workflow uploads `guardrail-report.txt`; guardrail failures fail the workflow.
- Minimum Azure rollout plan (infra + services): `docs/Azure_Deployment_Minimum_Plan.md`
- Dev/prod rollout checklist: `docs/Azure_Env_Dev_Prod_Checklist.md`
- Dev/prod secret templates: `deploy/k8s/env/dev/platform-secrets.dev.template.yaml`, `deploy/k8s/env/prod/platform-secrets.prod.template.yaml`
- Dev/prod env value templates: `deploy/k8s/env/dev/dev.env.template`, `deploy/k8s/env/prod/prod.env.template`
- Secret rendering helper: `deploy/k8s/env/render-platform-secret.ps1`
- Dev one-command rollout script: `deploy/k8s/env/dev/apply-dev.ps1`
- Prod one-command rollout script: `deploy/k8s/env/prod/apply-prod.ps1`
- Frontend App Service deployment workflow: `.github/workflows/frontend-appservice.yml`
- Frontend App Service env templates: `deploy/appservice/frontend/dev.appsettings.template`, `deploy/appservice/frontend/prod.appsettings.template`
- Preflight mode supported: `-Preflight` validates kubectl context, namespace, and secret template completeness without applying resources.
- DR evidence files should be maintained in:
    - `docs/dr/evidence/drills/`
    - `docs/dr/evidence/backups/`

Frontend:

```bash
cd frontend
npm install
npm run build
```

Frontend deployment (App Service, architecture path Browser -> App Service UI -> APIM -> API Gateway -> services):

1. Configure repository `vars` from the environment template files under `deploy/appservice/frontend/`.
2. Configure repository or environment `secrets`: `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`.
3. Run `.github/workflows/frontend-appservice.yml` (or push to `main` with frontend changes).

Backend deployment (GitHub Actions -> GHCR -> AKS):

1. Push backend changes to `main` or run `.github/workflows/build-and-push-images.yml` manually to publish service images to GHCR.
2. The publish workflow now emits both the requested tag and the commit SHA, so AKS can deploy an exact image version instead of relying on `latest`.
3. After the image publish workflow succeeds, `.github/workflows/redeploy-aks.yml` runs automatically and pins every service deployment to the triggering commit SHA.
4. For a manual redeploy, run `.github/workflows/redeploy-aks.yml` and set `imageTag` to the tag you want to pin.
5. Configure repository or environment secrets for the AKS workflow: `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`.
6. If you need to target another cluster or namespace, override the workflow inputs `resourceGroup`, `clusterName`, and `namespace`.

## Core services quick start (gateway + careplan + telemetry)

Use this when validating telemetry-by-patient and careplan responsibility routes through API gateway.

```powershell
Set-Location scripts
.\start-core-services.ps1
```

Optional flags:

```powershell
# include test phase while launching
.\start-core-services.ps1 -SkipTests:$false

# only stop listeners on 8080/8083/8087 and exit
.\start-core-services.ps1 -StopOnly
```

Stop all three services:

```powershell
Set-Location scripts
.\stop-core-services.ps1
```

After startup, verify:

```powershell
Invoke-WebRequest -UseBasicParsing "http://localhost:8080/api/telemetry/by-patient/pat-seed-1001?metricType=HEART_RATE"
Invoke-WebRequest -UseBasicParsing "http://localhost:8080/api/careplans/responsibility/pat-seed-1001"
```

## UC-03 end-to-end quick run

This quick flow validates Appointment Scheduling (UC-03) through API Gateway.

1. Start backend services required for UC-03.

```bash
# from repository root, use separate terminals
mvn -pl services/svc-consent spring-boot:run
mvn -pl services/svc-appointment spring-boot:run
mvn -pl services/api-gateway spring-boot:run
```

2. Optional: enable strict consent enforcement in appointment service.

```bash
set CONSENT_ENFORCEMENT_ENABLED=true
set CONSENT_INTEGRATION_BASE_URL=http://localhost:8084
```

3. Book an appointment through gateway.

```bash
curl -X POST "http://localhost:8080/api/appointments" ^
    -H "Content-Type: application/json" ^
    -d "{\"patientId\":\"pat-1001\",\"providerId\":\"prov-44\",\"scheduledAt\":\"2026-06-15T09:30:00Z\",\"channel\":\"VIDEO\"}"
```

Expected: `201 Created` with `data.status=BOOKED`.

4. Try duplicate booking for same provider and slot.

```bash
curl -X POST "http://localhost:8080/api/appointments" ^
    -H "Content-Type: application/json" ^
    -d "{\"patientId\":\"pat-1002\",\"providerId\":\"prov-44\",\"scheduledAt\":\"2026-06-15T09:30:00Z\",\"channel\":\"VIDEO\"}"
```

Expected: `409 Conflict` with error code `SLOT_ALREADY_BOOKED`.

5. Fetch available slots for the provider.

```bash
curl "http://localhost:8080/api/appointments/available-slots?providerId=prov-44&date=2026-06-15"
```

Expected: `200 OK` and `data.availableSlots` does not include `2026-06-15T09:30:00Z`.

## Local PostgreSQL profile (core services)

Use this mode when you need local Postgres-backed verification for consent, appointment, and notification services.

This profile runs SQL initialization in order on service startup:
1. DDL from `seed/local-postgres-schema.sql`
2. Seed data from `seed/local-postgres-data.sql`

### Option A: standalone local PostgreSQL (no Docker)

Create these databases in your local PostgreSQL instance before starting services:

```sql
CREATE DATABASE consentdb;
CREATE DATABASE appointmentdb;
CREATE DATABASE notificationdb;
```

### Option B: Docker Compose (optional)

1. Start local Postgres container.

```bash
docker compose -f deploy/local/docker-compose.postgres.yml up -d
```

2. Start core services with `local-postgres` profile in separate terminals.

```bash
set SPRING_PROFILES_ACTIVE=local-postgres
set CONSENT_DB_URL=jdbc:postgresql://localhost:5432/postgres
set CONSENT_DB_USERNAME=postgres
set CONSENT_DB_PASSWORD=postgres
mvn -f services/svc-consent/pom.xml spring-boot:run
```

```bash
set SPRING_PROFILES_ACTIVE=local-postgres
set APPOINTMENT_DB_URL=jdbc:postgresql://localhost:5432/postgres
set APPOINTMENT_DB_USERNAME=postgres
set APPOINTMENT_DB_PASSWORD=postgres
mvn -f services/svc-appointment/pom.xml spring-boot:run
```

```bash
set SPRING_PROFILES_ACTIVE=local-postgres
set NOTIFICATION_DB_URL=jdbc:postgresql://localhost:5432/postgres
set NOTIFICATION_DB_USERNAME=postgres
set NOTIFICATION_DB_PASSWORD=postgres
mvn -f services/svc-notification/pom.xml spring-boot:run
```

```bash
mvn -f services/api-gateway/pom.xml spring-boot:run
```

3. Validate booking flow through gateway.

```bash
curl -X POST "http://localhost:8080/api/appointments" ^
    -H "Content-Type: application/json" ^
    -d "{\"patientId\":\"pat-1001\",\"providerId\":\"prov-44\",\"scheduledAt\":\"2026-06-15T09:30:00Z\",\"channel\":\"VIDEO\"}"
```

4. Optional: enable consent enforcement and set consent integration URL.

```bash
set CONSENT_ENFORCEMENT_ENABLED=true
set CONSENT_INTEGRATION_BASE_URL=http://localhost:8084
```
