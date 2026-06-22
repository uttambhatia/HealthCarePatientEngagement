Param(
    [Parameter(Mandatory = $true)]
    [string]$ResourceGroup,

    [string]$TemplateFile = "deploy/azure/main.bicep",

    [string]$ParametersFile = "deploy/azure/parameters/dev.parameters.json",

    [switch]$WhatIf
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
    throw "Azure CLI (az) is required."
}

if (-not (Test-Path $TemplateFile)) {
    throw "Template file not found: $TemplateFile"
}

if (-not (Test-Path $ParametersFile)) {
    throw "Parameters file not found: $ParametersFile"
}

$deploymentAction = if ($WhatIf) { "what-if" } else { "create" }

$baseArgs = @(
    "deployment"
    "group"
    $deploymentAction
    "--resource-group"
    $ResourceGroup
    "--template-file"
    $TemplateFile
    "--parameters"
    "@$ParametersFile"
)

Write-Host "Running az $($baseArgs -join ' ')"
az @baseArgs
