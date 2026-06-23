param location string = resourceGroup().location
param environment string = 'dev'

param iotHubName string = 'iothub-hpe-${environment}'
param iotHubSkuName string = 'S1'
param iotHubSkuCapacity int = 1

param dataSubnetId string

param iotPrivateDnsZoneId string = ''
param iotPrivateDnsZoneName string = 'privatelink.azure-devices.net'

// Create IoT Hub
resource iotHub 'Microsoft.Devices/IotHubs@2023-06-01-preview' = {
  name: iotHubName
  location: location
  sku: {
    name: iotHubSkuName
    capacity: iotHubSkuCapacity
  }
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    ipFilterRules: []
    minTlsVersion: '1.2'
    publicNetworkAccess: 'Enabled'
    routing: {
      endpoints: {
        serviceBusQueues: []
        serviceBusTopics: []
        eventHubs: []
        storageContainers: []
        cosmosDBSqlCollections: []
      }
      routes: []
      fallbackRoute: {
        name: '$fallback'
        source: 'DeviceMessages'
        condition: 'true'
        endpointNames: [
          'events'
        ]
        isEnabled: true
      }
    }
  }
}

// Private Endpoint for IoT Hub
resource iotPrivateEndpoint 'Microsoft.Network/privateEndpoints@2023-11-01' = {
  name: 'pep-iot-${environment}'
  location: location
  properties: {
    subnet: {
      id: dataSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'iot-connection'
        properties: {
          privateLinkServiceId: iotHub.id
          groupIds: [
            'iotHub'
          ]
        }
      }
    ]
  }
}

// Private DNS Zone Group for private endpoint
resource iotPrivateDnsZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-11-01' = if (!empty(iotPrivateDnsZoneId)) {
  parent: iotPrivateEndpoint
  name: 'default'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: iotPrivateDnsZoneName
        properties: {
          privateDnsZoneId: iotPrivateDnsZoneId
        }
      }
    ]
  }
}

@description('The IoT Hub ID')
output iotHubId string = iotHub.id

@description('The IoT Hub Name')
output iotHubName string = iotHub.name

@description('The IoT Hub Event Hub Endpoint')
output iotHubEventHubEndpoint string = iotHub.properties.eventHubEndpoints.events.endpoint

@description('The IoT Hub Connection String (reference only, retrieve from portal)')
output iotHubEndpointDescription string = 'Retrieve connection string from Azure Portal - IoT Hub → Shared access policies → owner'

@description('The IoT Private Endpoint ID')
output iotPrivateEndpointId string = iotPrivateEndpoint.id
