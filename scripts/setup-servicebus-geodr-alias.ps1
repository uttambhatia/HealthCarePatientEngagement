Param(
    [Parameter(Mandatory = $true)]
    [string]$ResourceGroup,

    [Parameter(Mandatory = $true)]
    [string]$PrimaryNamespace,

    [Parameter(Mandatory = $true)]
    [string]$SecondaryNamespace,

    [Parameter(Mandatory = $true)]
    [string]$AliasName
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
    throw "Azure CLI (az) is required."
}

Write-Host "Configuring Service Bus Geo-DR alias '$AliasName'"
az servicebus georecovery-alias set `
    --resource-group $ResourceGroup `
    --namespace-name $PrimaryNamespace `
    --alias $AliasName `
    --partner-namespace $SecondaryNamespace

Write-Host "Geo-DR alias status:"
az servicebus georecovery-alias show `
    --resource-group $ResourceGroup `
    --namespace-name $PrimaryNamespace `
    --alias $AliasName `
    --output table
