# Architecture Alignment Remediation Backlog

Date: 2026-05-31
Scope: Healthcare Patient Engagement platform alignment against HLD, FRD, NFR, and guardrails.

## Current Assessment Summary

- Structural alignment is good (service decomposition, k8s manifests, baseline API scaffolding).
- Production and compliance alignment is incomplete (security, persistence, real integrations, NFR controls, contract completeness).
- This backlog defines the minimum path to reach architecture-conformant implementation.

## Priority Model

- P0: Mandatory for compliance and production viability.
- P1: Required for reliability, operability, and integration completeness.
- P2: Hardening, optimization, and scale maturity.

## P0 - Mandatory Compliance and Security

### P0-1 Replace mock JWT decoders with real Entra/OIDC validation
- Requirement mapping:
  - SG-01 Entra ID + MFA
  - Zero Trust security in HLD
  - NFR-03 Security and Data Protection
- Current gap:
  - Services accept fabricated JWT with alg none.
- Evidence:
  - services/svc-patient/src/main/java/com/healthcare/patient/security/SecurityConfig.java
  - services/svc-appointment/src/main/java/com/healthcare/appointment/security/SecurityConfig.java
  - services/svc-careplan/src/main/java/com/healthcare/careplan/security/SecurityConfig.java
  - services/svc-consent/src/main/java/com/healthcare/consent/security/SecurityConfig.java
  - services/svc-medical-record/src/main/java/com/healthcare/medicalrecord/security/SecurityConfig.java
  - services/svc-notification/src/main/java/com/healthcare/notification/security/SecurityConfig.java
  - services/svc-telemetry/src/main/java/com/healthcare/telemetry/security/SecurityConfig.java
  - services/svc-device-ingestion/src/main/java/com/healthcare/deviceingestion/security/SecurityConfig.java
  - services/svc-alert-management/src/main/java/com/healthcare/alertmanagement/security/SecurityConfig.java
  - services/svc-identity-adapter/src/main/java/com/healthcare/identityadapter/security/SecurityConfig.java
  - services/svc-event-messaging/src/main/java/com/healthcare/eventmessaging/security/SecurityConfig.java
  - services/api-gateway/src/main/java/com/healthcare/gateway/security/SecurityConfig.java
- Remediation:
  - Configure issuer-uri/jwk-set-uri for Entra in all services.
  - Remove local JwtDecoder/ReactiveJwtDecoder beans that fabricate tokens.
  - Enforce role claim mapping and audience validation.
  - Add break-glass policy flow through identity adapter with auditable controls.
- Acceptance criteria:
  - Invalid signatures are rejected.
  - Tokens with alg none fail authentication.
  - Role-based authorization works with real identity provider claims.

### P0-2 Replace in-memory repositories with durable stores and FHIR persistence
- Requirement mapping:
  - DG-01 PHI in Azure Health Data Services (FHIR)
  - HLD Data View service-owned persistence
  - NFR-06 Auditability and Compliance
- Current gap:
  - Primary persistence is in-memory across domain services.
- Evidence:
  - services/svc-patient/src/main/java/com/healthcare/patient/repository/InMemoryPatientRepository.java
  - services/svc-appointment/src/main/java/com/healthcare/appointment/repository/InMemoryAppointmentRepository.java
  - services/svc-careplan/src/main/java/com/healthcare/careplan/repository/InMemoryCarePlanRepository.java
  - services/svc-consent/src/main/java/com/healthcare/consent/repository/InMemoryConsentRepository.java
  - services/svc-medical-record/src/main/java/com/healthcare/medicalrecord/repository/InMemoryMedicalRecordRepository.java
  - services/svc-notification/src/main/java/com/healthcare/notification/repository/InMemoryNotificationRepository.java
  - services/svc-telemetry/src/main/java/com/healthcare/telemetry/repository/InMemoryTelemetryRepository.java
  - services/svc-device-ingestion/src/main/java/com/healthcare/deviceingestion/repository/InMemoryDeviceIngestionRepository.java
  - services/svc-alert-management/src/main/java/com/healthcare/alertmanagement/repository/InMemoryAlertRepository.java
  - services/svc-identity-adapter/src/main/java/com/healthcare/identityadapter/repository/InMemoryIdentityRepository.java
  - services/svc-event-messaging/src/main/java/com/healthcare/eventmessaging/repository/InMemoryServiceBusMessageRepository.java
- Remediation:
  - Introduce JPA/Cosmos repositories by service data classification.
  - Persist PHI-related resources through FHIR API integration.
  - Add schema migration strategy for relational stores.
  - Keep repositories interface-first; phase out in-memory implementations.
- Acceptance criteria:
  - Service restarts do not lose state.
  - PHI writes flow to approved FHIR endpoints.
  - Data retention and retrieval are auditable.

### P0-3 Implement real external integration adapters (remove logging-only behavior)
- Requirement mapping:
  - AG-01 Event-driven integration via Service Bus/Event Grid
  - FHIR R4 interoperability
  - FRD section on event-driven behavior
- Current gap:
  - Adapters and messaging port mostly log calls without real delivery.
- Evidence:
  - platform-common/src/main/java/com/healthcare/platform/common/messaging/LoggingMessagingPort.java
  - services/svc-patient/src/main/java/com/healthcare/patient/integration/PatientFhirAdapter.java
  - services/svc-medical-record/src/main/java/com/healthcare/medicalrecord/integration/FhirAdapter.java
  - services/svc-careplan/src/main/java/com/healthcare/careplan/integration/CarePlanFhirAdapter.java
  - services/svc-event-messaging/src/main/java/com/healthcare/eventmessaging/integration/ServiceBusAdapter.java
- Remediation:
  - Add Service Bus publisher with retries, dead-letter handling, and idempotency.
  - Add FHIR client for patient, care plan, and medical record synchronization.
  - Add outbound integration health checks and circuit-breaker policies.
- Acceptance criteria:
  - AppointmentBooked, CarePlanUpdated, DeviceAlertRaised events are emitted and traceable.
  - FHIR resource operations are observable with correlation IDs.

### P0-4 Expand OpenAPI contract to full implemented and required domain surface
- Requirement mapping:
  - OpenAPI-first API governance
  - FRD module coverage completeness
- Current gap:
  - Contract includes only subset of platform endpoints.
- Evidence:
  - contracts/care-coordination-platform-openapi.yaml
  - services/svc-medical-record/src/main/java/com/healthcare/medicalrecord/controller/MedicalRecordController.java
  - services/svc-alert-management/src/main/java/com/healthcare/alertmanagement/controller/AlertController.java
  - services/svc-telemetry/src/main/java/com/healthcare/telemetry/controller/TelemetryController.java
  - services/svc-identity-adapter/src/main/java/com/healthcare/identityadapter/controller/IdentityController.java
  - services/svc-device-ingestion/src/main/java/com/healthcare/deviceingestion/controller/DeviceIngestionController.java
- Remediation:
  - Add full paths/schemas/security requirements for all modules.
  - Add error models and correlation-id headers in contract.
  - Generate contract validation tests against controllers.
- Acceptance criteria:
  - No implemented public endpoint is missing from OpenAPI.
  - CI fails on contract drift.

## P1 - Reliability, Orchestration, and Operability

### P1-1 Implement real saga orchestration with compensation
- Requirement mapping:
  - HLD process view on care lifecycle orchestration
  - NFR-05 reliability and fault tolerance
- Current gap:
  - Orchestration methods are placeholders, no saga state machine.
- Evidence:
  - services/svc-patient/src/main/java/com/healthcare/patient/service/PatientApplicationServiceImpl.java
  - services/svc-appointment/src/main/java/com/healthcare/appointment/service/AppointmentApplicationServiceImpl.java
  - services/svc-careplan/src/main/java/com/healthcare/careplan/service/CarePlanApplicationServiceImpl.java
- Remediation:
  - Define saga states/events/commands for onboarding and care workflows.
  - Implement compensating actions for downstream failures.
  - Persist saga state with retry semantics.
- Acceptance criteria:
  - End-to-end workflow survives transient failures.
  - Compensation paths are tested and auditable.

### P1-2 Complete API gateway routing and policy controls
- Requirement mapping:
  - Unified gateway control plane
  - Security policy enforcement at ingress/gateway
- Current gap:
  - Routes only configured for four services.
- Evidence:
  - services/api-gateway/src/main/java/com/healthcare/gateway/config/GatewayRoutesConfig.java
- Remediation:
  - Add routes for consent, medical-record, telemetry, device-ingestion, alert-management, identity-adapter, event-messaging.
  - Add route-level authz and rate-limits.
  - Add path rewrite and versioning conventions.
- Acceptance criteria:
  - All platform APIs are reachable through gateway only.
  - Gateway applies consistent security headers and policies.

### P1-3 Introduce autoscaling and disruption protections
- Requirement mapping:
  - NFR-01 HA
  - NFR-04 scalability
  - NFR-07 DR readiness
- Current gap:
  - No HPA and no PDB manifests.
- Evidence:
  - deploy/k8s
- Remediation:
  - Add HPA per critical service with CPU/memory and custom metrics.
  - Add PodDisruptionBudget for critical services.
  - Add anti-affinity/topology spread constraints.
- Acceptance criteria:
  - Platform tolerates node drain without major disruption.
  - Auto-scale triggers under synthetic load.

### P1-4 Strengthen observability and audit immutability
- Requirement mapping:
  - NFR-08 observability
  - NFR-06 auditability and compliance
- Current gap:
  - Basic correlation logs exist, but no immutable audit pipeline and limited metrics exposure.
- Evidence:
  - services/*/src/main/resources/application.yml
- Remediation:
  - Add OpenTelemetry tracing and centralized log shipping.
  - Implement append-only audit store for clinical/admin events.
  - Add SLO dashboards and alert policies.
- Acceptance criteria:
  - Each request trace spans gateway and downstream services.
  - Audit retrieval supports actor, action, timestamp filters.

## P2 - Productization and Hardening

### P2-1 Frontend real auth and API integration
- Requirement mapping:
  - FRD role-aware user flows
  - Zero trust and consent-driven access
- Current gap:
  - UI uses localStorage mock jwt and static module composition.
- Evidence:
  - frontend/src/auth/AuthProvider.tsx
  - frontend/src/App.tsx
  - frontend/src/services/apiClient.ts
  - frontend/src/services/platformApi.ts
- Remediation:
  - Integrate real OIDC/OAuth2 login flow.
  - Replace mock token issuance with secure auth code flow.
  - Wire modules to backend APIs and role-filtered content.
- Acceptance criteria:
  - No mock authentication code in production profile.
  - UI behavior differs by role and permission scope.

### P2-2 Expand test strategy to architecture-level quality gates
- Requirement mapping:
  - Delivery and governance guardrails
  - NFR confidence requirements
- Current gap:
  - Very low automated test coverage.
- Evidence:
  - services/svc-patient/src/test/java/com/healthcare/patient/service/PatientApplicationServiceImplTest.java
  - services/api-gateway/src/test/java/com/healthcare/gateway/service/BffDashboardServiceImplTest.java
- Remediation:
  - Add contract tests, integration tests, security tests, and resilience tests.
  - Add consumer-driven contract tests and performance smoke tests.
  - Enforce minimum coverage and mandatory test stages in CI.
- Acceptance criteria:
  - All services have unit + integration test suites.
  - Build fails when architecture quality gates fail.

### P2-3 APIM and environment parity alignment
- Requirement mapping:
  - AG-01 all external APIs behind APIM
  - Multi-region and policy governance
- Current gap:
  - Current ingress points directly to gateway; APIM policy artifacts not present in repo.
- Evidence:
  - deploy/k8s/api-gateway/ingress.yaml
- Remediation:
  - Define APIM front-door integration and policy set.
  - Introduce environment-specific release templates and policy checks.
  - Add region failover runbooks and DR drills.
- Acceptance criteria:
  - Public API traffic path is APIM -> gateway -> services.
  - Release gates validate policy compliance before deploy.

## Suggested Execution Sequence

### Phase 1 (Week 1-2)
- Complete P0-1, P0-2 architecture and dependency decisions.
- Implement P0-3 for at least patient, appointment, careplan event and FHIR flows.
- Start contract expansion P0-4 with endpoint inventory.

### Phase 2 (Week 3-4)
- Finish P0 items across all services.
- Implement P1-2 gateway completion and P1-3 scaling/disruption controls.
- Add first wave observability and immutable audit pipeline from P1-4.

### Phase 3 (Week 5-6)
- Implement P1-1 full saga orchestration with compensation tests.
- Execute P2-1 frontend auth and real API wiring.
- Establish P2-2 comprehensive automated quality gates.

## Service Ownership Matrix (Proposed)

- api-gateway: P0-1, P1-2, P2-3
- svc-patient: P0-1, P0-2, P0-3, P1-1
- svc-appointment: P0-1, P0-2, P0-3, P1-1
- svc-careplan: P0-1, P0-2, P0-3, P1-1
- svc-consent: P0-1, P0-2, P0-3
- svc-medical-record: P0-1, P0-2, P0-3
- svc-notification: P0-1, P0-2, P0-3
- svc-telemetry: P0-1, P0-2, P0-3
- svc-device-ingestion: P0-1, P0-2, P0-3
- svc-alert-management: P0-1, P0-2, P0-3
- svc-identity-adapter: P0-1, P0-2, P0-3
- svc-event-messaging: P0-1, P0-2, P0-3
- frontend: P2-1
- platform-common: P0-3, P1-4

## Implementation Kickoff (Approved Decisions Baseline)

This section converts approved architecture decisions into immediate execution tasks.

### K0-1 Azure SQL Single Database migration baseline
- Requirement mapping:
  - Data platform decision: Azure SQL Single Database per environment
  - NFR-06 auditability and durability
- Scope:
  - Replace production-target runtime assumptions for PostgreSQL/H2 with Azure SQL connectivity patterns.
  - Introduce migration plan for service schemas and seed strategy by environment.
  - Define managed identity based database authentication path and remove secret-first dependency where feasible.
- Initial deliverables:
  - Environment variable contract for Azure SQL connection and auth mode.
  - Service-by-service migration checklist from in-memory persistence to durable repositories.
  - Rollback strategy for data-layer deployment.
- Acceptance criteria:
  - Services can run in deployment profile using Azure SQL endpoints.
  - Database access path supports managed identity and private networking requirements.

### K0-2 DR controls for backup, replication, and synchronization
- Requirement mapping:
  - NFR-07 DR readiness
  - Approved DR scope decision (backup + replication + sync mandatory)
- Scope:
  - Define backup cadence and retention policy per data store.
  - Define cross-region replication topology and acceptable lag thresholds.
  - Define synchronization integrity checks and failover/failback runbooks.
- Initial deliverables:
  - DR matrix per component (data, messaging, configuration, secrets).
  - Drill schedule for restore validation and region failover exercises.
  - Evidence checklist for RTO/RPO compliance.
- Acceptance criteria:
  - Backup/restore drills are documented and repeatable.
  - Replication lag and sync integrity checks are monitored and alertable.
  - Failover and failback runbooks are validated in non-production.

### K0-3 Actor-based RBAC and user provisioning model
- Requirement mapping:
  - SG-01 Entra ID + MFA
  - FRD use-case actor alignment
  - NFR-03 security and data protection
- Scope:
  - Map use-case actors to human roles and machine identities.
  - Enforce gateway-level coarse authorization and service-level fine-grained checks.
  - Introduce explicit deny paths for actor-role mismatches and ownership violations.
- Initial deliverables:
  - Actor-to-role matrix with use-case permissions (patient, doctor, coordinator, admin, system integration, device identity).
  - Entra app-role and group assignment model including MFA policy baseline.
  - Authorization test cases for positive and negative role scenarios.
- Acceptance criteria:
  - Each critical endpoint has an explicit allowed-role set.
  - Machine identities cannot access human-only APIs and vice versa.
  - Audit logs capture actor, role, decision, and correlation ID for authorization outcomes.

#### Initial RBAC matrix v0 (implementation seed)

| Capability area | PATIENT_USER | DOCTOR_USER | CARE_COORDINATOR_USER | PLATFORM_ADMIN_USER | SYSTEM_INTEGRATION_ID | DEVICE_IDENTITY |
|---|---|---|---|---|---|---|
| Patient profile (create self / read own) | ALLOW | LIMITED_READ | LIMITED_READ | ADMIN_OVERRIDE | DENY | DENY |
| Consent management | ALLOW_OWN | READ | READ_WRITE_SCOPED | AUDIT_READ | DENY | DENY |
| Appointment scheduling | ALLOW_OWN | READ_ASSIGNED | READ_WRITE_QUEUE | AUDIT_READ | EVENT_TRIGGER_ONLY | DENY |
| Care plan management | READ_OWN | READ_WRITE_CLINICAL | READ_WRITE_OWNER | AUDIT_READ | EVENT_TRIGGER_ONLY | DENY |
| Teleconsultation | JOIN_OWN | HOST_AND_UPDATE | OPERATIONS_READ | AUDIT_READ | DENY | DENY |
| Medical record (FHIR) | READ_SCOPED | READ_WRITE | READ_SCOPED | AUDIT_READ | SYNC_ONLY | DENY |
| Notification workflows | READ_OWN | READ_ASSIGNED | TRIGGER_AND_READ | POLICY_ADMIN | PROCESS_EVENTS | DENY |
| Telemetry ingest | DENY | DENY | DENY | AUDIT_READ | PROCESS_EVENTS | WRITE_ONLY |
| Alert management | READ_OWN_ALERTS | READ_ESCALATE | READ_ESCALATE | THRESHOLD_ADMIN | GENERATE_ONLY | DENY |
| Audit and monitoring | DENY | LIMITED_READ | OPERATIONAL_READ | FULL_READ | WRITE_EVENTS | DENY |

Matrix implementation notes:
- Map human roles to Entra app roles and security groups.
- Use machine identities for SYSTEM_INTEGRATION_ID and DEVICE_IDENTITY with dedicated scopes.
- Enforce ownership and tenant checks at service level even when gateway authorization passes.

### K0-4 First implementation checkpoint
- Gate to move from kickoff to full execution:
  - K0-1, K0-2, K0-3 deliverables reviewed and approved.
  - Technical owners assigned per service and cross-cutting platform areas.
  - Sprint backlog updated with story-level tasks and dependency order.

#### Execution progress update (2026-06-05)
- Completed implementation slice:
  - Gateway route-level RBAC policies and integration tests.
  - Service-level RBAC refinements for appointment, careplan, and patient services.
  - Ownership enforcement for patient-scoped access at controller and teleconsultation service paths.
  - Ownership enforcement expanded to consent, telemetry patient-scoped queries, and medical-record controller flows.
  - JWT decoder hardening completed across gateway and service security configs with required issuer + audience validation for security-enabled mode.
  - Security configuration templates aligned across all service `application.yml` files with explicit `platform.security.enabled` and `platform.security.oauth2.audience` placeholders.
  - Centralized Entra configuration contract documented in `docs/Azure_Entra_OAuth2_Configuration.md`.
  - AKS deployment manifests aligned for all services with OAuth2 runtime env wiring (security enabled flag + issuer/audience/jwk-set-uri sourced from `platform-secrets`).
  - Added baseline CPU/memory requests and limits to all service AKS deployments to eliminate no-resource scheduling risk.
  - CI guardrail added to validate k8s OAuth2 env wiring via `scripts/validate-k8s-security-env.sh` before build/test.
  - CI guardrail added to validate deployment baseline hygiene (`readinessProbe`, `livenessProbe`, and resources requests/limits) via `scripts/validate-k8s-deployment-baseline.sh`.
  - CI guardrail added to validate service/ingress routing hygiene (selector, targetPort/containerPort, and ingress backend mapping) via `scripts/validate-k8s-routing-hygiene.sh`.
  - AKS DB deployments aligned to single Azure SQL contract by wiring all service-specific `*_DB_URL` env vars to shared secret key `azure-sql-jdbc-url` with companion username/password keys and managed identity client id.
  - CI guardrail added to validate Azure SQL + managed identity env wiring via `scripts/validate-k8s-azure-sql-env.sh`.
  - Private endpoint dependency env wiring added across backend services for Service Bus, Event Hubs, and Key Vault, with CI validation via `scripts/validate-k8s-private-endpoint-env.sh`.
  - DR artifacts baseline established with `docs/Disaster_Recovery_Runbook.md` and CI section checks via `scripts/validate-dr-readiness-artifacts.sh`.
  - DR operational evidence templates added (`docs/dr/DR_Drill_Report_Template.md`, `docs/dr/DR_Backup_Verification_Log_Template.md`) and enforced by DR artifact validation.
  - Added scheduled guardrail automation via `.github/workflows/infra-dr-guardrails.yml` to run infra/DR checks monthly, upload `guardrail-report.txt`, and fail on guardrail violations.
  - Added DR evidence repository structure (`docs/dr/evidence/drills`, `docs/dr/evidence/backups`) with starter monthly artifacts and validator enforcement.
  - Added Kubernetes NetworkPolicy manifests for gateway and all backend services to enforce ingress restriction, with CI validation via `scripts/validate-k8s-networkpolicy-hygiene.sh`.
  - Added HPA and PodDisruptionBudget manifests for critical services (gateway, patient, appointment, careplan, consent, medical-record, telemetry) with CI validation via `scripts/validate-k8s-resilience-hygiene.sh`.
  - Added scheduling resilience controls (pod anti-affinity + zone topology spread constraints) for critical services with CI validation via `scripts/validate-k8s-scheduling-hygiene.sh`.
  - Added baseline OpenTelemetry endpoint wiring across service deployments with CI validation via `scripts/validate-k8s-observability-env.sh`.
  - Added period-based DR evidence freshness validation (monthly backup log + quarterly drill report) via `scripts/validate-dr-evidence-freshness.sh`.
  - Added runtime security baseline in deployment manifests (runAsNonRoot, RuntimeDefault seccomp, no privilege escalation, read-only root FS, dropped Linux capabilities) with CI validation via `scripts/validate-k8s-runtime-security-hygiene.sh`.
  - Added rollout safety controls for critical services (RollingUpdate strategy with maxUnavailable=0/maxSurge=1 and startupProbe) with CI validation via `scripts/validate-k8s-rollout-hygiene.sh`.
  - Added OpenAPI controller-coverage guardrail via `scripts/validate-openapi-controller-coverage.sh` and CI enforcement to fail on contract drift between implemented controller mappings and contract paths.
  - Expanded OpenAPI consent contract coverage for `/consents/history` and `/consents/check-access` including request parameters and response schemas.
  - Added OpenAPI operation-parity guardrail via `scripts/validate-openapi-operation-parity.sh` and CI enforcement to fail on method-level and path/query parameter drift between implemented controller mappings and contract operations.
  - Added integration adapter readiness guardrail via `scripts/validate-integration-adapter-readiness.sh` and CI enforcement for FHIR (patient/careplan/medical-record) and Service Bus (event-messaging) configuration contracts, including required health endpoint exposure and deployment env-to-secret wiring.
  - Added integration reliability hygiene guardrail via `scripts/validate-integration-reliability-hygiene.sh` and CI enforcement for `platform.messaging.retryAttempts` + `deadLetterQueue` configuration and adapter retry-exhaustion failure handling patterns.
  - Added adapter contract-test guardrail via `scripts/validate-integration-adapter-contract-tests.sh` and CI enforcement with focused retry-exhaustion tests for `PatientFhirAdapter`, `ServiceBusAdapter`, `FhirAdapter` (medical-record), and `CarePlanFhirAdapter`.
  - Added correlation-id propagation guardrail via `scripts/validate-integration-correlation-id-propagation.sh` and CI enforcement to require explicit `X-Correlation-Id` outbound assertions in all critical adapter contract tests.
  - Added API gateway route coverage guardrail via `scripts/validate-api-gateway-route-coverage.sh` and CI enforcement to require every platform service route to remain declared in `GatewayRoutesConfig`.
  - Added API gateway security policy guardrail via `scripts/validate-api-gateway-security-policy.sh` and CI enforcement to require route-level authorization and `X-Correlation-Id` response-header exposure remain configured in `SecurityConfig`.
  - Added correlation-id contract guardrail via `scripts/validate-correlation-id-contract.sh` and CI enforcement to keep reusable OpenAPI `X-Correlation-Id` request-header path coverage plus success (`200`/`201`) and standard error response-header declarations aligned with runtime propagation filters and outbound adapter headers.
- Verification evidence (targeted integration/security tests):
  - `mvn -pl services/svc-appointment "-Dtest=AppointmentControllerIntegrationTest,TeleconsultationWorkflowServiceTest" test`
  - `mvn -pl services/svc-careplan -Dtest=CarePlanResponsibilityControllerIntegrationTest test`
  - `mvn -pl services/svc-patient -Dtest=PatientControllerIntegrationTest test`
  - `mvn -pl services/svc-consent -Dtest=ConsentControllerIntegrationTest test`
  - `mvn -pl services/svc-telemetry -Dtest=TelemetryControllerIntegrationTest test`
  - `mvn -pl services/svc-medical-record -Dtest=MedicalRecordControllerIntegrationTest test`
  - `mvn -pl services/api-gateway -Dtest=GatewaySecurityRbacIntegrationTest test` (updated test properties for issuer/audience)
  - `mvn -pl services/api-gateway,services/svc-patient,services/svc-consent -DskipTests compile`
- Current checkpoint status:
  - Authorization and ownership controls are now enforced and test-validated on key patient-facing read/write flows.
  - Residual scope remains for full Entra token validation hardening (P0-1) and broader endpoint coverage beyond the current high-risk paths.

## Definition of Aligned Implementation

The platform is considered aligned when all below are true:
- No mock security/token logic in runtime path.
- No in-memory data persistence in production profile.
- Contract is complete and enforced in CI.
- Required events are delivered to real messaging infrastructure.
- FHIR and PHI handling complies with architecture guardrails.
- HA/scaling/disruption controls are enforced in k8s.
- Audit and observability satisfy NFR acceptance criteria.
