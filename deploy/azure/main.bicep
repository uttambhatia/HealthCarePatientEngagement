param location string = resourceGroup().location
param secondaryLocation string = 'eastus2'
param environment string = 'dev'

param hubVnetName string = 'hpe-hub-vnet-${environment}'
param primaryVnetName string = 'hpe-primary-vnet-${environment}'
param secondaryVnetName string = 'hpe-secondary-vnet-${environment}'

param hubAddressPrefix string = '10.0.0.0/16'
param primaryAddressPrefix string = '10.1.0.0/16'
param secondaryAddressPrefix string = '10.2.0.0/16'

param trafficManagerProfileName string = 'hpe-tm-${environment}'
param trafficManagerDnsLabel string = 'hpe-tm-${environment}'
param primaryUiHostname string = 'hpe-primary-ui-${environment}.azurewebsites.net'
param secondaryUiHostname string = 'hpe-secondary-ui-${environment}.azurewebsites.net'
param trafficManagerHealthProbePath string = '/health'

param primaryFrontDoorProfileName string = 'hpe-afd-primary-${environment}'
param primaryFrontDoorEndpointName string = 'hpe-afd-primary-${environment}'
param secondaryFrontDoorProfileName string = 'hpe-afd-secondary-${environment}'
param secondaryFrontDoorEndpointName string = 'hpe-afd-secondary-${environment}'
param frontDoorHealthProbePath string = '/health'

param sqlServerName string = ''
param storageAccountName string = ''
param keyVaultName string = ''
param serviceBusNamespaceName string = ''
param eventHubNamespaceName string = ''

param aksClusterName string = 'aks-hpe-${environment}'
param aksNodeCount int = 3
param aksNodeVmSize string = 'Standard_B2s_v2'
param aksVersion string = '1.34.8'

param iotHubName string = 'iothub-hpe-${environment}'
param iotHubSkuName string = 'S1'
param iotHubSkuCapacity int = 1

param apimServiceName string = 'hpe-apim-${environment}'
param apimPublisherName string = 'Healthcare Platform'
param apimPublisherEmail string = 'noreply@example.com'
param apimSkuName string = 'Developer'
param apimSkuCapacity int = 1
param oauthOpenIdConfigUrl string = ''
param oauthAudience string = 'api://<app-id-uri>'

param apimApiName string = 'platform-api'
param apimApiDisplayName string = 'Healthcare Platform API'
param apimApiPath string = 'api'
param primaryApiBackendUrl string = 'http://primary-app-gateway.internal'
param secondaryApiBackendUrl string = 'http://secondary-app-gateway.internal'
@allowed([
  'primary'
  'secondary'
])
param apimActiveBackend string = 'primary'

module nsg './07-network-security-groups.bicep' = {
  name: 'hub-spoke-nsg'
  params: {
    location: location
    secondaryLocation: secondaryLocation
    environment: environment
  }
}

module hub './01-hub-vnet.bicep' = {
  name: 'hub-vnet'
  dependsOn: [ nsg ]
  params: {
    location: location
    environment: environment
    hubVnetName: hubVnetName
    hubAddressPrefix: hubAddressPrefix
    apimSubnetNsgId: nsg.outputs.hubApimNsgId
  }
}

module primary './02-primary-vnet.bicep' = {
  name: 'primary-vnet'
  dependsOn: [ nsg ]
  params: {
    location: location
    environment: environment
    primaryVnetName: primaryVnetName
    primaryAddressPrefix: primaryAddressPrefix
    uiSubnetNsgId: nsg.outputs.primaryNsgIds.ui
    iotSubnetNsgId: nsg.outputs.primaryNsgIds.iot
    applicationSubnetNsgId: nsg.outputs.primaryNsgIds.application
    dataSubnetNsgId: nsg.outputs.primaryNsgIds.data
  }
}

module secondary './03-secondary-vnet.bicep' = {
  name: 'secondary-vnet'
  dependsOn: [ nsg ]
  params: {
    location: secondaryLocation
    environment: environment
    secondaryVnetName: secondaryVnetName
    secondaryAddressPrefix: secondaryAddressPrefix
    uiSubnetNsgId: nsg.outputs.secondaryNsgIds.ui
    iotSubnetNsgId: nsg.outputs.secondaryNsgIds.iot
    applicationSubnetNsgId: nsg.outputs.secondaryNsgIds.application
    dataSubnetNsgId: nsg.outputs.secondaryNsgIds.data
  }
}

module peering './04-vnet-peering.bicep' = {
  name: 'hub-spoke-peerings'
  params: {
    hubVnetName: hub.outputs.hubVnetName
    primaryVnetName: primary.outputs.primaryVnetName
    secondaryVnetName: secondary.outputs.secondaryVnetName
  }
}

module aksCluster './04-aks.bicep' = {
  name: 'primary-aks-cluster'
  dependsOn: [
    peering
    primary
    nsg
  ]
  params: {
    location: location
    environment: environment
    aksClusterName: aksClusterName
    aksNodeCount: aksNodeCount
    aksNodeVmSize: aksNodeVmSize
    aksVersion: aksVersion
    primaryApplicationSubnetId: primary.outputs.primaryApplicationSubnetId
  }
}

module privateDns './08-private-dns.bicep' = {
  name: 'hub-spoke-private-dns'
  params: {
    environment: environment
    hubVnetId: hub.outputs.hubVnetId
    primaryVnetId: primary.outputs.primaryVnetId
    secondaryVnetId: secondary.outputs.secondaryVnetId
  }
}

module privateEndpoints './09-private-endpoints.bicep' = {
  name: 'hub-spoke-private-endpoints'
  params: {
    location: location
    environment: environment
    primaryDataSubnetId: primary.outputs.primaryDataSubnetId
    secondaryDataSubnetId: secondary.outputs.secondaryDataSubnetId
    sqlServerName: sqlServerName
    storageAccountName: storageAccountName
    keyVaultName: keyVaultName
    serviceBusNamespaceName: serviceBusNamespaceName
    sqlPrivateDnsZoneId: privateDns.outputs.privateDnsZoneIds.sql
    blobPrivateDnsZoneId: privateDns.outputs.privateDnsZoneIds.blob
    keyVaultPrivateDnsZoneId: privateDns.outputs.privateDnsZoneIds.keyVault
    serviceBusPrivateDnsZoneId: privateDns.outputs.privateDnsZoneIds.serviceBus
  }
}

module iotHub './13-iot-hub.bicep' = {
  name: 'primary-iot-hub'
  params: {
    location: location
    environment: environment
    iotHubName: iotHubName
    iotHubSkuName: iotHubSkuName
    iotHubSkuCapacity: iotHubSkuCapacity
    dataSubnetId: primary.outputs.primaryDataSubnetId
    iotPrivateDnsZoneId: privateDns.outputs.privateDnsZoneIds.iot
  }
}

module dataNetworkSecurity './14-data-network-security.bicep' = {
  name: 'data-layer-security'
  dependsOn: [
    privateEndpoints
    iotHub
  ]
  params: {
    location: location
    environment: environment
    sqlServerName: sqlServerName
    storageAccountName: storageAccountName
    keyVaultName: keyVaultName
    serviceBusNamespaceName: serviceBusNamespaceName
    eventHubNamespaceName: eventHubNamespaceName
    dataSubnetId: primary.outputs.primaryDataSubnetId
    primaryApplicationSubnetId: primary.outputs.primaryApplicationSubnetId
  }
}

module apim './10-apim.bicep' = {
  name: 'hub-apim'
  dependsOn: [
    nsg
  ]
  params: {
    location: location
    environment: environment
    apimServiceName: apimServiceName
    apimPublisherName: apimPublisherName
    apimPublisherEmail: apimPublisherEmail
    apimSkuName: apimSkuName
    apimSkuCapacity: apimSkuCapacity
    apimSubnetResourceId: hub.outputs.hubApimSubnetId
    oauthOpenIdConfigUrl: oauthOpenIdConfigUrl
    oauthAudience: oauthAudience
  }
}

module apimApiRouting './11-apim-api-routing.bicep' = {
  name: 'hub-apim-api-routing'
  dependsOn: [
    apim
  ]
  params: {
    apimServiceName: apimServiceName
    apiName: apimApiName
    apiDisplayName: apimApiDisplayName
    apiPath: apimApiPath
    primaryBackendUrl: primaryApiBackendUrl
    secondaryBackendUrl: secondaryApiBackendUrl
    activeBackend: apimActiveBackend
  }
}

module primaryFrontDoor './06-front-door.bicep' = {
  name: 'front-door-primary-edge'
  params: {
    frontDoorProfileName: primaryFrontDoorProfileName
    frontDoorEndpointName: primaryFrontDoorEndpointName
    appServiceHostname: primaryUiHostname
    healthProbePath: frontDoorHealthProbePath
  }
}

module secondaryFrontDoor './06-front-door.bicep' = {
  name: 'front-door-secondary-edge'
  params: {
    frontDoorProfileName: secondaryFrontDoorProfileName
    frontDoorEndpointName: secondaryFrontDoorEndpointName
    appServiceHostname: secondaryUiHostname
    healthProbePath: frontDoorHealthProbePath
  }
}

module trafficManager './05-traffic-manager.bicep' = {
  name: 'traffic-manager-edge'
  params: {
    trafficManagerProfileName: trafficManagerProfileName
    trafficManagerDnsLabel: trafficManagerDnsLabel
    primaryEndpointTarget: primaryFrontDoor.outputs.frontDoorEndpointHost
    secondaryEndpointTarget: secondaryFrontDoor.outputs.frontDoorEndpointHost
    monitorPath: trafficManagerHealthProbePath
  }
}

module appService './12-appservice.bicep' = {
  name: 'ui-appservice'
  dependsOn: [
    primary
  ]
  params: {
    location: location
    environment: environment
    appServicePlanName: 'appservice-plan-hpe-${environment}'
    appServiceName: 'healthcarepatientengagement'
    appServiceSkuName: 'B2'
    nodeVersion: '24-lts'
    uiSubnetId: primary.outputs.primaryUiSubnetId
  }
}

output hubVnetId string = hub.outputs.hubVnetId
output primaryVnetId string = primary.outputs.primaryVnetId
output secondaryVnetId string = secondary.outputs.secondaryVnetId
output peeringIds object = peering.outputs.peeringIds
output nsgIds object = {
  primary: nsg.outputs.primaryNsgIds
  secondary: nsg.outputs.secondaryNsgIds
}
output privateDnsZoneIds object = privateDns.outputs.privateDnsZoneIds
output privateEndpointSummary object = privateEndpoints.outputs.privateEndpointSummary
output apimServiceId string = apim.outputs.apimServiceId
output apimGatewayUrl string = apim.outputs.apimGatewayUrl
output apimApiId string = apimApiRouting.outputs.apiId
output trafficManagerDnsName string = trafficManager.outputs.trafficManagerDnsName
output primaryFrontDoorEndpointHost string = primaryFrontDoor.outputs.frontDoorEndpointHost
output secondaryFrontDoorEndpointHost string = secondaryFrontDoor.outputs.frontDoorEndpointHost
output appServiceId string = appService.outputs.appServiceId
output appServiceName string = appService.outputs.appServiceName
output appServiceUrl string = appService.outputs.appServiceUrl
output appServiceHostname string = appService.outputs.appServiceHostname
output aksClusterId string = aksCluster.outputs.aksClusterId
output aksClusterName string = aksCluster.outputs.aksClusterName
output aksClusterFqdn string = aksCluster.outputs.aksClusterFqdn
output aksNodeResourceGroup string = aksCluster.outputs.aksNodeResourceGroup
output iotHubId string = iotHub.outputs.iotHubId
output iotHubName string = iotHub.outputs.iotHubName
output iotHubEventHubEndpoint string = iotHub.outputs.iotHubEventHubEndpoint
output dataNetworkSecurityApplied object = {
  sql: dataNetworkSecurity.outputs.sqlVnetRulesApplied
  storage: dataNetworkSecurity.outputs.storageNetworkRulesApplied
  keyVault: dataNetworkSecurity.outputs.keyVaultNetworkRulesApplied
  serviceBus: dataNetworkSecurity.outputs.serviceBusNetworkRulesApplied
  eventHub: dataNetworkSecurity.outputs.eventHubNetworkRulesApplied
}
