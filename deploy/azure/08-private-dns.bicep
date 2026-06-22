param hubVnetId string
param primaryVnetId string
param secondaryVnetId string
param environment string = 'dev'

resource privateDnsSql 'Microsoft.Network/privateDnsZones@2020-06-01' = {
  name: 'privatelink.database.windows.net'
  location: 'global'
}

resource privateDnsBlob 'Microsoft.Network/privateDnsZones@2020-06-01' = {
  name: 'privatelink.blob.${az.environment().suffixes.storage}'
  location: 'global'
}

resource privateDnsKeyVault 'Microsoft.Network/privateDnsZones@2020-06-01' = {
  name: 'privatelink.vaultcore.azure.net'
  location: 'global'
}

resource privateDnsServiceBus 'Microsoft.Network/privateDnsZones@2020-06-01' = {
  name: 'privatelink.servicebus.windows.net'
  location: 'global'
}

resource hubVnet 'Microsoft.Network/virtualNetworks@2023-11-01' existing = {
  scope: resourceGroup()
  name: last(split(hubVnetId, '/'))
}

resource primaryVnet 'Microsoft.Network/virtualNetworks@2023-11-01' existing = {
  scope: resourceGroup()
  name: last(split(primaryVnetId, '/'))
}

resource secondaryVnet 'Microsoft.Network/virtualNetworks@2023-11-01' existing = {
  scope: resourceGroup()
  name: last(split(secondaryVnetId, '/'))
}

resource sqlLinkHub 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: privateDnsSql
  name: 'link-hub-${environment}'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: hubVnet.id
    }
    registrationEnabled: false
  }
}

resource sqlLinkPrimary 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: privateDnsSql
  name: 'link-primary-${environment}'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: primaryVnet.id
    }
    registrationEnabled: false
  }
}

resource sqlLinkSecondary 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: privateDnsSql
  name: 'link-secondary-${environment}'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: secondaryVnet.id
    }
    registrationEnabled: false
  }
}

resource blobLinkHub 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: privateDnsBlob
  name: 'link-hub-${environment}'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: hubVnet.id
    }
    registrationEnabled: false
  }
}

resource blobLinkPrimary 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: privateDnsBlob
  name: 'link-primary-${environment}'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: primaryVnet.id
    }
    registrationEnabled: false
  }
}

resource blobLinkSecondary 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: privateDnsBlob
  name: 'link-secondary-${environment}'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: secondaryVnet.id
    }
    registrationEnabled: false
  }
}

resource keyvaultLinkHub 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: privateDnsKeyVault
  name: 'link-hub-${environment}'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: hubVnet.id
    }
    registrationEnabled: false
  }
}

resource keyvaultLinkPrimary 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: privateDnsKeyVault
  name: 'link-primary-${environment}'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: primaryVnet.id
    }
    registrationEnabled: false
  }
}

resource keyvaultLinkSecondary 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: privateDnsKeyVault
  name: 'link-secondary-${environment}'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: secondaryVnet.id
    }
    registrationEnabled: false
  }
}

resource serviceBusLinkHub 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: privateDnsServiceBus
  name: 'link-hub-${environment}'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: hubVnet.id
    }
    registrationEnabled: false
  }
}

resource serviceBusLinkPrimary 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: privateDnsServiceBus
  name: 'link-primary-${environment}'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: primaryVnet.id
    }
    registrationEnabled: false
  }
}

resource serviceBusLinkSecondary 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: privateDnsServiceBus
  name: 'link-secondary-${environment}'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: secondaryVnet.id
    }
    registrationEnabled: false
  }
}

output privateDnsZoneIds object = {
  sql: privateDnsSql.id
  blob: privateDnsBlob.id
  keyVault: privateDnsKeyVault.id
  serviceBus: privateDnsServiceBus.id
}
