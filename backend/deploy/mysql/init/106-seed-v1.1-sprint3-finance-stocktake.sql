SET NAMES utf8mb4;

INSERT INTO payables (id, tenant_id, payable_no, source_type, source_order_id, source_order_no, partner_id, warehouse_id, total_amount, paid_amount, balance_amount, status, remark, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(360001,1,'AP-V11-UNPAID-001','PURCHASE_ORDER',320002,'PO-V11-CONF-001',310003,300101,1180.00,0.00,1180.00,'UNPAID','v1.1 Sprint 3 unpaid payable sample',NOW(3),1,NOW(3),1,0),
(360002,1,'AP-V11-PARTIAL-001','PURCHASE_ORDER',320003,'PO-V11-PARTIAL-001',310001,300101,590.00,200.00,390.00,'PARTIALLY_PAID','v1.1 Sprint 3 partial payable sample',NOW(3),1,NOW(3),1,0)
ON DUPLICATE KEY UPDATE total_amount=VALUES(total_amount), paid_amount=VALUES(paid_amount), balance_amount=VALUES(balance_amount), status=VALUES(status), remark=VALUES(remark), deleted=0, updated_at=NOW(3);

INSERT INTO receivables (id, tenant_id, receivable_no, source_type, source_order_id, source_order_no, partner_id, warehouse_id, total_amount, received_amount, balance_amount, status, remark, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(360101,1,'AR-V11-UNPAID-001','SALES_ORDER',330002,'SO-V11-CONF-001',310003,300101,980.00,0.00,980.00,'UNPAID','v1.1 Sprint 3 unpaid receivable sample',NOW(3),1,NOW(3),1,0),
(360102,1,'AR-V11-PARTIAL-001','SALES_ORDER',330004,'SO-V11-UI-001',310003,300101,36.50,10.00,26.50,'PARTIALLY_PAID','v1.1 Sprint 3 partial receivable sample',NOW(3),1,NOW(3),1,0)
ON DUPLICATE KEY UPDATE total_amount=VALUES(total_amount), received_amount=VALUES(received_amount), balance_amount=VALUES(balance_amount), status=VALUES(status), remark=VALUES(remark), deleted=0, updated_at=NOW(3);

INSERT INTO payments (id, tenant_id, payment_no, direction, ledger_id, ledger_no, partner_id, amount, paid_at, method, remark, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(360201,1,'PAY-V11-AP-001','PAYABLE',360002,'AP-V11-PARTIAL-001',310001,200.00,CURDATE(),'MANUAL','v1.1 Sprint 3 payable registration seed',NOW(3),1,NOW(3),1,0),
(360202,1,'PAY-V11-AR-001','RECEIVABLE',360102,'AR-V11-PARTIAL-001',310003,10.00,CURDATE(),'MANUAL','v1.1 Sprint 3 receivable registration seed',NOW(3),1,NOW(3),1,0)
ON DUPLICATE KEY UPDATE amount=VALUES(amount), paid_at=VALUES(paid_at), method=VALUES(method), remark=VALUES(remark), deleted=0, updated_at=NOW(3);

INSERT INTO stocktake_orders (id, tenant_id, stocktake_no, status, warehouse_id, scope_type, planned_at, started_at, finished_at, diff_count, remark, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(360301,1,'ST-V11-DRAFT-001','DRAFT',300101,'WAREHOUSE',NOW(3),NULL,NULL,0,'v1.1 Sprint 3 draft stocktake sample',NOW(3),2,NOW(3),2,0),
(360302,1,'ST-V11-INPROG-001','IN_PROGRESS',300101,'WAREHOUSE',NOW(3),NOW(3),NULL,0,'v1.1 Sprint 3 in-progress stocktake sample',NOW(3),2,NOW(3),2,0),
(360303,1,'ST-V11-DIFF-001','DIFF_REVIEW',300101,'WAREHOUSE',NOW(3),NOW(3),NULL,1,'v1.1 Sprint 3 difference review stocktake sample',NOW(3),2,NOW(3),2,0)
ON DUPLICATE KEY UPDATE status=VALUES(status), warehouse_id=VALUES(warehouse_id), scope_type=VALUES(scope_type), started_at=VALUES(started_at), finished_at=VALUES(finished_at), diff_count=VALUES(diff_count), remark=VALUES(remark), deleted=0, updated_at=NOW(3);

INSERT INTO stocktake_order_items (id, tenant_id, stocktake_order_id, line_no, product_id, location_id, system_qty, counted_qty, diff_qty, result_type, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(360401,1,360302,1,300501,300203,540.000000,540.000000,0.000000,'PENDING',NOW(3),2,NOW(3),2,0),
(360402,1,360303,1,300501,300203,540.000000,538.000000,-2.000000,'DIFF',NOW(3),2,NOW(3),2,0)
ON DUPLICATE KEY UPDATE product_id=VALUES(product_id), location_id=VALUES(location_id), system_qty=VALUES(system_qty), counted_qty=VALUES(counted_qty), diff_qty=VALUES(diff_qty), result_type=VALUES(result_type), deleted=0, updated_at=NOW(3);
