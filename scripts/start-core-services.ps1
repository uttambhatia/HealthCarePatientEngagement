param(
    [switch]$SkipTests = $true,
    [switch]$StopOnly
)

$ErrorActionPreference = "Stop"

function Stop-PortListener {
    param([int]$Port)

    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $conn) {
        Write-Host "No listener on port $Port"
        return
    }

    $pids = @($conn | Select-Object -ExpandProperty OwningProcess -Unique)
    foreach ($ownerPid in $pids) {
        try {
            $proc = Get-Process -Id $ownerPid -ErrorAction Stop
            Write-Host "Stopping PID $ownerPid ($($proc.ProcessName)) on port $Port"
            Stop-Process -Id $ownerPid -Force -ErrorAction Stop
        } catch {
            Write-Warning "Failed to stop PID $ownerPid on port ${Port}: $($_.Exception.Message)"
        }
    }
}

$repoRoot = Split-Path -Parent $PSScriptRoot

# Prevent stale processes from serving outdated routes.
Stop-PortListener -Port 8080
Stop-PortListener -Port 8083
Stop-PortListener -Port 8087

if ($StopOnly) {
    Write-Host "Stopped listeners only. Exiting because -StopOnly was provided."
    exit 0
}

$runSuffix = if ($SkipTests) { " -DskipTests" } else { "" }

$services = @(
    @{ Name = "svc-careplan";  Pom = "services/svc-careplan/pom.xml"; Port = 8083 },
    @{ Name = "svc-telemetry"; Pom = "services/svc-telemetry/pom.xml"; Port = 8087 },
    @{ Name = "api-gateway";   Pom = "services/api-gateway/pom.xml"; Port = 8080 }
)

foreach ($service in $services) {
    $command = "Set-Location '$repoRoot'; mvn -f '$($service.Pom)' spring-boot:run$runSuffix"
    Start-Process -FilePath "powershell.exe" -ArgumentList @("-NoExit", "-Command", $command) | Out-Null
    Write-Host "Started $($service.Name) on port $($service.Port)"
}

Write-Host "Core services launch initiated."
Write-Host "- Gateway:   http://localhost:8080"
Write-Host "- CarePlan:  http://localhost:8083"
Write-Host "- Telemetry: http://localhost:8087"
