param hubVnetName string
param primaryVnetName string
param secondaryVnetName string

resource hubVnet 'Microsoft.Network/virtualNetworks@2023-11-01' existing = {
  name: hubVnetName
}

resource primaryVnet 'Microsoft.Network/virtualNetworks@2023-11-01' existing = {
  name: primaryVnetName
}

resource secondaryVnet 'Microsoft.Network/virtualNetworks@2023-11-01' existing = {
  name: secondaryVnetName
}

resource hubToPrimary 'Microsoft.Network/virtualNetworks/virtualNetworkPeerings@2023-11-01' = {
  parent: hubVnet
  name: 'hub-to-primary'
  properties: {
    remoteVirtualNetwork: {
      id: primaryVnet.id
    }
    allowVirtualNetworkAccess: true
    allowForwardedTraffic: true
    allowGatewayTransit: false
    useRemoteGateways: false
  }
}

resource primaryToHub 'Microsoft.Network/virtualNetworks/virtualNetworkPeerings@2023-11-01' = {
  parent: primaryVnet
  name: 'primary-to-hub'
  properties: {
    remoteVirtualNetwork: {
      id: hubVnet.id
    }
    allowVirtualNetworkAccess: true
    allowForwardedTraffic: true
    allowGatewayTransit: false
    useRemoteGateways: false
  }
}

resource hubToSecondary 'Microsoft.Network/virtualNetworks/virtualNetworkPeerings@2023-11-01' = {
  parent: hubVnet
  name: 'hub-to-secondary'
  properties: {
    remoteVirtualNetwork: {
      id: secondaryVnet.id
    }
    allowVirtualNetworkAccess: true
    allowForwardedTraffic: true
    allowGatewayTransit: false
    useRemoteGateways: false
  }
}

resource secondaryToHub 'Microsoft.Network/virtualNetworks/virtualNetworkPeerings@2023-11-01' = {
  parent: secondaryVnet
  name: 'secondary-to-hub'
  properties: {
    remoteVirtualNetwork: {
      id: hubVnet.id
    }
    allowVirtualNetworkAccess: true
    allowForwardedTraffic: true
    allowGatewayTransit: false
    useRemoteGateways: false
  }
}

output peeringIds object = {
  hubToPrimary: hubToPrimary.id
  primaryToHub: primaryToHub.id
  hubToSecondary: hubToSecondary.id
  secondaryToHub: secondaryToHub.id
}
