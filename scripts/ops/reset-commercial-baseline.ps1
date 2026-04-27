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
  Write-Host "[reset-commercial] applying $Path"
  $fileName = Split-Path $Path -Leaf
  $containerPath = "/docker-entrypoint-initdb.d/$fileName"
  docker compose exec -T mysql sh -lc "mysql --default-character-set=utf8mb4 -uroot -proot novadepot < $containerPath"
}

try {
  Assert-DockerReady
  Write-Host "[reset-commercial] resetting to commercial baseline (Phase 4)..."
  Run-SqlFile "backend/deploy/mysql/init/96-reset-business-sample.sql"
  Run-SqlFile "backend/deploy/mysql/init/03-schema-erp-system-ai-cs.sql"
  Run-SqlFile "backend/deploy/mysql/init/99-seed-mvp.sql"
  Run-SqlFile "backend/deploy/mysql/init/100-seed-business-sample.sql"
  Run-SqlFile "backend/deploy/mysql/init/101-seed-stress-lite.sql"
  Run-SqlFile "backend/deploy/mysql/init/102-data-repair-zh-semantic.sql"
  Run-SqlFile "backend/deploy/mysql/init/104-seed-knowledge-sprint3.sql"
  Write-Host "[reset-commercial] done. Next: ./scripts/ops/release-checklist.ps1"
} catch {
  Write-Error "[reset-commercial] failed: $($_.Exception.Message)"
  exit 1
}
