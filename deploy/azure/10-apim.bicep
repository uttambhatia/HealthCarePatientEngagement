param location string = resourceGroup().location
param environment string = 'dev'

param apimServiceName string = 'hpe-apim-${environment}'
param apimPublisherName string = 'Healthcare Platform'
param apimPublisherEmail string = 'noreply@example.com'
param apimSkuName string = 'Developer'
param apimSkuCapacity int = 1
param apimSubnetResourceId string

param oauthOpenIdConfigUrl string
param oauthAudience string

resource apim 'Microsoft.ApiManagement/service@2022-08-01' = {
  name: apimServiceName
  location: location
  sku: {
    name: apimSkuName
    capacity: apimSkuCapacity
  }
  properties: {
    publisherName: apimPublisherName
    publisherEmail: apimPublisherEmail
    virtualNetworkType: 'External'
    virtualNetworkConfiguration: {
      subnetResourceId: apimSubnetResourceId
    }
  }
}

resource oauthOpenIdConfigUrlNamedValue 'Microsoft.ApiManagement/service/namedValues@2022-08-01' = {
  parent: apim
  name: 'oauth-openid-config-url'
  properties: {
    displayName: 'oauth-openid-config-url'
    value: oauthOpenIdConfigUrl
    secret: false
  }
}

resource oauthAudienceNamedValue 'Microsoft.ApiManagement/service/namedValues@2022-08-01' = {
  parent: apim
  name: 'oauth-audience'
  properties: {
    displayName: 'oauth-audience'
    value: oauthAudience
    secret: false
  }
}

resource globalPolicy 'Microsoft.ApiManagement/service/policies@2022-08-01' = {
  parent: apim
  name: 'policy'
  properties: {
    format: 'rawxml'
    value: loadTextContent('./apim/global-policy.xml')
  }
  dependsOn: [
    oauthOpenIdConfigUrlNamedValue
    oauthAudienceNamedValue
  ]
}

output apimServiceId string = apim.id
output apimGatewayUrl string = 'https://${apim.name}.azure-api.net'
