SET NAMES utf8mb4;

INSERT INTO purchase_orders (id, tenant_id, purchase_no, status, partner_id, supplier_id, warehouse_id, total_amount, expected_arrival_date, remark, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(320003,1,'PO-V11-PARTIAL-001','PARTIAL_RECEIVED',310001,310001,300101,590.00,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'v1.1 partial received sample',NOW(3),2,NOW(3),2,0)
ON DUPLICATE KEY UPDATE status=VALUES(status), partner_id=VALUES(partner_id), supplier_id=VALUES(supplier_id), warehouse_id=VALUES(warehouse_id), total_amount=VALUES(total_amount), expected_arrival_date=VALUES(expected_arrival_date), remark=VALUES(remark), updated_at=NOW(3), deleted=0;

INSERT INTO purchase_order_items (id, tenant_id, purchase_order_id, line_no, product_id, unit_price, order_qty, received_qty, tax_rate, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(320104,1,320003,1,300503,11.80,50.000000,20.000000,0.0000,NOW(3),2,NOW(3),2,0)
ON DUPLICATE KEY UPDATE product_id=VALUES(product_id), unit_price=VALUES(unit_price), order_qty=VALUES(order_qty), received_qty=VALUES(received_qty), tax_rate=VALUES(tax_rate), updated_at=NOW(3), deleted=0;

INSERT INTO sales_orders (id, tenant_id, sales_no, status, partner_id, customer_id, warehouse_id, total_amount, delivery_date, remark, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(330003,1,'SO-V11-SHORT-001','CONFIRMED',310002,310002,300102,9900.00,DATE_ADD(CURDATE(), INTERVAL 4 DAY),'v1.1 stock shortage sample',NOW(3),2,NOW(3),2,0),
(330004,1,'SO-V11-UI-001','CONFIRMED',310003,310003,300101,36.50,DATE_ADD(CURDATE(), INTERVAL 4 DAY),'v1.1 UI outbound conversion sample',NOW(3),2,NOW(3),2,0)
ON DUPLICATE KEY UPDATE status=VALUES(status), partner_id=VALUES(partner_id), customer_id=VALUES(customer_id), warehouse_id=VALUES(warehouse_id), total_amount=VALUES(total_amount), delivery_date=VALUES(delivery_date), remark=VALUES(remark), updated_at=NOW(3), deleted=0;

INSERT INTO sales_order_items (id, tenant_id, sales_order_id, line_no, product_id, unit_price, order_qty, shipped_qty, tax_rate, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(330104,1,330003,1,300505,99.00,100.000000,0.000000,0.0000,NOW(3),2,NOW(3),2,0),
(330105,1,330004,1,300501,36.50,1.000000,0.000000,0.0000,NOW(3),2,NOW(3),2,0)
ON DUPLICATE KEY UPDATE product_id=VALUES(product_id), unit_price=VALUES(unit_price), order_qty=VALUES(order_qty), shipped_qty=VALUES(shipped_qty), tax_rate=VALUES(tax_rate), updated_at=NOW(3), deleted=0;

INSERT INTO inventory (id, tenant_id, warehouse_id, location_id, product_id, available_qty, locked_qty, in_transit_qty, version_no, created_at, updated_at, deleted)
VALUES
(340001,1,300101,300201,300501,100.000000,0.000000,0.000000,1,NOW(3),NOW(3),0)
ON DUPLICATE KEY UPDATE available_qty=VALUES(available_qty), locked_qty=VALUES(locked_qty), in_transit_qty=VALUES(in_transit_qty), version_no=VALUES(version_no), updated_at=NOW(3), deleted=0;
