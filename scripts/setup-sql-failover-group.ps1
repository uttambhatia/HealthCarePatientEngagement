Param(
    [Parameter(Mandatory = $true)]
    [string]$PrimaryResourceGroup,

    [Parameter(Mandatory = $true)]
    [string]$PrimaryServerName,

    [Parameter(Mandatory = $true)]
    [string]$PrimaryDatabaseName,

    [Parameter(Mandatory = $true)]
    [string]$FailoverGroupName,

    [Parameter(Mandatory = $true)]
    [string]$SecondaryServerName,

    [string]$SecondaryResourceGroup = "",

    [ValidateRange(1, 24)]
    [int]$GracePeriodHours = 1
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
    throw "Azure CLI (az) is required."
}

if ([string]::IsNullOrWhiteSpace($SecondaryResourceGroup)) {
    $SecondaryResourceGroup = $PrimaryResourceGroup
}

Write-Host "Configuring SQL failover group '$FailoverGroupName'"
az sql failover-group create `
    --name $FailoverGroupName `
    --resource-group $PrimaryResourceGroup `
    --server $PrimaryServerName `
    --partner-server $SecondaryServerName `
    --partner-resource-group $SecondaryResourceGroup `
    --add-db $PrimaryDatabaseName `
    --failover-policy Automatic `
    --grace-period $GracePeriodHours

Write-Host "Failover group status:"
az sql failover-group show `
    --name $FailoverGroupName `
    --resource-group $PrimaryResourceGroup `
    --server $PrimaryServerName `
    --output table
