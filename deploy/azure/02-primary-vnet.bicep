param location string = resourceGroup().location
param environment string = 'dev'
param primaryVnetName string = 'hpe-primary-vnet-${environment}'
param primaryAddressPrefix string = '10.1.0.0/16'
param uiSubnetNsgId string = ''
param iotSubnetNsgId string = ''
param applicationSubnetNsgId string = ''
param dataSubnetNsgId string = ''

resource primaryVnet 'Microsoft.Network/virtualNetworks@2023-11-01' = {
  name: primaryVnetName
  location: location
  properties: {
    addressSpace: {
      addressPrefixes: [
        primaryAddressPrefix
      ]
    }
    subnets: [
      {
        name: 'ui-layer-subnet'
        properties: {
          addressPrefix: '10.1.1.0/24'
          networkSecurityGroup: empty(uiSubnetNsgId) ? null : { id: uiSubnetNsgId }
          delegations: [
            {
              name: 'appservice-delegation'
              properties: {
                serviceName: 'Microsoft.Web/serverFarms'
              }
            }
          ]
        }
      }
      {
        name: 'iot-subnet'
        properties: {
          addressPrefix: '10.1.2.0/24'
          networkSecurityGroup: empty(iotSubnetNsgId) ? null : { id: iotSubnetNsgId }
        }
      }
      {
        name: 'application-subnet'
        properties: {
          addressPrefix: '10.1.3.0/24'
          networkSecurityGroup: empty(applicationSubnetNsgId) ? null : { id: applicationSubnetNsgId }
        }
      }
      {
        name: 'data-subnet'
        properties: {
          addressPrefix: '10.1.4.0/24'
          networkSecurityGroup: empty(dataSubnetNsgId) ? null : { id: dataSubnetNsgId }
        }
      }
    ]
  }
}

output primaryVnetId string = primaryVnet.id
output primaryVnetName string = primaryVnet.name
output primaryUiSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', primaryVnet.name, 'ui-layer-subnet')
output primaryIotSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', primaryVnet.name, 'iot-subnet')
output primaryApplicationSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', primaryVnet.name, 'application-subnet')
output primaryDataSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', primaryVnet.name, 'data-subnet')
