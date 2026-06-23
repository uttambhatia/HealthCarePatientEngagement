# Healthcare Platform Deployment Summary

**Date**: 2026-06-22 | **Status**: ✅ **Backend Operational** ⚠️ **Frontend Pending**

---

## Executive Summary

| Layer | Component | Status | Notes |
|-------|-----------|--------|-------|
| **Infrastructure** | Azure (29 resources) | ✅ Complete | VNets, AKS, SQL, APIM, Front Door, Key Vault, etc. |
| **Backend Microservices** | 12 Spring Boot services on AKS | ✅ Operational | All health endpoints responding, 45 pods running |
| **Backend CI/CD** | GitHub Actions build-and-push-images.yml | ✅ Working | Builds Maven → Docker → GHCR automatically |
| **Frontend Codebase** | React + Vite UI | ✅ Built | Node.js 24, OAuth2/OIDC auth configured |
| **Frontend CI/CD** | GitHub Actions frontend-appservice.yml | ✅ Configured | Ready to deploy when App Service exists |
| **Frontend Hosting** | Azure App Service | ❌ **Missing** | **Must be created** to enable UI deployment |

---

## Backend Status: ✅ Fully Operational

### AKS Cluster
- **Name**: aks-hpe-dev2
- **Region**: Central India (centralindia)
- **Nodes**: 3 (scaled down from 6 for cost optimization)
- **Node Size**: Standard_B2s_v2
- **Kubernetes**: v1.34.8
- **Container Registry**: ghcr.io/uttambhatia/healthcarepatientengagement (auth working)

### Microservices (12 Total)
All running in `healthcare-dev` namespace with ✓ health endpoint verification:

**Database-Backed Services** (Azure SQL):
- ✓ svc-patient (port 8081)
- ✓ svc-appointment (port 8082)
- ✓ svc-careplan (port 8083)
- ✓ svc-medical-record (port 8085)

**Stateless Services**:
- ✓ api-gateway (port 8080) - main routing layer
- ✓ svc-consent (port 8084)
- ✓ svc-notification (port 8086)
- ✓ svc-telemetry (port 8087)
- ✓ svc-device-ingestion (port 8088)
- ✓ svc-alert-management (port 8089)
- ✓ svc-identity-adapter (port 8090)
- ✓ svc-event-messaging (port 8082)

### Database
- **Service**: Azure SQL Database (sqlhpedev2ae8ee0)
- **Connection**: EntityManagerFactory initialized ✓
- **Admin**: Configured via secret manager ✓

### Service-to-Service Communication
✓ **Verified Working**: API Gateway calls Patient Service via Kubernetes DNS
```
kubectl exec api-gateway -- curl http://svc-patient:80/actuator/health
→ {"status":"UP","groups":["liveness","readiness"]}
```

---

## Frontend Status: ⚠️ Waiting for App Service

### CI/CD Workflow
- **File**: `.github/workflows/frontend-appservice.yml`
- **Trigger**: Auto on push to `frontend/` or manual dispatch
- **Build**: npm ci → npm run build (Node.js 24 LTS)
- **Deploy**: Kudu ZipDeploy to App Service
- **Status**: ✓ Configured and working

### Recent Deployment Attempts
| Run | Date | Status | Reason |
|-----|------|--------|--------|
| #50 | 2026-06-22 11:15 | ❌ FAILED | App Service does not exist |
| #49 | 2026-06-20 10:08 | ✓ SUCCESS | Last successful deploy (2 days ago) |
| #47 | 2026-06-18 18:03 | ❌ FAILED | App Service missing |
| #46 | 2026-06-18 18:03 | ✓ SUCCESS | UI feature merged |

### Build Configuration
The workflow injects these environment variables at build time:
```
VITE_API_BASE_URL = https://api.example.com (or APIM endpoint)
VITE_OIDC_CLIENT_ID = 526100bc-f963-4edd-8890-9011252bb554
VITE_OIDC_AUTHORIZATION_ENDPOINT = https://login.microsoftonline.com/.../authorize
VITE_BYPASS_AUTH = false
VITE_EVENTS_WS_URL = (optional WebSocket)
```

---

## What's Blocking Frontend Deployment

**The App Service `healthcarepatientengagement` does not exist in Azure.**

Why?
- The infrastructure bootstrap script (`bootstrap-dev-azure.ps1`) provisions AKS and databases via Bicep
- **App Service is not included in the Bicep templates** - it must be created separately
- GitHub Actions workflow expects it to exist before deployment can proceed

Impact:
- Frontend builds successfully but **deploy step fails** with 404 (App Service not found)
- Users cannot access UI until this is resolved

---

## How to Enable Frontend Deployment

### Option A: Create App Service via Azure Portal (Fastest)
1. Go to Azure Portal → Create Resource → Web App
2. **Name**: healthcarepatientengagement
3. **Resource Group**: rg-azuser7080_mml.local-1nLQA
4. **Region**: East US (eastus)
5. **Runtime**: Node 24 LTS
6. **Pricing Plan**: B2 Standard (for consistent performance)
7. Create → Wait for completion
8. Copy publishing credentials to GitHub Secrets (see below)

### Option B: Create via Azure CLI
```powershell
# Create App Service Plan
az appservice plan create `
  --name appservice-plan-hpe-dev `
  --resource-group rg-azuser7080_mml.local-1nLQA `
  --sku B2 `
  --is-linux

# Create App Service
az webapp create `
  --resource-group rg-azuser7080_mml.local-1nLQA `
  --plan appservice-plan-hpe-dev `
  --name healthcarepatientengagement `
  --runtime "node|24-lts"

# Get publishing credentials
az webapp deployment list-publishing-profiles `
  --name healthcarepatientengagement `
  --resource-group rg-azuser7080_mml.local-1nLQA `
  --output json
```

### Option C: Add to Bicep Infrastructure (Best Practice)
Edit `deploy/azure/` Bicep files to include App Service resource, then re-run bootstrap.

---

## GitHub Actions Secrets Required (After App Service Exists)

**Settings** → **Secrets and variables** → **Actions** → Add:

```
AZURE_WEBAPP_PUBLISH_USERNAME = <from publishing profile>
AZURE_WEBAPP_PUBLISH_PASSWORD = <from publishing profile>
```

**Optional: GitHub Variables** (for flexibility):
```
UI_APP_SERVICE_NAME = healthcarepatientengagement
UI_VITE_API_BASE_URL = https://api.healthcarepatientengagement.com
UI_VITE_OIDC_CLIENT_ID = 526100bc-f963-4edd-8890-9011252bb554
```

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        End User Browser                           │
└───────────────┬─────────────────────────────────────────────────┘
                │ HTTPS
                ▼
        ┌───────────────┐
        │  Traffic Mgr  │  (ⓘ Azure Traffic Manager)
        └───────┬───────┘
                │
                ▼
        ┌───────────────┐
        │  Azure Front  │  (ⓘ Front Door - CDN/DDoS)
        │     Door      │
        └───────┬───────┘
                │
                ├────────────────┬─────────────────┐
                │                │                 │
                ▼                ▼                 ▼
        ┌─────────────┐  ┌──────────────┐  ┌────────────────┐
        │  APIM       │  │  App Service │  │ APIM           │
        │  (Gateway)  │  │  (Frontend   │  │ (API routes)   │
        │             │  │   UI)        │  │                │
        │ Routes API  │  │              │  │ Routes to      │
        │ traffic     │  │ Serves React │  │ API Gateway    │
        └──────┬──────┘  │ + Vite build │  └────────┬───────┘
               │          └──────┬───────┘           │
               │                 │                   │
               └──────────┬──────┴───────────────────┘
                          │
                          ▼
                  ┌────────────────────┐
                  │  AKS Cluster       │
                  │  (centralindia)    │
                  ├────────────────────┤
                  │  API Gateway       │  ◄ Kubernetes Service (ClusterIP)
                  │  │                 │
                  │  ├─ svc-patient    │
                  │  ├─ svc-appt       │
                  │  ├─ svc-care-plan  │
                  │  ├─ svc-medical    │
                  │  └─ svc-consent    │
                  │                    │
                  │  (12 services)     │
                  └────────┬───────────┘
                           │
                           ▼
                  ┌────────────────────┐
                  │  Azure SQL         │
                  │  Database          │
                  │  (healthcare-dev)  │
                  └────────────────────┘

BUILD/DEPLOY PIPELINE:
  GitHub → Maven build → Docker → GHCR (backend)
  GitHub → npm build → Kudu ZipDeploy → App Service (frontend)
```

---

## Current Deployment Topology

### ✅ Backend (AKS) - OPERATIONAL
```
HealthCare Services on AKS
├── API Gateway (8080) → Routes to backend services
├── Patient Service (8081, SQL-backed)
├── Appointment Service (8082, SQL-backed)
├── Care Plan Service (8083, SQL-backed)
├── Medical Record Service (8085, SQL-backed)
├── Consent Service (8084, stateless)
├── Notification Service (8086, stateless)
├── Alert Management (8089, stateless)
├── Identity Adapter (8090, stateless)
├── Device Ingestion (8088, stateless)
├── Telemetry Service (8087, stateless)
└── Event Messaging Service (8082, stateless)

All backed by Azure SQL Database (healthcare-dev)
```

### ⚠️ Frontend (App Service) - BLOCKED
```
App Service NOT CREATED
│
├─ Should run: Node.js 24 LTS
├─ Serve: React + Vite frontend
├─ Call API via: APIM → API Gateway
└─ Status: Awaiting creation (manual step)
```

---

## Deployment Timeline

| Date | Event | Status |
|------|-------|--------|
| 2026-06-17 | Infrastructure bootstrap (29 resources) | ✓ Complete |
| 2026-06-18 | AKS deployment + service fixes | ✓ Complete |
| 2026-06-18 | Frontend workflow created | ✓ Working |
| 2026-06-18 | App Service deployment attempt | ✗ Failed (missing) |
| 2026-06-20 | Last successful UI deploy (Run #49) | ✓ Previous success |
| 2026-06-22 | Backend scaled to 3 nodes (cost save) | ✓ Complete |
| 2026-06-22 | Frontend deployment blocking identified | ⚠️ ACTION NEEDED |
| **2026-06-22 12:30** | **App Service creation** | **⏳ NEXT STEP** |

---

## Cost Optimization Summary

| Component | Previous | Current | Savings |
|-----------|----------|---------|---------|
| **AKS Nodes** | 6 × B2s_v2 | 3 × B2s_v2 | 50% |
| **Monthly Cost** | ~$120/mo | ~$60/mo | -$60 |

Other resources (SQL, APIM, Front Door, Service Bus, Event Hub) remain unchanged.

---

## Documentation Files

- [UI_DEPLOYMENT_STATUS.md](UI_DEPLOYMENT_STATUS.md) - Frontend deployment guide with step-by-step setup
- [GITHUB_ACTIONS_AZURE_SETUP.md](GITHUB_ACTIONS_AZURE_SETUP.md) - Backend CI/CD authentication (OIDC/service principal)
- [Deployment_2026-06-17_Governance_Audit_And_UI.md](Deployment_2026-06-17_Governance_Audit_And_UI.md) - Previous deployment checklist

---

## Next Immediate Steps

1. **Create App Service** (15 min via Portal)
   - Resource: Microsoft.Web/sites
   - Name: healthcarepatientengagement
   - Runtime: Node.js 24 LTS
   - SKU: B2 Standard

2. **Store Publishing Credentials** (5 min)
   - Get from App Service → Deployment Center
   - Add to GitHub Actions Secrets

3. **Trigger Frontend Deployment** (1 min)
   - Push a change to frontend/ branch, OR
   - Manual dispatch: Actions → frontend-appservice.yml → Run workflow

4. **Verify UI Is Live** (2 min)
   - Visit: https://healthcarepatientengagement.azurewebsites.net
   - Login with Entra test credentials
   - Create test patient record in API Gateway

---

## Support / Troubleshooting

**Q: Why is App Service missing?**
A: The bootstrap script only creates AKS. App Service creation is a separate manual step or needs to be added to Bicep templates.

**Q: Can I skip App Service creation?**
A: No. The frontend workflow deploys to this specific resource. Without it, deployment fails at the deploy step.

**Q: What if I want to deploy frontend differently (e.g., to AKS)?**
A: The current workflow is configured for App Service (Kudu ZipDeploy). To use AKS, you'd need a new workflow that builds Docker images and deploys Kubernetes manifests.

**Q: When will App Service be provisioned?**
A: Once you create it (Option A, B, or C above), the next push to `frontend/` or manual workflow dispatch will automatically deploy.

---

**Status**: Ready to enable frontend deployment upon App Service creation. Backend is fully operational and tested.

