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

Stop-PortListener -Port 8080
Stop-PortListener -Port 8083
Stop-PortListener -Port 8087

Write-Host "Core services stopped (if they were running)."
