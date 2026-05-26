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
mvn test
```

Frontend:

```bash
cd frontend
npm install
npm run build
```
