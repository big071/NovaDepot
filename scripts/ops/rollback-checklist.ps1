param(
  [string]$BackupFile = ""
)

$ErrorActionPreference = "Stop"

function Assert-DockerReady {
  try {
    docker info | Out-Null
  } catch {
    throw "Docker daemon is not available. Start Docker Desktop and retry."
  }
}

try {
  Assert-DockerReady

  Write-Host "[rollback-checklist] 1/4 backup validation"
  if (-not [string]::IsNullOrWhiteSpace($BackupFile)) {
    if (-not (Test-Path $BackupFile)) {
      throw "Backup file does not exist: $BackupFile"
    }
    Write-Host "backup exists: $BackupFile"
  } else {
    Write-Warning "No -BackupFile provided; skip backup file check"
  }

  Write-Host "[rollback-checklist] 2/4 script presence"
  $required = @(
    "scripts/ops/restore.ps1",
    "scripts/ops/reset-rc-baseline.ps1",
    "scripts/ops/reset-commercial-baseline.ps1"
  )
  foreach ($item in $required) {
    if (-not (Test-Path $item)) {
      throw "Required script missing: $item"
    }
  }

  Write-Host "[rollback-checklist] 3/4 service status"
  docker compose ps

  Write-Host "[rollback-checklist] 4/4 health probe"
  $health = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:18080/actuator/health" -TimeoutSec 10
  if ($health.StatusCode -ne 200) {
    throw "Health endpoint status=$($health.StatusCode)"
  }

  Write-Host "[rollback-checklist] passed"
} catch {
  Write-Error "[rollback-checklist] failed: $($_.Exception.Message)"
  exit 1
}
