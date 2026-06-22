param apimServiceName string

param apiName string = 'platform-api'
param apiDisplayName string = 'Healthcare Platform API'
param apiPath string = 'api'
param primaryBackendUrl string
param secondaryBackendUrl string
param activeBackend string = 'primary'

resource apim 'Microsoft.ApiManagement/service@2022-08-01' existing = {
  name: apimServiceName
}

resource activeBackendNamedValue 'Microsoft.ApiManagement/service/namedValues@2022-08-01' = {
  parent: apim
  name: 'apim-active-backend'
  properties: {
    displayName: 'apim-active-backend'
    value: activeBackend
    secret: false
  }
}

resource backendPrimary 'Microsoft.ApiManagement/service/backends@2022-08-01' = {
  parent: apim
  name: 'backend-primary-appgw'
  properties: {
    title: 'Primary Application Gateway Backend'
    protocol: 'http'
    url: primaryBackendUrl
  }
}

resource backendSecondary 'Microsoft.ApiManagement/service/backends@2022-08-01' = {
  parent: apim
  name: 'backend-secondary-appgw'
  properties: {
    title: 'Secondary Application Gateway Backend'
    protocol: 'http'
    url: secondaryBackendUrl
  }
}

resource api 'Microsoft.ApiManagement/service/apis@2022-08-01' = {
  parent: apim
  name: apiName
  properties: {
    displayName: apiDisplayName
    path: apiPath
    protocols: [
      'https'
    ]
    subscriptionRequired: false
  }
}

resource apiPolicy 'Microsoft.ApiManagement/service/apis/policies@2022-08-01' = {
  parent: api
  name: 'policy'
  properties: {
    format: 'rawxml'
    value: loadTextContent('./apim/api-routing-policy.xml')
  }
  dependsOn: [
    backendPrimary
    backendSecondary
    activeBackendNamedValue
  ]
}

output apiId string = api.id
output primaryBackendId string = backendPrimary.id
output secondaryBackendId string = backendSecondary.id
