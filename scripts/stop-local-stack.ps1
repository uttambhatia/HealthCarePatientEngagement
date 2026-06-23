param(
    [switch]$IncludeBuildTools
)

$ErrorActionPreference = "Stop"

function Stop-PortListeners {
    param([int[]]$Ports)

    foreach ($port in $Ports) {
        $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if (-not $listeners) {
            Write-Host "No listener on port $port"
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

function Stop-RepoBuildTools {
    param([string]$RepoRoot)

    $repoRootLower = $RepoRoot.ToLowerInvariant()
    $targets = Get-CimInstance Win32_Process -Filter "name = 'java.exe' OR name = 'node.exe'" -ErrorAction SilentlyContinue

    foreach ($proc in $targets) {
        $cmd = [string]$proc.CommandLine
        if ([string]::IsNullOrWhiteSpace($cmd)) {
            continue
        }

        $cmdLower = $cmd.ToLowerInvariant()
        if ($cmdLower.Contains($repoRootLower) -or $cmdLower.Contains("healthcarepatientengagement")) {
            try {
                Write-Host "Stopping extra process PID $($proc.ProcessId) ($($proc.Name))"
                Stop-Process -Id $proc.ProcessId -Force -ErrorAction Stop
            }
            catch {
                Write-Warning "Unable to stop PID $($proc.ProcessId): $($_.Exception.Message)"
            }
        }
    }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$ports = @(5173) + (8080..8091)

Stop-PortListeners -Ports $ports

if ($IncludeBuildTools) {
    Stop-RepoBuildTools -RepoRoot $repoRoot
}

Write-Host ""
Write-Host "Local stack stop completed."
Write-Host "Ports handled: 5173, 8080-8091"
if ($IncludeBuildTools) {
    Write-Host "Extra java/node processes tied to this repo were also stopped."
}
