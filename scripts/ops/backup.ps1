param(
  [string]$OutputDir = "./backups"
)

$ErrorActionPreference = "Stop"

function Assert-DockerReady {
  try {
    docker info | Out-Null
  } catch {
    throw "Docker daemon is not available. Start Docker Desktop and retry."
  }
}

function Assert-MySQLService {
  docker compose ps mysql | Out-Null
}

try {
  Assert-DockerReady
  Assert-MySQLService

  New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
  $ts = Get-Date -Format "yyyyMMdd-HHmmss"
  $backupFile = Join-Path $OutputDir ("novadepot-backup-" + $ts + ".sql")

  Write-Host "[backup] writing to $backupFile"
  docker compose exec -T mysql mysqldump -uroot -proot --single-transaction --set-gtid-purged=OFF novadepot > $backupFile

  if (-not (Test-Path $backupFile)) {
    throw "Backup file was not generated: $backupFile"
  }
  if ((Get-Item $backupFile).Length -le 0) {
    throw "Backup file is empty: $backupFile"
  }

  Write-Host "[backup] done: $backupFile"
} catch {
  Write-Error "[backup] failed: $($_.Exception.Message)"
  exit 1
}
