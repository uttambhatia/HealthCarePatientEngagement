Param(
    [Parameter(Mandatory = $true)]
    [string]$ResourceGroup,

    [Parameter(Mandatory = $true)]
    [string]$TrafficManagerProfileName,

    [Parameter(Mandatory = $true)]
    [string]$ApimServiceName,

    [ValidateSet("failover", "failback")]
    [string]$Mode = "failover",

    [switch]$PrecheckOnly
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$trafficScript = Join-Path $scriptDir "switch-traffic-manager-priority.ps1"
$apimScript = Join-Path $scriptDir "switch-apim-active-backend.ps1"

if (-not (Test-Path $trafficScript)) {
    throw "Missing script: $trafficScript"
}

if (-not (Test-Path $apimScript)) {
    throw "Missing script: $apimScript"
}

if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
    throw "Azure CLI (az) is required."
}

function Invoke-DrControlPlanePrecheck {
    Param(
        [string]$ResourceGroupName,
        [string]$TrafficManagerName,
        [string]$ApimName
    )

    Write-Host "Running DR control-plane precheck"

    az account show --output none

    az network traffic-manager profile show `
        --resource-group $ResourceGroupName `
        --name $TrafficManagerName `
        --output none

    $endpointNames = @(az network traffic-manager endpoint list `
        --resource-group $ResourceGroupName `
        --profile-name $TrafficManagerName `
        --type externalEndpoints `
        --query "[].name" `
        --output tsv)

    if ($endpointNames -notcontains "primary-ui-endpoint") {
        throw "Traffic Manager endpoint 'primary-ui-endpoint' was not found in profile '$TrafficManagerName'."
    }

    if ($endpointNames -notcontains "secondary-ui-endpoint") {
        throw "Traffic Manager endpoint 'secondary-ui-endpoint' was not found in profile '$TrafficManagerName'."
    }

    az apim show `
        --resource-group $ResourceGroupName `
        --name $ApimName `
        --output none

    $namedValue = az apim nv show `
        --resource-group $ResourceGroupName `
        --service-name $ApimName `
        --named-value-id apim-active-backend `
        --query value `
        --output tsv

    if ([string]::IsNullOrWhiteSpace($namedValue)) {
        throw "APIM named value 'apim-active-backend' is missing or empty on service '$ApimName'."
    }

    Write-Host "Precheck passed. Current APIM backend value: $namedValue"
}

Invoke-DrControlPlanePrecheck `
    -ResourceGroupName $ResourceGroup `
    -TrafficManagerName $TrafficManagerProfileName `
    -ApimName $ApimServiceName

if ($PrecheckOnly) {
    Write-Host "Precheck-only mode enabled. No switch changes were applied."
    exit 0
}

Write-Host "Starting combined DR control-plane switch in mode '$Mode'"

# Switch global edge routing first so new requests begin draining toward target region.
& $trafficScript `
    -ResourceGroup $ResourceGroup `
    -TrafficManagerProfileName $TrafficManagerProfileName `
    -Mode $Mode

# Then switch APIM backend routing so API traffic follows the same active region.
& $apimScript `
    -ResourceGroup $ResourceGroup `
    -ApimServiceName $ApimServiceName `
    -Mode $Mode

Write-Host "Combined DR control-plane switch completed successfully."
