SET NAMES utf8mb4;

-- NovaDepot 开发演示数据重置脚本
-- 说明：仅用于本地开发/演示环境，会清空 tenant_id=1 的业务样本数据

DELETE FROM ai_messages WHERE tenant_id = 1;
DELETE FROM ai_conversations WHERE tenant_id = 1;

DELETE FROM customer_service_messages WHERE tenant_id = 1;
DELETE FROM customer_service_tickets WHERE tenant_id = 1;
DELETE FROM customer_service_sessions WHERE tenant_id = 1;

DELETE FROM inventory_transactions WHERE tenant_id = 1;
DELETE FROM outbound_order_items WHERE tenant_id = 1;
DELETE FROM outbound_orders WHERE tenant_id = 1;
DELETE FROM inbound_order_items WHERE tenant_id = 1;
DELETE FROM inbound_orders WHERE tenant_id = 1;
DELETE FROM inventory WHERE tenant_id = 1;

DELETE FROM warehouse_locations WHERE tenant_id = 1;
DELETE FROM warehouses WHERE tenant_id = 1;
DELETE FROM products WHERE tenant_id = 1;
DELETE FROM product_units WHERE tenant_id = 1;
DELETE FROM product_categories WHERE tenant_id = 1;

DELETE FROM faq_knowledge WHERE tenant_id = 1;

DELETE FROM user_roles WHERE tenant_id = 1;
DELETE FROM role_permissions WHERE tenant_id = 1;
DELETE FROM users WHERE tenant_id = 1;
DELETE FROM roles WHERE tenant_id = 1;

-- 保留 default tenant 行，交由 99-seed-mvp.sql 重新写入标准演示数据
