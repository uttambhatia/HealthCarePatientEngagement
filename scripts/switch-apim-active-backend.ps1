Param(
    [Parameter(Mandatory = $true)]
    [string]$ResourceGroup,

    [Parameter(Mandatory = $true)]
    [string]$ApimServiceName,

    [ValidateSet("failover", "failback")]
    [string]$Mode = "failover"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
    throw "Azure CLI (az) is required."
}

$activeBackend = if ($Mode -eq "failover") { "secondary" } else { "primary" }

Write-Host "Setting APIM active backend to '$activeBackend' on service '$ApimServiceName'"

az apim nv update `
    --resource-group $ResourceGroup `
    --service-name $ApimServiceName `
    --named-value-id apim-active-backend `
    --value $activeBackend | Out-Null

$updatedValue = az apim nv show `
    --resource-group $ResourceGroup `
    --service-name $ApimServiceName `
    --named-value-id apim-active-backend `
    --query value `
    --output tsv

Write-Host "Current APIM active backend: $updatedValue"

if ($updatedValue -ne $activeBackend) {
    throw "APIM backend switch verification failed. Expected '$activeBackend' but found '$updatedValue'."
}

Write-Host "APIM backend switch completed."
