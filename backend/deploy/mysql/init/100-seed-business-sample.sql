SET NAMES utf8mb4;

-- =========================
-- Phase 3 真实业务场景样本层（ID 300000-399999）
-- 目标：演示、联调、测试三场景统一
-- =========================

-- 仓库与库位：命名规律 + 用途区分 + 容量分层
INSERT INTO warehouses (id, tenant_id, warehouse_code, warehouse_name, warehouse_type, address, manager_user_id, status, created_at, updated_at, deleted)
VALUES
(300101,1,'成都-成品仓-01','成都一号成品仓','STANDARD','成都市双流区物流大道 99 号',2,'ACTIVE',NOW(3),NOW(3),0),
(300102,1,'成都-快发仓-01','成都电商快发仓','ECOM','成都市温江区创新路 88 号',2,'ACTIVE',NOW(3),NOW(3),0),
(300103,1,'成都-退货仓-01','成都退货处理仓','RETURN','成都市新都区回流街 16 号',2,'ACTIVE',NOW(3),NOW(3),0)
ON DUPLICATE KEY UPDATE warehouse_name=VALUES(warehouse_name), warehouse_type=VALUES(warehouse_type), address=VALUES(address), manager_user_id=VALUES(manager_user_id), status=VALUES(status), updated_at=NOW(3), deleted=0;

INSERT INTO warehouse_locations (id, tenant_id, warehouse_id, location_code, location_name, location_type, capacity_qty, status, created_at, updated_at, deleted)
VALUES
(300201,1,300101,'A-01-01','成品仓-A区-拣选位-01','PICK',180,'ACTIVE',NOW(3),NOW(3),0),
(300202,1,300101,'A-02-01','成品仓-A区-拣选位-02','PICK',180,'ACTIVE',NOW(3),NOW(3),0),
(300203,1,300101,'B-01-01','成品仓-B区-存储位-01','STORAGE',520,'ACTIVE',NOW(3),NOW(3),0),
(300204,1,300101,'B-02-01','成品仓-B区-存储位-02','STORAGE',520,'ACTIVE',NOW(3),NOW(3),0),
(300205,1,300101,'C-01-01','成品仓-C区-暂存位-01','STAGING',260,'ACTIVE',NOW(3),NOW(3),0),
(300206,1,300101,'R-01-01','成品仓-退货复检位-01','RETURN',120,'ACTIVE',NOW(3),NOW(3),0),
(300207,1,300102,'A-01-01','快发仓-A区-拣选位-01','PICK',140,'ACTIVE',NOW(3),NOW(3),0),
(300208,1,300102,'B-01-01','快发仓-B区-存储位-01','STORAGE',420,'ACTIVE',NOW(3),NOW(3),0),
(300209,1,300103,'R-01-01','退货仓-R区-待检位-01','RETURN',160,'ACTIVE',NOW(3),NOW(3),0)
ON DUPLICATE KEY UPDATE location_name=VALUES(location_name), location_type=VALUES(location_type), capacity_qty=VALUES(capacity_qty), status=VALUES(status), updated_at=NOW(3), deleted=0;

-- 供应商与客户：真实业务关系
INSERT INTO suppliers (id, tenant_id, supplier_code, supplier_name, contact_name, phone, credit_level, status, created_at, updated_at, deleted)
VALUES
(300301,1,'SUP-SW-FOOD','西南乳品供应链有限公司','刘成','13988880001','A','ACTIVE',NOW(3),NOW(3),0),
(300302,1,'SUP-HX-HPC','华西家清日化有限公司','王敏','13988880002','A','ACTIVE',NOW(3),NOW(3),0),
(300303,1,'SUP-CD-OFFICE','成都办公用品集采中心','陈林','13988880003','B','ACTIVE',NOW(3),NOW(3),0)
ON DUPLICATE KEY UPDATE supplier_name=VALUES(supplier_name), contact_name=VALUES(contact_name), phone=VALUES(phone), credit_level=VALUES(credit_level), status=VALUES(status), updated_at=NOW(3), deleted=0;

INSERT INTO customers (id, tenant_id, customer_code, customer_name, contact_name, phone, customer_level, status, created_at, updated_at, deleted)
VALUES
(300401,1,'CUS-RETAIL-001','川渝连锁生活超市','赵宁','13677770001','A','ACTIVE',NOW(3),NOW(3),0),
(300402,1,'CUS-ONLINE-001','西南电商分销平台','唐雪','13677770002','A','ACTIVE',NOW(3),NOW(3),0),
(300403,1,'CUS-B2B-001','成都企业团购服务中心','宋岩','13677770003','B','ACTIVE',NOW(3),NOW(3),0)
ON DUPLICATE KEY UPDATE customer_name=VALUES(customer_name), contact_name=VALUES(contact_name), phone=VALUES(phone), customer_level=VALUES(customer_level), status=VALUES(status), updated_at=NOW(3), deleted=0;

INSERT INTO partners (id, tenant_id, partner_code, partner_name, partner_type, contact_name, phone, address, status, remark, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(310001,1,'PTN-SW-FOOD','西南乳品供应链有限公司','SUPPLIER','刘成','13988880001','成都高新区供应链园区 8 号','ACTIVE','v1.1 采购供应商样例',NOW(3),1,NOW(3),1,0),
(310002,1,'PTN-RETAIL-001','川渝连锁生活超市','CUSTOMER','赵宁','13677770001','重庆渝北配送中心 19 号','ACTIVE','v1.1 销售客户样例',NOW(3),1,NOW(3),1,0),
(310003,1,'PTN-TRADE-001','成渝综合贸易伙伴','BOTH','周然','13866660003','成都青白江综合贸易园 6 号','ACTIVE','既可采购也可销售的双向往来单位',NOW(3),1,NOW(3),1,0)
ON DUPLICATE KEY UPDATE partner_name=VALUES(partner_name), partner_type=VALUES(partner_type), contact_name=VALUES(contact_name), phone=VALUES(phone), address=VALUES(address), status=VALUES(status), remark=VALUES(remark), updated_at=NOW(3), deleted=0;

INSERT INTO purchase_orders (id, tenant_id, purchase_no, status, partner_id, supplier_id, warehouse_id, total_amount, expected_arrival_date, remark, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(320001,1,'PO-V11-DRAFT-001','DRAFT',310001,310001,300101,3360.00,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'v1.1 采购草稿样例',NOW(3),2,NOW(3),2,0),
(320002,1,'PO-V11-CONF-001','CONFIRMED',310003,310003,300101,1180.00,DATE_ADD(CURDATE(), INTERVAL 5 DAY),'v1.1 已确认采购样例',NOW(3),2,NOW(3),2,0)
ON DUPLICATE KEY UPDATE status=VALUES(status), partner_id=VALUES(partner_id), supplier_id=VALUES(supplier_id), warehouse_id=VALUES(warehouse_id), total_amount=VALUES(total_amount), expected_arrival_date=VALUES(expected_arrival_date), remark=VALUES(remark), updated_at=NOW(3), deleted=0;

INSERT INTO purchase_order_items (id, tenant_id, purchase_order_id, line_no, product_id, unit_price, order_qty, received_qty, tax_rate, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(320101,1,320001,1,300501,18.00,120.000000,0.000000,0.0000,NOW(3),2,NOW(3),2,0),
(320102,1,320001,2,300502,20.00,60.000000,0.000000,0.0000,NOW(3),2,NOW(3),2,0),
(320103,1,320002,1,300503,11.80,100.000000,0.000000,0.0000,NOW(3),2,NOW(3),2,0)
ON DUPLICATE KEY UPDATE product_id=VALUES(product_id), unit_price=VALUES(unit_price), order_qty=VALUES(order_qty), received_qty=VALUES(received_qty), tax_rate=VALUES(tax_rate), updated_at=NOW(3), deleted=0;

INSERT INTO sales_orders (id, tenant_id, sales_no, status, partner_id, customer_id, warehouse_id, total_amount, delivery_date, remark, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(330001,1,'SO-V11-DRAFT-001','DRAFT',310002,310002,300101,2190.00,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'v1.1 销售草稿样例',NOW(3),2,NOW(3),2,0),
(330002,1,'SO-V11-CONF-001','CONFIRMED',310003,310003,300101,980.00,DATE_ADD(CURDATE(), INTERVAL 4 DAY),'v1.1 已确认销售样例',NOW(3),2,NOW(3),2,0)
ON DUPLICATE KEY UPDATE status=VALUES(status), partner_id=VALUES(partner_id), customer_id=VALUES(customer_id), warehouse_id=VALUES(warehouse_id), total_amount=VALUES(total_amount), delivery_date=VALUES(delivery_date), remark=VALUES(remark), updated_at=NOW(3), deleted=0;

INSERT INTO sales_order_items (id, tenant_id, sales_order_id, line_no, product_id, unit_price, order_qty, shipped_qty, tax_rate, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(330101,1,330001,1,300501,36.50,40.000000,0.000000,0.0000,NOW(3),2,NOW(3),2,0),
(330102,1,330001,2,300502,24.33,30.000000,0.000000,0.0000,NOW(3),2,NOW(3),2,0),
(330103,1,330002,1,300503,19.60,50.000000,0.000000,0.0000,NOW(3),2,NOW(3),2,0)
ON DUPLICATE KEY UPDATE product_id=VALUES(product_id), unit_price=VALUES(unit_price), order_qty=VALUES(order_qty), shipped_qty=VALUES(shipped_qty), tax_rate=VALUES(tax_rate), updated_at=NOW(3), deleted=0;

-- 商品：分类 + 规格 + 条码 + 安全库存语义 + 状态
INSERT INTO products (id, tenant_id, product_code, product_name, category_id, unit_id, barcode, spec, batch_enabled, shelf_life_days, status, created_at, updated_at, deleted)
VALUES
(300501,1,'乳品-高钙奶24','高钙纯牛奶 250ml*24盒',9001,9102,'6931000005012','24盒/箱;安全库存=120盒;补货周期=3天;场景:早餐高频补货',1,180,'ACTIVE',NOW(3),NOW(3),0),
(300502,1,'饮料-无糖绿茶15','无糖绿茶 500ml*15瓶',9001,9102,'6931000005029','15瓶/箱;安全库存=80箱;补货周期=5天;场景:门店促销备货',1,365,'ACTIVE',NOW(3),NOW(3),0),
(300503,1,'日化-洗衣凝珠60','洗衣凝珠 60颗装',9002,9101,'6931000005036','60颗/袋;安全库存=40袋;补货周期=7天;场景:电商周转品',0,NULL,'ACTIVE',NOW(3),NOW(3),0),
(300504,1,'家清-厨房抽纸120','厨房抽纸 120抽*3包',9002,9101,'6931000005043','3包/提;安全库存=60提;补货周期=4天;场景:商超高频消耗',0,NULL,'ACTIVE',NOW(3),NOW(3),0),
(300505,1,'SKU-GAN-65W','65W 氮化镓快充头',9003,9101,'6931000005050','单件;安全库存=30件;补货周期=10天',0,NULL,'ACTIVE',NOW(3),NOW(3),0),
(300506,1,'SKU-PAPER-A4-70','A4 打印纸 70g 500张',9004,9101,'6931000005067','500张/包;安全库存=200包;补货周期=6天',0,NULL,'ACTIVE',NOW(3),NOW(3),0),
(300507,1,'数码-TypeC数据线','Type-C 数据线 1m',9003,9101,'6931000005074','1条/盒;安全库存=50条;补货周期=8天;场景:配件补货',0,NULL,'ACTIVE',NOW(3),NOW(3),0),
(300508,1,'办公-75mm档案盒','档案盒 75mm',9004,9101,'6931000005081','1个/件;安全库存=80个;补货周期=14天;场景:办公耗材',0,NULL,'DISABLED',NOW(3),NOW(3),0)
ON DUPLICATE KEY UPDATE product_name=VALUES(product_name), category_id=VALUES(category_id), barcode=VALUES(barcode), spec=VALUES(spec), batch_enabled=VALUES(batch_enabled), shelf_life_days=VALUES(shelf_life_days), status=VALUES(status), updated_at=NOW(3), deleted=0;

-- 库存：刻意设置低库存SKU用于场景B/D
INSERT INTO inventory (id, tenant_id, warehouse_id, location_id, product_id, available_qty, locked_qty, in_transit_qty, version_no, created_at, updated_at, deleted)
VALUES
(300601,1,300101,300203,300501,540,30,20,1,NOW(3),NOW(3),0),
(300602,1,300101,300204,300502,210,20,15,1,NOW(3),NOW(3),0),
(300603,1,300101,300203,300503,66,6,0,1,NOW(3),NOW(3),0),
(300604,1,300102,300208,300504,24,2,0,1,NOW(3),NOW(3),0),
(300605,1,300102,300207,300505,8,0,12,1,NOW(3),NOW(3),0),
(300606,1,300101,300204,300506,920,0,40,1,NOW(3),NOW(3),0),
(300607,1,300102,300207,300507,11,1,0,1,NOW(3),NOW(3),0),
(300608,1,300103,300209,300508,18,0,0,1,NOW(3),NOW(3),0)
ON DUPLICATE KEY UPDATE available_qty=VALUES(available_qty), locked_qty=VALUES(locked_qty), in_transit_qty=VALUES(in_transit_qty), version_no=VALUES(version_no), updated_at=NOW(3), deleted=0;

-- 入库单：覆盖 草稿/已提交/已审核/已过账 + 原因与时间分布
INSERT INTO inbound_orders (id, tenant_id, inbound_no, biz_type, status, warehouse_id, supplier_id, expected_at, completed_at, remark, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(300701,1,'IN-SCN-2026042201','PURCHASE','DRAFT',300101,300301,DATE_ADD(NOW(3), INTERVAL 1 DAY),NULL,'原因:采购到货-乳品补货；场景A草稿编辑起点',DATE_SUB(NOW(3), INTERVAL 3 DAY),2,DATE_SUB(NOW(3), INTERVAL 3 DAY),2,0),
(300702,1,'IN-SCN-2026042202','PURCHASE','SUBMITTED',300101,300302,DATE_ADD(NOW(3), INTERVAL 6 HOUR),NULL,'原因:临采补货-日化促销备货；场景A提交后',DATE_SUB(NOW(3), INTERVAL 1 DAY),2,DATE_SUB(NOW(3), INTERVAL 20 HOUR),2,0),
(300703,1,'IN-SCN-2026042203','RETURN_IN','APPROVED',300103,300303,DATE_ADD(NOW(3), INTERVAL 2 HOUR),NULL,'原因:客户退回入库；场景A审核后待过账',DATE_SUB(NOW(3), INTERVAL 10 HOUR),2,DATE_SUB(NOW(3), INTERVAL 8 HOUR),2,0),
(300704,1,'IN-SCN-2026042204','PURCHASE','POSTED',300101,300301,DATE_SUB(NOW(3), INTERVAL 2 DAY),DATE_SUB(NOW(3), INTERVAL 2 DAY),'原因:采购常规收货；场景A历史完成单',DATE_SUB(NOW(3), INTERVAL 2 DAY),2,DATE_SUB(NOW(3), INTERVAL 2 DAY),2,0)
ON DUPLICATE KEY UPDATE biz_type=VALUES(biz_type), status=VALUES(status), expected_at=VALUES(expected_at), completed_at=VALUES(completed_at), remark=VALUES(remark), created_at=VALUES(created_at), updated_at=VALUES(updated_at), deleted=0;

INSERT INTO inbound_order_items (id, tenant_id, inbound_order_id, line_no, product_id, location_id, unit_id, batch_no, production_date, expire_date, plan_qty, received_qty, qualified_qty, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(300711,1,300701,1,300501,300203,9102,'B-MILK-240420','2026-04-20','2026-10-20',80,0,0,DATE_SUB(NOW(3), INTERVAL 3 DAY),2,DATE_SUB(NOW(3), INTERVAL 3 DAY),2,0),
(300712,1,300702,1,300503,300203,9101,NULL,NULL,NULL,60,0,0,DATE_SUB(NOW(3), INTERVAL 1 DAY),2,DATE_SUB(NOW(3), INTERVAL 20 HOUR),2,0),
(300713,1,300703,1,300504,300209,9101,NULL,NULL,NULL,30,0,0,DATE_SUB(NOW(3), INTERVAL 10 HOUR),2,DATE_SUB(NOW(3), INTERVAL 8 HOUR),2,0),
(300714,1,300704,1,300506,300204,9101,NULL,NULL,NULL,220,220,218,DATE_SUB(NOW(3), INTERVAL 2 DAY),2,DATE_SUB(NOW(3), INTERVAL 2 DAY),2,0)
ON DUPLICATE KEY UPDATE product_id=VALUES(product_id), location_id=VALUES(location_id), plan_qty=VALUES(plan_qty), received_qty=VALUES(received_qty), qualified_qty=VALUES(qualified_qty), updated_at=VALUES(updated_at), deleted=0;

-- 出库单：覆盖 草稿/已提交/已审核/已发运 + 低库存联动
INSERT INTO outbound_orders (id, tenant_id, outbound_no, biz_type, status, warehouse_id, customer_id, expected_ship_at, shipped_at, remark, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(300801,1,'OUT-SCN-2026042201','SALES','DRAFT',300101,300401,DATE_ADD(NOW(3), INTERVAL 1 DAY),NULL,'原因:门店补货发货；场景B草稿单',DATE_SUB(NOW(3), INTERVAL 2 DAY),2,DATE_SUB(NOW(3), INTERVAL 2 DAY),2,0),
(300802,1,'OUT-SCN-2026042202','SALES','SUBMITTED',300102,300402,DATE_ADD(NOW(3), INTERVAL 8 HOUR),NULL,'原因:电商高峰订单；场景B待审核单',DATE_SUB(NOW(3), INTERVAL 9 HOUR),2,DATE_SUB(NOW(3), INTERVAL 7 HOUR),2,0),
(300803,1,'OUT-SCN-2026042203','SALES_PROMO','APPROVED',300102,300402,DATE_ADD(NOW(3), INTERVAL 3 HOUR),NULL,'原因:促销活动发货；场景B待发运单',DATE_SUB(NOW(3), INTERVAL 6 HOUR),2,DATE_SUB(NOW(3), INTERVAL 5 HOUR),2,0),
(300804,1,'OUT-SCN-2026042204','SALES','SHIPPED',300101,300403,DATE_SUB(NOW(3), INTERVAL 1 DAY),DATE_SUB(NOW(3), INTERVAL 1 DAY),'原因:企业团购发运；场景B历史完成单',DATE_SUB(NOW(3), INTERVAL 1 DAY),2,DATE_SUB(NOW(3), INTERVAL 1 DAY),2,0)
ON DUPLICATE KEY UPDATE biz_type=VALUES(biz_type), status=VALUES(status), expected_ship_at=VALUES(expected_ship_at), shipped_at=VALUES(shipped_at), remark=VALUES(remark), created_at=VALUES(created_at), updated_at=VALUES(updated_at), deleted=0;

INSERT INTO outbound_order_items (id, tenant_id, outbound_order_id, line_no, product_id, location_id, unit_id, batch_no, plan_qty, picked_qty, shipped_qty, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(300811,1,300801,1,300501,300201,9102,'B-MILK-240420',30,0,0,DATE_SUB(NOW(3), INTERVAL 2 DAY),2,DATE_SUB(NOW(3), INTERVAL 2 DAY),2,0),
(300812,1,300802,1,300504,300207,9101,NULL,18,0,0,DATE_SUB(NOW(3), INTERVAL 9 HOUR),2,DATE_SUB(NOW(3), INTERVAL 7 HOUR),2,0),
(300813,1,300803,1,300505,300207,9101,NULL,6,0,0,DATE_SUB(NOW(3), INTERVAL 6 HOUR),2,DATE_SUB(NOW(3), INTERVAL 5 HOUR),2,0),
(300814,1,300804,1,300506,300204,9101,NULL,140,140,140,DATE_SUB(NOW(3), INTERVAL 1 DAY),2,DATE_SUB(NOW(3), INTERVAL 1 DAY),2,0)
ON DUPLICATE KEY UPDATE product_id=VALUES(product_id), location_id=VALUES(location_id), plan_qty=VALUES(plan_qty), picked_qty=VALUES(picked_qty), shipped_qty=VALUES(shipped_qty), updated_at=VALUES(updated_at), deleted=0;

-- 库存流水：用于复盘库存变化
INSERT INTO inventory_transactions (id, tenant_id, txn_no, biz_type, biz_no, warehouse_id, location_id, product_id, change_qty, before_qty, after_qty, request_id, operator_id, occurred_at, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(300851,1,'TXN-SCN-001','INBOUND_POST','IN-SCN-2026042204',300101,300204,300506,220,700,920,'REQ-SCN-IN-001',2,DATE_SUB(NOW(3), INTERVAL 1 DAY),NOW(3),2,NOW(3),2,0),
(300852,1,'TXN-SCN-002','OUTBOUND_SHIP','OUT-SCN-2026042204',300101,300204,300506,-140,1060,920,'REQ-SCN-OUT-001',2,DATE_SUB(NOW(3), INTERVAL 1 DAY),NOW(3),2,NOW(3),2,0),
(300853,1,'TXN-SCN-003','OUTBOUND_SHIP','OUT-SCN-2026042203',300102,300207,300505,-6,14,8,'REQ-SCN-OUT-002',2,DATE_SUB(NOW(3), INTERVAL 4 HOUR),NOW(3),2,NOW(3),2,0)
ON DUPLICATE KEY UPDATE biz_type=VALUES(biz_type), biz_no=VALUES(biz_no), change_qty=VALUES(change_qty), before_qty=VALUES(before_qty), after_qty=VALUES(after_qty), operator_id=VALUES(operator_id), occurred_at=VALUES(occurred_at), updated_at=NOW(3), deleted=0;

-- 客服会话 + 工单：类型、优先级、责任人、状态
INSERT INTO customer_service_sessions (id, tenant_id, session_no, channel, customer_id, status, assigned_user_id, priority, first_response_at, closed_at, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(300901,1,'CS-SCN-20260422-001','WEB',300401,'OPEN',4,'HIGH',DATE_SUB(NOW(3), INTERVAL 5 HOUR),NULL,DATE_SUB(NOW(3), INTERVAL 6 HOUR),4,DATE_SUB(NOW(3), INTERVAL 5 HOUR),4,0),
(300902,1,'CS-SCN-20260422-002','WEB',300402,'PROCESSING',4,'MEDIUM',DATE_SUB(NOW(3), INTERVAL 3 HOUR),NULL,DATE_SUB(NOW(3), INTERVAL 4 HOUR),4,DATE_SUB(NOW(3), INTERVAL 3 HOUR),4,0),
(300903,1,'CS-SCN-20260421-001','WEB',300403,'CLOSED',4,'LOW',DATE_SUB(NOW(3), INTERVAL 30 HOUR),DATE_SUB(NOW(3), INTERVAL 24 HOUR),DATE_SUB(NOW(3), INTERVAL 31 HOUR),4,DATE_SUB(NOW(3), INTERVAL 24 HOUR),4,0)
ON DUPLICATE KEY UPDATE status=VALUES(status), assigned_user_id=VALUES(assigned_user_id), priority=VALUES(priority), first_response_at=VALUES(first_response_at), closed_at=VALUES(closed_at), created_at=VALUES(created_at), updated_at=VALUES(updated_at), deleted=0;

INSERT INTO customer_service_messages (id, tenant_id, session_id, sender_type, sender_id, content, msg_type, ai_suggested, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(300911,1,300901,'CUSTOMER',300401,'【物流催发】订单 OUT-SCN-2026042202 能否今天 18:00 前发出？','TEXT',0,DATE_SUB(NOW(3), INTERVAL 6 HOUR),4,NOW(3),4,0),
(300912,1,300901,'AGENT',4,'已升级为高优先级工单，仓库正在安排拣货，预计 17:30 前回传运单号。','TEXT',0,DATE_SUB(NOW(3), INTERVAL 5 HOUR),4,NOW(3),4,0),
(300913,1,300902,'CUSTOMER',300402,'【发票变更】请将抬头改为“成都分公司”，并补充税号。','TEXT',0,DATE_SUB(NOW(3), INTERVAL 4 HOUR),4,NOW(3),4,0),
(300914,1,300902,'AI',4,'建议回复：可变更抬头，请提供最新税号和开票地址，财务将在 2 小时内确认。','AI_SUGGESTION',1,DATE_SUB(NOW(3), INTERVAL 3 HOUR),4,NOW(3),4,0)
ON DUPLICATE KEY UPDATE content=VALUES(content), msg_type=VALUES(msg_type), ai_suggested=VALUES(ai_suggested), updated_at=NOW(3), deleted=0;

INSERT INTO customer_service_tickets (id, tenant_id, ticket_no, session_id, priority, content, status, assignee_user_id, remark, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(300921,1,'TCK-SCN-20260422-001',300901,'HIGH','类型:物流催发；客户要求优先发运并同步物流单号。','OPEN',4,'责任仓库:成都电商快发仓；SLA:4小时',DATE_SUB(NOW(3), INTERVAL 5 HOUR),4,NOW(3),4,0),
(300922,1,'TCK-SCN-20260422-002',300902,'MEDIUM','类型:发票信息变更；待财务确认抬头与税号。','PROCESSING',4,'已转财务队列，预计2小时内反馈',DATE_SUB(NOW(3), INTERVAL 3 HOUR),4,NOW(3),4,0),
(300923,1,'TCK-SCN-20260421-001',300903,'LOW','类型:售后咨询；客户咨询历史优惠券补发。','CLOSED',4,'已短信通知客户，客户确认完结',DATE_SUB(NOW(3), INTERVAL 29 HOUR),4,NOW(3),4,0)
ON DUPLICATE KEY UPDATE priority=VALUES(priority), content=VALUES(content), status=VALUES(status), assignee_user_id=VALUES(assignee_user_id), remark=VALUES(remark), updated_at=NOW(3), deleted=0;

-- FAQ：分类与关键词命中
INSERT INTO faq_knowledge (id, tenant_id, faq_code, question, answer, tags, scene, priority, enabled, version_no, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(300931,1,'FAQ-SCN-001','库存不足导致无法发运怎么办？','先核查可用库存、锁定库存、在途库存；若不足，优先执行加急采购或同城调拨。','库存,缺货,发运,补货','inventory',95,1,1,NOW(3),4,NOW(3),4,0),
(300932,1,'FAQ-SCN-002','入库单过账后库存没有变化怎么办？','确认状态为 APPROVED 后再过账，检查明细库位与数量是否完整，成功后库存流水应同步生成。','入库,过账,库存流水','inbound',92,1,1,NOW(3),4,NOW(3),4,0),
(300933,1,'FAQ-SCN-003','客服建单后如何回查处理进度？','在客服工作台按会话ID或工单号检索，关注 OPEN/PROCESSING/RESOLVED/CLOSED 状态流转。','客服,工单,回查,状态','customer_service',90,1,1,NOW(3),4,NOW(3),4,0),
(300934,1,'FAQ-SCN-004','AI补货建议为什么优先 SKU-GAN-65W？','该SKU已低于安全库存且在途不足，系统按缺口与销量风险综合排序后优先建议补货。','AI,补货建议,安全库存','ai',88,1,1,NOW(3),4,NOW(3),4,0)
ON DUPLICATE KEY UPDATE question=VALUES(question), answer=VALUES(answer), tags=VALUES(tags), scene=VALUES(scene), priority=VALUES(priority), enabled=VALUES(enabled), version_no=VALUES(version_no), updated_at=NOW(3), deleted=0;

-- AI会话：问题类型 + 分析输出样本
INSERT INTO ai_conversations (id, tenant_id, conversation_no, scene, biz_type, biz_no, provider_type, model_name, status, started_at, ended_at, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(300941,1,'AI-SCN-20260422-001','warehouse','INVENTORY','SKU-GAN-65W','rule','rule-v1','ACTIVE',DATE_SUB(NOW(3), INTERVAL 4 HOUR),NULL,DATE_SUB(NOW(3), INTERVAL 4 HOUR),1,NOW(3),1,0),
(300942,1,'AI-SCN-20260422-002','enterprise','REPORT','DASHBOARD-WEEKLY','rule','rule-v1','ACTIVE',DATE_SUB(NOW(3), INTERVAL 2 HOUR),NULL,DATE_SUB(NOW(3), INTERVAL 2 HOUR),1,NOW(3),1,0),
(300943,1,'AI-SCN-20260422-003','sop','CUSTOMER_SERVICE','TCK-SCN-20260422-001','rule','rule-v1','ACTIVE',DATE_SUB(NOW(3), INTERVAL 1 HOUR),NULL,DATE_SUB(NOW(3), INTERVAL 1 HOUR),1,NOW(3),1,0)
ON DUPLICATE KEY UPDATE scene=VALUES(scene), biz_type=VALUES(biz_type), biz_no=VALUES(biz_no), provider_type=VALUES(provider_type), model_name=VALUES(model_name), status=VALUES(status), started_at=VALUES(started_at), ended_at=VALUES(ended_at), updated_at=NOW(3), deleted=0;

INSERT INTO ai_messages (id, tenant_id, conversation_id, role, content, tokens, latency_ms, confidence, error_code, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(300951,1,300941,'USER','请给出 SKU-GAN-65W 的补货建议，并说明依据。',126,230,0.92,NULL,DATE_SUB(NOW(3), INTERVAL 4 HOUR),1,NOW(3),1,0),
(300952,1,300941,'ASSISTANT','结论:SKU-GAN-65W 需在3天内补货80件。依据:当前可用8件，安全库存30件，在途12件，促销单 OUT-SCN-2026042203 待发运6件。建议:今日发起采购并优先入快发仓。',238,312,0.89,NULL,DATE_SUB(NOW(3), INTERVAL 4 HOUR),1,NOW(3),1,0),
(300953,1,300942,'USER','生成本周管理层摘要，包含入库、出库和低库存风险。',118,220,0.90,NULL,DATE_SUB(NOW(3), INTERVAL 2 HOUR),1,NOW(3),1,0),
(300954,1,300942,'ASSISTANT','本周摘要:入库重点为乳品补货与退货回流，出库重点为电商高峰发运。风险:SKU-GAN-65W、SKU-CABLE-C2C 接近阈值。建议:先补货后排促销单，保持低库存SKU不超过2个。',226,298,0.87,NULL,DATE_SUB(NOW(3), INTERVAL 2 HOUR),1,NOW(3),1,0),
(300955,1,300943,'USER','针对工单 TCK-SCN-20260422-001，给客服一个标准处理SOP。',106,210,0.91,NULL,DATE_SUB(NOW(3), INTERVAL 1 HOUR),1,NOW(3),1,0),
(300956,1,300943,'ASSISTANT','SOP:1)核对订单与库存 2)与仓库确认拣货波次 3)回传预计发运时间 4)更新工单备注与责任人 5)发运后同步运单并回访客户。',212,286,0.88,NULL,DATE_SUB(NOW(3), INTERVAL 1 HOUR),1,NOW(3),1,0)
ON DUPLICATE KEY UPDATE content=VALUES(content), tokens=VALUES(tokens), latency_ms=VALUES(latency_ms), confidence=VALUES(confidence), error_code=VALUES(error_code), updated_at=NOW(3), deleted=0;

-- 审计样本：支持审计中心回放
INSERT INTO audit_logs (id, tenant_id, module, action, resource_type, resource_id, biz_no, operator_id, operator_name, before_json, after_json, ip, user_agent, occurred_at, created_at, created_by, updated_at, updated_by, deleted)
VALUES
(300961,1,'WMS_INBOUND','SUBMIT','INBOUND_ORDER','300702','IN-SCN-2026042202',2,'warehouse01','{"status":"DRAFT"}','{"status":"SUBMITTED"}','127.0.0.1','seed-script',DATE_SUB(NOW(3), INTERVAL 20 HOUR),NOW(3),2,NOW(3),2,0),
(300962,1,'WMS_INBOUND','POST','INBOUND_ORDER','300704','IN-SCN-2026042204',2,'warehouse01','{"status":"APPROVED"}','{"status":"POSTED"}','127.0.0.1','seed-script',DATE_SUB(NOW(3), INTERVAL 1 DAY),NOW(3),2,NOW(3),2,0),
(300963,1,'WMS_OUTBOUND','SHIP','OUTBOUND_ORDER','300804','OUT-SCN-2026042204',2,'warehouse01','{"status":"APPROVED"}','{"status":"SHIPPED"}','127.0.0.1','seed-script',DATE_SUB(NOW(3), INTERVAL 1 DAY),NOW(3),2,NOW(3),2,0),
(300964,1,'CS','UPDATE_TICKET_STATUS','CS_TICKET','300922','TCK-SCN-20260422-002',4,'cs01','{"status":"OPEN"}','{"status":"PROCESSING"}','127.0.0.1','seed-script',DATE_SUB(NOW(3), INTERVAL 3 HOUR),NOW(3),4,NOW(3),4,0),
(300965,1,'AI','CHAT','AI_CONVERSATION','300941','AI-SCN-20260422-001',1,'admin',NULL,'{"scene":"warehouse","provider":"rule"}','127.0.0.1','seed-script',DATE_SUB(NOW(3), INTERVAL 4 HOUR),NOW(3),1,NOW(3),1,0)
ON DUPLICATE KEY UPDATE module=VALUES(module), action=VALUES(action), resource_type=VALUES(resource_type), resource_id=VALUES(resource_id), biz_no=VALUES(biz_no), operator_id=VALUES(operator_id), operator_name=VALUES(operator_name), before_json=VALUES(before_json), after_json=VALUES(after_json), ip=VALUES(ip), user_agent=VALUES(user_agent), occurred_at=VALUES(occurred_at), updated_at=NOW(3), deleted=0;
