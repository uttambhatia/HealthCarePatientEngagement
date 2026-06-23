// Minimal deployment: AKS cluster only
targetScope = 'resourceGroup'

param location string = 'centralindia'
param environment string = 'dev'
param aksClusterName string = 'aks-hpe-${environment}'
param aksNodeCount int = 3
param aksNodeVmSize string = 'Standard_B2s_v2'
param aksVersion string = '1.34.8'

// References to existing infrastructure
var primaryVNetName = 'hpe-primary-vnet-${environment}'
var primaryApplicationSubnetName = 'application-subnet'

// Get references to existing VNets and subnets
resource primaryVNet 'Microsoft.Network/virtualNetworks@2023-02-01' existing = {
  name: primaryVNetName
}

resource primaryApplicationSubnet 'Microsoft.Network/virtualNetworks/subnets@2023-02-01' existing = {
  parent: primaryVNet
  name: primaryApplicationSubnetName
}

// Deploy AKS cluster
module aksCluster './04-aks.bicep' = {
  name: 'primary-aks-cluster'
  params: {
    location: location
    environment: environment
    aksClusterName: aksClusterName
    aksNodeCount: aksNodeCount
    aksNodeVmSize: aksNodeVmSize
    aksVersion: aksVersion
    primaryApplicationSubnetId: primaryApplicationSubnet.id
  }
}

// Outputs
output aksClusterId string = aksCluster.outputs.aksClusterId
output aksClusterName string = aksCluster.outputs.aksClusterName
output aksClusterFqdn string = aksCluster.outputs.aksClusterFqdn
output deploymentStatus string = 'AKS cluster deployed successfully in primary application subnet'
