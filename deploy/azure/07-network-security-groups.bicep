param location string = resourceGroup().location
param secondaryLocation string = 'eastus2'
param environment string = 'dev'

resource primaryUiNsg 'Microsoft.Network/networkSecurityGroups@2023-11-01' = {
  name: 'nsg-primary-ui-${environment}'
  location: location
}

resource primaryIotNsg 'Microsoft.Network/networkSecurityGroups@2023-11-01' = {
  name: 'nsg-primary-iot-${environment}'
  location: location
}

resource primaryAppNsg 'Microsoft.Network/networkSecurityGroups@2023-11-01' = {
  name: 'nsg-primary-app-${environment}'
  location: location
}

resource primaryDataNsg 'Microsoft.Network/networkSecurityGroups@2023-11-01' = {
  name: 'nsg-primary-data-${environment}'
  location: location
}

resource secondaryUiNsg 'Microsoft.Network/networkSecurityGroups@2023-11-01' = {
  name: 'nsg-secondary-ui-${environment}-${secondaryLocation}'
  location: secondaryLocation
}

resource secondaryIotNsg 'Microsoft.Network/networkSecurityGroups@2023-11-01' = {
  name: 'nsg-secondary-iot-${environment}-${secondaryLocation}'
  location: secondaryLocation
}

resource secondaryAppNsg 'Microsoft.Network/networkSecurityGroups@2023-11-01' = {
  name: 'nsg-secondary-app-${environment}-${secondaryLocation}'
  location: secondaryLocation
}

resource secondaryDataNsg 'Microsoft.Network/networkSecurityGroups@2023-11-01' = {
  name: 'nsg-secondary-data-${environment}-${secondaryLocation}'
  location: secondaryLocation
}

resource hubApimNsg 'Microsoft.Network/networkSecurityGroups@2023-11-01' = {
  name: 'nsg-hub-apim-${environment}'
  location: location
  properties: {
    securityRules: [
      {
        name: 'AllowAPIMManagement'
        properties: {
          priority: 100
          direction: 'Inbound'
          access: 'Allow'
          protocol: 'Tcp'
          sourceAddressPrefix: 'ApiManagement'
          sourcePortRange: '*'
          destinationAddressPrefix: 'VirtualNetwork'
          destinationPortRange: '3443'
        }
      }
      {
        name: 'AllowAzureLB'
        properties: {
          priority: 110
          direction: 'Inbound'
          access: 'Allow'
          protocol: 'Tcp'
          sourceAddressPrefix: 'AzureLoadBalancer'
          sourcePortRange: '*'
          destinationAddressPrefix: '*'
          destinationPortRange: '6390'
        }
      }
    ]
  }
}

output primaryNsgIds object = {
  ui: primaryUiNsg.id
  iot: primaryIotNsg.id
  application: primaryAppNsg.id
  data: primaryDataNsg.id
}

output secondaryNsgIds object = {
  ui: secondaryUiNsg.id
  iot: secondaryIotNsg.id
  application: secondaryAppNsg.id
  data: secondaryDataNsg.id
}

output hubApimNsgId string = hubApimNsg.id
