# GitHub Actions Setup for Healthcare Patient Engagement Platform

## Overview
This guide explains how to set up and use GitHub Actions workflows to automatically build Docker images and deploy them to AKS.

## Workflows Created

### 1. Build and Push Docker Images to GHCR
**File:** `.github/workflows/build-and-push-images.yml`

**Triggers:**
- Automatically on push to `main` or `develop` branches
- Manual trigger via `workflow_dispatch` (Actions tab)
- Only runs if changes detected in:
  - `services/` directory
  - `platform-common/` directory
  - `pom.xml`
  - Workflow file itself

**What it does:**
1. Sets up Java 17 and Maven
2. Builds parent pom and platform-common module
3. Builds Docker images for all 12 services
4. Pushes images to GHCR (ghcr.io/uttambhatia/healthcarepatientengagement/*)

**Configuration:**
- Registry: `ghcr.io`
- Owner: `uttambhatia`
- Repository: `healthcarepatientengagement`
- Tag: `latest` (customizable via workflow_dispatch)

### 2. Redeploy Services to AKS
**File:** `.github/workflows/redeploy-aks.yml`

**Triggers:**
- Automatically after successful build workflow
- Manual trigger via workflow_dispatch

**What it does:**
1. Configures kubectl with AKS credentials
2. Restarts all deployments to pull new images
3. Monitors rollout status for all services
4. Verifies connectivity to API Gateway

---

## Setup Instructions

### Step 1: Add GitHub Secrets

Add the following secrets to your GitHub repository (`Settings → Secrets and variables → Actions`):

1. **KUBE_CONFIG** (Required for AKS deployment)
   ```bash
   # Encode your kubeconfig file as base64
   cat ~/.kube/config | base64 -w 0 > /tmp/kubeconfig.b64
   # Copy the output and add as KUBE_CONFIG secret
   ```
   
   Or use Azure CLI:
   ```bash
   az aks get-credentials --resource-group "rg-azuser7080_mml.local-1nLQA" --name "aks-hpe-devx" --admin
   # Then encode the resulting config
   ```

2. **GITHUB_TOKEN** (Automatically available, used for GHCR authentication)
   - Already available in workflow context
   - Uses GITHUB_ACTOR (your username) for login

### Step 2: Commit and Push

```bash
# From repository root
git add .github/workflows/
git commit -m "feat: Add GitHub Actions workflows for GHCR builds and AKS deployment"
git push origin main
```

### Step 3: Trigger Build (Manual)

Option A: Automatic trigger (on next push to main/develop)
```bash
git push origin main
```

Option B: Manual trigger via GitHub UI
1. Go to **Actions** tab
2. Click **Build and Push Docker Images to GHCR**
3. Click **Run workflow** button
4. (Optional) Customize inputs: registry, owner, tag

---

## Usage Examples

### Rebuild All Images with Custom Tag

1. Go to **Actions → Build and Push Docker Images to GHCR**
2. Click **Run workflow**
3. Enter custom inputs:
   - `tag`: `v1.0.0` (or any other tag)
   - Click **Run workflow**

### Deploy Specific Version

1. Go to **Actions → Redeploy Services to AKS**
2. Click **Run workflow**
3. Enter:
   - `namespace`: `healthcare-dev`
   - `tag`: `v1.0.0` (or version to deploy)
4. Click **Run workflow**

### Monitor Workflow Execution

1. Open **Actions** tab
2. Click on active workflow run
3. Click on job for detailed logs
4. Check **Summary** section for deployment status

---

## Workflow Status Checks

### Build Workflow
✅ Success indicates:
- All 12 images built successfully
- All images pushed to GHCR
- Images ready for deployment

### Deploy Workflow
✅ Success indicates:
- All pods restarted and pulling new images
- All deployments reached Ready state
- Connectivity test passed

❌ Failures indicate:
- Check pod logs: `kubectl logs -n healthcare-dev <pod-name>`
- Check deployment status: `kubectl get deployments -n healthcare-dev`
- Review GitHub Actions logs for build/push errors

---

## Troubleshooting

### KUBE_CONFIG Not Set
**Error:** `Unable to access kubeconfig`

**Solution:**
```bash
# Get current kubeconfig
az aks get-credentials --resource-group "rg-azuser7080_mml.local-1nLQA" --name "aks-hpe-devx" --admin
cat ~/.kube/config | base64 -w 0
# Add output as KUBE_CONFIG secret
```

### Build Fails - Maven Error
**Error:** `Maven package failed for [service-name]`

**Solution:**
- Check if pom.xml files are valid
- Verify Java 17 is available
- Check build logs in GitHub Actions

### Images Not Updated
**Problem:** Pods still running old version

**Solution:**
1. Verify new image in GHCR:
   ```bash
   docker pull ghcr.io/uttambhatia/healthcarepatientengagement/svc-patient:latest
   ```
2. Force redeploy workflow manually
3. Check pod image: `kubectl describe pod -n healthcare-dev <pod-name>`

### AKS Rollout Timeout
**Error:** `deployment "svc-xxx" exceeded its progress deadline`

**Solution:**
1. Check pod status: `kubectl get pods -n healthcare-dev`
2. Review logs: `kubectl logs -n healthcare-dev <pod-name>`
3. Manually trigger redeploy workflow
4. Increase timeout in `.github/workflows/redeploy-aks.yml`

---

## Environment Variables

**Build Workflow:**
- `REGISTRY`: Container registry (ghcr.io)
- `OWNER`: Registry owner (uttambhatia)
- `REPOSITORY`: Repository name (healthcarepatientengagement)
- `TAG`: Image tag (latest or custom)

**Deploy Workflow:**
- `NAMESPACE`: Kubernetes namespace (healthcare-dev)
- `TAG`: Image tag to deploy
- `REGISTRY`, `OWNER`, `REPOSITORY`: Same as build workflow

---

## Next Steps

1. ✅ Add `KUBE_CONFIG` secret to GitHub repository
2. ✅ Commit workflow files to main/develop branch
3. ✅ Trigger build workflow manually or via git push
4. ✅ Monitor Actions tab for build completion
5. ✅ Verify pods are ready: `kubectl get deployments -n healthcare-dev`

---

## CI/CD Pipeline Summary

```
Git Push → Build Images → Push to GHCR → Trigger Deploy → Update AKS Pods
   ↓           ↓             ↓               ↓                ↓
  main/     Maven Build   docker push    Restart Pods    Health Check
 develop    Docker Build    Upload       Rollout Wait     API Gateway Test
```

---

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Docker Build and Push Action](https://github.com/docker/build-push-action)
- [Kubernetes Setup Action](https://github.com/azure/setup-kubectl)
