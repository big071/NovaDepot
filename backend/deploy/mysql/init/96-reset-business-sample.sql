SET NAMES utf8mb4;

-- 清理 Phase 1 业务样本层 + 轻压测层（ID 300000-399999）
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
