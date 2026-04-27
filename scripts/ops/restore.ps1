param(
  [Parameter(Mandatory = $true)]
  [string]$BackupFile
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
  if (-not (Test-Path $BackupFile)) {
    throw "Backup file does not exist: $BackupFile"
  }

  Write-Host "[restore] restoring from $BackupFile"
  Get-Content -Raw -Encoding UTF8 $BackupFile | docker compose exec -T mysql mysql -uroot -proot novadepot
  Write-Host "[restore] done"
} catch {
  Write-Error "[restore] failed: $($_.Exception.Message)"
  exit 1
}
