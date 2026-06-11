# Quick Start: GitHub Actions Workflows

## ⚡ 3 Steps to Enable CI/CD

### Step 1: Get Your kubeconfig (5 minutes)
```bash
# Get your AKS credentials in base64
az aks get-credentials --resource-group "rg-azuser7080_mml.local-1nLQA" --name "aks-hpe-devx" --admin
cat ~/.kube/config | base64 -w 0

# Copy the output (long base64 string)
```

### Step 2: Add GitHub Secret (2 minutes)
1. Go to your GitHub repo
2. **Settings → Secrets and variables → Actions**
3. Click **New repository secret**
4. Name: `KUBE_CONFIG`
5. Value: Paste the base64 string from Step 1
6. Click **Add secret**

### Step 3: Push Workflow Files (1 minute)
```bash
cd f:\StackRoute\U2A\HealthCarePatientEngagement
git add .github/workflows/
git add GITHUB_ACTIONS_SETUP.md
git commit -m "ci: add github actions workflows for docker build and aks deploy"
git push origin main
```

---

## 🚀 Run Your First Build

### Option A: Automatic (on next push)
```bash
# Any commit to main/develop that touches services/ or pom.xml triggers build
git push origin main
```

### Option B: Manual Trigger
1. Go to GitHub → **Actions** tab
2. Select **Build and Push Docker Images to GHCR**
3. Click **Run workflow** (use workflow_dispatch)
4. Leave defaults or customize tag
5. Click **Run workflow**

---

## ✅ Verify Build Status

1. Go to **Actions** tab
2. Watch the workflow run in real-time
3. Expected time: ~10-15 minutes for all 12 images
4. Check **Build Summary** in workflow output

---

## 📋 What Happens Next

After build completes successfully:
1. 12 Docker images pushed to GHCR
2. Auto-trigger deployment workflow
3. Pods restart and pull new images
4. Health checks verify services are running
5. All 11 services should reach **READY 1/1** state ✅

---

## 🔍 Troubleshooting

### Build Fails?
- Check Maven build logs in GitHub Actions
- Verify all pom.xml files were updated correctly
- Run locally: `mvn clean package -DskipTests`

### Deploy Fails or Pods Not Ready?
- Check pod logs: `kubectl logs -n healthcare-dev <pod-name>`
- Verify images pulled: `kubectl get pods -n healthcare-dev -o wide`
- Manual redeploy:
  1. Go to **Actions → Redeploy Services to AKS**
  2. Click **Run workflow**

### KUBE_CONFIG Secret Missing?
- Error: `Unable to access kubeconfig`
- Solution: Re-run Step 1 and Step 2 above

---

## 📊 Expected Outcome

**Before:** 
```
api-gateway               1/1     READY  ✅
svc-appointment           0/1     CrashLoopBackOff ❌
svc-careplan              0/1     CrashLoopBackOff ❌
... (10 more services failing)
```

**After workflow completes:**
```
api-gateway               1/1     READY  ✅
svc-appointment           1/1     READY  ✅
svc-careplan              1/1     READY  ✅
svc-consent               1/1     READY  ✅
svc-medical-record        1/1     READY  ✅
svc-notification          1/1     READY  ✅
svc-telemetry             1/1     READY  ✅
svc-device-ingestion      1/1     READY  ✅
svc-alert-management      1/1     READY  ✅
svc-identity-adapter      1/1     READY  ✅
svc-event-messaging       1/1     READY  ✅
svc-patient               1/1     READY  ✅
```

---

## 📚 More Information
See **GITHUB_ACTIONS_SETUP.md** for detailed configuration, advanced options, and troubleshooting guides.
