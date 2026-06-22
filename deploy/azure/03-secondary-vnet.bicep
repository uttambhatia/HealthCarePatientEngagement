param location string = 'eastus2'
param environment string = 'dev'
param secondaryVnetName string = 'hpe-secondary-vnet-${environment}'
param secondaryAddressPrefix string = '10.2.0.0/16'
param uiSubnetNsgId string = ''
param iotSubnetNsgId string = ''
param applicationSubnetNsgId string = ''
param dataSubnetNsgId string = ''

resource secondaryVnet 'Microsoft.Network/virtualNetworks@2023-11-01' = {
  name: secondaryVnetName
  location: location
  properties: {
    addressSpace: {
      addressPrefixes: [
        secondaryAddressPrefix
      ]
    }
    subnets: [
      {
        name: 'ui-layer-subnet'
        properties: {
          addressPrefix: '10.2.1.0/24'
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
          addressPrefix: '10.2.2.0/24'
          networkSecurityGroup: empty(iotSubnetNsgId) ? null : { id: iotSubnetNsgId }
        }
      }
      {
        name: 'application-subnet'
        properties: {
          addressPrefix: '10.2.3.0/24'
          networkSecurityGroup: empty(applicationSubnetNsgId) ? null : { id: applicationSubnetNsgId }
        }
      }
      {
        name: 'data-subnet'
        properties: {
          addressPrefix: '10.2.4.0/24'
          networkSecurityGroup: empty(dataSubnetNsgId) ? null : { id: dataSubnetNsgId }
        }
      }
    ]
  }
}

output secondaryVnetId string = secondaryVnet.id
output secondaryVnetName string = secondaryVnet.name
output secondaryUiSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', secondaryVnet.name, 'ui-layer-subnet')
output secondaryIotSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', secondaryVnet.name, 'iot-subnet')
output secondaryApplicationSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', secondaryVnet.name, 'application-subnet')
output secondaryDataSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', secondaryVnet.name, 'data-subnet')
