param(
  [ValidateSet('dev', 'prod')]
  [string]$Environment = '',

  [switch]$NoPrompt
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Resolve-Environment {
  param(
    [string]$Current,
    [switch]$SkipPrompt
  )

  if (-not [string]::IsNullOrWhiteSpace($Current)) {
    return $Current.ToLowerInvariant()
  }

  if ($SkipPrompt) {
    return 'dev'
  }

  $answer = Read-Host "Choose environment [dev/prod] (default: dev)"
  if ([string]::IsNullOrWhiteSpace($answer)) {
    return 'dev'
  }

  $normalized = $answer.Trim().ToLowerInvariant()
  if ($normalized -ne 'dev' -and $normalized -ne 'prod') {
    throw "Invalid environment '$answer'. Use dev or prod."
  }

  return $normalized
}

function Set-FrontendBuildEnv {
  param(
    [string]$TargetEnvironment
  )

  if ($TargetEnvironment -eq 'dev') {
    $env:VITE_API_BASE_URL = 'https://healthcarepatientengagement.azurewebsites.net'
    $env:VITE_OIDC_CLIENT_ID = '526100bc-f963-4edd-8890-9011252bb554'
    $env:VITE_OIDC_AUTHORIZATION_ENDPOINT = 'https://login.microsoftonline.com/65087c47-0017-4258-8086-72832006d566/oauth2/v2.0/authorize'
    $env:VITE_OIDC_TOKEN_ENDPOINT = 'https://login.microsoftonline.com/65087c47-0017-4258-8086-72832006d566/oauth2/v2.0/token'
    $env:VITE_OIDC_LOGOUT_ENDPOINT = 'https://login.microsoftonline.com/65087c47-0017-4258-8086-72832006d566/oauth2/v2.0/logout'
    $env:VITE_OIDC_REDIRECT_URI = 'https://healthcarepatientengagement.azurewebsites.net'
    $env:VITE_OIDC_SCOPE = 'openid profile api://65087c47-0017-4258-8086-72832006d566/hpe-devx-api/access_as_user'
    $env:VITE_BYPASS_AUTH = 'false'
    $env:VITE_EVENTS_WS_URL = ''
    $env:VITE_EVENTS_SSE_URL = ''
    return
  }

  throw 'Prod frontend env values are not configured in this script yet. Add them before running with -Environment prod.'
}

function Get-AppServiceName {
  param(
    [string]$TargetEnvironment
  )

  if ($TargetEnvironment -eq 'dev') {
    return 'healthcarepatientengagement'
  }

  throw 'Prod app service name is not configured in this script yet.'
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$targetEnv = Resolve-Environment -Current $Environment -SkipPrompt:$NoPrompt

Write-Host "Deploying frontend to environment: $targetEnv" -ForegroundColor Cyan

Set-FrontendBuildEnv -TargetEnvironment $targetEnv
$appServiceName = Get-AppServiceName -TargetEnvironment $targetEnv
$resourceGroup = 'rg-azuser7080_mml.local-1nLQA'

Push-Location (Join-Path $repoRoot 'frontend')
try {
  npm run build
  if ($LASTEXITCODE -ne 0) {
    throw 'Frontend build failed.'
  }

  $asset = Get-ChildItem 'dist/assets/index-*.js' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
  if ($null -eq $asset) {
    throw 'Could not find built index asset in frontend/dist/assets.'
  }

  $localhostMatches = (Select-String -Path $asset.FullName -Pattern 'localhost:8080|http://localhost:8080' -AllMatches | Measure-Object).Count
  if ($localhostMatches -gt 0) {
    throw "Build blocked: found $localhostMatches localhost API references in $($asset.Name)."
  }
} finally {
  Pop-Location
}

$releaseDir = Join-Path $repoRoot 'release'
$zipPath = Join-Path $repoRoot 'frontend-dist.zip'

if (Test-Path $zipPath) {
  Remove-Item $zipPath -Force
}

if (Test-Path $releaseDir) {
  Remove-Item (Join-Path $releaseDir 'dist') -Recurse -Force -ErrorAction SilentlyContinue
  Remove-Item (Join-Path $releaseDir 'server.js') -Force -ErrorAction SilentlyContinue
} else {
  New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null
}

Copy-Item (Join-Path $repoRoot 'frontend/dist') (Join-Path $releaseDir 'dist') -Recurse -Force
Copy-Item (Join-Path $repoRoot 'frontend/server.js') (Join-Path $releaseDir 'server.js') -Force

# Use tar zip packaging so Kudu on Linux gets forward-slash paths.
tar -a -c -f $zipPath -C $releaseDir .
if ($LASTEXITCODE -ne 0) {
  throw 'Failed to package frontend-dist.zip'
}

az webapp deploy `
  -g $resourceGroup `
  -n $appServiceName `
  --src-path $zipPath `
  --type zip `
  --clean true `
  --restart true `
  --track-status true

if ($LASTEXITCODE -ne 0) {
  throw 'App Service deployment failed.'
}

Write-Host "Frontend deployment completed successfully for $targetEnv." -ForegroundColor Green
