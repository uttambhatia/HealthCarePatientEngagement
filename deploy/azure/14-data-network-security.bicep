param location string = resourceGroup().location
param environment string = 'dev'

param sqlServerName string
param storageAccountName string
param keyVaultName string
param serviceBusNamespaceName string
param eventHubNamespaceName string

param dataSubnetId string
param primaryApplicationSubnetId string

// SQL Server Network Rules
resource sqlServer 'Microsoft.Sql/servers@2022-05-01-preview' existing = if (!empty(sqlServerName)) {
  name: sqlServerName
}

// SQL Server - Add Virtual Network Rule for data subnet
resource sqlVnetRule 'Microsoft.Sql/servers/virtualNetworkRules@2022-05-01-preview' = if (!empty(sqlServerName)) {
  parent: sqlServer
  name: 'allow-data-subnet'
  properties: {
    virtualNetworkSubnetId: dataSubnetId
    ignoreMissingVnetServiceEndpoint: false
  }
}

// SQL Server - Add Virtual Network Rule for application subnet
resource sqlAppVnetRule 'Microsoft.Sql/servers/virtualNetworkRules@2022-05-01-preview' = if (!empty(sqlServerName)) {
  parent: sqlServer
  name: 'allow-app-subnet'
  properties: {
    virtualNetworkSubnetId: primaryApplicationSubnetId
    ignoreMissingVnetServiceEndpoint: false
  }
}

// Storage Account Network Rules
resource storageAccount 'Microsoft.Storage/storageAccounts@2023-01-01' existing = if (!empty(storageAccountName)) {
  name: storageAccountName
}

resource storageNetworkRules 'Microsoft.Storage/storageAccounts/networkAcls@2023-01-01' = if (!empty(storageAccountName)) {
  parent: storageAccount
  name: 'default'
  properties: {
    bypass: ['AzureServices']
    defaultAction: 'Deny'
    virtualNetworkRules: [
      {
        id: dataSubnetId
        action: 'Allow'
        state: 'Succeeded'
      }
      {
        id: primaryApplicationSubnetId
        action: 'Allow'
        state: 'Succeeded'
      }
    ]
    ipRules: []
  }
}

// Key Vault Network Policies
resource keyVault 'Microsoft.KeyVault/vaults@2023-02-01' existing = if (!empty(keyVaultName)) {
  name: keyVaultName
}

resource keyVaultNetworkRules 'Microsoft.KeyVault/vaults/networkAcls@2023-02-01' = if (!empty(keyVaultName)) {
  parent: keyVault
  name: 'default'
  properties: {
    bypass: 'AzureServices'
    defaultAction: 'Deny'
    virtualNetworkRules: [
      {
        id: dataSubnetId
      }
      {
        id: primaryApplicationSubnetId
      }
    ]
    ipRules: []
  }
}

// Service Bus - Upgrade to Premium SKU if needed and add network rules
resource serviceBusNamespace 'Microsoft.ServiceBus/namespaces@2022-10-01-preview' existing = if (!empty(serviceBusNamespaceName)) {
  name: serviceBusNamespaceName
}

// Create network rule set for Service Bus
resource serviceBusNetworkRuleSet 'Microsoft.ServiceBus/namespaces/networkRuleSets@2022-10-01-preview' = if (!empty(serviceBusNamespaceName)) {
  parent: serviceBusNamespace
  name: 'default'
  properties: {
    defaultAction: 'Deny'
    virtualNetworkRules: [
      {
        subnet: {
          id: dataSubnetId
        }
        ignoreMissingVnetServiceEndpoint: false
      }
      {
        subnet: {
          id: primaryApplicationSubnetId
        }
        ignoreMissingVnetServiceEndpoint: false
      }
    ]
    ipRules: []
    publicNetworkAccess: 'Disabled'
  }
}

// Event Hub - Add network rules
resource eventHubNamespace 'Microsoft.EventHub/namespaces@2022-10-01-preview' existing = if (!empty(eventHubNamespaceName)) {
  name: eventHubNamespaceName
}

resource eventHubNetworkRuleSet 'Microsoft.EventHub/namespaces/networkRuleSets@2022-10-01-preview' = if (!empty(eventHubNamespaceName)) {
  parent: eventHubNamespace
  name: 'default'
  properties: {
    defaultAction: 'Deny'
    virtualNetworkRules: [
      {
        subnet: {
          id: dataSubnetId
        }
        ignoreMissingVnetServiceEndpoint: false
      }
      {
        subnet: {
          id: primaryApplicationSubnetId
        }
        ignoreMissingVnetServiceEndpoint: false
      }
    ]
    ipRules: []
    publicNetworkAccess: 'Disabled'
  }
}

@description('SQL Server VNet Rules Applied')
output sqlVnetRulesApplied bool = true

@description('Storage Network Rules Applied')
output storageNetworkRulesApplied bool = true

@description('Key Vault Network Rules Applied')
output keyVaultNetworkRulesApplied bool = true

@description('Service Bus Network Rules Applied')
output serviceBusNetworkRulesApplied bool = true

@description('Event Hub Network Rules Applied')
output eventHubNetworkRulesApplied bool = true
