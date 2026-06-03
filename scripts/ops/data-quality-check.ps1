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

  Write-Host "[data-quality] sprint3 finance and stocktake consistency"
  $sprint3Inconsistent = [int](Query-Scalar @"
select
  (select count(*)
     from payables p
     left join purchase_orders po
       on po.id = p.source_order_id
      and po.tenant_id = p.tenant_id
      and po.deleted = 0
    where p.tenant_id = 1
      and p.deleted = 0
      and (p.source_type <> 'PURCHASE_ORDER' or po.id is null)) +
  (select count(*)
     from receivables r
     left join sales_orders so
       on so.id = r.source_order_id
      and so.tenant_id = r.tenant_id
      and so.deleted = 0
    where r.tenant_id = 1
      and r.deleted = 0
      and (r.source_type <> 'SALES_ORDER' or so.id is null)) +
  (select count(*)
     from payables
    where tenant_id = 1
      and deleted = 0
      and (total_amount < 0 or paid_amount < 0 or balance_amount < 0 or paid_amount > total_amount
        or status not in ('UNPAID','PARTIALLY_PAID','PAID','CANCELLED')
        or (status <> 'CANCELLED' and balance_amount <> total_amount - paid_amount)
        or (status <> 'CANCELLED' and paid_amount = 0 and balance_amount > 0 and status <> 'UNPAID')
        or (status <> 'CANCELLED' and paid_amount > 0 and paid_amount < total_amount and status <> 'PARTIALLY_PAID')
        or (status <> 'CANCELLED' and paid_amount = total_amount and status <> 'PAID'))) +
  (select count(*)
     from receivables
    where tenant_id = 1
      and deleted = 0
      and (total_amount < 0 or received_amount < 0 or balance_amount < 0 or received_amount > total_amount
        or status not in ('UNPAID','PARTIALLY_PAID','PAID','CANCELLED')
        or (status <> 'CANCELLED' and balance_amount <> total_amount - received_amount)
        or (status <> 'CANCELLED' and received_amount = 0 and balance_amount > 0 and status <> 'UNPAID')
        or (status <> 'CANCELLED' and received_amount > 0 and received_amount < total_amount and status <> 'PARTIALLY_PAID')
        or (status <> 'CANCELLED' and received_amount = total_amount and status <> 'PAID'))) +
  (select count(*)
     from payments pm
    where pm.tenant_id = 1
      and pm.deleted = 0
      and (pm.amount <= 0
        or pm.direction not in ('PAYABLE','RECEIVABLE')
        or (pm.direction = 'PAYABLE' and not exists (
          select 1 from payables p where p.tenant_id = pm.tenant_id and p.id = pm.ledger_id and p.deleted = 0
        ))
        or (pm.direction = 'RECEIVABLE' and not exists (
          select 1 from receivables r where r.tenant_id = pm.tenant_id and r.id = pm.ledger_id and r.deleted = 0
        )))) +
  (select count(*)
     from stocktake_order_items si
     left join stocktake_orders so
       on so.id = si.stocktake_order_id
      and so.tenant_id = si.tenant_id
      and so.deleted = 0
    where si.tenant_id = 1
      and si.deleted = 0
      and so.id is null) +
  (select count(*)
     from stocktake_orders so
    where so.tenant_id = 1
      and so.deleted = 0
      and so.status = 'COMPLETED'
      and exists (
        select 1
          from stocktake_order_items si
         where si.tenant_id = so.tenant_id
           and si.stocktake_order_id = so.id
           and si.deleted = 0
           and si.diff_qty <> 0
           and not exists (
             select 1
               from inventory_transactions it
              where it.tenant_id = si.tenant_id
                and it.biz_type = 'STOCKTAKE_ADJUST'
                and it.biz_no = so.stocktake_no
                and it.product_id = si.product_id
                and it.location_id = si.location_id
                and it.change_qty = si.diff_qty
                and it.deleted = 0
           )
      )) +
  (select count(*)
     from inventory_transactions
    where tenant_id = 1
      and deleted = 0
      and biz_type = 'STOCKTAKE_ADJUST'
      and before_qty + change_qty <> after_qty) +
  (select count(*)
     from inventory
    where tenant_id = 1
      and deleted = 0
      and available_qty < 0);
"@)
  if ($sprint3Inconsistent -gt 0) { throw "sprint3 finance/stocktake inconsistent rows found: $sprint3Inconsistent" }
  Write-Host "sprint3 finance and stocktake consistency passed"

  Write-Host "[data-quality] sprint4 import, backup and tenant reserve consistency"
  $tenantMissing = [int](Query-Scalar @"
select count(*)
from information_schema.tables t
where t.table_schema = database()
  and t.table_type = 'BASE TABLE'
  and t.table_name in (
    'products','product_categories','product_units','warehouses','warehouse_locations','inventory','inventory_transactions',
    'inbound_orders','inbound_order_items','outbound_orders','outbound_order_items','partners','purchase_orders',
    'purchase_order_items','sales_orders','sales_order_items','payables','receivables','payments','stocktake_orders',
    'stocktake_order_items','import_error_reports','backup_records','audit_logs','customer_service_sessions',
    'customer_service_messages','customer_service_tickets','faq_knowledge','sop_knowledge','ai_conversations',
    'ai_messages','agent_task_runs','notifications'
  )
  and not exists (
    select 1 from information_schema.columns c
    where c.table_schema = t.table_schema
      and c.table_name = t.table_name
      and c.column_name = 'tenant_id'
  );
"@)
  if ($tenantMissing -gt 0) { throw "tenant_id missing business tables found: $tenantMissing" }

  $sprint4Inconsistent = [int](Query-Scalar @"
select
  (select count(*)
     from inventory_transactions
    where tenant_id = 1
      and deleted = 0
      and biz_type = 'INVENTORY_IMPORT'
      and before_qty + change_qty <> after_qty) +
  (select count(*)
     from import_error_reports
    where tenant_id = 1
      and deleted = 0
      and module in ('PRODUCT_IMPORT','INVENTORY_IMPORT','PARTNER_IMPORT')
      and (report_id is null or report_id = '' or content is null or content = '')) +
  (select count(*)
     from backup_records
    where tenant_id = 1
      and deleted = 0
      and (status not in ('RUNNING','SUCCESS','FAILED')
        or backup_no is null
        or backup_no = ''
        or (status = 'SUCCESS' and (file_size is null or file_size <= 0)))) +
  (select count(*)
     from products p
    where p.tenant_id = 1
      and p.deleted = 0
      and exists (
        select 1 from products d
         where d.tenant_id = p.tenant_id
           and d.product_code = p.product_code
           and d.deleted = 0
           and d.id <> p.id
      )) +
  (select count(*)
     from partners p
    where p.tenant_id = 1
      and p.deleted = 0
      and exists (
        select 1 from partners d
         where d.tenant_id = p.tenant_id
           and d.partner_code = p.partner_code
           and d.deleted = 0
           and d.id <> p.id
      ));
"@)
  if ($sprint4Inconsistent -gt 0) { throw "sprint4 import/backup inconsistent rows found: $sprint4Inconsistent" }
  Write-Host "sprint4 import, backup and tenant reserve consistency passed"

  Write-Host "[data-quality] v1.2 sprint2 AI streaming/session consistency"
  $aiSprint2Inconsistent = [int](Query-Scalar @"
select
  (select count(*)
     from ai_conversations
    where tenant_id = 1
      and deleted = 0
      and (last_active_at is null or status not in ('ACTIVE','ARCHIVED'))) +
  (select count(*)
     from ai_conversations
    where tenant_id = 1
      and deleted = 0
      and status = 'ARCHIVED'
      and ended_at is null) +
  (select count(*)
     from ai_messages
    where tenant_id = 1
      and deleted = 0
      and status not in ('PENDING','STREAMING','COMPLETED','FAILED','STOPPED')) +
  (select count(*)
     from ai_messages m
     left join ai_conversations c
       on c.id = m.conversation_id
      and c.tenant_id = m.tenant_id
      and c.deleted = 0
    where m.tenant_id = 1
      and m.deleted = 0
      and c.id is null);
"@)
  if ($aiSprint2Inconsistent -gt 0) { throw "v1.2 sprint2 AI inconsistent rows found: $aiSprint2Inconsistent" }
  Write-Host "v1.2 sprint2 AI streaming/session consistency passed"

  Write-Host "[data-quality] v1.2 sprint3 AI tool call consistency"
  $aiToolLogTableExists = [int](Query-Scalar "select count(*) from information_schema.tables where table_schema = database() and table_name = 'ai_tool_call_logs';")
  if ($aiToolLogTableExists -ne 1) { throw "ai_tool_call_logs table missing" }
  $aiToolLogInconsistent = [int](Query-Scalar @"
select
  (select count(*)
     from ai_tool_call_logs
    where tenant_id = 1
      and (tool_name is null or tool_name = ''
        or success not in (0, 1)
        or permission_result not in ('ALLOWED','DENIED','UNKNOWN_TOOL')
        or duration_ms is null
        or result_count is null
        or char_length(coalesce(arguments_summary, '')) > 512
        or arguments_summary like '%sk-%')) +
  (select count(*)
     from ai_tool_call_logs l
     left join ai_conversations c
       on c.id = l.conversation_id
      and c.tenant_id = l.tenant_id
      and c.deleted = 0
    where l.tenant_id = 1
      and l.conversation_id is not null
      and c.id is null) +
  (select count(*)
     from ai_tool_call_logs l
     left join ai_messages m
       on m.id = l.message_id
      and m.tenant_id = l.tenant_id
      and m.deleted = 0
    where l.tenant_id = 1
      and l.message_id is not null
      and m.id is null);
"@)
  if ($aiToolLogInconsistent -gt 0) { throw "v1.2 sprint3 AI tool log inconsistent rows found: $aiToolLogInconsistent" }
  Write-Host "v1.2 sprint3 AI tool call consistency passed"

  Write-Host "[data-quality] v1.2 sprint4 notification/report/audit consistency"
  $sprint4ColumnCount = [int](Query-Scalar @"
select count(*)
from information_schema.columns
where table_schema = database()
  and table_name = 'notifications'
  and column_name in ('severity','jump_path');
"@)
  if ($sprint4ColumnCount -ne 2) { throw "notifications sprint4 columns missing" }

  $sprint4IndexCount = [int](Query-Scalar @"
select count(*)
from information_schema.statistics
where table_schema = database()
  and table_name = 'notifications'
  and index_name = 'idx_notify_biz_receiver';
"@)
  if ($sprint4IndexCount -lt 1) { throw "notifications idx_notify_biz_receiver missing" }

  $sprint4NotificationInconsistent = [int](Query-Scalar @"
select
  (select count(*)
     from notifications n
     left join users u
       on u.id = n.receiver_user_id
      and u.tenant_id = n.tenant_id
      and u.deleted = 0
    where n.tenant_id = 1
      and n.deleted = 0
      and u.id is null) +
  (select count(*)
     from notifications
    where tenant_id = 1
      and deleted = 0
      and ((read_flag = 1 and read_at is null) or (read_flag = 0 and read_at is not null))) +
  (select count(*)
     from notifications
    where tenant_id = 1
      and deleted = 0
      and jump_path is not null
      and jump_path <> ''
      and jump_path not like '/%') +
  (select count(*)
     from (
       select tenant_id, biz_type, biz_no, receiver_user_id, notify_type, count(*) cnt
         from notifications
        where tenant_id = 1
          and deleted = 0
          and notify_type in ('LOW_STOCK_PATROL','ORDER_OVERDUE_PATROL','TICKET_OVERDUE_PATROL')
        group by tenant_id, biz_type, biz_no, receiver_user_id, notify_type
       having count(*) > 1
     ) d);
"@)
  if ($sprint4NotificationInconsistent -gt 0) { throw "v1.2 sprint4 notification inconsistent rows found: $sprint4NotificationInconsistent" }

  $reportInventory = Invoke-RestMethod -Method Get -Uri "http://localhost:18080/api/v1/reports/inventory-turnover" -Headers $headers
  Assert-SuccessCode $reportInventory "inventory-turnover"
  $reportInout = Invoke-RestMethod -Method Get -Uri "http://localhost:18080/api/v1/reports/inout-summary" -Headers $headers
  Assert-SuccessCode $reportInout "inout-summary"
  $reportPurchaseSales = Invoke-RestMethod -Method Get -Uri "http://localhost:18080/api/v1/reports/purchase-sales-summary" -Headers $headers
  Assert-SuccessCode $reportPurchaseSales "purchase-sales-summary"
  $reportTicket = Invoke-RestMethod -Method Get -Uri "http://localhost:18080/api/v1/reports/ticket-efficiency" -Headers $headers
  Assert-SuccessCode $reportTicket "ticket-efficiency"
  Write-Host "v1.2 sprint4 notification/report/audit consistency passed"

  Write-Host "[data-quality] v1.4 RBAC management readiness"
  $v14RbacInconsistent = [int](Query-Scalar @"
select
  (select count(*) from roles where tenant_id = 1 and role_code in ('TENANT_ADMIN','WAREHOUSE_MANAGER','WAREHOUSE_OPERATOR','CS_AGENT','DATA_VIEWER') and deleted = 0 and status = 'ACTIVE') <> 5
  or (select count(*) from users where tenant_id = 1 and username in ('admin','warehouse01','cs01','observer01') and deleted = 0 and status = 'ACTIVE') <> 4
  or not exists (select 1 from permissions where perm_code = 'ROLE_MANAGE' and deleted = 0 and status = 'ACTIVE')
  or not exists (
    select 1
      from role_permissions rp
      join roles r on r.id = rp.role_id and r.deleted = 0
      join permissions p on p.id = rp.permission_id and p.deleted = 0
     where r.tenant_id = 1
       and r.role_code = 'TENANT_ADMIN'
       and p.perm_code = 'ROLE_MANAGE'
       and rp.deleted = 0
  )
  or exists (
    select 1
      from role_permissions rp
      join roles r on r.id = rp.role_id and r.deleted = 0
      join permissions p on p.id = rp.permission_id and p.deleted = 0
     where r.tenant_id = 1
       and r.role_code in ('WAREHOUSE_MANAGER','WAREHOUSE_OPERATOR','CS_AGENT','DATA_VIEWER')
       and p.perm_code = 'ROLE_MANAGE'
       and rp.deleted = 0
  );
"@)
  if ($v14RbacInconsistent -gt 0) { throw "v1.4 RBAC management readiness failed" }
  Write-Host "v1.4 RBAC management readiness passed"

  Write-Host "[data-quality] passed"
} catch {
  Write-Error "[data-quality] failed: $($_.Exception.Message)"
  exit 1
}

