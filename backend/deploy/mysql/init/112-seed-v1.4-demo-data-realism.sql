SET NAMES utf8mb4;

-- =========================
-- v1.4 Demo Data Realism
-- Final DML-only baseline normalization. Keep existing ids and flows stable.
-- =========================

UPDATE tenants
SET tenant_name = 'NovaDepot 演示租户',
    updated_at = NOW(3)
WHERE id = 1;

UPDATE roles
SET role_name = CASE role_code
  WHEN 'TENANT_ADMIN' THEN '系统管理员'
  WHEN 'WAREHOUSE_MANAGER' THEN '仓储主管'
  WHEN 'WAREHOUSE_OPERATOR' THEN '仓储操作员'
  WHEN 'CS_AGENT' THEN '客服专员'
  WHEN 'DATA_VIEWER' THEN '只读观察员'
  ELSE role_name END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE users
SET real_name = CASE username
  WHEN 'admin' THEN '演示管理员'
  WHEN 'warehouse01' THEN '仓储运营-小林'
  WHEN 'operator' THEN '仓储操作-小周'
  WHEN 'cs01' THEN '客服运营-小陈'
  WHEN 'observer01' THEN '数据观察员-小何'
  ELSE real_name END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

INSERT INTO product_categories (id, tenant_id, parent_id, category_code, category_name, sort_no, status, created_at, updated_at, deleted) VALUES
(9001,1,NULL,'CAT-OFF','办公耗材',1,'ACTIVE',NOW(3),NOW(3),0),
(9002,1,NULL,'CAT-IT','电脑配件',2,'ACTIVE',NOW(3),NOW(3),0),
(9003,1,NULL,'CAT-CLEAN','清洁用品',3,'ACTIVE',NOW(3),NOW(3),0),
(9004,1,NULL,'CAT-PACK','包装材料',4,'ACTIVE',NOW(3),NOW(3),0),
(9005,1,NULL,'CAT-STORAGE','仓储用品',5,'ACTIVE',NOW(3),NOW(3),0),
(9006,1,NULL,'CAT-DAILY','生活日用品',6,'ACTIVE',NOW(3),NOW(3),0)
ON DUPLICATE KEY UPDATE category_code=VALUES(category_code), category_name=VALUES(category_name), sort_no=VALUES(sort_no), status=VALUES(status), deleted=0, updated_at=NOW(3);

UPDATE product_units
SET unit_name = CASE unit_code
  WHEN 'PCS' THEN '件'
  WHEN 'BOX' THEN '箱'
  WHEN 'UNIT-PCS' THEN '件'
  ELSE unit_name END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE warehouses
SET warehouse_code = CASE id
  WHEN 11001 THEN 'WH-GZ-CENTER'
  WHEN 11002 THEN 'WH-SZ-FRONT'
  WHEN 11003 THEN 'WH-FS-RESERVE'
  WHEN 300101 THEN 'WH-GZ-CENTER-BIZ'
  WHEN 300102 THEN 'WH-SZ-FRONT-BIZ'
  WHEN 300103 THEN 'WH-FS-RESERVE-BIZ'
  ELSE warehouse_code END,
  warehouse_name = CASE id
  WHEN 11001 THEN '广州中心仓'
  WHEN 11002 THEN '深圳前置仓'
  WHEN 11003 THEN '佛山备货仓'
  WHEN 300101 THEN '广州中心仓'
  WHEN 300102 THEN '深圳前置仓'
  WHEN 300103 THEN '佛山备货仓'
  ELSE warehouse_name END,
  address = CASE id
  WHEN 11001 THEN '广州市黄埔区云埔物流园 18 号'
  WHEN 11002 THEN '深圳市南山区前海仓配中心 6 号'
  WHEN 11003 THEN '佛山市禅城区季华仓储园 3 号'
  WHEN 300101 THEN '广州市黄埔区云埔物流园 18 号'
  WHEN 300102 THEN '深圳市南山区前海仓配中心 6 号'
  WHEN 300103 THEN '佛山市禅城区季华仓储园 3 号'
  ELSE address END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE warehouse_locations
SET location_code = CASE
  WHEN id = 12001 THEN 'A区-01-01'
  WHEN id = 12002 THEN 'A区-01-02'
  WHEN id = 12003 THEN 'B区-02-01'
  WHEN id = 12004 THEN 'C区-03-01'
  WHEN id = 12033 THEN '退货暂存区'
  WHEN id = 12017 THEN '低库存补货区'
  WHEN id = 300201 THEN 'A区-01-01-BIZ'
  WHEN id = 300202 THEN 'A区-01-02-BIZ'
  WHEN id = 300203 THEN 'B区-02-01-BIZ'
  WHEN id = 300204 THEN 'C区-03-01-BIZ'
  WHEN id = 300209 THEN '退货暂存区-BIZ'
  WHEN id = 300207 THEN '低库存补货区-BIZ'
  WHEN id = 300208 THEN 'B区-02-01-BIZ'
  WHEN id = 300205 THEN 'C区-03-01-暂存'
  WHEN id = 300206 THEN '退货暂存区-复检'
  ELSE CONCAT(
    CASE warehouse_id WHEN 11001 THEN 'GZ' WHEN 11002 THEN 'SZ' WHEN 11003 THEN 'FS' ELSE 'LOC' END,
    '-',
    LPAD(((id - 12001) % 16) + 1, 2, '0')
  ) END,
  location_name = CASE
  WHEN id IN (12001,300201) THEN '广州中心仓 A区-01-01 拣选位'
  WHEN id IN (12002,300202) THEN '广州中心仓 A区-01-02 拣选位'
  WHEN id IN (12003,300203) THEN '广州中心仓 B区-02-01 存储位'
  WHEN id IN (12004,300204) THEN '广州中心仓 C区-03-01 整箱位'
  WHEN id IN (12033,300209) THEN '佛山备货仓 退货暂存区'
  WHEN id IN (12017,300207) THEN '深圳前置仓 低库存补货区'
  WHEN id = 300208 THEN '深圳前置仓 B区-02-01 存储位'
  WHEN id = 300205 THEN '广州中心仓 C区-03-01 暂存位'
  WHEN id = 300206 THEN '广州中心仓 退货暂存区'
  ELSE CONCAT(COALESCE((SELECT w.warehouse_name FROM warehouses w WHERE w.id = warehouse_locations.warehouse_id), '仓库'), ' 标准库位 ', LPAD(((id - 12001) % 16) + 1, 2, '0')) END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

INSERT INTO suppliers (id, tenant_id, supplier_code, supplier_name, contact_name, phone, credit_level, status, created_at, updated_at, deleted) VALUES
(22001,1,'SUP-GZ-MG','广州晨光文具有限公司','刘敏','13900010001','A','ACTIVE',NOW(3),NOW(3),0),
(22002,1,'SUP-SZ-DIGI','深圳数码配件供应链有限公司','王磊','13900010002','A','ACTIVE',NOW(3),NOW(3),0),
(22003,1,'SUP-FS-OFFICE','佛山办公耗材批发中心','陈林','13900010003','B','ACTIVE',NOW(3),NOW(3),0),
(22004,1,'SUP-DG-PACK','东莞包装材料有限公司','周琪','13900010004','B','ACTIVE',NOW(3),NOW(3),0),
(300301,1,'SUP-GZ-MG-BIZ','广州晨光文具有限公司','刘敏','13900010001','A','ACTIVE',NOW(3),NOW(3),0),
(300302,1,'SUP-SZ-DIGI-BIZ','深圳数码配件供应链有限公司','王磊','13900010002','A','ACTIVE',NOW(3),NOW(3),0),
(300303,1,'SUP-FS-OFFICE-BIZ','佛山办公耗材批发中心','陈林','13900010003','B','ACTIVE',NOW(3),NOW(3),0)
ON DUPLICATE KEY UPDATE supplier_code=VALUES(supplier_code), supplier_name=VALUES(supplier_name), contact_name=VALUES(contact_name), phone=VALUES(phone), credit_level=VALUES(credit_level), status=VALUES(status), deleted=0, updated_at=NOW(3);

INSERT INTO customers (id, tenant_id, customer_code, customer_name, contact_name, phone, customer_level, status, created_at, updated_at, deleted) VALUES
(22001,1,'CUS-GZ-TH','广州天河门店','赵宁','13600020001','A','ACTIVE',NOW(3),NOW(3),0),
(22002,1,'CUS-SZ-NS','深圳南山门店','唐雪','13600020002','A','ACTIVE',NOW(3),NOW(3),0),
(22003,1,'CUS-FS-CC','佛山禅城门店','宋岩','13600020003','B','ACTIVE',NOW(3),NOW(3),0),
(22004,1,'CUS-ZH-XZ','珠海香洲门店','何静','13600020004','B','ACTIVE',NOW(3),NOW(3),0),
(22005,1,'CUS-DG-SSL','东莞松山湖门店','罗军','13600020005','A','ACTIVE',NOW(3),NOW(3),0),
(300401,1,'CUS-GZ-TH-BIZ','广州天河门店','赵宁','13600020001','A','ACTIVE',NOW(3),NOW(3),0),
(300402,1,'CUS-SZ-NS-BIZ','深圳南山门店','唐雪','13600020002','A','ACTIVE',NOW(3),NOW(3),0),
(300403,1,'CUS-FS-CC-BIZ','佛山禅城门店','宋岩','13600020003','B','ACTIVE',NOW(3),NOW(3),0)
ON DUPLICATE KEY UPDATE customer_code=VALUES(customer_code), customer_name=VALUES(customer_name), contact_name=VALUES(contact_name), phone=VALUES(phone), customer_level=VALUES(customer_level), status=VALUES(status), deleted=0, updated_at=NOW(3);

INSERT INTO partners (id, tenant_id, partner_code, partner_name, partner_type, contact_name, phone, address, status, remark, created_at, created_by, updated_at, updated_by, deleted) VALUES
(310001,1,'PT-SUP-GZ-MG','广州晨光文具有限公司','SUPPLIER','刘敏','13900010001','广州市白云区文具供应链园区 8 号','ACTIVE','办公耗材采购供应商',NOW(3),1,NOW(3),1,0),
(310002,1,'PT-CUS-GZ-TH','广州天河门店','CUSTOMER','赵宁','13600020001','广州市天河区体育西路 88 号','ACTIVE','办公用品门店销售客户',NOW(3),1,NOW(3),1,0),
(310003,1,'PT-BOTH-SZ-DIGI','深圳数码配件供应链有限公司','BOTH','王磊','13900010002','深圳市南山区科技园仓配中心 9 号','ACTIVE','电脑配件采购与门店调拨双向单位',NOW(3),1,NOW(3),1,0),
(310004,1,'PT-SUP-DG-PACK','东莞包装材料有限公司','SUPPLIER','周琪','13900010004','东莞市松山湖包装材料园 12 号','ACTIVE','包装材料采购供应商',NOW(3),1,NOW(3),1,0),
(310005,1,'PT-CUS-SZ-NS','深圳南山门店','CUSTOMER','唐雪','13600020002','深圳市南山区海岸城商圈 16 号','ACTIVE','门店销售客户',NOW(3),1,NOW(3),1,0)
ON DUPLICATE KEY UPDATE partner_code=VALUES(partner_code), partner_name=VALUES(partner_name), partner_type=VALUES(partner_type), contact_name=VALUES(contact_name), phone=VALUES(phone), address=VALUES(address), status=VALUES(status), remark=VALUES(remark), deleted=0, updated_at=NOW(3);

UPDATE products
SET product_code = CASE id
  WHEN 13001 THEN 'SKU-OFF-A4-001'
  WHEN 13002 THEN 'SKU-OFF-PEN-001'
  WHEN 13003 THEN 'SKU-IT-MOUSE-001'
  WHEN 13004 THEN 'SKU-OFF-POWER-001'
  WHEN 13005 THEN 'SKU-DAILY-BATTERY-001'
  WHEN 13006 THEN 'SKU-CLEAN-HAND-001'
  WHEN 13007 THEN 'SKU-DAILY-TISSUE-001'
  WHEN 13008 THEN 'SKU-IT-CHARGER-001'
  WHEN 13009 THEN 'SKU-IT-USB-001'
  WHEN 13010 THEN 'SKU-STORAGE-BOX-001'
  WHEN 13011 THEN 'SKU-OFF-TONER-001'
  WHEN 13012 THEN 'SKU-IT-DOCK-001'
  ELSE CONCAT('SKU-OFF-REPLENISH-', LPAD(id - 13000, 3, '0')) END,
  product_name = CASE id
  WHEN 13001 THEN '得力 A4 复印纸 70g'
  WHEN 13002 THEN '晨光黑色中性笔 0.5mm'
  WHEN 13003 THEN '罗技 M220 无线鼠标'
  WHEN 13004 THEN '公牛六位插线板 3米'
  WHEN 13005 THEN '南孚 5号电池 24粒装'
  WHEN 13006 THEN '蓝月亮洗手液 500ml'
  WHEN 13007 THEN '维达抽纸 3层 100抽'
  WHEN 13008 THEN '小米 20W 快充头'
  WHEN 13009 THEN '闪迪 64GB U盘'
  WHEN 13010 THEN '京东京造收纳箱 45L'
  WHEN 13011 THEN '佳能 CRG-337 硒鼓'
  WHEN 13012 THEN '联想 USB-C 扩展坞'
  ELSE CONCAT('门店办公补货商品 ', LPAD(id - 13000, 3, '0')) END,
  category_id = CASE
  WHEN id IN (13001,13002,13004,13011) THEN 9001
  WHEN id IN (13003,13008,13009,13012) THEN 9002
  WHEN id = 13006 THEN 9003
  WHEN id = 13010 THEN 9005
  WHEN id IN (13005,13007) THEN 9006
  ELSE 9001 END,
  spec = CASE id
  WHEN 13001 THEN '500张/包；安全库存=50；门店日常复印耗材'
  WHEN 13002 THEN '12支/盒；安全库存=120；收银台与办公区常备'
  WHEN 13003 THEN '静音无线鼠标；安全库存=20；电脑配件补货'
  WHEN 13004 THEN '六位总控 3米；安全库存=18；门店设备用电'
  WHEN 13005 THEN '24粒/盒；安全库存=30；设备电池备用'
  WHEN 13006 THEN '500ml/瓶；安全库存=40；门店清洁用品'
  WHEN 13007 THEN '3层100抽/包；安全库存=80；生活日用品'
  WHEN 13008 THEN '20W USB-C；安全库存=25；数码配件销售'
  WHEN 13009 THEN 'USB 3.0 64GB；安全库存=22；电脑配件销售'
  WHEN 13010 THEN '45L 带盖；安全库存=16；仓储整理用品'
  WHEN 13011 THEN '适配佳能 MF 系列；安全库存=12；打印耗材'
  WHEN 13012 THEN 'USB-C 多口扩展；安全库存=10；办公电脑配件'
  ELSE CONCAT('门店常规补货；安全库存=', CASE WHEN (id - 13000) % 5 = 0 THEN 10 ELSE 30 END) END,
  updated_at = NOW(3)
WHERE tenant_id = 1 AND id BETWEEN 13001 AND 13060;

UPDATE products
SET product_code = CASE id
  WHEN 300501 THEN 'SKU-BIZ-OFF-A4-001'
  WHEN 300502 THEN 'SKU-BIZ-OFF-PEN-001'
  WHEN 300503 THEN 'SKU-BIZ-CLEAN-HAND-001'
  WHEN 300504 THEN 'SKU-BIZ-DAILY-TISSUE-001'
  WHEN 300505 THEN 'SKU-BIZ-IT-CHARGER-001'
  WHEN 300506 THEN 'SKU-BIZ-OFF-TONER-001'
  WHEN 300507 THEN 'SKU-BIZ-IT-USB-001'
  WHEN 300508 THEN 'SKU-BIZ-STORAGE-BOX-001'
  ELSE product_code END,
  product_name = CASE id
  WHEN 300501 THEN '得力 A4 复印纸 70g'
  WHEN 300502 THEN '晨光黑色中性笔 0.5mm'
  WHEN 300503 THEN '蓝月亮洗手液 500ml'
  WHEN 300504 THEN '维达抽纸 3层 100抽'
  WHEN 300505 THEN '小米 20W 快充头'
  WHEN 300506 THEN '佳能 CRG-337 硒鼓'
  WHEN 300507 THEN '闪迪 64GB U盘'
  WHEN 300508 THEN '京东京造收纳箱 45L'
  ELSE product_name END,
  category_id = CASE id
  WHEN 300501 THEN 9001
  WHEN 300502 THEN 9001
  WHEN 300503 THEN 9003
  WHEN 300504 THEN 9006
  WHEN 300505 THEN 9002
  WHEN 300506 THEN 9001
  WHEN 300507 THEN 9002
  WHEN 300508 THEN 9005
  ELSE category_id END,
  spec = CASE id
  WHEN 300501 THEN '500张/包；安全库存=120；采购到货与销售出库演示'
  WHEN 300502 THEN '12支/盒；安全库存=80；门店办公耗材补货'
  WHEN 300503 THEN '500ml/瓶；安全库存=40；清洁用品低库存预警'
  WHEN 300504 THEN '3层100抽/包；安全库存=60；门店高频日用品'
  WHEN 300505 THEN '20W 快充；安全库存=30；低库存补货样例'
  WHEN 300506 THEN 'CRG-337 黑色；安全库存=200；库存充足样例'
  WHEN 300507 THEN '64GB USB 3.0；安全库存=50；配件补货样例'
  WHEN 300508 THEN '45L 带盖；安全库存=80；退货暂存整理样例'
  ELSE spec END,
  updated_at = NOW(3)
WHERE tenant_id = 1 AND id BETWEEN 300501 AND 300508;

UPDATE products
SET product_code = CONCAT('SKU-BATCH-OFF-', LPAD(id - 390000, 3, '0')),
    product_name = CONCAT(
      CASE
        WHEN (id - 390000) % 4 = 1 THEN '门店补货文件夹 '
        WHEN (id - 390000) % 4 = 2 THEN '门店补货便利贴 '
        WHEN (id - 390000) % 4 = 3 THEN '门店补货标签纸 '
        ELSE '门店补货打包胶带 '
      END,
      LPAD(id - 390000, 3, '0')
    ),
    spec = CONCAT('批量补货清单；安全库存=', CASE WHEN (id - 390000) % 10 = 0 THEN 5 WHEN (id - 390000) % 10 <= 2 THEN 10 ELSE 20 END),
    updated_at = NOW(3)
WHERE tenant_id = 1 AND id BETWEEN 390001 AND 390060;

UPDATE inbound_orders
SET inbound_no = CASE id
  WHEN 300701 THEN 'IN-202606-001'
  WHEN 300702 THEN 'IN-202606-002'
  WHEN 300703 THEN 'IN-202606-003'
  WHEN 300704 THEN 'IN-202606-004'
  ELSE inbound_no END,
  remark = CASE id
  WHEN 300701 THEN '向广州晨光文具有限公司采购办公耗材，待提交入库。'
  WHEN 300702 THEN '深圳前置仓清洁用品补货，已提交待审核。'
  WHEN 300703 THEN '门店退货暂存入库，已审核待过账。'
  WHEN 300704 THEN '佳能硒鼓常规采购入库，已完成过账。'
  ELSE CONCAT('演示入库单 ', inbound_no, '，用于采购到货入库流程。') END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE outbound_orders
SET outbound_no = CASE id
  WHEN 300801 THEN 'OUT-202606-001'
  WHEN 300802 THEN 'OUT-202606-002'
  WHEN 300803 THEN 'OUT-202606-003'
  WHEN 300804 THEN 'OUT-202606-004'
  ELSE outbound_no END,
  remark = CASE id
  WHEN 300801 THEN '向广州天河门店销售办公耗材，待提交出库。'
  WHEN 300802 THEN '深圳南山门店日用品补货，已提交待审核。'
  WHEN 300803 THEN '小米快充头低库存发运校验样例。'
  WHEN 300804 THEN '佛山禅城门店打印耗材销售出库，已发运。'
  ELSE CONCAT('演示出库单 ', outbound_no, '，用于门店销售出库流程。') END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE purchase_orders
SET purchase_no = CASE id
  WHEN 320001 THEN 'PO-202606-001'
  WHEN 320002 THEN 'PO-202606-002'
  WHEN 320003 THEN 'PO-202606-003'
  ELSE purchase_no END,
  remark = CASE id
  WHEN 320001 THEN '向广州晨光文具有限公司采购 A4 复印纸和中性笔，草稿待确认。'
  WHEN 320002 THEN '向深圳数码配件供应链有限公司采购电脑配件，已确认待入库。'
  WHEN 320003 THEN '佛山办公耗材补货采购，部分到货。'
  ELSE remark END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE sales_orders
SET sales_no = CASE id
  WHEN 330001 THEN 'SO-202606-001'
  WHEN 330002 THEN 'SO-202606-002'
  WHEN 330003 THEN 'SO-202606-003'
  WHEN 330004 THEN 'SO-202606-004'
  ELSE sales_no END,
  remark = CASE id
  WHEN 330001 THEN '向广州天河门店销售办公用品，草稿待确认。'
  WHEN 330002 THEN '向深圳南山门店销售电脑配件，已确认待出库。'
  WHEN 330003 THEN '库存不足拦截样例：小米快充头需求量超过可用库存。'
  WHEN 330004 THEN '向佛山禅城门店销售 A4 复印纸，供 UI 转出库演示。'
  ELSE remark END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE payables
SET payable_no = CASE id
  WHEN 360001 THEN 'AP-202606-001'
  WHEN 360002 THEN 'AP-202606-002'
  ELSE payable_no END,
  source_order_no = CASE source_order_id
  WHEN 320002 THEN 'PO-202606-002'
  WHEN 320003 THEN 'PO-202606-003'
  ELSE source_order_no END,
  remark = CASE id
  WHEN 360001 THEN '深圳数码配件采购应付，未付款。'
  WHEN 360002 THEN '佛山办公耗材采购应付，已部分付款。'
  ELSE remark END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE receivables
SET receivable_no = CASE id
  WHEN 360101 THEN 'AR-202606-001'
  WHEN 360102 THEN 'AR-202606-002'
  ELSE receivable_no END,
  source_order_no = CASE source_order_id
  WHEN 330002 THEN 'SO-202606-002'
  WHEN 330004 THEN 'SO-202606-004'
  ELSE source_order_no END,
  remark = CASE id
  WHEN 360101 THEN '深圳南山门店销售应收，未收款。'
  WHEN 360102 THEN '佛山禅城门店销售应收，已部分收款。'
  ELSE remark END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE payments
SET payment_no = CASE id
  WHEN 360201 THEN 'PAY-202606-AP-001'
  WHEN 360202 THEN 'PAY-202606-AR-001'
  ELSE payment_no END,
  ledger_no = CASE ledger_id
  WHEN 360002 THEN 'AP-202606-002'
  WHEN 360102 THEN 'AR-202606-002'
  ELSE ledger_no END,
  remark = CASE direction
  WHEN 'PAYABLE' THEN '办公耗材采购付款登记。'
  WHEN 'RECEIVABLE' THEN '门店销售收款登记。'
  ELSE remark END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE stocktake_orders
SET stocktake_no = CASE id
  WHEN 360301 THEN 'ST-202606-001'
  WHEN 360302 THEN 'ST-202606-002'
  WHEN 360303 THEN 'ST-202606-003'
  ELSE stocktake_no END,
  remark = CASE id
  WHEN 360301 THEN '广州中心仓月度盘点草稿。'
  WHEN 360302 THEN '广州中心仓办公耗材盘点中。'
  WHEN 360303 THEN '广州中心仓 A4 复印纸盘点差异复核。'
  ELSE remark END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE customer_service_messages
SET content = CASE
  WHEN session_id IN (17001,300901) AND sender_type = 'CUSTOMER' THEN '商品缺货咨询：广州天河门店反馈晨光黑色中性笔库存不足，询问补货时间。'
  WHEN session_id IN (17001,300901) THEN '已查询库存与待入库单，建议优先安排广州中心仓补货并同步预计到货时间。'
  WHEN session_id IN (17002,300902) AND sender_type = 'CUSTOMER' THEN '发货延迟咨询：深圳南山门店询问销售单 SO-202606-002 今天能否发运。'
  WHEN session_id IN (17002,300902) THEN '已升级仓储确认拣货波次，预计 17:30 前反馈运单号。'
  WHEN session_id IN (17003,300903) AND sender_type = 'CUSTOMER' THEN '入库数量不一致：供应商到货的 A4 复印纸少于采购单数量。'
  WHEN session_id IN (17003,300903) THEN '请按采购到货入库流程记录实收数量，差异留在采购单明细中复核。'
  ELSE '客服对话：围绕门店补货、发货延迟、入库差异、退货暂存或发票信息进行处理。' END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE customer_service_tickets
SET ticket_no = CASE id
  WHEN 17201 THEN 'TK-202606-001'
  WHEN 17202 THEN 'TK-202606-002'
  WHEN 17203 THEN 'TK-202606-003'
  WHEN 17204 THEN 'TK-202606-004'
  WHEN 17205 THEN 'TK-202606-005'
  WHEN 300921 THEN 'TK-202606-101'
  WHEN 300922 THEN 'TK-202606-102'
  WHEN 300923 THEN 'TK-202606-103'
  ELSE ticket_no END,
  content = CASE
  WHEN id IN (17201,300921) THEN '商品缺货咨询：晨光黑色中性笔库存不足，门店要求确认补货时间。'
  WHEN id IN (17202,300922) THEN '发货延迟咨询：深圳南山门店订单等待发运，需要同步预计运单。'
  WHEN id = 17203 THEN '入库数量不一致：A4 复印纸实收到货数量少于采购单。'
  WHEN id = 17204 THEN '退货暂存处理：门店退回收纳箱需进入退货暂存区复核。'
  WHEN id = 17205 THEN '发票信息确认：客户要求核对销售单抬头和税号。'
  WHEN id = 300923 THEN '退货暂存处理：客户咨询退回商品复核进度。'
  ELSE '客服工单：门店补货、发货、入库差异、退货暂存或发票信息处理。' END,
  remark = CASE
  WHEN id IN (300921,17201) THEN '责任仓库：广州中心仓；SLA：4小时内反馈补货计划。'
  WHEN id IN (300922,17202) THEN '责任仓库：深圳前置仓；SLA：2小时内反馈运单。'
  WHEN id = 300923 THEN '退货暂存区已登记，待仓储复核。'
  ELSE remark END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE faq_knowledge
SET question = CASE
  WHEN id IN (6001,300931,300501) THEN '如何查询当前库存？'
  WHEN id IN (6002,300502) THEN '库存不足时如何处理？'
  WHEN id IN (6003,300932) THEN '入库数量和采购单不一致怎么办？'
  WHEN id IN (6004,300933) THEN '出库发运失败如何排查？'
  WHEN id IN (6005,300503) THEN '客服建单后如何回查处理进度？'
  ELSE CONCAT('门店补货常见问题 ', LPAD(id, 6, '0')) END,
  answer = CASE
  WHEN id IN (6001,300931,300501) THEN '进入库存列表，按商品编码、仓库或库位筛选，核对可用库存、锁定库存和在途库存。'
  WHEN id IN (6002,300502) THEN '先确认安全库存和待入库单，再选择采购补货、同城调拨或延迟发运，并在工单中同步客户。'
  WHEN id IN (6003,300932) THEN '按实收数量过账，保留采购单未收数量，备注差异原因并通知供应商补发或冲减。'
  WHEN id IN (6004,300933) THEN '检查出库单状态、库位库存、锁定数量和权限；库存不足时系统会阻止发运并提示差额。'
  WHEN id IN (6005,300503) THEN '在客服工单列表按工单号、客户或状态检索，关注 OPEN、PROCESSING、RESOLVED、CLOSED 流转。'
  ELSE '按标准业务流程核对单据、库存和责任人，必要时创建工单并记录处理结论。' END,
  tags = '库存,入库,出库,客服,演示数据',
  scene = CASE WHEN id IN (6005,300503) THEN 'customer-service' ELSE 'inventory' END,
  review_status = 'APPROVED',
  updated_at = NOW(3)
WHERE tenant_id = 1;

INSERT INTO sop_knowledge (id, tenant_id, sop_code, title, scene, steps, risks, review_checks, tags, priority, enabled, review_status, source_type, source_ref_id, created_at, created_by, updated_at, updated_by, deleted) VALUES
(300601,1,'SOP-DEMO-INBOUND-001','采购到货入库处理流程','inbound','1. 核对采购单和供应商；2. 选择广州中心仓或深圳前置仓库位；3. 录入实收和合格数量；4. 提交审核；5. 审核通过后过账并生成库存流水。','未核对采购单会造成数量差异；库位错误会影响后续拣货。','采购单号、供应商、库位、实收数量、库存流水均已核对。','采购,入库,库存流水',100,1,'APPROVED','DEMO_DATA_REALISM',NULL,NOW(3),1,NOW(3),1,0),
(300602,1,'SOP-DEMO-OUTBOUND-001','门店销售出库处理流程','outbound','1. 核对销售单和客户门店；2. 检查可用库存；3. 生成出库草稿；4. 提交并审核；5. 发运后回写销售单已发数量。','库存不足时不能强行发运；客户和库位选错会导致配送异常。','销售单号、客户、库位库存、发运状态和销售单状态均已核对。','销售,出库,门店补货',95,1,'APPROVED','DEMO_DATA_REALISM',NULL,NOW(3),1,NOW(3),1,0),
(300603,1,'SOP-DEMO-LOW-STOCK-001','低库存补货处理流程','inventory','1. 查看低库存预警；2. 核对安全库存、在途库存和待出库数量；3. 判断采购补货或仓间调拨；4. 记录补货计划；5. 跟进到货入库。','忽略在途库存会重复采购；未记录计划会造成客服无法回复客户。','低库存商品、缺口数量、补货动作和责任人均已记录。','低库存,补货,采购',90,1,'APPROVED','DEMO_DATA_REALISM',NULL,NOW(3),1,NOW(3),1,0),
(300604,1,'SOP-DEMO-TICKET-001','客服工单升级处理流程','customer-service','1. 确认客户门店和问题类型；2. 查询库存或订单证据；3. 建立或更新工单；4. 指派责任人；5. 按 SLA 回访并关闭。','缺少数据证据会导致误回复；未设置责任人会造成超时。','工单号、客户、证据、责任人、下一次反馈时间均已填写。','客服,工单,SLA',85,1,'APPROVED','DEMO_DATA_REALISM',NULL,NOW(3),1,NOW(3),1,0)
ON DUPLICATE KEY UPDATE title=VALUES(title), scene=VALUES(scene), steps=VALUES(steps), risks=VALUES(risks), review_checks=VALUES(review_checks), tags=VALUES(tags), priority=VALUES(priority), enabled=VALUES(enabled), review_status=VALUES(review_status), source_type=VALUES(source_type), source_ref_id=VALUES(source_ref_id), deleted=0, updated_at=NOW(3);

UPDATE rule_configs
SET config_name = CASE config_key
  WHEN 'LOW_STOCK_DEFAULT_THRESHOLD' THEN '低库存默认阈值'
  WHEN 'AUTO_REPLY_PRIORITY' THEN '客服自动回复优先级'
  WHEN 'TICKET_CATEGORY_KEYWORDS' THEN '工单分类关键词'
  WHEN 'CS_REPLY_CANDIDATE_PRIORITY' THEN '客服候选回复优先级'
  WHEN 'AGENT_RESULT_DISPLAY_THRESHOLD' THEN 'Agent 结果展示阈值'
  ELSE config_name END,
  config_value = CASE config_key
  WHEN 'TICKET_CATEGORY_KEYWORDS' THEN '商品缺货=缺货,库存不足,补货;发货延迟=发货,物流,运单;入库差异=入库,少到,多到;退货暂存=退货,暂存,复核;发票确认=发票,抬头,税号'
  WHEN 'CS_REPLY_CANDIDATE_PRIORITY' THEN 'FAQ答案,SOP下一步,标准安抚话术'
  ELSE config_value END,
  remark = CASE config_key
  WHEN 'LOW_STOCK_DEFAULT_THRESHOLD' THEN '商品规格未配置安全库存时使用。'
  WHEN 'AUTO_REPLY_PRIORITY' THEN '客服 AI 回复先引用 FAQ，再引用 SOP，最后使用规则提供者。'
  ELSE remark END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE ai_conversations
SET conversation_no = CASE id
  WHEN 300941 THEN 'AI-202606-001'
  WHEN 300942 THEN 'AI-202606-002'
  WHEN 300943 THEN 'AI-202606-003'
  ELSE CONCAT('AI-202606-HIST-', LPAD(id - 18000, 3, '0')) END,
  biz_no = CASE id
  WHEN 300941 THEN 'SKU-BIZ-IT-CHARGER-001'
  WHEN 300942 THEN 'REPORT-202606-WEEKLY'
  WHEN 300943 THEN 'TK-202606-101'
  WHEN biz_type = 'INVENTORY' THEN 'SKU-OFF-A4-001'
  WHEN biz_type = 'CUSTOMER_SERVICE' THEN 'TK-202606-001'
  WHEN biz_type = 'REPORT' THEN 'REPORT-202606-DAILY'
  ELSE biz_no END,
  status = CASE WHEN status = 'ARCHIVED' THEN 'ARCHIVED' ELSE 'ACTIVE' END,
  last_active_at = COALESCE(last_active_at, updated_at, started_at, NOW(3)),
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE ai_messages
SET content = CASE
  WHEN role = 'USER' AND conversation_id IN (18002,300941) THEN '请查询小米 20W 快充头当前库存，并给出补货建议。'
  WHEN role = 'ASSISTANT' AND conversation_id IN (18002,300941) THEN '当前结论：小米 20W 快充头在深圳前置仓可用库存偏低。主要风险：待发销售单会继续消耗库存。建议动作：向深圳数码配件供应链有限公司发起补货，并优先补入低库存补货区。数据依据：商品 SKU-BIZ-IT-CHARGER-001，安全库存 30 件，当前可用 8 件，在途 12 件。下一步：创建采购单并跟进入库。'
  WHEN role = 'USER' AND conversation_id IN (18003,300943) THEN '请根据工单 TK-202606-101 给客服一份处理步骤。'
  WHEN role = 'ASSISTANT' AND conversation_id IN (18003,300943) THEN '当前结论：该工单属于商品缺货咨询。建议动作：先核对库存和待入库单，再向客户说明预计补货时间。数据依据：客户为广州天河门店，问题商品为晨光黑色中性笔 0.5mm。下一步：指派仓储运营确认补货计划并在工单备注中记录。'
  WHEN role = 'USER' THEN '请汇总本周库存、订单和客服风险。'
  ELSE '当前结论：演示环境存在低库存、待发订单和客服跟进事项。主要风险：快充头和中性笔补货不及时会影响门店发运。建议动作：优先处理低库存补货和延迟发货工单。数据依据：库存、采购单、销售单和客服工单均来自当前基线数据。下一步：在报表和工单页面复核责任人。' END,
  status = COALESCE(status, 'COMPLETED'),
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE audit_logs
SET biz_no = CASE biz_no
  WHEN 'IN-SCN-2026042202' THEN 'IN-202606-002'
  WHEN 'IN-SCN-2026042204' THEN 'IN-202606-004'
  WHEN 'OUT-SCN-2026042204' THEN 'OUT-202606-004'
  WHEN 'TCK-SCN-20260422-002' THEN 'TK-202606-102'
  WHEN 'AI-SCN-20260422-001' THEN 'AI-202606-001'
  ELSE biz_no END,
  updated_at = NOW(3)
WHERE tenant_id = 1;
