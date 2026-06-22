param location string = resourceGroup().location
param environment string = 'dev'
param hubVnetName string = 'hpe-hub-vnet-${environment}'
param hubAddressPrefix string = '10.0.0.0/16'
param apimSubnetNsgId string = ''

resource hubVnet 'Microsoft.Network/virtualNetworks@2023-11-01' = {
  name: hubVnetName
  location: location
  properties: {
    addressSpace: {
      addressPrefixes: [
        hubAddressPrefix
      ]
    }
    subnets: [
      {
        name: 'waf-subnet'
        properties: {
          addressPrefix: '10.0.1.0/24'
        }
      }
      {
        name: 'apim-subnet'
        properties: {
          addressPrefix: '10.0.2.0/24'
          networkSecurityGroup: empty(apimSubnetNsgId) ? null : { id: apimSubnetNsgId }
        }
      }
      {
        name: 'keyvault-subnet'
        properties: {
          addressPrefix: '10.0.3.0/24'
        }
      }
      {
        name: 'private-dns-subnet'
        properties: {
          addressPrefix: '10.0.4.0/24'
        }
      }
      {
        name: 'acs-subnet'
        properties: {
          addressPrefix: '10.0.5.0/24'
        }
      }
      {
        name: 'monitoring-subnet'
        properties: {
          addressPrefix: '10.0.6.0/24'
        }
      }
    ]
  }
}

output hubVnetId string = hubVnet.id
output hubVnetName string = hubVnet.name
output hubApimSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', hubVnet.name, 'apim-subnet')
output hubPrivateDnsSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', hubVnet.name, 'private-dns-subnet')
output hubKeyVaultSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', hubVnet.name, 'keyvault-subnet')
