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
    "AZURE_BLOB_CONNECTION_STRING",
    "ENTRA_API_APP_ID",
    "ENTRA_GRAPH_BASE_URL",
    "ENTRA_PATIENT_GROUP_NAME",
    "ENTRA_PATIENT_ROLE_VALUE",
    "ENTRA_INVITE_REDIRECT_URL",
    "AZURE_SQL_JDBC_URL",
    "AZURE_SQL_USERNAME",
    "AZURE_SQL_PASSWORD",
    "AZURE_MANAGED_IDENTITY_CLIENT_ID"
)

$optional = @(
    "ACS_INTEGRATION_BASE_URL",
    "TELECONSULT_ACS_INTEGRATION_BASE_URL",
    "TELECONSULT_JOIN_BASE_URL",
    "ACS_EMAIL_ENDPOINT",
    "ACS_EMAIL_ACCESS_KEY",
    "ACS_EMAIL_FROM_ADDRESS",
    "ACS_SMS_ENDPOINT",
    "ACS_SMS_ACCESS_KEY",
    "ACS_SMS_FROM_NUMBER",
    "ACS_IDENTITY_CONNECTION_STRING"
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

$content += "`n  azure-blob-connection-string: `"$($values['AZURE_BLOB_CONNECTION_STRING'])`""
$content += "`n  entra-api-app-id: `"$($values['ENTRA_API_APP_ID'])`""
$content += "`n  entra-graph-base-url: `"$($values['ENTRA_GRAPH_BASE_URL'])`""
$content += "`n  entra-patient-group-name: `"$($values['ENTRA_PATIENT_GROUP_NAME'])`""
$content += "`n  entra-patient-role-value: `"$($values['ENTRA_PATIENT_ROLE_VALUE'])`""
$content += "`n  entra-invite-redirect-url: `"$($values['ENTRA_INVITE_REDIRECT_URL'])`""

foreach ($key in $optional) {
    if ($values.ContainsKey($key) -and -not [string]::IsNullOrWhiteSpace($values[$key])) {
        if ($values[$key] -match "<[^>]+>") {
            throw "Unresolved placeholder for key $key in env file."
        }

        switch ($key) {
            "ACS_INTEGRATION_BASE_URL" {
                $content += "`n  acs-integration-base-url: `"$($values[$key])`""
            }
            "TELECONSULT_ACS_INTEGRATION_BASE_URL" {
                $content += "`n  teleconsult-acs-integration-base-url: `"$($values[$key])`""
            }
            "TELECONSULT_JOIN_BASE_URL" {
                $content += "`n  teleconsult-join-base-url: `"$($values[$key])`""
            }
            "ACS_EMAIL_ENDPOINT" {
                $content += "`n  acs-email-endpoint: `"$($values[$key])`""
            }
            "ACS_EMAIL_ACCESS_KEY" {
                $content += "`n  acs-email-access-key: `"$($values[$key])`""
            }
            "ACS_EMAIL_FROM_ADDRESS" {
                $content += "`n  acs-email-from-address: `"$($values[$key])`""
            }
            "ACS_SMS_ENDPOINT" {
                $content += "`n  acs-sms-endpoint: `"$($values[$key])`""
            }
            "ACS_SMS_ACCESS_KEY" {
                $content += "`n  acs-sms-access-key: `"$($values[$key])`""
            }
            "ACS_SMS_FROM_NUMBER" {
                $content += "`n  acs-sms-from-number: `"$($values[$key])`""
            }
            "ACS_IDENTITY_CONNECTION_STRING" {
                $content += "`n  acs-identity-connection-string: `"$($values[$key])`""
            }
        }
    }
}

if ($values.ContainsKey("PATIENT_IDPROOF_CONTAINER") -and -not [string]::IsNullOrWhiteSpace($values["PATIENT_IDPROOF_CONTAINER"])) {
    if ($values["PATIENT_IDPROOF_CONTAINER"] -match "<[^>]+>") {
        throw "Unresolved placeholder for key PATIENT_IDPROOF_CONTAINER in env file."
    }
    $content += "`n  patient-idproof-container: `"$($values['PATIENT_IDPROOF_CONTAINER'])`""
}

if ($values.ContainsKey("PROFILE_PHOTO_CONTAINER") -and -not [string]::IsNullOrWhiteSpace($values["PROFILE_PHOTO_CONTAINER"])) {
    if ($values["PROFILE_PHOTO_CONTAINER"] -match "<[^>]+>") {
        throw "Unresolved placeholder for key PROFILE_PHOTO_CONTAINER in env file."
    }
    $content += "`n  profile-photo-container: `"$($values['PROFILE_PHOTO_CONTAINER'])`""
}

Set-Content -Path $OutputFile -Value $content -NoNewline
Write-Host "Rendered secret manifest: $OutputFile"
