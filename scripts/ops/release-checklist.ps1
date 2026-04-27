$ErrorActionPreference = "Stop"

function Assert-DockerReady {
  try {
    docker info | Out-Null
  } catch {
    throw "Docker daemon is not available. Start Docker Desktop and retry."
  }
}

function Assert-SuccessCode($Response, [string]$Name) {
  if ($null -eq $Response -or $Response.code -ne "0") {
    throw "$Name returned non-zero code"
  }
}

function Query-Scalar([string]$Sql) {
  $result = docker compose exec -T mysql mysql --default-character-set=utf8mb4 -uroot -proot -Nse "$Sql" novadepot
  return ($result | Out-String).Trim()
}

function Parse-SafetyStock([string]$Spec, [decimal]$DefaultThreshold = 10) {
  if ([string]::IsNullOrWhiteSpace($Spec)) {
    return $DefaultThreshold
  }
  $match = [regex]::Match($Spec, "安全库存\s*\D*?(\d+(?:\.\d+)?)")
  if ($match.Success) {
    return [decimal]::Parse($match.Groups[1].Value)
  }
  return $DefaultThreshold
}

function Get-LowStockCountFromFacts {
  $rows = docker compose exec -T mysql mysql --default-character-set=utf8mb4 -uroot -proot -Nse "select cast(i.available_qty as char), coalesce(p.spec,'') from inventory i left join products p on p.id=i.product_id and p.tenant_id=i.tenant_id and p.deleted=0 where i.tenant_id=1 and i.deleted=0 and i.available_qty is not null;" novadepot
  $count = 0
  foreach ($line in $rows) {
    $parts = $line -split "`t", 2
    if ($parts.Length -lt 1) { continue }
    $available = 0
    [void][decimal]::TryParse($parts[0], [ref]$available)
    $spec = if ($parts.Length -gt 1) { $parts[1] } else { "" }
    $threshold = Parse-SafetyStock $spec 10
    if ($available -le $threshold) {
      $count++
    }
  }
  return $count
}

try {
  Assert-DockerReady

  Write-Host "[release-checklist] 1/4 service status"
  docker compose ps

  Write-Host "[release-checklist] 2/4 auth smoke"
  $loginBody = @{ tenantCode = "default"; username = "admin"; password = "123456" } | ConvertTo-Json
  $loginResp = Invoke-RestMethod -Method Post -Uri "http://localhost:18080/api/v1/auth/login" -ContentType "application/json" -Body $loginBody
  Assert-SuccessCode $loginResp "login"
  $token = $loginResp.data.accessToken
  $headers = @{ Authorization = "Bearer $token" }
  if (-not $token) {
    throw "login succeeded but accessToken is empty"
  }

  Write-Host "[release-checklist] 3/4 backend health (authenticated)"
  $dashboardHealth = Invoke-RestMethod -Method Get -Uri "http://localhost:18080/api/v1/reports/dashboard" -Headers $headers
  Assert-SuccessCode $dashboardHealth "dashboard-health"

  Write-Host "[release-checklist] 4/4 scenario consistency"
  $dashboard = Invoke-RestMethod -Method Get -Uri "http://localhost:18080/api/v1/reports/dashboard" -Headers $headers
  Assert-SuccessCode $dashboard "dashboard"
  Write-Host "dashboard:" ($dashboard.data | ConvertTo-Json -Compress)

  $alerts = Invoke-RestMethod -Method Get -Uri "http://localhost:18080/api/v1/inventory/alerts/low-stock" -Headers $headers
  Assert-SuccessCode $alerts "low-stock"
  Write-Host "low-stock count:" ($alerts.data.Count)

  $tickets = Invoke-RestMethod -Method Get -Uri "http://localhost:18080/api/v1/customer-service/tickets?pageNo=1&pageSize=5" -Headers $headers
  Assert-SuccessCode $tickets "tickets"
  Write-Host "ticket total:" ($tickets.data.total)

  $aiBody = @{ scene = "warehouse"; message = "请基于库存事实给出补货建议"; providerHint = "rule" } | ConvertTo-Json
  $aiResp = Invoke-RestMethod -Method Post -Uri "http://localhost:18080/api/v1/ai/chat" -Headers $headers -ContentType "application/json" -Body $aiBody
  Assert-SuccessCode $aiResp "ai"
  Write-Host "ai conversation:" ($aiResp.data.conversationNo)

  Write-Host "[release-checklist] data consistency verify"
  $sqlTodayInbound = [int](Query-Scalar "select count(*) from inbound_orders where tenant_id=1 and created_at>=curdate() and created_at<date_add(curdate(), interval 1 day);")
  $sqlTodayOutbound = [int](Query-Scalar "select count(*) from outbound_orders where tenant_id=1 and created_at>=curdate() and created_at<date_add(curdate(), interval 1 day);")
  $dashboardInbound = [int]$dashboard.data.todayInbound
  $dashboardOutbound = [int]$dashboard.data.todayOutbound
  $dashboardLowStock = [int]$dashboard.data.lowStockCount
  if ($dashboardInbound -ne $sqlTodayInbound) { throw "dashboard.todayInbound mismatch: api=$dashboardInbound sql=$sqlTodayInbound" }
  if ($dashboardOutbound -ne $sqlTodayOutbound) { throw "dashboard.todayOutbound mismatch: api=$dashboardOutbound sql=$sqlTodayOutbound" }
  if ($alerts.data.Count -ne $dashboardLowStock) { throw "low-stock alerts mismatch: dashboard=$dashboardLowStock alerts=$($alerts.data.Count)" }
  Write-Host "consistency matched: inbound/outbound sql + low-stock api口径"

  Write-Host "[release-checklist] passed"
} catch {
  Write-Error "[release-checklist] failed: $($_.Exception.Message)"
  exit 1
}
