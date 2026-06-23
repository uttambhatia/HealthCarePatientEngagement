param location string = resourceGroup().location
param environment string = 'dev'

param aksClusterName string = 'aks-hpe-${environment}'
param aksNodeCount int = 3
param aksNodeVmSize string = 'Standard_B2s_v2'
param aksVersion string = '1.34.8'

param primaryApplicationSubnetId string

// Create AKS Cluster with VNet integration
resource aksCluster 'Microsoft.ContainerService/managedClusters@2024-02-01' = {
  name: aksClusterName
  location: location
  identity: {
    type: 'SystemAssigned'
  }
  sku: {
    name: 'Base'
    tier: 'Standard'
  }
  properties: {
    kubernetesVersion: aksVersion
    dnsPrefix: '${aksClusterName}-dns'
    agentPoolProfiles: [
      {
        name: 'nodepool1'
        count: aksNodeCount
        vmSize: aksNodeVmSize
        mode: 'System'
        osType: 'Linux'
        osDiskSizeGB: 128
        vnetSubnetID: primaryApplicationSubnetId
        enableNodePublicIP: false
        nodeLabels: {
          workload: 'general'
        }
      }
    ]
    networkProfile: {
      networkPlugin: 'kubenet'
      networkPolicy: 'calico'
      serviceCidrs: [
        '10.10.0.0/16'
      ]
      dnsServiceIP: '10.10.0.10'
      outboundType: 'loadBalancer'
      loadBalancerProfile: {
        managedOutboundIPs: {
          count: 1
        }
      }
    }
    addonProfiles: {
      httpApplicationRouting: {
        enabled: false
      }
      omsagent: {
        enabled: false
      }
    }
    apiServerAccessProfile: {
      enablePrivateCluster: false
      authorizedIPRanges: [
        '0.0.0.0/0'
      ]
    }
    autoUpgradeProfile: {
      upgradeChannel: 'stable'
    }
  }
}

@description('The AKS Cluster ID')
output aksClusterId string = aksCluster.id

@description('The AKS Cluster Name')
output aksClusterName string = aksCluster.name

@description('The AKS Cluster FQDN')
output aksClusterFqdn string = aksCluster.properties.fqdn

@description('The AKS Cluster Kubelet Identity')
output aksKubeletIdentity object = aksCluster.properties.identityProfile.kubeletidentity

@description('The AKS Node Resource Group')
output aksNodeResourceGroup string = aksCluster.properties.nodeResourceGroup
