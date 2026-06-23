// Add Service Endpoints to subnets first
targetScope = 'resourceGroup'

param location string = 'centralindia'
param environment string = 'dev'

var primaryVNetName = 'hpe-primary-vnet-${environment}'
var dataSubnetName = 'data-subnet'
var applicationSubnetName = 'application-subnet'

// Get references to existing VNet and subnets
resource primaryVNet 'Microsoft.Network/virtualNetworks@2023-02-01' existing = {
  name: primaryVNetName
}

// Update data-subnet with service endpoints
resource dataSubnet 'Microsoft.Network/virtualNetworks/subnets@2023-02-01' = {
  parent: primaryVNet
  name: dataSubnetName
  properties: {
    addressPrefix: '10.1.4.0/24'
    serviceEndpoints: [
      {
        service: 'Microsoft.Sql'
      }
      {
        service: 'Microsoft.Storage'
      }
      {
        service: 'Microsoft.KeyVault'
      }
      {
        service: 'Microsoft.ServiceBus'
      }
      {
        service: 'Microsoft.EventHub'
      }
    ]
    delegations: []
    privateEndpointNetworkPolicies: 'Disabled'
    privateLinkServiceNetworkPolicies: 'Enabled'
  }
}

// Update application-subnet with service endpoints
resource applicationSubnet 'Microsoft.Network/virtualNetworks/subnets@2023-02-01' = {
  parent: primaryVNet
  name: applicationSubnetName
  properties: {
    addressPrefix: '10.1.3.0/24'
    serviceEndpoints: [
      {
        service: 'Microsoft.Sql'
      }
      {
        service: 'Microsoft.Storage'
      }
      {
        service: 'Microsoft.KeyVault'
      }
      {
        service: 'Microsoft.ServiceBus'
      }
      {
        service: 'Microsoft.EventHub'
      }
    ]
    delegations: []
    privateEndpointNetworkPolicies: 'Disabled'
    privateLinkServiceNetworkPolicies: 'Enabled'
  }
}

output dataSubnetId string = dataSubnet.id
output applicationSubnetId string = applicationSubnet.id
output serviceEndpointsConfigured bool = true
