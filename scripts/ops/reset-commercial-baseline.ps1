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
  Run-SqlFile "backend/deploy/mysql/init/105-seed-v1.1-sprint2-wms-link.sql"
  Run-SqlFile "backend/deploy/mysql/init/106-seed-v1.1-sprint3-finance-stocktake.sql"
  if (Test-Path "backend/deploy/mysql/init/109-schema-v1.2-sprint2-streaming-context.sql") {
    Run-SqlFile "backend/deploy/mysql/init/109-schema-v1.2-sprint2-streaming-context.sql"
  }
  docker compose exec -T mysql mysql --default-character-set=utf8mb4 -uroot -proot -e "drop table if exists ai_tool_call_logs;" novadepot
  if (Test-Path "backend/deploy/mysql/init/110-schema-v1.2-sprint3-function-calling.sql") {
    Run-SqlFile "backend/deploy/mysql/init/110-schema-v1.2-sprint3-function-calling.sql"
  }
  if (Test-Path "backend/deploy/mysql/init/111-schema-v1.2-sprint4-agent-notification-report.sql") {
    Run-SqlFile "backend/deploy/mysql/init/111-schema-v1.2-sprint4-agent-notification-report.sql"
  }
  if (Test-Path "backend/deploy/mysql/init/112-seed-v1.4-demo-data-realism.sql") {
    Run-SqlFile "backend/deploy/mysql/init/112-seed-v1.4-demo-data-realism.sql"
  }
  Write-Host "[reset-commercial] done. Next: ./scripts/ops/release-checklist.ps1"
} catch {
  Write-Error "[reset-commercial] failed: $($_.Exception.Message)"
  exit 1
}
