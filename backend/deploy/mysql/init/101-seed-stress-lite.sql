SET NAMES utf8mb4;

-- =========================
-- Phase 1 轻量压测样本层（列表/筛选/分页稳定性）
-- 约 60 个 SKU + 对应库存
-- =========================

INSERT INTO products (id, tenant_id, product_code, product_name, category_id, unit_id, barcode, spec, batch_enabled, shelf_life_days, status, created_at, created_by, updated_at, updated_by, deleted)
SELECT
  390000 + nums.n,
  1,
  CONCAT('演示商品-', LPAD(nums.n, 3, '0')),
  CONCAT(
    CASE
      WHEN nums.n % 4 = 1 THEN '饮料演示样本-'
      WHEN nums.n % 4 = 2 THEN '家清演示样本-'
      WHEN nums.n % 4 = 3 THEN '数码演示样本-'
      ELSE '办公演示样本-'
    END,
    LPAD(nums.n, 3, '0')
  ),
  CASE
    WHEN nums.n % 4 = 1 THEN 9001
    WHEN nums.n % 4 = 2 THEN 9002
    WHEN nums.n % 4 = 3 THEN 9003
    ELSE 9004
  END,
  9101,
  CONCAT('6988888', LPAD(nums.n, 6, '0')),
  CONCAT(
    '演示规格-', LPAD(nums.n, 3, '0'),
    ';安全库存=',
    CASE WHEN nums.n % 10 = 0 THEN 5 WHEN nums.n % 10 <= 2 THEN 10 ELSE 20 END,
    '件;场景=列表筛选与分页稳定性'
  ),
  0,
  NULL,
  'ACTIVE',
  NOW(3),
  1,
  NOW(3),
  1,
  0
FROM (
  SELECT (tens.i * 10 + ones.i + 1) AS n
  FROM (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) tens
  CROSS JOIN (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
  WHERE (tens.i * 10 + ones.i + 1) <= 60
) nums
ON DUPLICATE KEY UPDATE
  product_name = VALUES(product_name),
  category_id = VALUES(category_id),
  barcode = VALUES(barcode),
  spec = VALUES(spec),
  status = VALUES(status),
  updated_at = NOW(3),
  deleted = 0;

INSERT INTO inventory (id, tenant_id, warehouse_id, location_id, product_id, available_qty, locked_qty, in_transit_qty, version_no, created_at, created_by, updated_at, updated_by, deleted)
SELECT
  391000 + nums.n,
  1,
  CASE WHEN nums.n % 2 = 0 THEN 11001 ELSE 11002 END,
  CASE WHEN nums.n % 2 = 0 THEN 12001 ELSE 12017 END,
  390000 + nums.n,
  CASE
    WHEN nums.n % 10 = 0 THEN 0
    WHEN nums.n % 10 <= 2 THEN 5
    WHEN nums.n % 10 <= 6 THEN 60
    ELSE 300
  END,
  CASE WHEN nums.n % 6 = 0 THEN 8 ELSE 0 END,
  CASE WHEN nums.n % 7 = 0 THEN 12 ELSE 0 END,
  1,
  NOW(3),
  1,
  NOW(3),
  1,
  0
FROM (
  SELECT (tens.i * 10 + ones.i + 1) AS n
  FROM (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) tens
  CROSS JOIN (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
  WHERE (tens.i * 10 + ones.i + 1) <= 60
) nums
ON DUPLICATE KEY UPDATE
  available_qty = VALUES(available_qty),
  locked_qty = VALUES(locked_qty),
  in_transit_qty = VALUES(in_transit_qty),
  version_no = VALUES(version_no),
  updated_at = NOW(3),
  deleted = 0;
