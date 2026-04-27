SET NAMES utf8mb4;

-- =========================
-- Phase 6.1 文本完整性与中文语义编码修复（可重复执行）
-- =========================

UPDATE warehouses
SET warehouse_code = CASE id
    WHEN 11001 THEN '华东-成品总仓'
    WHEN 11002 THEN '华东-电商快发仓'
    WHEN 11003 THEN '华南-退货处理仓'
    WHEN 300101 THEN '成都-成品一号仓'
    WHEN 300102 THEN '成都-电商快发仓'
    WHEN 300103 THEN '成都-退货处理仓'
    ELSE warehouse_code END,
    warehouse_name = CASE id
    WHEN 11001 THEN '华东成品总仓'
    WHEN 11002 THEN '华东电商快发仓'
    WHEN 11003 THEN '华南退货处理仓'
    WHEN 300101 THEN '成都一号成品仓'
    WHEN 300102 THEN '成都电商快发仓'
    WHEN 300103 THEN '成都退货处理仓'
    ELSE warehouse_name END,
    address = CASE id
    WHEN 11001 THEN '上海市浦东新区物流中转园 88 号'
    WHEN 11002 THEN '杭州市滨江区电商物流中心 18 号'
    WHEN 11003 THEN '深圳市宝安区退货处理中心 66 号'
    WHEN 300101 THEN '成都市双流区物流大道 99 号'
    WHEN 300102 THEN '成都市温江区创新路 88 号'
    WHEN 300103 THEN '成都市新都区回流街 16 号'
    ELSE address END,
    updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE warehouse_locations
SET location_code = CASE id
    WHEN 300201 THEN '成品A-拣选01'
    WHEN 300202 THEN '成品A-拣选02'
    WHEN 300203 THEN '成品B-存储01'
    WHEN 300204 THEN '成品B-存储02'
    WHEN 300205 THEN '成品C-暂存01'
    WHEN 300206 THEN '退货R-复检01'
    WHEN 300207 THEN '快发A-拣选01'
    WHEN 300208 THEN '快发B-存储01'
    WHEN 300209 THEN '退货R-待检01'
    ELSE location_code END,
    location_name = CASE id
    WHEN 300201 THEN '成品仓A区拣选位-01'
    WHEN 300202 THEN '成品仓A区拣选位-02'
    WHEN 300203 THEN '成品仓B区存储位-01'
    WHEN 300204 THEN '成品仓B区存储位-02'
    WHEN 300205 THEN '成品仓C区暂存位-01'
    WHEN 300206 THEN '成品仓退货复检位-01'
    WHEN 300207 THEN '快发仓A区拣选位-01'
    WHEN 300208 THEN '快发仓B区存储位-01'
    WHEN 300209 THEN '退货仓R区待检位-01'
    ELSE location_name END,
    updated_at = NOW(3)
WHERE tenant_id = 1 AND id BETWEEN 300201 AND 300209;

UPDATE warehouse_locations
SET location_name = CONCAT(
  COALESCE((SELECT w.warehouse_name FROM warehouses w WHERE w.id = warehouse_locations.warehouse_id), '仓库'),
  '-仓位',
  LPAD(((id - 12001) % 16) + 1, 2, '0')
),
    updated_at = NOW(3)
WHERE tenant_id = 1 AND id BETWEEN 12001 AND 12048;

UPDATE products
SET product_code = CONCAT('基础商品-', LPAD(id - 13000, 3, '0')),
    product_name = CONCAT('基础商品-', LPAD(id - 13000, 3, '0')),
    spec = CONCAT('基础规格-', LPAD(id - 13000, 3, '0')),
    updated_at = NOW(3)
WHERE tenant_id = 1 AND id BETWEEN 13001 AND 13060;

UPDATE products
SET product_code = CONCAT('压测商品-', LPAD(id - 390000, 3, '0')),
    product_name = CONCAT('压测演示商品-', LPAD(id - 390000, 3, '0')),
    spec = CONCAT('压测规格-', LPAD(id - 390000, 3, '0'), ';安全库存规则已配置'),
    updated_at = NOW(3)
WHERE tenant_id = 1 AND id BETWEEN 390001 AND 390060;

UPDATE products
SET product_code = CASE id
    WHEN 300501 THEN '乳品-高钙奶24'
    WHEN 300502 THEN '饮料-无糖绿茶15'
    WHEN 300503 THEN '日化-洗衣凝珠60'
    WHEN 300504 THEN '家清-厨房抽纸120'
    WHEN 300505 THEN '数码-65W快充头'
    WHEN 300506 THEN '办公-A4打印纸'
    WHEN 300507 THEN '数码-TypeC数据线'
    WHEN 300508 THEN '办公-75mm档案盒'
    ELSE product_code END,
    product_name = CASE id
    WHEN 300501 THEN '高钙纯牛奶 250ml*24盒'
    WHEN 300502 THEN '无糖绿茶 500ml*15瓶'
    WHEN 300503 THEN '洗衣凝珠 60颗装'
    WHEN 300504 THEN '厨房抽纸 120抽*3包'
    WHEN 300505 THEN '65W 氮化镓快充头'
    WHEN 300506 THEN 'A4 打印纸 70g 500张'
    WHEN 300507 THEN 'Type-C 数据线 1m'
    WHEN 300508 THEN '档案盒 75mm'
    ELSE product_name END,
    updated_at = NOW(3)
WHERE tenant_id = 1 AND id BETWEEN 300501 AND 300508;

UPDATE product_categories
SET category_name = CASE category_code
  WHEN 'CAT-FOOD' THEN '食品饮料'
  WHEN 'CAT-HOUSE' THEN '家清日化'
  WHEN 'CAT-3C' THEN '3C数码'
  WHEN 'CAT-OFFICE' THEN '办公耗材'
  ELSE category_name END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE product_units
SET unit_name = CASE unit_code
  WHEN 'PCS' THEN '件'
  WHEN 'BOX' THEN '盒'
  ELSE unit_name END,
  updated_at = NOW(3)
WHERE tenant_id = 1;

UPDATE products
SET product_name = CONCAT('商品-', LPAD(id, 6, '0')),
    updated_at = NOW(3)
WHERE tenant_id = 1
  AND (product_name LIKE '%?%' OR product_name LIKE '%�%' OR product_name IS NULL OR product_name = '');

UPDATE warehouses
SET warehouse_name = CONCAT('仓库-', warehouse_code),
    updated_at = NOW(3)
WHERE tenant_id = 1
  AND (warehouse_name LIKE '%?%' OR warehouse_name LIKE '%�%' OR warehouse_name IS NULL OR warehouse_name = '');

UPDATE warehouse_locations
SET location_name = CONCAT('库位-', location_code),
    updated_at = NOW(3)
WHERE tenant_id = 1
  AND (location_name LIKE '%?%' OR location_name LIKE '%�%' OR location_name IS NULL OR location_name = '');

UPDATE ai_messages
SET content = '系统已根据当前库存与安全库存口径生成建议，请在低库存页面核对后执行补货。',
    updated_at = NOW(3)
WHERE tenant_id = 1
  AND (content LIKE '%???%' OR content LIKE '%????????%' OR content LIKE '%�%' OR content LIKE '%锟%' OR content LIKE '%ï»¿%');
