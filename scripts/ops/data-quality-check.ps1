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

function Get-LowStockCountFromFacts {
  return [int](Query-Scalar @"
select count(*)
from inventory i
left join products p
  on p.id = i.product_id
 and p.tenant_id = i.tenant_id
 and p.deleted = 0
where i.tenant_id = 1
  and i.deleted = 0
  and i.available_qty is not null
  and i.available_qty <= coalesce(
    cast(
      nullif(
        regexp_replace(
          regexp_substr(coalesce(p.spec, ''), '安全库存[[:space:]]*[=:：][[:space:]]*[0-9]+(\\.[0-9]+)?'),
          '[^0-9.]',
          ''
        ),
        ''
      ) as decimal(18, 6)
    ),
    10
  );
"@)
}

try {
  Assert-DockerReady

  Write-Host "[data-quality] 1/5 auth and api readiness"
$loginBody = @{ tenantCode = "default"; username = "admin"; password = "admin123" } | ConvertTo-Json
  $loginResp = Invoke-RestMethod -Method Post -Uri "http://localhost:18080/api/v1/auth/login" -ContentType "application/json" -Body $loginBody
  Assert-SuccessCode $loginResp "login"
  $token = $loginResp.data.accessToken
  $headers = @{ Authorization = "Bearer $token" }

  $dashboard = Invoke-RestMethod -Method Get -Uri "http://localhost:18080/api/v1/reports/dashboard" -Headers $headers
  Assert-SuccessCode $dashboard "dashboard"
  $alerts = Invoke-RestMethod -Method Get -Uri "http://localhost:18080/api/v1/inventory/alerts/low-stock" -Headers $headers
  Assert-SuccessCode $alerts "low-stock"

  Write-Host "[data-quality] 2/5 garbled text check"
  $garbledCount = [int](Query-Scalar @"
select
  (select count(*) from products where tenant_id=1 and (product_name like '%锟?' or product_name like '%閿?' or product_name like '%茂禄驴%')) +
  (select count(*) from warehouses where tenant_id=1 and (warehouse_name like '%锟?' or warehouse_name like '%閿?' or warehouse_name like '%茂禄驴%')) +
  (select count(*) from warehouse_locations where tenant_id=1 and (location_name like '%锟?' or location_name like '%閿?' or location_name like '%茂禄驴%')) +
  (select count(*) from customer_service_messages where tenant_id=1 and (content like '%锟?' or content like '%閿?' or content like '%茂禄驴%')) +
  (select count(*) from faq_knowledge where tenant_id=1 and ((question like '%锟?' or question like '%閿?' or question like '%茂禄驴%') or (answer like '%锟?' or answer like '%閿?' or answer like '%茂禄驴%'))) +
  (select count(*) from sop_knowledge where tenant_id=1 and ((title like '%锟?' or title like '%閿?' or title like '%茂禄驴%') or (steps like '%锟?' or steps like '%閿?' or steps like '%茂禄驴%'))) +
  (select count(*) from rule_configs where tenant_id=1 and ((config_name like '%锟?' or config_name like '%閿?' or config_name like '%茂禄驴%') or (config_value like '%锟?' or config_value like '%閿?' or config_value like '%茂禄驴%'))) +
  (select count(*) from ai_messages where tenant_id=1 and (content like '%锟?' or content like '%閿?' or content like '%茂禄驴%'));
"@)
  if ($garbledCount -gt 0) { throw "garbled text rows found: $garbledCount" }
  Write-Host "garbled text check passed"

  Write-Host "[data-quality] 3/5 key field empty check"
  $emptyKeyCount = [int](Query-Scalar @"
select
  (select count(*) from products where tenant_id=1 and (product_code is null or product_code='' or product_name is null or product_name='')) +
  (select count(*) from warehouses where tenant_id=1 and (warehouse_code is null or warehouse_code='' or warehouse_name is null or warehouse_name='')) +
  (select count(*) from warehouse_locations where tenant_id=1 and (location_code is null or location_code='' or location_name is null or location_name='')) +
  (select count(*) from inventory where tenant_id=1 and (warehouse_id is null or location_id is null or product_id is null or available_qty is null)) +
  (select count(*) from customer_service_tickets where tenant_id=1 and (ticket_no is null or ticket_no='' or status is null or priority is null)) +
  (select count(*) from faq_knowledge where tenant_id=1 and (question is null or question='' or answer is null or answer='' or review_status not in ('DRAFT','APPROVED'))) +
  (select count(*) from sop_knowledge where tenant_id=1 and (title is null or title='' or steps is null or steps='' or review_status not in ('DRAFT','APPROVED'))) +
  (select count(*) from rule_configs where tenant_id=1 and (config_key is null or config_key='' or config_value is null or config_value=''));
"@)
  if ($emptyKeyCount -gt 0) { throw "key empty rows found: $emptyKeyCount" }
  Write-Host "key field check passed"

  Write-Host "[data-quality] 4/5 dashboard and detail consistency"
  $sqlTodayInbound = [int](Query-Scalar "select count(*) from inbound_orders where tenant_id=1 and deleted=0 and created_at>=curdate() and created_at<date_add(curdate(), interval 1 day);")
  $sqlTodayOutbound = [int](Query-Scalar "select count(*) from outbound_orders where tenant_id=1 and deleted=0 and created_at>=curdate() and created_at<date_add(curdate(), interval 1 day);")
  $sqlLowStock = [int](Get-LowStockCountFromFacts)
  if ([int]$dashboard.data.todayInbound -ne $sqlTodayInbound) { throw "todayInbound mismatch: api=$($dashboard.data.todayInbound) sql=$sqlTodayInbound" }
  if ([int]$dashboard.data.todayOutbound -ne $sqlTodayOutbound) { throw "todayOutbound mismatch: api=$($dashboard.data.todayOutbound) sql=$sqlTodayOutbound" }
  if ([int]$dashboard.data.lowStockCount -ne $sqlLowStock) { throw "lowStockCount mismatch: api=$($dashboard.data.lowStockCount) sql=$sqlLowStock" }
  if ($alerts.data.Count -ne $sqlLowStock) { throw "alerts mismatch: api=$($alerts.data.Count) sql=$sqlLowStock" }
  Write-Host "dashboard consistency passed"

  Write-Host "[data-quality] 5/5 ticket consistency"
  $ticketInconsistent = [int](Query-Scalar "select count(*) from customer_service_tickets t left join customer_service_sessions s on s.id=t.session_id and s.tenant_id=t.tenant_id and s.deleted=0 where t.tenant_id=1 and t.deleted=0 and (s.id is null or t.status not in ('OPEN','PROCESSING','RESOLVED','CLOSED'));")
  if ($ticketInconsistent -gt 0) { throw "ticket inconsistent rows found: $ticketInconsistent" }
  Write-Host "ticket consistency passed"

  Write-Host "[data-quality] sprint2 ERP/WMS link consistency"
  $sprint2Inconsistent = [int](Query-Scalar @"
select
  (select count(*)
     from inbound_orders io
     left join purchase_orders po
       on po.id = io.source_order_id
      and po.tenant_id = io.tenant_id
      and po.deleted = 0
    where io.tenant_id = 1
      and io.deleted = 0
      and io.source_type = 'PURCHASE_ORDER'
      and po.id is null) +
  (select count(*)
     from outbound_orders oo
     left join sales_orders so
       on so.id = oo.source_order_id
      and so.tenant_id = oo.tenant_id
      and so.deleted = 0
    where oo.tenant_id = 1
      and oo.deleted = 0
      and oo.source_type = 'SALES_ORDER'
      and so.id is null) +
  (select count(*)
     from inbound_order_items ii
     join inbound_orders io
       on io.id = ii.inbound_order_id
      and io.tenant_id = ii.tenant_id
      and io.deleted = 0
     left join purchase_order_items pi
       on pi.id = ii.source_order_item_id
      and pi.tenant_id = ii.tenant_id
      and pi.deleted = 0
    where ii.tenant_id = 1
      and ii.deleted = 0
      and io.source_type = 'PURCHASE_ORDER'
      and pi.id is null) +
  (select count(*)
     from outbound_order_items oi
     join outbound_orders oo
       on oo.id = oi.outbound_order_id
      and oo.tenant_id = oi.tenant_id
      and oo.deleted = 0
     left join sales_order_items si
       on si.id = oi.source_order_item_id
      and si.tenant_id = oi.tenant_id
      and si.deleted = 0
    where oi.tenant_id = 1
      and oi.deleted = 0
      and oo.source_type = 'SALES_ORDER'
      and si.id is null) +
  (select count(*) from purchase_order_items where tenant_id = 1 and deleted = 0 and received_qty > order_qty) +
  (select count(*) from sales_order_items where tenant_id = 1 and deleted = 0 and shipped_qty > order_qty) +
  (select count(*)
     from purchase_orders po
    where po.tenant_id = 1
      and po.deleted = 0
      and po.status = 'FULLY_RECEIVED'
      and exists (
        select 1 from purchase_order_items pi
         where pi.tenant_id = po.tenant_id
           and pi.purchase_order_id = po.id
           and pi.deleted = 0
           and pi.received_qty < pi.order_qty
      )) +
  (select count(*)
     from sales_orders so
    where so.tenant_id = 1
      and so.deleted = 0
      and so.status = 'FULLY_SHIPPED'
      and exists (
        select 1 from sales_order_items si
         where si.tenant_id = so.tenant_id
           and si.sales_order_id = so.id
           and si.deleted = 0
           and si.shipped_qty < si.order_qty
      ));
"@)
  if ($sprint2Inconsistent -gt 0) { throw "sprint2 ERP/WMS inconsistent rows found: $sprint2Inconsistent" }
  Write-Host "sprint2 ERP/WMS link consistency passed"

  Write-Host "[data-quality] passed"
} catch {
  Write-Error "[data-quality] failed: $($_.Exception.Message)"
  exit 1
}

