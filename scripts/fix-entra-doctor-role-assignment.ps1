param(
  [string]$SubscriptionId = '4116346b-5ded-4f3b-8387-f8d055802adc',
  [string]$ApiIdentifierUri = 'api://65087c47-0017-4258-8086-72832006d566/hpe-devx-api',
  [string]$ApiDisplayName = 'hcpe-api-backend',
  [string]$GroupName = 'HCPE-DOCTOR',
  [string]$RoleValue = 'DOCTOR'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Invoke-AzJson {
  param(
    [Parameter(Mandatory = $true)]
    [string[]]$Arguments,

    [switch]$AllowFailure
  )

  $output = & az @Arguments -o json 2>&1
  $exitCode = $LASTEXITCODE

  if ($exitCode -ne 0) {
    if ($AllowFailure) {
      return $null
    }

    $joined = ($output | Out-String).Trim()
    throw "az command failed: az $($Arguments -join ' ') ; $joined"
  }

  $text = ($output | Out-String).Trim()
  if ([string]::IsNullOrWhiteSpace($text)) {
    return $null
  }

  return $text | ConvertFrom-Json
}

function Write-Step {
  param([string]$Message)
  Write-Host "=== $Message ==="
}

az account set --subscription $SubscriptionId | Out-Null

Write-Step 'API app lookup'
$apiApp = Invoke-AzJson -Arguments @('ad', 'app', 'list', '--filter', "identifierUris/any(u:u eq '$ApiIdentifierUri')", '--query', '[0]')
if (-not $apiApp) {
  $apiApp = Invoke-AzJson -Arguments @('ad', 'app', 'list', '--display-name', $ApiDisplayName, '--query', '[0]')
}
if (-not $apiApp) {
  throw "API app not found for $ApiIdentifierUri or $ApiDisplayName."
}

$apiAppId = $apiApp.appId
Write-Host "API_APP_ID=$apiAppId"

$apiSp = Invoke-AzJson -Arguments @('ad', 'sp', 'show', '--id', $apiAppId, '--query', '{id:id,appId:appId,displayName:displayName}')
if (-not $apiSp) {
  throw "Service principal not found for appId $apiAppId."
}

Write-Host "API_SP_OBJECT_ID=$($apiSp.id)"

Write-Step 'Role lookup'
$role = Invoke-AzJson -Arguments @('ad', 'app', 'show', '--id', $apiAppId, '--query', "appRoles[?value=='$RoleValue'] | [0]")
if (-not $role) {
  throw "App role '$RoleValue' not found on API app $apiAppId."
}

Write-Host "ROLE_VALUE=$RoleValue"
Write-Host "ROLE_ID=$($role.id)"

Write-Step 'Group lookup'
$group = Invoke-AzJson -Arguments @('ad', 'group', 'show', '--group', $GroupName, '--query', '{id:id,displayName:displayName}')
if (-not $group) {
  throw "Group '$GroupName' not found."
}

Write-Host "GROUP_NAME=$GroupName"
Write-Host "GROUP_ID=$($group.id)"

Write-Step 'Assignment check'
$existing = Invoke-AzJson -Arguments @(
  'rest',
  '--method', 'GET',
  '--url', "https://graph.microsoft.com/v1.0/servicePrincipals/$($apiSp.id)/appRoleAssignedTo",
  '--query', "value[?principalId=='$($group.id)' && appRoleId=='$($role.id)']"
)

if ($existing -and $existing.Count -gt 0) {
  Write-Host 'Assignment already exists.'
}
else {
  Write-Step 'Create assignment'
  $payload = @{ principalId = $group.id; resourceId = $apiSp.id; appRoleId = $role.id } | ConvertTo-Json -Compress
  $tmp = Join-Path $PWD 'tmp-entra-role-assignment.json'
  Set-Content -Path $tmp -Value $payload -Encoding utf8

  try {
    az rest --method POST --url "https://graph.microsoft.com/v1.0/servicePrincipals/$($apiSp.id)/appRoleAssignedTo" --headers 'Content-Type=application/json' --body "@$tmp" | Out-Null
    Write-Host 'Assignment created.'
  }
  finally {
    if (Test-Path $tmp) {
      Remove-Item $tmp -Force
    }
  }
}

Write-Step 'Verification'
$verify = Invoke-AzJson -Arguments @(
  'rest',
  '--method', 'GET',
  '--url', "https://graph.microsoft.com/v1.0/servicePrincipals/$($apiSp.id)/appRoleAssignedTo",
  '--query', "value[?principalId=='$($group.id)']"
)

if ($verify) {
  $verify | Select-Object principalDisplayName, appRoleId, principalId, resourceDisplayName | Format-Table -AutoSize | Out-String | Write-Host
}
else {
  Write-Host 'No assignments returned during verification.'
}
