Param(
    [string]$Namespace = "healthcare-prod",
    [string]$KubeContext = "",
    [switch]$SkipGuardrails,
    [switch]$AcknowledgeProd,
    [switch]$Preflight
)

$ErrorActionPreference = "Stop"

if ((-not $Preflight) -and (-not $AcknowledgeProd)) {
    throw "Production rollout requires -AcknowledgeProd."
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "../../../../")
$gatewayDir = Join-Path $repoRoot "deploy/k8s/api-gateway"
$servicesRoot = Join-Path $repoRoot "deploy/k8s"
$secretFile = Join-Path $scriptDir "platform-secrets.prod.template.yaml"

function Test-SecretTemplate {
    Param([string]$FilePath)

    $requiredKeys = @(
        "servicebus-namespace",
        "servicebus-connection-string",
        "eventhub-namespace",
        "key-vault-url",
        "fhir-integration-base-url",
        "service-bus-integration-base-url",
        "acs-integration-base-url",
        "acs-email-endpoint",
        "acs-email-access-key",
        "acs-email-from-address",
        "acs-identity-connection-string",
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

Write-Host "Applying prod platform secrets"
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

Write-Host "Prod rollout completed."
