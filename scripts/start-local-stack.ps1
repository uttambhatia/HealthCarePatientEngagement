param(
    [switch]$SkipBackendBuild,
    [switch]$SkipFrontendInstall,
    [switch]$SkipStopExisting,
    [string]$PostgresHost = "localhost",
    [int]$PostgresPort = 5432,
    [string]$PostgresDatabase = "postgres",
    [string]$PostgresUsername = "postgres",
    [string]$PostgresPassword = "postgres"
)

$ErrorActionPreference = "Stop"

function Stop-PortListeners {
    param([int[]]$Ports)

    foreach ($port in $Ports) {
        $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if (-not $listeners) {
            continue
        }

        $pids = @($listeners | Select-Object -ExpandProperty OwningProcess -Unique)
        foreach ($pid in $pids) {
            try {
                $proc = Get-Process -Id $pid -ErrorAction Stop
                Write-Host "Stopping PID $pid ($($proc.ProcessName)) on port $port"
                Stop-Process -Id $pid -Force -ErrorAction Stop
            }
            catch {
                Write-Warning "Unable to stop PID $pid on port ${port}: $($_.Exception.Message)"
            }
        }
    }
}

function New-EnvScript {
    param([hashtable]$Environment)

    $parts = @()
    foreach ($key in ($Environment.Keys | Sort-Object)) {
        $value = [string]$Environment[$key]
        $escaped = $value.Replace("'", "''")
        $parts += "`$env:$key='$escaped'"
    }

    return ($parts -join "; ")
}

function Start-Service {
    param(
        [string]$Name,
        [string]$PomPath,
        [int]$Port,
        [hashtable]$Environment,
        [string]$RepoRoot
    )

    $envScript = New-EnvScript -Environment $Environment
    $command = "Set-Location '$RepoRoot'; $envScript; mvn -f '$PomPath' spring-boot:run -DskipTests"

    Start-Process -FilePath "powershell.exe" -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $command) -WindowStyle Minimized | Out-Null
    Write-Host "Started $Name on port $Port"
}

function Start-Frontend {
    param(
        [string]$RepoRoot
    )

    $frontendPath = Join-Path $RepoRoot "frontend"
    $command = "Set-Location '$frontendPath'; `$env:VITE_API_BASE_URL='http://localhost:8080'; `$env:VITE_BYPASS_AUTH='true'; npx vite --host 0.0.0.0 --port 5173"

    Start-Process -FilePath "powershell.exe" -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $command) -WindowStyle Minimized | Out-Null
    Write-Host "Started frontend on port 5173"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (-not $SkipStopExisting) {
    $allPorts = @(5173) + (8080..8091)
    Stop-PortListeners -Ports $allPorts
}

if (-not $SkipBackendBuild) {
    Write-Host "Building backend modules (skip tests)..."
    mvn clean install -DskipTests
}

if (-not $SkipFrontendInstall) {
    Write-Host "Installing frontend dependencies..."
    $frontendPath = Join-Path $repoRoot "frontend"
    Push-Location $frontendPath
    try {
        if (Test-Path "package-lock.json") {
            npm ci
        }
        else {
            npm install
        }
    }
    finally {
        Pop-Location
    }
}

$jdbcUrl = "jdbc:postgresql://${PostgresHost}:${PostgresPort}/${PostgresDatabase}"

$commonEnv = @{
    SPRING_PROFILES_ACTIVE = "local-postgres"
    PLATFORM_SECURITY_ENABLED = "false"
    SERVICEBUS_NAMESPACE = ""
    EVENTHUB_NAMESPACE = ""
    SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE = "2"
    SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE = "0"
}

$services = @(
    @{ Name = "svc-telemetry"; Pom = "services/svc-telemetry/pom.xml"; Port = 8087; DbPrefix = "TELEMETRY"; Extra = @{ TELEMETRY_PROCESSOR_ENABLED = "false"; TIME_SERIES_INTEGRATION_BASE_URL = "" } },
    @{ Name = "svc-notification"; Pom = "services/svc-notification/pom.xml"; Port = 8086; DbPrefix = "NOTIFICATION"; Extra = @{ NOTIFICATION_PROCESSOR_ENABLED = "false"; TELECONSULT_TOPIC_PROCESSOR_ENABLED = "false"; ACS_INTEGRATION_BASE_URL = "" } },
    @{ Name = "svc-identity-adapter"; Pom = "services/svc-identity-adapter/pom.xml"; Port = 8090; DbPrefix = "IDENTITY_ADAPTER"; Extra = @{ IDENTITY_PROCESSOR_ENABLED = "false"; PLATFORM_INTEGRATION_BLOB_CONNECTION_STRING = ""; ACS_IDENTITY_CONNECTION_STRING = ""; ACS_EMAIL_ENDPOINT = ""; ACS_EMAIL_ACCESS_KEY = ""; ACS_EMAIL_FROM_ADDRESS = ""; ACS_SMS_ENDPOINT = ""; ACS_SMS_ACCESS_KEY = ""; ACS_SMS_FROM_NUMBER = ""; AZURE_AD_INTEGRATION_BASE_URL = "" } },
    @{ Name = "svc-consent"; Pom = "services/svc-consent/pom.xml"; Port = 8084; DbPrefix = "CONSENT"; Extra = @{ CONSENT_PROCESSOR_ENABLED = "false"; AUDIT_INTEGRATION_BASE_URL = "" } },
    @{ Name = "svc-medical-record"; Pom = "services/svc-medical-record/pom.xml"; Port = 8085; DbPrefix = "MEDICAL_RECORD"; Extra = @{ MEDICAL_RECORD_PROCESSOR_ENABLED = "false"; FHIR_INTEGRATION_BASE_URL = "" } },
    @{ Name = "svc-patient"; Pom = "services/svc-patient/pom.xml"; Port = 8081; DbPrefix = "PATIENT"; Extra = @{ PATIENT_PROCESSOR_ENABLED = "false"; FHIR_INTEGRATION_BASE_URL = ""; IDENTITY_ADAPTER_INTEGRATION_BASE_URL = "http://localhost:8090"; NOTIFICATION_INTEGRATION_BASE_URL = "http://localhost:8086" } },
    @{ Name = "svc-appointment"; Pom = "services/svc-appointment/pom.xml"; Port = 8082; DbPrefix = "APPOINTMENT"; Extra = @{ APPOINTMENT_PROCESSOR_ENABLED = "false"; CONSENT_ENFORCEMENT_ENABLED = "false"; NOTIFICATION_INTEGRATION_BASE_URL = "http://localhost:8086"; CONSENT_INTEGRATION_BASE_URL = "http://localhost:8084"; MEDICAL_RECORD_INTEGRATION_BASE_URL = "http://localhost:8085"; TELECONSULT_ACS_INTEGRATION_BASE_URL = "http://localhost:8090" } },
    @{ Name = "svc-careplan"; Pom = "services/svc-careplan/pom.xml"; Port = 8083; DbPrefix = "CAREPLAN"; Extra = @{ CAREPLAN_PROCESSOR_ENABLED = "false"; TELECONSULT_TOPIC_PROCESSOR_ENABLED = "false"; FHIR_INTEGRATION_BASE_URL = ""; NOTIFICATION_INTEGRATION_BASE_URL = "http://localhost:8086" } },
    @{ Name = "svc-device-ingestion"; Pom = "services/svc-device-ingestion/pom.xml"; Port = 8088; DbPrefix = "DEVICE_INGESTION"; Extra = @{ DEVICE_INGESTION_PROCESSOR_ENABLED = "false"; IOT_INTEGRATION_BASE_URL = ""; TELEMETRY_INTEGRATION_BASE_URL = "http://localhost:8087" } },
    @{ Name = "svc-event-messaging"; Pom = "services/svc-event-messaging/pom.xml"; Port = 8091; DbPrefix = "EVENT_MESSAGING"; Extra = @{ EVENT_MESSAGING_PROCESSOR_ENABLED = "false"; SERVICE_BUS_INTEGRATION_BASE_URL = ""; MONITORING_INTEGRATION_BASE_URL = "" } },
    @{ Name = "svc-alert-management"; Pom = "services/svc-alert-management/pom.xml"; Port = 8089; DbPrefix = "ALERT_MANAGEMENT"; Extra = @{ ALERT_PROCESSOR_ENABLED = "false"; TELECONSULT_TOPIC_PROCESSOR_ENABLED = "false"; ALERT_ESCALATION_BASE_URL = "" } }
)

foreach ($service in $services) {
    $envMap = @{}

    foreach ($key in $commonEnv.Keys) {
        $envMap[$key] = $commonEnv[$key]
    }

    $prefix = $service.DbPrefix
    $envMap["${prefix}_DB_URL"] = $jdbcUrl
    $envMap["${prefix}_DB_USERNAME"] = $PostgresUsername
    $envMap["${prefix}_DB_PASSWORD"] = $PostgresPassword
    $envMap["${prefix}_DB_DDL_AUTO"] = "update"

    foreach ($key in $service.Extra.Keys) {
        $envMap[$key] = $service.Extra[$key]
    }

    Start-Service -Name $service.Name -PomPath $service.Pom -Port $service.Port -Environment $envMap -RepoRoot $repoRoot
}

$gatewayEnv = @{
    PLATFORM_SECURITY_ENABLED = "false"
    PLATFORM_ROUTES_PATIENTS_URI = "http://localhost:8081"
    PLATFORM_ROUTES_APPOINTMENTS_URI = "http://localhost:8082"
    PLATFORM_ROUTES_CAREPLANS_URI = "http://localhost:8083"
    PLATFORM_ROUTES_CONSENTS_URI = "http://localhost:8084"
    PLATFORM_ROUTES_MEDICAL_RECORDS_URI = "http://localhost:8085"
    PLATFORM_ROUTES_NOTIFICATIONS_URI = "http://localhost:8086"
    PLATFORM_ROUTES_TELEMETRY_URI = "http://localhost:8087"
    PLATFORM_ROUTES_DEVICE_EVENTS_URI = "http://localhost:8088"
    PLATFORM_ROUTES_ALERTS_URI = "http://localhost:8089"
    PLATFORM_ROUTES_IDENTITY_ASSERTIONS_URI = "http://localhost:8090"
    PLATFORM_ROUTES_SERVICEBUS_MESSAGES_URI = "http://localhost:8091"
}

Start-Service -Name "api-gateway" -PomPath "services/api-gateway/pom.xml" -Port 8080 -Environment $gatewayEnv -RepoRoot $repoRoot
Start-Frontend -RepoRoot $repoRoot

Write-Host ""
Write-Host "Local stack launch initiated."
Write-Host "Gateway:  http://localhost:8080"
Write-Host "Frontend: http://localhost:5173"
Write-Host ""
Write-Host "Tip: run scripts/stop-core-services.ps1 for core stop, or rerun this script to recycle ports 5173 and 8080..8091."
