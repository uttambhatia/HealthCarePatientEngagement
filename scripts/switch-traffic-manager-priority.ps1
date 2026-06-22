Param(
    [Parameter(Mandatory = $true)]
    [string]$ResourceGroup,

    [Parameter(Mandatory = $true)]
    [string]$TrafficManagerProfileName,

    [ValidateSet("failover", "failback")]
    [string]$Mode = "failover"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
    throw "Azure CLI (az) is required."
}

$primaryPriority = if ($Mode -eq "failover") { 2 } else { 1 }
$secondaryPriority = if ($Mode -eq "failover") { 1 } else { 2 }

Write-Host "Applying Traffic Manager mode '$Mode' on profile '$TrafficManagerProfileName'"

az network traffic-manager endpoint update `
    --resource-group $ResourceGroup `
    --profile-name $TrafficManagerProfileName `
    --type externalEndpoints `
    --name primary-ui-endpoint `
    --priority $primaryPriority `
    --endpoint-status Enabled

az network traffic-manager endpoint update `
    --resource-group $ResourceGroup `
    --profile-name $TrafficManagerProfileName `
    --type externalEndpoints `
    --name secondary-ui-endpoint `
    --priority $secondaryPriority `
    --endpoint-status Enabled

Write-Host "Current endpoint priorities:"
az network traffic-manager endpoint list `
    --resource-group $ResourceGroup `
    --profile-name $TrafficManagerProfileName `
    --type externalEndpoints `
    --query "[].{name:name,priority:priority,status:endpointStatus,target:target}" `
    --output table
