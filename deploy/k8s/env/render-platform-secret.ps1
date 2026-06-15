Param(
    [Parameter(Mandatory = $true)]
    [string]$EnvFile,
    [Parameter(Mandatory = $true)]
    [string]$OutputFile,
    [string]$Namespace = ""
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $EnvFile)) {
    throw "Env file not found: $EnvFile"
}

$required = @(
    "SERVICEBUS_NAMESPACE",
    "EVENTHUB_NAMESPACE",
    "KEY_VAULT_URL",
    "FHIR_INTEGRATION_BASE_URL",
    "SERVICE_BUS_INTEGRATION_BASE_URL",
    "OTEL_OTLP_ENDPOINT",
    "OAUTH2_ISSUER",
    "OAUTH2_AUDIENCE",
    "OAUTH2_JWK_SET_URI",
    "AZURE_SQL_JDBC_URL",
    "AZURE_SQL_USERNAME",
    "AZURE_SQL_PASSWORD",
    "AZURE_MANAGED_IDENTITY_CLIENT_ID"
)

$values = @{}
Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }

    $parts = $line.Split("=", 2)
    if ($parts.Count -ne 2) { return }

    $key = $parts[0].Trim()
    $value = $parts[1].Trim()
    $values[$key] = $value
}

foreach ($key in $required) {
    if (-not $values.ContainsKey($key)) {
        throw "Missing required key in env file: $key"
    }

    if ($values[$key] -match "<[^>]+>") {
        throw "Unresolved placeholder for key $key in env file."
    }
}

if ($Namespace -eq "") {
    if ($values.ContainsKey("NAMESPACE")) {
        $Namespace = $values["NAMESPACE"]
    } else {
        throw "Namespace not provided and NAMESPACE key is missing in env file."
    }
}

$content = @"
apiVersion: v1
kind: Secret
metadata:
  name: platform-secrets
  namespace: $Namespace
type: Opaque
stringData:
  servicebus-namespace: "$($values['SERVICEBUS_NAMESPACE'])"
  eventhub-namespace: "$($values['EVENTHUB_NAMESPACE'])"
  key-vault-url: "$($values['KEY_VAULT_URL'])"
  fhir-integration-base-url: "$($values['FHIR_INTEGRATION_BASE_URL'])"
  service-bus-integration-base-url: "$($values['SERVICE_BUS_INTEGRATION_BASE_URL'])"
  otel-otlp-endpoint: "$($values['OTEL_OTLP_ENDPOINT'])"
  oauth2-issuer: "$($values['OAUTH2_ISSUER'])"
  oauth2-audience: "$($values['OAUTH2_AUDIENCE'])"
  oauth2-jwk-set-uri: "$($values['OAUTH2_JWK_SET_URI'])"
  azure-sql-jdbc-url: "$($values['AZURE_SQL_JDBC_URL'])"
  azure-sql-username: "$($values['AZURE_SQL_USERNAME'])"
  azure-sql-password: "$($values['AZURE_SQL_PASSWORD'])"
  azure-managed-identity-client-id: "$($values['AZURE_MANAGED_IDENTITY_CLIENT_ID'])"
"@

Set-Content -Path $OutputFile -Value $content -NoNewline
Write-Host "Rendered secret manifest: $OutputFile"
