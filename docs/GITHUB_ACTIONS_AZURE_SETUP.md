# GitHub Actions Azure OIDC Setup

## Status
The redeploy-aks.yml workflow is ready but requires Azure credentials to enable automated deployments.

## Options

### Option 1: OIDC Federated Credentials (Recommended - Zero Secrets)
Requires Azure subscription Owner or IAM Admin to complete.

```powershell
# 1. Create service principal
$sp = az ad sp create-for-rbac `
  --name "github-aks-deployer" `
  --role "Contributor" `
  --scopes "/subscriptions/4116346b-5ded-4f3b-8387-f8d055802adc" `
  -o json | ConvertFrom-Json

$clientId = $sp.appId
$tenantId = $sp.tenant
$subscriptionId = "4116346b-5ded-4f3b-8387-f8d055802adc"

Write-Host "Client ID: $clientId"
Write-Host "Tenant ID: $tenantId"
Write-Host "Subscription ID: $subscriptionId"

# 2. Create federated identity credential
$fedCred = @{
  name = "github-oidc-credential"
  issuer = "https://token.actions.githubusercontent.com"
  subject = "repo:uttambhatia/HealthCarePatientEngagement:ref:refs/heads/main"
  description = "GitHub Actions OIDC for AKS deployment"
  audiences = @("api://AzureADTokenExchange")
}

az ad app federated-credential create `
  --id $clientId `
  --parameters $fedCred

# 3. Add to GitHub Actions secrets (GitHub > Settings > Secrets and variables > Actions)
# AZURE_CLIENT_ID: <clientId>
# AZURE_TENANT_ID: <tenantId>
# AZURE_SUBSCRIPTION_ID: <subscriptionId>
```

### Option 2: Service Principal (Fallback - Requires Secret Storage)
If OIDC federated credentials cannot be set up:

```powershell
# Get credentials from existing SP or create new one
$creds = az ad sp create-for-rbac --name "github-aks-deployer" -o json
# Store entire JSON output as AZURE_CREDENTIALS secret in GitHub
```

## Current State
- **Cluster**: aks-hpe-dev2 (3 nodes, centralindia)
- **Resource Group**: rg-azuser7080_mml.local-1nLQA
- **Namespace**: healthcare-dev
- **Deployments**: 12 services, all available
- **Container Registry**: ghcr.io/uttambhatia/healthcarepatientengagement (GHCR auth working)

## Workflow Details
File: `.github/workflows/redeploy-aks.yml`
- Trigger: Auto-runs after build-and-push-images.yml completes
- Steps: Azure login → kubectl apply manifests → scale nodepool if needed
- Current issue: Azure login fails without credentials

## Next Steps
1. **Ask subscription Owner/IAM Admin** to run Option 1 setup above
2. **Add secrets to GitHub repository**: Settings → Secrets and variables → Actions
3. **Verify**: Push to main → Watch GitHub Actions → Confirm Deploy workflow succeeds

## Cost State
- **Current**: 3 nodes (Standard_B2s_v2) = ~$60/month
- **Previous**: 6 nodes = ~$120/month
- **Savings**: 50% reduction post-validation
