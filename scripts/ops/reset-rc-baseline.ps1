$ErrorActionPreference = "Stop"

function Assert-DockerReady {
  try {
    docker info | Out-Null
  } catch {
    throw "Docker daemon is not available. Start Docker Desktop and retry."
  }
}

function Run-SqlFile([string]$Path) {
  if (-not (Test-Path $Path)) {
    throw "SQL file does not exist: $Path"
  }
  Write-Host "[reset-rc] applying $Path"
  Get-Content -Raw -Encoding UTF8 $Path | docker compose exec -T mysql mysql -uroot -proot novadepot
}

try {
  Assert-DockerReady
  Write-Host "[reset-rc] resetting to RC baseline..."
  Run-SqlFile "backend/deploy/mysql/init/97-reset-demo-data.sql"
  Run-SqlFile "backend/deploy/mysql/init/99-seed-mvp.sql"
  Write-Host "[reset-rc] done"
} catch {
  Write-Error "[reset-rc] failed: $($_.Exception.Message)"
  exit 1
}
