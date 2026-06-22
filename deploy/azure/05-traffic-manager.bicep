param trafficManagerProfileName string
param trafficManagerDnsLabel string
param primaryEndpointTarget string
param secondaryEndpointTarget string
param monitorPath string = '/health'

resource trafficManager 'Microsoft.Network/trafficManagerProfiles@2022-04-01' = {
  name: trafficManagerProfileName
  location: 'global'
  properties: {
    profileStatus: 'Enabled'
    trafficRoutingMethod: 'Priority'
    dnsConfig: {
      relativeName: trafficManagerDnsLabel
      ttl: 60
    }
    monitorConfig: {
      protocol: 'HTTPS'
      port: 443
      path: monitorPath
      intervalInSeconds: 30
      timeoutInSeconds: 10
      toleratedNumberOfFailures: 3
    }
    endpoints: [
      {
        name: 'primary-ui-endpoint'
        type: 'Microsoft.Network/trafficManagerProfiles/externalEndpoints'
        properties: {
          target: primaryEndpointTarget
          endpointStatus: 'Enabled'
          priority: 1
        }
      }
      {
        name: 'secondary-ui-endpoint'
        type: 'Microsoft.Network/trafficManagerProfiles/externalEndpoints'
        properties: {
          target: secondaryEndpointTarget
          endpointStatus: 'Enabled'
          priority: 2
        }
      }
    ]
  }
}

output trafficManagerId string = trafficManager.id
output trafficManagerDnsName string = trafficManager.properties.dnsConfig.fqdn
