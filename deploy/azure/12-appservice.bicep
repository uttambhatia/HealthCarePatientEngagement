param location string = resourceGroup().location
param environment string = 'dev'

param appServicePlanName string = 'appservice-plan-hpe-${environment}'
param appServiceName string = 'healthcarepatientengagement'
param appServiceSkuName string = 'B2'
param nodeVersion string = '24-lts'
param uiSubnetId string = ''

// Create App Service Plan (Linux, Node.js)
resource appServicePlan 'Microsoft.Web/serverfarms@2023-01-01' = {
  name: appServicePlanName
  location: location
  kind: 'linux'
  sku: {
    name: appServiceSkuName
    tier: 'Standard'
    capacity: 1
  }
  properties: {
    reserved: true
  }
}

// Create App Service
resource appService 'Microsoft.Web/sites@2023-01-01' = {
  name: appServiceName
  location: location
  kind: 'app,linux'
  properties: {
    serverFarmId: appServicePlan.id
    virtualNetworkSubnetId: empty(uiSubnetId) ? null : uiSubnetId
    siteConfig: {
      linuxFxVersion: 'NODE|${nodeVersion}'
      alwaysOn: true
      appCommandLine: 'node server.js'
      http20Enabled: true
      minTlsVersion: '1.2'
      scmIpSecurityRestrictions: []
      ipSecurityRestrictions: []
      vnetRouteAllEnabled: true
      appSettings: [
        {
          name: 'WEBSITES_ENABLE_APP_SERVICE_STORAGE'
          value: 'false'
        }
        {
          name: 'PORT'
          value: '8080'
        }
      ]
      connectionStrings: []
    }
    httpsOnly: true
    publicNetworkAccess: empty(uiSubnetId) ? 'Enabled' : 'Disabled'
  }
}

// Configure web app health check (optional)
resource appServiceHealthCheck 'Microsoft.Web/sites/config@2023-01-01' = {
  parent: appService
  name: 'web'
  properties: {
    healthCheckPath: '/health'
  }
}

@description('The App Service Plan ID')
output appServicePlanId string = appServicePlan.id

@description('The App Service ID')
output appServiceId string = appService.id

@description('The App Service default hostname')
output appServiceHostname string = appService.properties.defaultHostName

@description('The App Service URL')
output appServiceUrl string = 'https://${appService.properties.defaultHostName}'

@description('The App Service name')
output appServiceName string = appService.name
