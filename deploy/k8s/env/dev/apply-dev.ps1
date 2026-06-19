Param(
    [string]$Namespace = "healthcare-dev",
    [string]$KubeContext = "",
    [string]$EnvFile = "",
    [string]$SecretFile = "",
    [switch]$SkipGuardrails,
    [switch]$Preflight
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "../../../../")
$gatewayDir = Join-Path $repoRoot "deploy/k8s/api-gateway"
$servicesRoot = Join-Path $repoRoot "deploy/k8s"
$renderScript = Join-Path (Split-Path -Parent $scriptDir) "render-platform-secret.ps1"
$defaultSecretFile = Join-Path $scriptDir "platform-secrets.dev.template.yaml"

if ($SecretFile -ne "") {
    $secretFile = $SecretFile
} else {
    $secretFile = $defaultSecretFile
}

if ($EnvFile -ne "") {
    if (-not (Test-Path $renderScript)) {
        throw "Missing render helper: $renderScript"
    }

    $secretFile = Join-Path $scriptDir "platform-secrets.dev.generated.yaml"

    Write-Host "Rendering dev secret manifest from $EnvFile"
    try {
        & $renderScript -EnvFile $EnvFile -OutputFile $secretFile -Namespace $Namespace
    }
    catch {
        throw "Failed to render secret manifest from env file."
    }
}

function Test-SecretTemplate {
    Param([string]$FilePath)

    $requiredKeys = @(
        "servicebus-namespace",
        "eventhub-namespace",
        "key-vault-url",
        "fhir-integration-base-url",
        "service-bus-integration-base-url",
        "otel-otlp-endpoint",
        "oauth2-issuer",
        "oauth2-audience",
        "oauth2-jwk-set-uri",
        "azure-blob-connection-string",
        "entra-api-app-id",
        "entra-graph-base-url",
        "entra-patient-group-name",
        "entra-patient-role-value",
        "entra-invite-redirect-url",
        "azure-sql-jdbc-url",
        "azure-sql-username",
        "azure-sql-password",
        "azure-managed-identity-client-id"
    )

    $content = Get-Content -Raw -Path $FilePath

    foreach ($key in $requiredKeys) {
        if ($content -notmatch "(?m)^\s{2}$([regex]::Escape($key)):\s*") {
            throw "Secret template missing required key: $key"
        }
    }

    if ($content -match "<[^>]+>") {
        throw "Secret template contains unresolved placeholder markers (<...>)."
    }
}

if (-not (Test-Path $secretFile)) {
    throw "Missing secret template: $secretFile"
}

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    throw "kubectl is not available in PATH."
}

if ($KubeContext -ne "") {
    Write-Host "Switching kubectl context to $KubeContext"
    kubectl config use-context $KubeContext | Out-Null
}

$currentContext = kubectl config current-context
if (-not $currentContext) {
    throw "Unable to resolve current kubectl context."
}

Write-Host "Using kubectl context: $currentContext"

Write-Host "Checking namespace: $Namespace"
kubectl get namespace $Namespace | Out-Null

Write-Host "Validating secret template completeness"
Test-SecretTemplate -FilePath $secretFile

if ($Preflight) {
    Write-Host "Preflight checks passed. No resources were applied."
    exit 0
}

Write-Host "Applying dev platform secrets"
kubectl apply -f $secretFile

Write-Host "Applying API gateway manifests"
kubectl apply -n $Namespace -f $gatewayDir

$serviceDirs = @(
    "svc-patient",
    "svc-appointment",
    "svc-careplan",
    "svc-consent",
    "svc-medical-record",
    "svc-notification",
    "svc-telemetry",
    "svc-device-ingestion",
    "svc-alert-management",
    "svc-identity-adapter",
    "svc-event-messaging"
)

foreach ($svc in $serviceDirs) {
    $svcPath = Join-Path $servicesRoot $svc
    if (Test-Path $svcPath) {
        Write-Host "Applying manifests for $svc"
        kubectl apply -n $Namespace -f $svcPath
    }
}

if (-not $SkipGuardrails) {
    Write-Host "Running minimum guardrails"
    Push-Location $repoRoot
    try {
        bash scripts/run-infra-dr-guardrails.sh
    }
    finally {
        Pop-Location
    }
}

Write-Host "Dev rollout completed."
