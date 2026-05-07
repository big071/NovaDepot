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

function Invoke-RetentionPolicy {
  param([string]$Dir)
  $files = Get-ChildItem -Path $Dir -Filter "novadepot-*.sql.gz" -File | Sort-Object LastWriteTime -Descending
  $dailyKeep = $files | Where-Object { $_.LastWriteTime -ge (Get-Date).AddDays(-7) }
  $weeklyKeep = $files |
    Group-Object { [System.Globalization.ISOWeek]::GetWeekOfYear($_.LastWriteTime) } |
    ForEach-Object { $_.Group | Sort-Object LastWriteTime -Descending | Select-Object -First 1 } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 4
  $keep = @($dailyKeep + $weeklyKeep | Select-Object -Unique)
  foreach ($file in $files) {
    if ($keep.FullName -notcontains $file.FullName) {
      Remove-Item -LiteralPath $file.FullName -Force
      Write-Host "[backup] removed old backup $($file.Name)"
    }
  }
}

try {
  Assert-DockerReady
  New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
  $ts = Get-Date -Format "yyyyMMdd-HHmmss"
  $backupFile = Join-Path $OutputDir ("novadepot-" + $ts + ".sql.gz")
  $sqlFile = Join-Path $OutputDir ("novadepot-" + $ts + ".sql")

  Write-Host "[backup] writing compressed dump to $backupFile"
  docker compose exec -T mysql mysqldump -uroot -proot --single-transaction --set-gtid-purged=OFF novadepot | Set-Content -LiteralPath $sqlFile -Encoding UTF8

  $source = [System.IO.File]::OpenRead((Resolve-Path -LiteralPath $sqlFile))
  try {
    $target = [System.IO.File]::Create((Join-Path (Resolve-Path -LiteralPath $OutputDir) (Split-Path -Leaf $backupFile)))
    try {
      $gzip = [System.IO.Compression.GZipStream]::new($target, [System.IO.Compression.CompressionLevel]::Optimal)
      try {
        $source.CopyTo($gzip)
      } finally {
        $gzip.Dispose()
      }
    } finally {
      $target.Dispose()
    }
  } finally {
    $source.Dispose()
    Remove-Item -LiteralPath $sqlFile -Force -ErrorAction SilentlyContinue
  }

  if (-not (Test-Path $backupFile)) {
    throw "Backup file was not generated: $backupFile"
  }
  if ((Get-Item $backupFile).Length -le 0) {
    throw "Backup file is empty: $backupFile"
  }

  Invoke-RetentionPolicy -Dir $OutputDir
  Write-Host "[backup] done: $backupFile"
} catch {
  Write-Error "[backup] failed: $($_.Exception.Message)"
  exit 1
}
