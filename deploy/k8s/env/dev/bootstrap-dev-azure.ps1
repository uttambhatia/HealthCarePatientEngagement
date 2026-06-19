Param(
    [string]$ResourceGroupName = "rg-azuser7080_mml.local-1nLQA",
    [string]$Location = "centralindia",
    [string]$Namespace = "healthcare-dev",
    [string]$NamePrefix = "hpe-dev",
    [string]$AksClusterName = "",
    [string]$NodeVmSize = "Standard_B2s_v2",
    [int]$NodeCount = 1,
    [string]$SqlDatabaseName = "healthcare-dev",
    [string]$SqlAdminUsername = "hpesqladmin",
    [string]$SqlAdminPassword = "",
    [string]$EventHubName = "healthcare-events",
    [string]$AcsResourceName = "",
    [string]$AcsDataLocation = "",
    [string]$AcsIdentityConnectionString = "",
    [string]$TeleconsultJoinBaseUrl = "https://<dev-teleconsult-public-host>/session",
    [string]$FhirIntegrationBaseUrl = "",
    [string]$ServiceBusIntegrationBaseUrl = "",
    [string]$OtelOtlpEndpoint = "http://otel-collector.monitoring.svc.cluster.local:4317",
    [string]$OAuth2Audience = "",
    [string]$OutputEnvFile = "",
    [string]$RenderedSecretFile = "",
    [switch]$SkipNamespaceSetup,
    [switch]$SkipAcsInfrastructure,
    [switch]$EnableEntraAppRegistration
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$envRoot = Split-Path -Parent $scriptDir
$renderScript = Join-Path $envRoot "render-platform-secret.ps1"

if ($OutputEnvFile -eq "") {
    $OutputEnvFile = Join-Path $scriptDir "dev.env"
}

if ($RenderedSecretFile -eq "") {
    $RenderedSecretFile = Join-Path $scriptDir "platform-secrets.dev.generated.yaml"
}

function Invoke-AzCli {
    Param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    $previousPreference = $ErrorActionPreference
    if ($AllowFailure) {
        $ErrorActionPreference = "Continue"
    }

    try {
        $output = & az @Arguments --only-show-errors 2>&1
        $exitCode = $LASTEXITCODE
    }
    finally {
        if ($AllowFailure) {
            $ErrorActionPreference = $previousPreference
        }
    }

    if ($exitCode -ne 0 -and -not $AllowFailure) {
        $message = ($output | Out-String).Trim()
        throw "Azure CLI command failed: az $($Arguments -join ' ')`n$message"
    }

    if ($exitCode -ne 0 -and $AllowFailure) {
        return $null
    }

    return $output
}

function Invoke-AzJson {
    Param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    $jsonText = Invoke-AzCli -Arguments ($Arguments + @("-o", "json")) -AllowFailure:$AllowFailure
    if ($null -eq $jsonText) {
        return $null
    }

    $serialized = ($jsonText | Out-String).Trim()
    if ($serialized -eq "") {
        return $null
    }

    return $serialized | ConvertFrom-Json
}

function Get-HashSuffix {
    Param([Parameter(Mandatory = $true)][string]$Text)

    $md5 = [System.Security.Cryptography.MD5]::Create()
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($Text)
        $hash = $md5.ComputeHash($bytes)
        return ([System.BitConverter]::ToString($hash)).Replace("-", "").ToLowerInvariant().Substring(0, 6)
    }
    finally {
        $md5.Dispose()
    }
}

function Get-SafeBaseName {
    Param(
        [Parameter(Mandatory = $true)]
        [string]$Value,
        [switch]$KeepHyphen
    )

    $lower = $Value.ToLowerInvariant()
    if ($KeepHyphen) {
        $sanitized = $lower -replace "[^a-z0-9-]", "-"
        $sanitized = $sanitized -replace "-+", "-"
        $sanitized = $sanitized.Trim("-")
    } else {
        $sanitized = $lower -replace "[^a-z0-9]", ""
    }

    if ([string]::IsNullOrWhiteSpace($sanitized)) {
        throw "Unable to derive a safe resource name from '$Value'."
    }

    return $sanitized
}

function Get-TrimmedName {
    Param(
        [Parameter(Mandatory = $true)]
        [string]$Base,
        [Parameter(Mandatory = $true)]
        [int]$MaxLength
    )

    if ($Base.Length -le $MaxLength) {
        return $Base
    }

    return $Base.Substring(0, $MaxLength).Trim('-')
}

function New-Password {
    Param([int]$Length = 24)

    $chars = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#$%^&*()-_=+"
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $bytes = New-Object byte[] ($Length)
        $rng.GetBytes($bytes)
        $passwordChars = for ($i = 0; $i -lt $Length; $i++) {
            $chars[$bytes[$i] % $chars.Length]
        }

        return -join $passwordChars
    }
    finally {
        $rng.Dispose()
    }
}

function Ensure-AzLogin {
    $account = Invoke-AzJson -Arguments @("account", "show") -AllowFailure
    if ($null -eq $account) {
        throw "Azure CLI is not logged in. Run 'az login' first."
    }

    return $account
}

function Ensure-ResourceGroup {
    Param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Region
    )

    Write-Host "Ensuring resource group $Name in $Region"
    Invoke-AzJson -Arguments @("group", "create", "--name", $Name, "--location", $Region) | Out-Null
}

function Ensure-UserAssignedIdentity {
    Param(
        [Parameter(Mandatory = $true)][string]$Name
    )

    $existing = Invoke-AzJson -Arguments @("identity", "show", "--resource-group", $ResourceGroupName, "--name", $Name) -AllowFailure
    if ($null -ne $existing) {
        return $existing
    }

    Write-Host "Creating managed identity $Name"
    return Invoke-AzJson -Arguments @("identity", "create", "--resource-group", $ResourceGroupName, "--name", $Name, "--location", $Location)
}

function Ensure-KeyVault {
    Param(
        [Parameter(Mandatory = $true)][string]$Name
    )

    $existing = Invoke-AzJson -Arguments @("keyvault", "show", "--resource-group", $ResourceGroupName, "--name", $Name) -AllowFailure
    if ($null -ne $existing) {
        return $existing
    }

    Write-Host "Creating Key Vault $Name"
    return Invoke-AzJson -Arguments @(
        "keyvault", "create",
        "--resource-group", $ResourceGroupName,
        "--name", $Name,
        "--location", $Location,
        "--enable-rbac-authorization", "true"
    )
}

function Ensure-ServiceBusNamespace {
    Param(
        [Parameter(Mandatory = $true)][string]$Name
    )

    $existing = Invoke-AzJson -Arguments @("servicebus", "namespace", "show", "--resource-group", $ResourceGroupName, "--name", $Name) -AllowFailure
    if ($null -ne $existing) {
        return $existing
    }

    Write-Host "Creating Service Bus namespace $Name"
    return Invoke-AzJson -Arguments @(
        "servicebus", "namespace", "create",
        "--resource-group", $ResourceGroupName,
        "--name", $Name,
        "--location", $Location,
        "--sku", "Standard"
    )
}

function Ensure-EventHubNamespace {
    Param(
        [Parameter(Mandatory = $true)][string]$Name
    )

    $existing = Invoke-AzJson -Arguments @("eventhubs", "namespace", "show", "--resource-group", $ResourceGroupName, "--name", $Name) -AllowFailure
    if ($null -ne $existing) {
        return $existing
    }

    Write-Host "Creating Event Hubs namespace $Name"
    return Invoke-AzJson -Arguments @(
        "eventhubs", "namespace", "create",
        "--resource-group", $ResourceGroupName,
        "--name", $Name,
        "--location", $Location,
        "--sku", "Standard"
    )
}

function Ensure-EventHub {
    Param(
        [Parameter(Mandatory = $true)][string]$NamespaceName,
        [Parameter(Mandatory = $true)][string]$Name
    )

    $existing = Invoke-AzJson -Arguments @("eventhubs", "eventhub", "show", "--resource-group", $ResourceGroupName, "--namespace-name", $NamespaceName, "--name", $Name) -AllowFailure
    if ($null -ne $existing) {
        return $existing
    }

    Write-Host "Creating Event Hub $Name"
    return Invoke-AzJson -Arguments @(
        "eventhubs", "eventhub", "create",
        "--resource-group", $ResourceGroupName,
        "--namespace-name", $NamespaceName,
        "--name", $Name,
        "--cleanup-policy", "Delete",
        "--retention-time-in-hours", "24",
        "--partition-count", "2"
    )
}

function Get-AcsDataLocationForRegion {
    Param(
        [Parameter(Mandatory = $true)][string]$Region
    )

    $normalized = $Region.ToLowerInvariant()
    switch ($normalized) {
        "centralindia" { return "India" }
        "southindia" { return "India" }
        "westindia" { return "India" }
        "eastus" { return "United States" }
        "eastus2" { return "United States" }
        "westus" { return "United States" }
        "westus2" { return "United States" }
        "centralus" { return "United States" }
        "northeurope" { return "Europe" }
        "westeurope" { return "Europe" }
        "uksouth" { return "United Kingdom" }
        "ukwest" { return "United Kingdom" }
        "japaneast" { return "Japan" }
        "japanwest" { return "Japan" }
        "australiaeast" { return "Australia" }
        default { return "India" }
    }
}

function Ensure-CommunicationService {
    Param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$DataLocation
    )

    $existing = Invoke-AzJson -Arguments @("communication", "show", "--resource-group", $ResourceGroupName, "--name", $Name) -AllowFailure
    if ($null -ne $existing) {
        return $existing
    }

    Write-Host "Creating Azure Communication Services resource $Name (data location: $DataLocation)"
    return Invoke-AzJson -Arguments @(
        "communication", "create",
        "--resource-group", $ResourceGroupName,
        "--name", $Name,
        "--data-location", $DataLocation,
        "--location", "global"
    )
}

function Resolve-AcsIdentityConnectionString {
    Param(
        [Parameter(Mandatory = $true)][string]$ResourceName
    )

    $keys = Invoke-AzJson -Arguments @("communication", "list-key", "--resource-group", $ResourceGroupName, "--name", $ResourceName) -AllowFailure
    if ($null -eq $keys) {
        return ""
    }

    if ($keys.primaryConnectionString) {
        return $keys.primaryConnectionString
    }

    if ($keys.primaryKey) {
        $resource = Invoke-AzJson -Arguments @("communication", "show", "--resource-group", $ResourceGroupName, "--name", $ResourceName) -AllowFailure
        if ($null -ne $resource -and $resource.hostName) {
            return "endpoint=https://$($resource.hostName)/;accesskey=$($keys.primaryKey)"
        }
    }

    return ""
}

function Ensure-SqlServer {
    Param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$AdminUser,
        [Parameter(Mandatory = $true)][string]$AdminPassword
    )

    $existing = Invoke-AzJson -Arguments @("sql", "server", "show", "--resource-group", $ResourceGroupName, "--name", $Name) -AllowFailure
    if ($null -eq $existing) {
        Write-Host "Creating Azure SQL logical server $Name"
        $existing = Invoke-AzJson -Arguments @(
            "sql", "server", "create",
            "--resource-group", $ResourceGroupName,
            "--name", $Name,
            "--location", $Location,
            "--admin-user", $AdminUser,
            "--admin-password", $AdminPassword
        )
    }

    Write-Host "Ensuring firewall rule AllowAzureServices"
    Invoke-AzJson -Arguments @(
        "sql", "server", "firewall-rule", "create",
        "--resource-group", $ResourceGroupName,
        "--server", $Name,
        "--name", "AllowAzureServices",
        "--start-ip-address", "0.0.0.0",
        "--end-ip-address", "0.0.0.0"
    ) | Out-Null

    return $existing
}

function Ensure-SqlDatabase {
    Param(
        [Parameter(Mandatory = $true)][string]$ServerName,
        [Parameter(Mandatory = $true)][string]$DatabaseName
    )

    $existing = Invoke-AzJson -Arguments @("sql", "db", "show", "--resource-group", $ResourceGroupName, "--server", $ServerName, "--name", $DatabaseName) -AllowFailure
    if ($null -ne $existing) {
        return $existing
    }

    Write-Host "Creating Azure SQL database $DatabaseName"
    return Invoke-AzJson -Arguments @(
        "sql", "db", "create",
        "--resource-group", $ResourceGroupName,
        "--server", $ServerName,
        "--name", $DatabaseName,
        "--service-objective", "Basic"
    )
}

function Ensure-AksCluster {
    Param(
        [Parameter(Mandatory = $true)][string]$Name
    )

    $existing = Invoke-AzJson -Arguments @("aks", "show", "--resource-group", $ResourceGroupName, "--name", $Name) -AllowFailure
    if ($null -ne $existing) {
        return $existing
    }

    Write-Host "Creating AKS cluster $Name"
    return Invoke-AzJson -Arguments @(
        "aks", "create",
        "--resource-group", $ResourceGroupName,
        "--name", $Name,
        "--location", $Location,
        "--node-count", $NodeCount.ToString(),
        "--node-vm-size", $NodeVmSize,
        "--enable-managed-identity",
        "--generate-ssh-keys"
    )
}

function Ensure-AksCredentials {
    Param(
        [Parameter(Mandatory = $true)][string]$Name
    )

    Write-Host "Fetching AKS credentials for $Name"
    Invoke-AzCli -Arguments @("aks", "get-credentials", "--resource-group", $ResourceGroupName, "--name", $Name, "--overwrite-existing") | Out-Null
}

function Ensure-Namespace {
    Param(
        [Parameter(Mandatory = $true)][string]$Name
    )

    $existingNamespace = kubectl get namespace $Name --ignore-not-found -o name
    if ($existingNamespace) {
        return
    }

    Write-Host "Creating Kubernetes namespace $Name"
    kubectl create namespace $Name | Out-Null
}

function Ensure-EntraAudience {
    Param(
        [Parameter(Mandatory = $true)][string]$BaseName,
        [Parameter(Mandatory = $true)][string]$Suffix,
        [Parameter(Mandatory = $true)][string]$TenantId
    )

    if ($OAuth2Audience -ne "") {
        return $OAuth2Audience
    }

    if (-not $EnableEntraAppRegistration) {
        $fallbackAudience = "api://$TenantId/$BaseName-api"
        Write-Warning "OAuth2Audience was not provided. Using fallback audience '$fallbackAudience'."
        Write-Warning "If your tenant enforces strict app audience policies, pass -OAuth2Audience explicitly."
        return $fallbackAudience
    }

    $displayName = "$BaseName-api"
    $identifierUri = ""
    $existingApps = Invoke-AzJson -Arguments @("ad", "app", "list", "--display-name", $displayName)
    if ($existingApps -and $existingApps.Count -gt 0) {
        $existingAppId = $existingApps[0].appId
        if ($existingAppId) {
            $existingUri = "api://$existingAppId"
            Write-Host "Using existing Entra application $displayName"
            return $existingUri
        }
    }

    Write-Host "Creating Entra application registration $displayName"
    $created = Invoke-AzJson -Arguments @(
        "ad", "app", "create",
        "--display-name", $displayName,
        "--sign-in-audience", "AzureADMyOrg"
    )

    if ($null -eq $created) {
        throw "Failed to create Entra application registration for OAuth2 audience."
    }

    $identifierUri = "api://$($created.appId)"
    Invoke-AzJson -Arguments @(
        "ad", "app", "update",
        "--id", $created.appId,
        "--identifier-uris", $identifierUri
    ) | Out-Null

    return $identifierUri
}

$account = Ensure-AzLogin
$subscriptionId = $account.id
$tenantId = $account.tenantId

$baseAlphaNum = Get-SafeBaseName -Value $NamePrefix
$baseHyphen = Get-SafeBaseName -Value $NamePrefix -KeepHyphen
$suffix = Get-HashSuffix -Text "$subscriptionId|$ResourceGroupName|$baseHyphen"

if ($AksClusterName -eq "") {
    $AksClusterName = Get-TrimmedName -Base "aks-$baseHyphen" -MaxLength 63
}

$managedIdentityName = Get-TrimmedName -Base "id-$baseHyphen" -MaxLength 128
$serviceBusNamespaceName = Get-TrimmedName -Base "sb-$baseHyphen-$suffix" -MaxLength 50
$eventHubNamespaceName = Get-TrimmedName -Base "eh-$baseHyphen-$suffix" -MaxLength 50
$sqlServerName = Get-TrimmedName -Base "sql$baseAlphaNum$suffix" -MaxLength 63
$keyVaultName = Get-TrimmedName -Base "kv-$baseHyphen-$suffix" -MaxLength 24

if ($AcsResourceName -eq "") {
    $AcsResourceName = Get-TrimmedName -Base "acs$baseAlphaNum$suffix" -MaxLength 63
}

if ($AcsDataLocation -eq "") {
    $AcsDataLocation = Get-AcsDataLocationForRegion -Region $Location
}

if ($SqlAdminPassword -eq "") {
    $SqlAdminPassword = New-Password
}

if ($FhirIntegrationBaseUrl -eq "") {
    $FhirIntegrationBaseUrl = "https://$baseHyphen-fhir.azurehealthcareapis.com"
    Write-Warning "FHIR integration base URL was not provided. Using a convention-based placeholder URL: $FhirIntegrationBaseUrl"
}

if ($ServiceBusIntegrationBaseUrl -eq "") {
    $ServiceBusIntegrationBaseUrl = "https://svc-event-messaging.$Namespace.svc.cluster.local"
}

$oauthAudienceValue = Ensure-EntraAudience -BaseName $baseHyphen -Suffix $suffix -TenantId $tenantId
$entraApiAppId = ""
if ($oauthAudienceValue -match '^api://(?<appId>.+)$') {
    $entraApiAppId = $Matches.appId
}

if ($entraApiAppId -eq "") {
    Write-Warning "Unable to derive ENTRA_API_APP_ID from the OAuth2 audience. Set it manually if approved-patient automation is required."
}

Ensure-ResourceGroup -Name $ResourceGroupName -Region $Location
$managedIdentity = Ensure-UserAssignedIdentity -Name $managedIdentityName
$keyVault = Ensure-KeyVault -Name $keyVaultName
$null = Ensure-ServiceBusNamespace -Name $serviceBusNamespaceName
$null = Ensure-EventHubNamespace -Name $eventHubNamespaceName
$null = Ensure-EventHub -NamespaceName $eventHubNamespaceName -Name $EventHubName
$sqlServer = Ensure-SqlServer -Name $sqlServerName -AdminUser $SqlAdminUsername -AdminPassword $SqlAdminPassword
$null = Ensure-SqlDatabase -ServerName $sqlServerName -DatabaseName $SqlDatabaseName

if ($AcsIdentityConnectionString -eq "" -and -not $SkipAcsInfrastructure) {
    try {
        $null = Ensure-CommunicationService -Name $AcsResourceName -DataLocation $AcsDataLocation
        $AcsIdentityConnectionString = Resolve-AcsIdentityConnectionString -ResourceName $AcsResourceName
        if ($AcsIdentityConnectionString -eq "") {
            Write-Warning "ACS resource exists but connection string lookup failed. Set ACS_IDENTITY_CONNECTION_STRING manually in dev.env."
        }
    }
    catch {
        Write-Warning "Unable to ensure ACS resource automatically. Set ACS_IDENTITY_CONNECTION_STRING manually in dev.env. Error: $($_.Exception.Message)"
    }
}

$null = Ensure-AksCluster -Name $AksClusterName

Write-Host "Storing bootstrap secrets in Key Vault $keyVaultName"
try {
    Invoke-AzCli -Arguments @("keyvault", "secret", "set", "--vault-name", $keyVaultName, "--name", "sql-admin-password", "--value", $SqlAdminPassword) | Out-Null
    Invoke-AzCli -Arguments @("keyvault", "secret", "set", "--vault-name", $keyVaultName, "--name", "oauth2-audience", "--value", $oauthAudienceValue) | Out-Null
    if ($AcsIdentityConnectionString -ne "") {
        Invoke-AzCli -Arguments @("keyvault", "secret", "set", "--vault-name", $keyVaultName, "--name", "acs-identity-connection-string", "--value", $AcsIdentityConnectionString) | Out-Null
    }
}
catch {
    Write-Warning "Unable to store secrets in Key Vault with current permissions. Continuing bootstrap."
}

Ensure-AksCredentials -Name $AksClusterName

if (-not $SkipNamespaceSetup) {
    Ensure-Namespace -Name $Namespace
}

$currentContext = kubectl config current-context
if (-not $currentContext) {
    throw "Unable to resolve current kubectl context after AKS credential fetch."
}

$jdbcUrl = "jdbc:sqlserver://$sqlServerName.database.windows.net:1433;database=$SqlDatabaseName;encrypt=true;hostNameInCertificate=*.database.windows.net;loginTimeout=30;"
$oauthIssuer = "https://login.microsoftonline.com/$tenantId/v2.0"
$oauthJwkSetUri = "https://login.microsoftonline.com/$tenantId/discovery/v2.0/keys"

$envContent = @(
    "# Generated by bootstrap-dev-azure.ps1 on $(Get-Date -Format s)",
    "NAMESPACE=$Namespace",
    "KUBE_CONTEXT=$currentContext",
    "",
    "SERVICEBUS_NAMESPACE=$serviceBusNamespaceName.servicebus.windows.net",
    "EVENTHUB_NAMESPACE=$eventHubNamespaceName.servicebus.windows.net",
    "KEY_VAULT_URL=https://$keyVaultName.vault.azure.net/",
    "FHIR_INTEGRATION_BASE_URL=$FhirIntegrationBaseUrl",
    "SERVICE_BUS_INTEGRATION_BASE_URL=$ServiceBusIntegrationBaseUrl",
    "ACS_INTEGRATION_BASE_URL=http://svc-identity-adapter",
    "TELECONSULT_ACS_INTEGRATION_BASE_URL=http://svc-identity-adapter",
    "TELECONSULT_JOIN_BASE_URL=$TeleconsultJoinBaseUrl",
    "ACS_IDENTITY_CONNECTION_STRING=$AcsIdentityConnectionString",
    "OTEL_OTLP_ENDPOINT=$OtelOtlpEndpoint",
    "OAUTH2_ISSUER=$oauthIssuer",
    "OAUTH2_AUDIENCE=$oauthAudienceValue",
    "OAUTH2_JWK_SET_URI=$oauthJwkSetUri",
    "ENTRA_API_APP_ID=$entraApiAppId",
    "ENTRA_GRAPH_BASE_URL=https://graph.microsoft.com/v1.0",
    "ENTRA_PATIENT_GROUP_NAME=HCPE-PATIENT",
    "ENTRA_PATIENT_ROLE_VALUE=PATIENT",
    "ENTRA_INVITE_REDIRECT_URL=https://myapplications.microsoft.com",
    "AZURE_SQL_JDBC_URL=$jdbcUrl",
    "AZURE_SQL_USERNAME=$SqlAdminUsername",
    "AZURE_SQL_PASSWORD=$SqlAdminPassword",
    "AZURE_MANAGED_IDENTITY_CLIENT_ID=$($managedIdentity.clientId)"
)

Set-Content -Path $OutputEnvFile -Value $envContent
Write-Host "Wrote dev environment file: $OutputEnvFile"

if (-not (Test-Path $renderScript)) {
    throw "Missing render helper: $renderScript"
}

& $renderScript -EnvFile $OutputEnvFile -OutputFile $RenderedSecretFile -Namespace $Namespace
if ($LASTEXITCODE -ne 0) {
    throw "Failed to render Kubernetes secret manifest from generated env file."
}

Write-Host "Wrote rendered Kubernetes secret manifest: $RenderedSecretFile"
Write-Host "Bootstrap complete. Next recommended step: .\deploy\k8s\env\dev\apply-dev.ps1 -Namespace $Namespace -EnvFile $OutputEnvFile -Preflight"