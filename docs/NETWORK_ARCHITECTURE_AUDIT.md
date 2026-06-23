# Network Architecture Audit: Subnet & VNet Placement

**Date**: 2026-06-22 | **Status**: ⚠️ INCOMPLETE - Architecture Gaps Identified

---

## Executive Summary

The Healthcare Patient Engagement platform uses a multi-tier network architecture with:
- **3 VNets**: Hub (10.0.0.0/16), Primary (10.1.0.0/16), Secondary (10.2.0.0/16)
- **12 Subnets**: 4 per VNet (UI, IoT, Application, Data layers)
- **Subnet Placement**: ✅ Some services correctly placed | ⚠️ Many services missing from IaC | ❌ AKS not in custom VNet

---

## VNet Architecture

### Hub VNet (10.0.0.0/16) - Centralized Shared Services
| Subnet Name | CIDR | Purpose | Delegations |
|---|---|---|---|
| `waf-subnet` | 10.0.1.0/24 | Web Application Firewall (WAF) | — |
| `apim-subnet` | 10.0.2.0/24 | API Management service | — |
| `keyvault-subnet` | 10.0.3.0/24 | Key Vault (private endpoints) | — |
| `private-dns-subnet` | 10.0.4.0/24 | Private DNS zones | — |
| `acs-subnet` | 10.0.5.0/24 | **Azure Communication Services (unused)** | — |
| `monitoring-subnet` | 10.0.6.0/24 | Monitoring & logging | — |

### Primary VNet (10.1.0.0/16) - Primary Region (Central India)
| Subnet Name | CIDR | Purpose | Delegations |
|---|---|---|---|
| `ui-layer-subnet` | 10.1.1.0/24 | **Frontend (App Service)** | `Microsoft.Web/serverFarms` |
| `iot-subnet` | 10.1.2.0/24 | **IoT Hub (not deployed)** | — |
| `application-subnet` | 10.1.3.0/24 | **AKS nodes (not integrated)** | — |
| `data-subnet` | 10.1.4.0/24 | **Private endpoints** | — |

### Secondary VNet (10.2.0.0/16) - Secondary Region (East US)
| Subnet Name | CIDR | Purpose | Delegations |
|---|---|---|---|
| `ui-layer-subnet` | 10.2.1.0/24 | **Frontend (App Service secondary)** | `Microsoft.Web/serverFarms` |
| `iot-subnet` | 10.2.2.0/24 | **IoT Hub (not deployed)** | — |
| `application-subnet` | 10.2.3.0/24 | **AKS (not integrated)** | — |
| `data-subnet` | 10.2.4.0/24 | **Private endpoints** | — |

---

## Service Placement Analysis

### ✅ CORRECTLY PLACED (In Code)

#### 1. **API Management (APIM)**
```
Status: ✅ Correctly Placed in Hub VNet
├─ VNet: hpe-hub-vnet-dev
├─ Subnet: apim-subnet (10.0.2.0/24)
├─ Module: deploy/azure/10-apim.bicep
├─ VNet Type: External
├─ Config: virtualNetworkConfiguration.subnetResourceId provided
└─ Details: Services communicate via APIM gateway in hub
```

#### 2. **App Service (Frontend)**
```
Status: ✅ Correctly Placed in Primary VNet
├─ VNet: hpe-primary-vnet-dev
├─ Subnet: ui-layer-subnet (10.1.1.0/24)
├─ Module: deploy/azure/12-appservice.bicep (NEW)
├─ VNet Integration: Enabled via virtualNetworkSubnetId
├─ Public Access: Disabled
└─ Routing: vnetRouteAllEnabled = true
   (All outbound traffic through VNet)
```

#### 3. **Private Endpoints (Data Layer Access)**
```
Status: ✅ Correctly Placed in Data Subnet
├─ Module: deploy/azure/09-private-endpoints.bicep
├─ Primary Data Subnet: 10.1.4.0/24
├─ Secondary Data Subnet: 10.2.4.0/24
├─ Services Accessed via Private Endpoints:
│  ├─ SQL Server (private endpoint in data-subnet) ✅
│  ├─ Storage Account (private endpoint in data-subnet) ✅
│  ├─ Key Vault (private endpoint in data-subnet) ✅
│  └─ Service Bus (private endpoint in data-subnet) ✅
└─ Private DNS Zones: Configured for internal resolution
```

#### 4. **VNet Peering**
```
Status: ✅ Correctly Implemented
├─ Module: deploy/azure/04-vnet-peering.bicep
├─ Peering Setup:
│  ├─ Hub ↔ Primary (bidirectional)
│  ├─ Hub ↔ Secondary (bidirectional)
│  └─ Primary ↔ Secondary (bidirectional)
└─ Traffic Flow: Any-to-any via peering
```

---

### ⚠️ PARTIALLY PLACED (Created Separately, Not in VNet Code)

#### 1. **Azure SQL Server & Database**
```
Status: ⚠️ Created Without VNet Integration
├─ Creation Method: bootstrap-dev-azure.ps1
├─ Firewall: AllowAzureServices rule added
├─ Access Pattern:
│  ├─ Internal: Via private endpoint (10.1.4.x)
│  └─ Public: Exposed to Azure services
├─ Issue: Created without --vnet-name parameter
└─ Recommended: Move to data-subnet with private endpoint only
   (No public access when private endpoint available)
```

#### 2. **Azure Storage Account**
```
Status: ⚠️ Created Without VNet Integration
├─ Creation Method: bootstrap-dev-azure.ps1
├─ Configuration: --https-only, --allow-blob-public-access=false
├─ Access Pattern:
│  ├─ Internal: Via private endpoint (10.1.4.x)
│  └─ Public: Exposed but restricted
├─ Issue: No VNet service endpoints or firewall rules
└─ Recommended: Add storage firewall rules to allow only:
   ├─ Private endpoint traffic (10.1.4.0/24)
   └─ Service Bus namespace
```

#### 3. **Key Vault**
```
Status: ⚠️ Created Without VNet Integration
├─ Creation Method: bootstrap-dev-azure.ps1
├─ Configuration: --enable-rbac-authorization=true
├─ Access Pattern:
│  ├─ Internal: Via private endpoint (10.1.4.x)
│  └─ Public: Exposed to entire internet
├─ Issue: No network security groups or firewall rules
└─ Recommended: Add Key Vault network policies:
   ├─ Default deny
   ├─ Allow private endpoint only
   └─ Add NSG rules: 443 inbound from data-subnet
```

#### 4. **Service Bus Namespace**
```
Status: ⚠️ Created Without VNet Integration
├─ Creation Method: bootstrap-dev-azure.ps1
├─ SKU: Standard (Premium required for VNet integration)
├─ Access Pattern:
│  ├─ Internal: Via private endpoint (10.1.4.x)
│  └─ Public: Exposed to entire internet
├─ Issue: Standard SKU doesn't support network rules
└─ Recommended: Upgrade to Premium SKU for VNet rules:
   ├─ Enable private endpoints
   ├─ Default deny public access
   └─ Allow only whitelisted network sources
```

#### 5. **Event Hub Namespace**
```
Status: ⚠️ Created Without VNet Integration
├─ Creation Method: bootstrap-dev-azure.ps1
├─ SKU: Standard
├─ Access Pattern:
│  ├─ Internal: Via private endpoint (10.1.4.x)
│  └─ Public: Exposed to entire internet
├─ Issue: Standard SKU doesn't support network rules
└─ Recommended: Upgrade to Premium SKU for VNet rules
```

---

### ❌ NOT PROPERLY PLACED (Architecture Gaps)

#### 1. **AKS Cluster (Kubernetes)**
```
Status: ❌ NOT in Custom VNet
├─ Current Deployment: Default node resource group
├─ Creation Method: bootstrap-dev-azure.ps1
│  └─ Command: az aks create (no --vnet-subnet-id parameter)
├─ Problem: 
│  ├─ AKS uses auto-generated MC_* resource group
│  ├─ Nodes in separate unmanaged VNet
│  └─ Cannot communicate with custom VNet services
├─ Intended Subnet: primary application-subnet (10.1.3.0/24)
├─ Impact:
│  ├─ Service-to-service communication via public IPs
│  ├─ No network isolation between AKS and other tiers
│  ├─ Data flow not going through NSGs/security policies
│  └─ Cost: Extra egress charges for cross-VNet traffic
└─ Fix Required: 
   ├─ Deploy AKS with --vnet-subnet-id pointing to primary-application-subnet
   ├─ Use kubenet CNI plugin with custom VNet
   └─ Redeploy all microservices to new cluster
```

#### 2. **IoT Hub**
```
Status: ❌ NOT DEPLOYED
├─ Subnet Allocated: primary iot-subnet (10.1.2.0/24)
├─ Status in Bicep: No module exists
├─ Status in Bootstrap Script: Not created
├─ Expected SKU: S1 (Standard tier) for VNet integration
└─ Next Steps Required:
   ├─ Create deploy/azure/13-iot-hub.bicep module
   ├─ Deploy to primary-iot-subnet (10.1.2.0/24)
   ├─ Or to secondary-iot-subnet (10.2.2.0/24) for DR
   └─ Configure private endpoints for AMQP/MQTT
```

#### 3. **Azure Communication Services (ACS)**
```
Status: ⚠️ SUBNET ALLOCATED BUT SERVICE NOT DEPLOYED
├─ Subnet Allocated: hub acs-subnet (10.0.5.0/24)
├─ Status in Code:
│  ├─ Subnet defined in 01-hub-vnet.bicep ✅
│  ├─ NSG created in 07-network-security-groups.bicep ✅
│  ├─ No Bicep module to deploy service ❌
│  └─ Not created in bootstrap script ❌
├─ Bootstrap Parameter: --AcsResourceName (accepted but unused)
├─ Intended Use: Teleconsultation signaling/call services
└─ Next Steps Required:
   ├─ Verify if ACS is actually needed for use case
   ├─ Create deploy/azure/14-acs.bicep module if needed
   └─ Or remove acs-subnet from architecture if not used
```

---

## Network Traffic Flow

### Current Architecture
```
┌──────────────┐
│   Internet   │
└──────┬───────┘
       │
       ↓ (Public IP)
┌──────────────────────────────────────┐
│  Front Door (Global Load Balancer)   │
│  ├─ Primary Endpoint                 │
│  └─ Secondary Endpoint               │
└──────┬───────┬──────────────┬────────┘
       │       │              │
       ↓       ↓              ↓
   [Primary]  [Secondary]  [Traffic Manager]
       │           │              │
       ↓           ↓              ↓
┌──────────────────────────────────────┐
│  Primary VNet (10.1.0.0/16)          │
├─ ui-layer-subnet (10.1.1.0/24)       │
│  └─ App Service ✅                   │
├─ application-subnet (10.1.3.0/24)    │
│  └─ AKS Cluster ❌ (not integrated)  │
├─ data-subnet (10.1.4.0/24)           │
│  ├─ SQL private endpoint ✅          │
│  ├─ Storage private endpoint ✅      │
│  ├─ Key Vault private endpoint ✅    │
│  └─ Service Bus private endpoint ✅  │
└─────────────────────────────────────┘
        │
        ↓ (VNet Peering)
┌──────────────────────────────────────┐
│  Hub VNet (10.0.0.0/16)              │
├─ apim-subnet (10.0.2.0/24)           │
│  └─ API Management ✅                │
└──────────────────────────────────────┘
```

### Issues with Current Flow
1. **AKS not in custom VNet** → Separate network path
2. **Public services without firewall rules** → Open to internet
3. **No NSG rules for internal communication** → Implicit allow all
4. **IoT Hub not deployed** → IoT integration missing

---

## NSG (Network Security Group) Configuration

### UI Layer NSG (Hub APIM + Primary UI)
```
Location: Hub APIM Subnet, Primary UI Subnet
Inbound Rules:
├─ HTTPS (443) from Front Door ✅
├─ HTTPS (443) from Traffic Manager ✅
└─ SSH (22) for management (if needed)

Outbound Rules:
├─ HTTPS (443) to API Gateway (AKS) ❌ (AKS not in VNet)
├─ HTTPS (443) to private endpoints (data-subnet)
└─ DNS (53) to private DNS
```

### Application Layer NSG (AKS Cluster)
```
Location: Primary Application Subnet (should be)
Status: ⚠️ AKS NOT IN CUSTOM VNET - NSG not applied
Expected Inbound Rules:
├─ HTTPS (8080) from UI Subnet ❌
├─ HTTPS (6379) from application-subnet (Redis)
└─ SQL (1433) from application-subnet (via data-subnet private endpoint)

Expected Outbound Rules:
├─ HTTPS (443) to private endpoints (data-subnet)
├─ DNS (53) to private DNS
└─ HTTPS (443) to external APIs (if needed)
```

### Data Layer NSG (Private Endpoints)
```
Location: Primary Data Subnet
Inbound Rules:
├─ SQL (1433) from application-subnet ✅
├─ HTTPS (443) from application-subnet ✅
├─ AMQP (5671) from application-subnet ✅
└─ EventHub (5671) from application-subnet ✅

Outbound Rules:
└─ Any (services are internal consumers)
```

---

## Remediation Plan

### Priority 1: CRITICAL (Blocking Production)
- [ ] **Redeploy AKS with custom VNet integration**
  ```
  Target: primary application-subnet (10.1.3.0/24)
  Estimated Effort: 4-6 hours (cluster recreation required)
  Impact: Enables secure microservice communication
  ```

### Priority 2: HIGH (Security Hardening)
- [ ] **Add Network Policies to Key Vault**
  - [ ] Enable firewall
  - [ ] Add private endpoint firewall rules
  - [ ] Deny public access
  
- [ ] **Upgrade Service Bus to Premium SKU**
  - [ ] Enable network rules
  - [ ] Add private endpoint access
  - [ ] Deny public access
  
- [ ] **Upgrade Event Hub to Premium SKU**
  - [ ] Enable network rules
  - [ ] Add private endpoint access

- [ ] **Add Storage Account Firewall Rules**
  - [ ] Allow private endpoint only
  - [ ] Deny public access (except CI/CD)

### Priority 3: MEDIUM (Architecture Completion)
- [ ] **Create IoT Hub Bicep Module** (if needed for use case)
  - Target: primary iot-subnet (10.1.2.0/24)
  - Add private endpoint
  
- [ ] **Clarify ACS Usage**
  - Decide if Azure Communication Services needed
  - Create module or remove subnet if not used

### Priority 4: LOW (Optimization)
- [ ] **Add NSG logging and monitoring**
- [ ] **Implement Azure Firewall** in hub for centralized egress
- [ ] **Add DDoS Protection** for public-facing services
- [ ] **Configure UDRs** (User Defined Routes) for advanced traffic control

---

## Infrastructure as Code Gaps

### Missing Bicep Modules
1. **`deploy/azure/13-iot-hub.bicep`** (not created)
   - Should deploy IoT Hub to iot-subnet
   - Should configure private endpoints in data-subnet

2. **`deploy/azure/14-acs.bicep`** (not created)
   - Decision needed: Is ACS required?
   - If yes: Deploy to acs-subnet in hub

3. **`deploy/azure/sql.bicep`** (not created)
   - SQL Server currently created via PowerShell
   - Should be in Bicep with VNet rules

4. **`deploy/azure/storage.bicep`** (not created)
   - Storage Account currently created via PowerShell
   - Should be in Bicep with firewall rules

5. **`deploy/azure/keyvault.bicep`** (not created)
   - Key Vault currently created via PowerShell
   - Should be in Bicep with network policies

### Incomplete Service Deployments
- Azure SQL: ✅ Deployed | ⚠️ No VNet firewall rules
- Storage: ✅ Deployed | ⚠️ No firewall rules
- Key Vault: ✅ Deployed | ⚠️ No network policies
- Service Bus: ✅ Deployed (Standard) | ❌ Premium SKU needed for VNet
- Event Hub: ✅ Deployed (Standard) | ❌ Premium SKU needed for VNet
- AKS: ✅ Deployed | ❌ Wrong VNet (not custom)
- IoT Hub: ❌ Not deployed
- ACS: ❌ Not deployed (subnet allocated, uncertain if needed)

---

## Verification Commands

```powershell
# Check APIM VNet integration
az apim show --resource-group rg-azuser7080_mml.local-1nLQA \
  --name hpe-apim-dev \
  --query "properties.{vnetType:virtualNetworkType, subnet:virtualNetworkConfiguration.subnetResourceId}"

# Check App Service VNet integration
az webapp vnet-integration list --name healthcarepatientengagement \
  --resource-group rg-azuser7080_mml.local-1nLQA

# Check AKS VNet integration
az aks show --resource-group rg-azuser7080_mml.local-1nLQA \
  --name aks-hpe-dev2 \
  --query "networkProfile.{vnetId:vnetId, subnetId:subnetId, podCidr:podCidr, dockerBridgeCidr:dockerBridgeCidr}"

# Check SQL Server network rules
az sql server vnet-rule list --resource-group rg-azuser7080_mml.local-1nLQA \
  --server sqlhpedev2ae8ee0

# Check Storage Account firewall
az storage account show --resource-group rg-azuser7080_mml.local-1nLQA \
  --name <storage-name> \
  --query "networkRulesBypassDefaults.{defaultRule:defaultAction, virtualNetworkRules:virtualNetworkRules}"
```

---

## Summary Scorecard

| Layer | Service | Placement | Status |
|-------|---------|-----------|--------|
| **Hub** | APIM | apim-subnet (10.0.2.0/24) | ✅ Correct |
| **Primary UI** | App Service | ui-layer-subnet (10.1.1.0/24) | ✅ Correct |
| **Primary App** | AKS Cluster | application-subnet (10.1.3.0/24) | ❌ Wrong VNet |
| **Primary Data** | SQL (Private Endpoint) | data-subnet (10.1.4.0/24) | ✅ Correct |
| **Primary Data** | Storage (Private Endpoint) | data-subnet (10.1.4.0/24) | ✅ Correct |
| **Primary Data** | Key Vault (Private Endpoint) | data-subnet (10.1.4.0/24) | ✅ Correct |
| **Primary Data** | Service Bus (Private Endpoint) | data-subnet (10.1.4.0/24) | ✅ Correct |
| **Primary Data** | Event Hub (Private Endpoint) | data-subnet (10.1.4.0/24) | ✅ Correct |
| **Primary IoT** | IoT Hub | iot-subnet (10.1.2.0/24) | ❌ Not Deployed |
| **Hub ACS** | Azure Communication Services | acs-subnet (10.0.5.0/24) | ⚠️ Uncertain |

**Overall Score**: 6/9 (67%) - Core networking correct, but critical gaps in AKS placement and missing services

