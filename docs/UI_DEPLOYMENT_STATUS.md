# UI Deployment Status (2026-06-22)

## Current State

| Component | Status | Details |
|-----------|--------|---------|
| **AKS (Backend)** | ✅ **OPERATIONAL** | 12 microservices, 3 nodes, all health endpoints responding |
| **Frontend CI/CD** | ⚠️ **CONFIGURED** | Workflow exists, auto-triggers on frontend/ changes |
| **App Service (UI)** | ❌ **NOT CREATED** | healthcarepatientengagement not in Azure resource group |
| **Frontend Build** | ⚠️ **CONDITIONAL** | Workflow can build but deploy will fail without App Service |

---

## Why UI Deployment Hasn't Started

The infrastructure bootstrap (`bootstrap-dev-azure.ps1`) provisions:
- ✅ Azure SQL Database (healthcare-dev)
- ✅ AKS Cluster (aks-hpe-dev2)
- ✅ Service Bus, Event Hub, Key Vault, APIM, Front Door, Traffic Manager
- ❌ **App Service (UI hosting)** - NOT in Bicep templates

**App Service must be created separately** before the frontend workflow can deploy.

---

## GitHub Actions Workflow Status

**File**: `.github/workflows/frontend-appservice.yml`

### Recent Runs:
| Run | Status | Commit | Date |
|-----|--------|--------|------|
| #50 | ❌ FAILED | "Bootstrap infrastructure hardening..." | 2026-06-22 11:15 UTC |
| #49 | ✅ SUCCESS | "Add post-teleconsultation summary UI" | 2026-06-20 10:08 UTC |
| #47 | ❌ FAILED | "Frontend UI Deploy (App Service)" | 2026-06-18 18:03 UTC |
| #46 | ✅ SUCCESS | "feat(ui): show page indicator in pager controls" | 2026-06-18 18:03 UTC |

**Reason for Failures**: Azure App Service `healthcarepatientengagement` does not exist in resource group `rg-azuser7080_mml.local-1nLQA`.

---

## How the Workflow Works

### Build Phase (GitHub Runner)
1. Checkout frontend source
2. npm ci + npm run build
3. Inject build-time environment variables:
   - `VITE_API_BASE_URL`: Points to APIM/API Gateway endpoint
   - `VITE_OIDC_*`: Entra OAuth2 endpoints
   - `VITE_EVENTS_*`: Optional WebSocket/SSE URLs
4. Validate no localhost references in build output
5. Upload dist/ + server.js as artifact

### Deploy Phase (GitHub Runner)
1. Download build artifact
2. Package release/dist + server.js into ZIP
3. Use **Kudu ZipDeploy** (OneDeploy) to push to App Service
4. Poll Kudu status endpoint until deployment completes

**Deployment Method**: Kudu ZipDeploy via Kudu REST API
- Requires: `AZURE_WEBAPP_PUBLISH_USERNAME` + `AZURE_WEBAPP_PUBLISH_PASSWORD` (secrets)
- Endpoint: `https://{appServiceName}.scm.azurewebsites.net/api/zipdeploy?isAsync=true`

---

## Next Steps to Enable Frontend Deployment

### Step 1: Create App Service (One-time Setup)

```powershell
# Variables
$resourceGroup = "rg-azuser7080_mml.local-1nLQA"
$location = "eastus"  # Must match Front Door origin region
$appServiceName = "healthcarepatientengagement"
$appServicePlanName = "appservice-plan-hpe-dev"

# Create App Service Plan
az appservice plan create `
  --name $appServicePlanName `
  --resource-group $resourceGroup `
  --sku B2 `
  --is-linux

# Create App Service (Node.js 24 runtime)
az webapp create `
  --resource-group $resourceGroup `
  --plan $appServicePlanName `
  --name $appServiceName `
  --runtime "node|24-lts"

# Enable health check (optional but recommended)
az webapp config set `
  --resource-group $resourceGroup `
  --name $appServiceName `
  --health-check-path "/health"

# Get publish credentials for GitHub Actions
az webapp deployment list-publishing-profiles `
  --name $appServiceName `
  --resource-group $resourceGroup `
  --output json > publishing-profile.json

# Extract and store in GitHub Secrets
# AZURE_WEBAPP_PUBLISH_USERNAME: <publishingUsername>
# AZURE_WEBAPP_PUBLISH_PASSWORD: <publishingPassword>
```

### Step 2: Add GitHub Actions Secrets

In GitHub repo: **Settings** → **Secrets and variables** → **Actions**

```
AZURE_WEBAPP_PUBLISH_USERNAME = <username from publishing profile>
AZURE_WEBAPP_PUBLISH_PASSWORD = <password from publishing profile>
```

### Step 3: Configure GitHub Variables (Repository Settings)

```
UI_APP_SERVICE_NAME = healthcarepatientengagement
UI_VITE_API_BASE_URL = https://api.healthcarepatientengagement.com  (or APIM endpoint)
UI_VITE_OIDC_CLIENT_ID = 526100bc-f963-4edd-8890-9011252bb554
UI_VITE_OIDC_AUTHORIZATION_ENDPOINT = https://login.microsoftonline.com/65087c47-0017-4258-8086-72832006d566/oauth2/v2.0/authorize
UI_VITE_OIDC_TOKEN_ENDPOINT = https://login.microsoftonline.com/65087c47-0017-4258-8086-72832006d566/oauth2/v2.0/token
UI_VITE_OIDC_LOGOUT_ENDPOINT = https://login.microsoftonline.com/65087c47-0017-4258-8086-72832006d566/oauth2/v2.0/logout
UI_VITE_OIDC_REDIRECT_URI = https://healthcarepatientengagement.azurewebsites.net
UI_VITE_OIDC_SCOPE = openid profile api://65087c47-0017-4258-8086-72832006d566/hpe-devx-api/access_as_user
UI_VITE_BYPASS_AUTH = false
```

### Step 4: Trigger Deployment

**Option A**: Push a frontend change to main
```bash
git commit --allow-empty -m "Trigger frontend deployment"
git push origin main
```

**Option B**: Manual workflow dispatch
GitHub → Actions → Frontend UI Deploy (App Service) → Run workflow

---

## Environment Configuration

### Development Environment

**Frontend Build Config** (`scripts/deploy-frontend-appservice.ps1`):
```powershell
VITE_API_BASE_URL = 'https://healthcarepatientengagement.azurewebsites.net'
VITE_OIDC_CLIENT_ID = '526100bc-f963-4edd-8890-9011252bb554'
VITE_OIDC_AUTHORIZATION_ENDPOINT = 'https://login.microsoftonline.com/65087c47-0017-4258-8086-72832006d566/oauth2/v2.0/authorize'
VITE_OIDC_REDIRECT_URI = 'https://healthcarepatientengagement.azurewebsites.net'
VITE_BYPASS_AUTH = 'false'
```

**App Service Networking**:
- Must be in same region as Front Door origin (eastus)
- Should have private endpoint or private link for secure APIM communication
- App Service Plan: B2 SKU (Standard) or higher

---

## Architecture: Frontend Deployment Flow

```
Developer Push (frontend/)
         ↓
GitHub Actions: build-and-push-images.yml (Backend)
         ↓
GitHub Actions: frontend-appservice.yml (Frontend)
         ├─ Build: npm run build (injects VITE_* env vars)
         ├─ Validate: No localhost references
         └─ Deploy: Kudu ZipDeploy → App Service
                    ↓
                    Kudu unzips and starts Node.js process
                    ↓
                    Browser: https://healthcarepatientengagement.azurewebsites.net
                    ↓
                    Frontend makes API calls → APIM endpoint
                    ↓
                    APIM routes → API Gateway (10.0.78.155:80)
                    ↓
                    Microservices (svc-patient, svc-appointment, etc.)
```

---

## Troubleshooting Deployment Failures

### If workflow fails at Build:
- Check `npm run build` logs for TypeScript/Vite errors
- Validate `VITE_API_BASE_URL` is set and valid
- Check for localhost hardcoding in frontend code

### If workflow fails at Deploy:
- **HTTP 401**: Publishing credentials (username/password) are wrong or expired
  - Solution: Regenerate credentials via Azure Portal → App Service → Deployment Center
- **HTTP 404**: App Service doesn't exist
  - Solution: Create App Service (see Step 1 above)
- **Deployment timeout**: Kudu is slow or service crashed
  - Check App Service logs: Azure Portal → App Service → Log Stream

### To debug locally:
```bash
cd frontend
npm run build
# Test build output
npx serve -s dist -p 3000
# Open http://localhost:3000
```

---

## Summary

- **Backend (AKS)**: ✅ Fully operational with all 12 services running
- **Frontend CI/CD**: ✅ Workflow configured and functional
- **Frontend Hosting (App Service)**: ❌ **MISSING** - must be created before deployments will succeed
- **Next Action**: Create Azure App Service using steps above, then re-trigger frontend workflow

