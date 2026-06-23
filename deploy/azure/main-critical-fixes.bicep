// Main Bicep file for critical infrastructure fixes
// Focuses on: AKS deployment and network security for existing data services
// Excludes: Secondary VNet private endpoints (requires Premium SKU upgrade first)

targetScope = 'resourceGroup'

param location string = 'centralindia'
param environment string = 'dev'
param aksClusterName string = 'aks-hpe-${environment}'
param aksNodeCount int = 3
param aksNodeVmSize string = 'Standard_B2s_v2'
param aksVersion string = '1.34.8'
param sqlServerName string
param storageAccountName string
param keyVaultName string
param serviceBusNamespaceName string
param eventHubNamespaceName string

// References to existing infrastructure
var primaryVNetName = 'hpe-primary-vnet-${environment}'
var primaryApplicationSubnetName = 'application-subnet'
var primaryDataSubnetName = 'data-subnet'
var hubVNetName = 'hpe-hub-vnet-${environment}'

// Get references to existing VNets and subnets
resource primaryVNet 'Microsoft.Network/virtualNetworks@2023-02-01' existing = {
  name: primaryVNetName
}

resource primaryApplicationSubnet 'Microsoft.Network/virtualNetworks/subnets@2023-02-01' existing = {
  parent: primaryVNet
  name: primaryApplicationSubnetName
}

resource primaryDataSubnet 'Microsoft.Network/virtualNetworks/subnets@2023-02-01' existing = {
  parent: primaryVNet
  name: primaryDataSubnetName
}

// Get reference to existing NSG for application subnet
resource applicationSubnetNsg 'Microsoft.Network/networkSecurityGroups@2023-02-01' existing = {
  name: 'nsg-application-${environment}'
}

// Deploy AKS cluster
module aksCluster './04-aks.bicep' = {
  name: 'primary-aks-cluster'
  params: {
    location: location
    environment: environment
    aksClusterName: aksClusterName
    aksNodeCount: aksNodeCount
    aksNodeVmSize: aksNodeVmSize
    aksVersion: aksVersion
    primaryApplicationSubnetId: primaryApplicationSubnet.id
  }
}

// Apply network security policies to existing data services
resource sqlServer 'Microsoft.Sql/servers@2021-02-01-preview' existing = {
  name: sqlServerName
}

resource storageAccount 'Microsoft.Storage/storageAccounts@2023-01-01' existing = {
  name: storageAccountName
}

resource keyVault 'Microsoft.KeyVault/vaults@2023-02-01' existing = {
  name: keyVaultName
}

resource serviceBusNamespace 'Microsoft.ServiceBus/namespaces@2022-10-01-preview' existing = {
  name: serviceBusNamespaceName
}

resource eventHubNamespace 'Microsoft.EventHub/namespaces@2022-10-01-preview' existing = {
  name: eventHubNamespaceName
}

// SQL Server VNet rules
resource sqlVNetRule1 'Microsoft.Sql/servers/virtualNetworkRules@2021-02-01-preview' = {
  parent: sqlServer
  name: 'data-subnet-rule'
  properties: {
    virtualNetworkSubnetId: primaryDataSubnet.id
    ignoreMissingVnetServiceEndpoint: false
  }
}

resource sqlVNetRule2 'Microsoft.Sql/servers/virtualNetworkRules@2021-02-01-preview' = {
  parent: sqlServer
  name: 'application-subnet-rule'
  properties: {
    virtualNetworkSubnetId: primaryApplicationSubnet.id
    ignoreMissingVnetServiceEndpoint: false
  }
}

// Storage Account network rules
resource storageNetworkRules 'Microsoft.Storage/storageAccounts/networkAcls@2023-01-01' = {
  parent: storageAccount
  name: 'default'
  properties: {
    bypass: 'AzureServices'
    defaultAction: 'Deny'
    virtualNetworkRules: [
      {
        id: primaryDataSubnet.id
        action: 'Allow'
      }
      {
        id: primaryApplicationSubnet.id
        action: 'Allow'
      }
    ]
    ipRules: []
  }
}

// Key Vault network policies
resource keyVaultNetworkRules 'Microsoft.KeyVault/vaults/networkAcls@2023-02-01' = {
  parent: keyVault
  name: 'default'
  properties: {
    bypass: 'AzureServices'
    defaultAction: 'Deny'
    virtualNetworkRules: [
      {
        id: primaryDataSubnet.id
      }
      {
        id: primaryApplicationSubnet.id
      }
    ]
    ipRules: []
  }
}

// Outputs
output aksClusterId string = aksCluster.outputs.aksClusterId
output aksClusterName string = aksCluster.outputs.aksClusterName
output aksClusterFqdn string = aksCluster.outputs.aksClusterFqdn
output sqlVNetRulesApplied bool = true
output storageNetworkSecurityApplied bool = true
output keyVaultNetworkSecurityApplied bool = true
output deploymentNote string = 'Service Bus and Event Hub network rules skipped - require Premium SKU'
