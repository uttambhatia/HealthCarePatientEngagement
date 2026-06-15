Param(
    [string]$Owner = "uttambhatia",
    [string]$Repository = "healthcarepatientengagement",
    [string]$Tag = "latest",
    [string]$Username = "",
    [string]$Token = "",
    [switch]$SkipMavenBuild,
    [switch]$SkipDockerLogin,
    [switch]$Push
)

$ErrorActionPreference = "Stop"

function Require-Command {
    Param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' is not available in PATH."
    }
}

function Invoke-Step {
    Param(
        [string]$Description,
        [scriptblock]$Action
    )

    Write-Host ""
    Write-Host "==> $Description"
    & $Action
}

function Invoke-CheckedCommand {
    Param(
        [string]$Command,
        [string[]]$Arguments,
        [string]$FailureMessage
    )

    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$FailureMessage (exit code: $LASTEXITCODE)"
    }
}

Require-Command -Name "docker"
if (-not $SkipMavenBuild) {
    Require-Command -Name "mvn"
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $repoRoot
try {
    $modules = @(
        @{ Name = "api-gateway"; Path = "services/api-gateway" },
        @{ Name = "svc-patient"; Path = "services/svc-patient" },
        @{ Name = "svc-appointment"; Path = "services/svc-appointment" },
        @{ Name = "svc-careplan"; Path = "services/svc-careplan" },
        @{ Name = "svc-consent"; Path = "services/svc-consent" },
        @{ Name = "svc-medical-record"; Path = "services/svc-medical-record" },
        @{ Name = "svc-notification"; Path = "services/svc-notification" },
        @{ Name = "svc-telemetry"; Path = "services/svc-telemetry" },
        @{ Name = "svc-device-ingestion"; Path = "services/svc-device-ingestion" },
        @{ Name = "svc-alert-management"; Path = "services/svc-alert-management" },
        @{ Name = "svc-identity-adapter"; Path = "services/svc-identity-adapter" },
        @{ Name = "svc-event-messaging"; Path = "services/svc-event-messaging" }
    )

    foreach ($m in $modules) {
        $dockerFile = Join-Path $m.Path "Dockerfile"
        $pomFile = Join-Path $m.Path "pom.xml"
        if (-not (Test-Path $dockerFile)) {
            throw "Missing Dockerfile: $dockerFile"
        }

        if (-not $SkipMavenBuild -and -not (Test-Path $pomFile)) {
            throw "Missing Maven module pom.xml: $pomFile"
        }
    }

    if (-not $SkipDockerLogin) {
        if ([string]::IsNullOrWhiteSpace($Username)) {
            $Username = Read-Host "Enter GHCR username"
        }

        if ([string]::IsNullOrWhiteSpace($Token)) {
            $secureToken = Read-Host "Enter GHCR PAT (write:packages, read:packages)" -AsSecureString
            $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureToken)
            try {
                $Token = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
            }
            finally {
                [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
            }
        }

        if ([string]::IsNullOrWhiteSpace($Username) -or [string]::IsNullOrWhiteSpace($Token)) {
            throw "GHCR credentials are required unless -SkipDockerLogin is used."
        }

        Invoke-Step -Description "Logging in to GHCR" -Action {
            $Token | docker login ghcr.io -u $Username --password-stdin | Out-Null
            if ($LASTEXITCODE -ne 0) {
                throw "Docker login to ghcr.io failed for user '$Username'."
            }
            Write-Host "Logged in to ghcr.io as $Username"
        }
    }

    Invoke-Step -Description "Checking Docker daemon connectivity" -Action {
        Invoke-CheckedCommand -Command "docker" -Arguments @("version") -FailureMessage "Docker daemon is not reachable. Start Docker Desktop and retry"
    }

    if (-not $SkipMavenBuild) {
        Invoke-Step -Description "Installing parent and shared module" -Action {
            Invoke-CheckedCommand -Command "mvn" -Arguments @("-N", "install") -FailureMessage "Parent pom install failed"
            Invoke-CheckedCommand -Command "mvn" -Arguments @("-pl", "platform-common", "install", "-DskipTests") -FailureMessage "platform-common install failed"
        }
    }

    foreach ($m in $modules) {
        $image = "ghcr.io/$Owner/$Repository/$($m.Name):$Tag"

        if (-not $SkipMavenBuild) {
            Invoke-Step -Description "Packaging JAR for $($m.Name)" -Action {
                Invoke-CheckedCommand -Command "mvn" -Arguments @("-f", "$($m.Path)/pom.xml", "clean", "package", "-DskipTests") -FailureMessage "Maven package failed for $($m.Name)"
            }
        }

        Invoke-Step -Description "Building image $image" -Action {
            Invoke-CheckedCommand -Command "docker" -Arguments @("build", "-t", $image, $m.Path) -FailureMessage "Docker build failed for $image"
        }

        if ($Push) {
            Invoke-Step -Description "Pushing image $image" -Action {
                Invoke-CheckedCommand -Command "docker" -Arguments @("push", $image) -FailureMessage "Docker push failed for $image"
            }
        } else {
            Write-Host "Skipping push for $image (use -Push to upload)"
        }
    }

    Write-Host ""
    Write-Host "Completed image build workflow."
    if (-not $Push) {
        Write-Host "Re-run with -Push to upload images to GHCR."
    }
}
finally {
    Pop-Location
}
