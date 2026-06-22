param location string = resourceGroup().location
param environment string = 'dev'

param primaryDataSubnetId string
param secondaryDataSubnetId string

param sqlServerName string = ''
param storageAccountName string = ''
param keyVaultName string = ''
param serviceBusNamespaceName string = ''

param sqlPrivateDnsZoneId string = ''
param blobPrivateDnsZoneId string = ''
param keyVaultPrivateDnsZoneId string = ''
param serviceBusPrivateDnsZoneId string = ''

resource sqlServer 'Microsoft.Sql/servers@2022-05-01-preview' existing = if (!empty(sqlServerName)) {
  name: sqlServerName
}

resource storageAccount 'Microsoft.Storage/storageAccounts@2023-01-01' existing = if (!empty(storageAccountName)) {
  name: storageAccountName
}

resource keyVault 'Microsoft.KeyVault/vaults@2023-02-01' existing = if (!empty(keyVaultName)) {
  name: keyVaultName
}

resource serviceBusNamespace 'Microsoft.ServiceBus/namespaces@2022-10-01-preview' existing = if (!empty(serviceBusNamespaceName)) {
  name: serviceBusNamespaceName
}

resource primarySqlPrivateEndpoint 'Microsoft.Network/privateEndpoints@2023-11-01' = if (!empty(sqlServerName)) {
  name: 'pep-primary-sql-${environment}'
  location: location
  properties: {
    subnet: {
      id: primaryDataSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'sql-connection'
        properties: {
          privateLinkServiceId: sqlServer.id
          groupIds: [
            'sqlServer'
          ]
        }
      }
    ]
  }
}

resource secondarySqlPrivateEndpoint 'Microsoft.Network/privateEndpoints@2023-11-01' = if (!empty(sqlServerName)) {
  name: 'pep-secondary-sql-${environment}'
  location: location
  properties: {
    subnet: {
      id: secondaryDataSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'sql-connection'
        properties: {
          privateLinkServiceId: sqlServer.id
          groupIds: [
            'sqlServer'
          ]
        }
      }
    ]
  }
}

resource primaryBlobPrivateEndpoint 'Microsoft.Network/privateEndpoints@2023-11-01' = if (!empty(storageAccountName)) {
  name: 'pep-primary-blob-${environment}'
  location: location
  properties: {
    subnet: {
      id: primaryDataSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'blob-connection'
        properties: {
          privateLinkServiceId: storageAccount.id
          groupIds: [
            'blob'
          ]
        }
      }
    ]
  }
}

resource secondaryBlobPrivateEndpoint 'Microsoft.Network/privateEndpoints@2023-11-01' = if (!empty(storageAccountName)) {
  name: 'pep-secondary-blob-${environment}'
  location: location
  properties: {
    subnet: {
      id: secondaryDataSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'blob-connection'
        properties: {
          privateLinkServiceId: storageAccount.id
          groupIds: [
            'blob'
          ]
        }
      }
    ]
  }
}

resource primaryKeyVaultPrivateEndpoint 'Microsoft.Network/privateEndpoints@2023-11-01' = if (!empty(keyVaultName)) {
  name: 'pep-primary-kv-${environment}'
  location: location
  properties: {
    subnet: {
      id: primaryDataSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'keyvault-connection'
        properties: {
          privateLinkServiceId: keyVault.id
          groupIds: [
            'vault'
          ]
        }
      }
    ]
  }
}

resource secondaryKeyVaultPrivateEndpoint 'Microsoft.Network/privateEndpoints@2023-11-01' = if (!empty(keyVaultName)) {
  name: 'pep-secondary-kv-${environment}'
  location: location
  properties: {
    subnet: {
      id: secondaryDataSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'keyvault-connection'
        properties: {
          privateLinkServiceId: keyVault.id
          groupIds: [
            'vault'
          ]
        }
      }
    ]
  }
}

resource primaryServiceBusPrivateEndpoint 'Microsoft.Network/privateEndpoints@2023-11-01' = if (!empty(serviceBusNamespaceName)) {
  name: 'pep-primary-sb-${environment}'
  location: location
  properties: {
    subnet: {
      id: primaryDataSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'servicebus-connection'
        properties: {
          privateLinkServiceId: serviceBusNamespace.id
          groupIds: [
            'namespace'
          ]
        }
      }
    ]
  }
}

resource secondaryServiceBusPrivateEndpoint 'Microsoft.Network/privateEndpoints@2023-11-01' = if (!empty(serviceBusNamespaceName)) {
  name: 'pep-secondary-sb-${environment}'
  location: location
  properties: {
    subnet: {
      id: secondaryDataSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'servicebus-connection'
        properties: {
          privateLinkServiceId: serviceBusNamespace.id
          groupIds: [
            'namespace'
          ]
        }
      }
    ]
  }
}

resource primarySqlZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-11-01' = if (!empty(sqlServerName) && !empty(sqlPrivateDnsZoneId)) {
  parent: primarySqlPrivateEndpoint
  name: 'default'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'sql-zone'
        properties: {
          privateDnsZoneId: sqlPrivateDnsZoneId
        }
      }
    ]
  }
}

resource primaryBlobZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-11-01' = if (!empty(storageAccountName) && !empty(blobPrivateDnsZoneId)) {
  parent: primaryBlobPrivateEndpoint
  name: 'default'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'blob-zone'
        properties: {
          privateDnsZoneId: blobPrivateDnsZoneId
        }
      }
    ]
  }
}

resource primaryKeyVaultZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-11-01' = if (!empty(keyVaultName) && !empty(keyVaultPrivateDnsZoneId)) {
  parent: primaryKeyVaultPrivateEndpoint
  name: 'default'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'keyvault-zone'
        properties: {
          privateDnsZoneId: keyVaultPrivateDnsZoneId
        }
      }
    ]
  }
}

resource primaryServiceBusZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-11-01' = if (!empty(serviceBusNamespaceName) && !empty(serviceBusPrivateDnsZoneId)) {
  parent: primaryServiceBusPrivateEndpoint
  name: 'default'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'servicebus-zone'
        properties: {
          privateDnsZoneId: serviceBusPrivateDnsZoneId
        }
      }
    ]
  }
}

resource secondarySqlZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-11-01' = if (!empty(sqlServerName) && !empty(sqlPrivateDnsZoneId)) {
  parent: secondarySqlPrivateEndpoint
  name: 'default'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'sql-zone'
        properties: {
          privateDnsZoneId: sqlPrivateDnsZoneId
        }
      }
    ]
  }
}

resource secondaryBlobZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-11-01' = if (!empty(storageAccountName) && !empty(blobPrivateDnsZoneId)) {
  parent: secondaryBlobPrivateEndpoint
  name: 'default'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'blob-zone'
        properties: {
          privateDnsZoneId: blobPrivateDnsZoneId
        }
      }
    ]
  }
}

resource secondaryKeyVaultZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-11-01' = if (!empty(keyVaultName) && !empty(keyVaultPrivateDnsZoneId)) {
  parent: secondaryKeyVaultPrivateEndpoint
  name: 'default'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'keyvault-zone'
        properties: {
          privateDnsZoneId: keyVaultPrivateDnsZoneId
        }
      }
    ]
  }
}

resource secondaryServiceBusZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-11-01' = if (!empty(serviceBusNamespaceName) && !empty(serviceBusPrivateDnsZoneId)) {
  parent: secondaryServiceBusPrivateEndpoint
  name: 'default'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'servicebus-zone'
        properties: {
          privateDnsZoneId: serviceBusPrivateDnsZoneId
        }
      }
    ]
  }
}

output privateEndpointSummary object = {
  sqlEnabled: !empty(sqlServerName)
  blobEnabled: !empty(storageAccountName)
  keyVaultEnabled: !empty(keyVaultName)
  serviceBusEnabled: !empty(serviceBusNamespaceName)
}
