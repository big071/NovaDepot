SET NAMES utf8mb4;

-- 清理 Phase 1 业务样本层 + 轻压测层（ID 300000-399999）和自动化测试生成的雪花 ID 数据
DELETE FROM ai_messages WHERE id >= 1000000000000000000;
DELETE FROM ai_conversations WHERE id >= 1000000000000000000;
DELETE FROM customer_service_messages WHERE id >= 1000000000000000000;
DELETE FROM customer_service_tickets WHERE id >= 1000000000000000000;
DELETE FROM customer_service_sessions WHERE id >= 1000000000000000000;
DELETE FROM faq_knowledge WHERE id >= 1000000000000000000;
DELETE FROM rule_configs WHERE id >= 1000000000000000000;
DELETE FROM audit_logs WHERE id >= 1000000000000000000;
DELETE FROM inventory_transactions WHERE id >= 1000000000000000000;
DELETE FROM outbound_order_items WHERE id >= 1000000000000000000;
DELETE FROM outbound_orders WHERE id >= 1000000000000000000;
DELETE FROM inbound_order_items WHERE id >= 1000000000000000000;
DELETE FROM inbound_orders WHERE id >= 1000000000000000000;
DELETE FROM inventory WHERE id >= 1000000000000000000;

DELETE FROM ai_messages WHERE id BETWEEN 300000 AND 399999;
DELETE FROM ai_conversations WHERE id BETWEEN 300000 AND 399999;

DELETE FROM customer_service_messages WHERE id BETWEEN 300000 AND 399999;
DELETE FROM customer_service_tickets WHERE id BETWEEN 300000 AND 399999;
DELETE FROM customer_service_sessions WHERE id BETWEEN 300000 AND 399999;

DELETE FROM faq_knowledge WHERE id BETWEEN 300000 AND 399999;
SET @stmt = (
  SELECT IF(
    COUNT(*) = 1,
    'DELETE FROM sop_knowledge WHERE id BETWEEN 300000 AND 399999',
    'SELECT 1'
  )
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'sop_knowledge'
);
PREPARE reset_sop FROM @stmt;
EXECUTE reset_sop;
DEALLOCATE PREPARE reset_sop;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 1,
    'DELETE FROM rule_configs WHERE id BETWEEN 300000 AND 399999',
    'SELECT 1'
  )
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'rule_configs'
);
PREPARE reset_rule_configs FROM @stmt;
EXECUTE reset_rule_configs;
DEALLOCATE PREPARE reset_rule_configs;
DELETE FROM audit_logs WHERE id BETWEEN 300000 AND 399999;
DELETE FROM inventory_transactions WHERE tenant_id = 1 AND biz_type = 'STOCKTAKE_ADJUST';

SET @stmt = (
  SELECT IF(
    COUNT(*) = 1,
    'DELETE FROM payments WHERE tenant_id = 1 AND (id BETWEEN 300000 AND 399999 OR id >= 1000000000000000000)',
    'SELECT 1'
  )
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'payments'
);
PREPARE reset_payments FROM @stmt;
EXECUTE reset_payments;
DEALLOCATE PREPARE reset_payments;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 1,
    'DELETE FROM payables WHERE tenant_id = 1 AND (id BETWEEN 300000 AND 399999 OR id >= 1000000000000000000)',
    'SELECT 1'
  )
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'payables'
);
PREPARE reset_payables FROM @stmt;
EXECUTE reset_payables;
DEALLOCATE PREPARE reset_payables;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 1,
    'DELETE FROM receivables WHERE tenant_id = 1 AND (id BETWEEN 300000 AND 399999 OR id >= 1000000000000000000)',
    'SELECT 1'
  )
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'receivables'
);
PREPARE reset_receivables FROM @stmt;
EXECUTE reset_receivables;
DEALLOCATE PREPARE reset_receivables;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 1,
    'DELETE FROM stocktake_order_items WHERE tenant_id = 1 AND (id BETWEEN 300000 AND 399999 OR id >= 1000000000000000000)',
    'SELECT 1'
  )
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'stocktake_order_items'
);
PREPARE reset_stocktake_items FROM @stmt;
EXECUTE reset_stocktake_items;
DEALLOCATE PREPARE reset_stocktake_items;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 1,
    'DELETE FROM stocktake_orders WHERE tenant_id = 1 AND (id BETWEEN 300000 AND 399999 OR id >= 1000000000000000000)',
    'SELECT 1'
  )
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'stocktake_orders'
);
PREPARE reset_stocktake_orders FROM @stmt;
EXECUTE reset_stocktake_orders;
DEALLOCATE PREPARE reset_stocktake_orders;

DELETE FROM outbound_order_items WHERE id BETWEEN 300000 AND 399999;
DELETE FROM outbound_orders WHERE id BETWEEN 300000 AND 399999;
DELETE FROM inbound_order_items WHERE id BETWEEN 300000 AND 399999;
DELETE FROM inbound_orders WHERE id BETWEEN 300000 AND 399999;

DELETE FROM inventory WHERE id BETWEEN 300000 AND 399999;
DELETE FROM products WHERE id BETWEEN 300000 AND 399999;
DELETE FROM warehouse_locations WHERE id BETWEEN 300000 AND 399999;
DELETE FROM warehouses WHERE id BETWEEN 300000 AND 399999;
DELETE FROM suppliers WHERE id BETWEEN 300000 AND 399999;
DELETE FROM customers WHERE id BETWEEN 300000 AND 399999;
DELETE FROM purchase_order_items WHERE id BETWEEN 300000 AND 399999;
DELETE FROM purchase_orders WHERE id BETWEEN 300000 AND 399999;
DELETE FROM sales_order_items WHERE id BETWEEN 300000 AND 399999;
DELETE FROM sales_orders WHERE id BETWEEN 300000 AND 399999;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 1,
    'DELETE FROM partners WHERE id BETWEEN 300000 AND 399999',
    'SELECT 1'
  )
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'partners'
);
PREPARE reset_partners FROM @stmt;
EXECUTE reset_partners;
DEALLOCATE PREPARE reset_partners;
