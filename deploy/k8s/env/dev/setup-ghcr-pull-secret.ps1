Param(
    [string]$Namespace = "healthcare-dev",
    [string]$SecretName = "ghcr-pull",
    [string]$Username = "",
    [string]$Token = "",
    [switch]$SkipRestart
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    throw "kubectl is not available in PATH."
}

$namespaceResult = kubectl get namespace $Namespace --ignore-not-found -o name
if (-not $?) {
    throw "Unable to query namespace $Namespace."
}

if ([string]::IsNullOrWhiteSpace($namespaceResult)) {
    throw "Namespace '$Namespace' does not exist."
}

if ([string]::IsNullOrWhiteSpace($Username)) {
    $Username = Read-Host "Enter GHCR username"
}

if ([string]::IsNullOrWhiteSpace($Username)) {
    throw "GHCR username is required."
}

if ([string]::IsNullOrWhiteSpace($Token)) {
    $secureToken = Read-Host "Enter GHCR Personal Access Token (read:packages)" -AsSecureString
    $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureToken)
    try {
        $Token = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

if ([string]::IsNullOrWhiteSpace($Token)) {
    throw "GHCR token is required."
}

Write-Host "Creating/updating image pull secret '$SecretName' in namespace '$Namespace'"
kubectl create secret docker-registry $SecretName `
  --namespace $Namespace `
  --docker-server=ghcr.io `
  --docker-username=$Username `
  --docker-password=$Token `
  --docker-email=unused@example.com `
  --dry-run=client -o yaml | kubectl apply -f -

Write-Host "Patching default service account with imagePullSecrets"
$patchFile = Join-Path $env:TEMP "sa-patch-$Namespace-$SecretName.yaml"
@"
imagePullSecrets:
    - name: $SecretName
"@ | Set-Content -Path $patchFile

kubectl patch serviceaccount default -n $Namespace --type merge --patch-file $patchFile

if (-not $SkipRestart) {
    Write-Host "Restarting deployments in namespace '$Namespace'"
    kubectl rollout restart deployment -n $Namespace
}

Write-Host "Completed GHCR pull-secret setup."
Write-Host "Recommended check: kubectl get pods -n $Namespace"