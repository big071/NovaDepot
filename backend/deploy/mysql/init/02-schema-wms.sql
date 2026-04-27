SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS inventory (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  available_qty DECIMAL(18,6) NOT NULL DEFAULT 0,
  locked_qty DECIMAL(18,6) NOT NULL DEFAULT 0,
  in_transit_qty DECIMAL(18,6) NOT NULL DEFAULT 0,
  version_no INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_wh_loc_prod (tenant_id, warehouse_id, location_id, product_id),
  KEY idx_inventory_product (tenant_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS inventory_transactions (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  txn_no VARCHAR(64) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_no VARCHAR(64) NOT NULL,
  warehouse_id BIGINT NOT NULL,
  location_id BIGINT NULL,
  product_id BIGINT NOT NULL,
  change_qty DECIMAL(18,6) NOT NULL,
  before_qty DECIMAL(18,6) NOT NULL,
  after_qty DECIMAL(18,6) NOT NULL,
  request_id VARCHAR(64) NULL,
  operator_id BIGINT NULL,
  occurred_at DATETIME(3) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_txn_no (tenant_id, txn_no),
  UNIQUE KEY uk_tenant_request_id (tenant_id, request_id),
  KEY idx_tenant_biz (tenant_id, biz_type, biz_no),
  KEY idx_tenant_occurred (tenant_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS inbound_orders (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  inbound_no VARCHAR(64) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  warehouse_id BIGINT NOT NULL,
  supplier_id BIGINT NULL,
  expected_at DATETIME(3) NULL,
  completed_at DATETIME(3) NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_inbound_no (tenant_id, inbound_no),
  KEY idx_tenant_status_created (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS inbound_order_items (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  inbound_order_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  product_id BIGINT NOT NULL,
  location_id BIGINT NULL,
  unit_id BIGINT NOT NULL,
  batch_no VARCHAR(64) NULL,
  production_date DATE NULL,
  expire_date DATE NULL,
  plan_qty DECIMAL(18,6) NOT NULL,
  received_qty DECIMAL(18,6) NOT NULL DEFAULT 0,
  qualified_qty DECIMAL(18,6) NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_inbound_line (tenant_id, inbound_order_id, line_no),
  KEY idx_inbound_item_product (tenant_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS outbound_orders (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  outbound_no VARCHAR(64) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  warehouse_id BIGINT NOT NULL,
  customer_id BIGINT NULL,
  expected_ship_at DATETIME(3) NULL,
  shipped_at DATETIME(3) NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_outbound_no (tenant_id, outbound_no),
  KEY idx_tenant_status_created (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS outbound_order_items (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  outbound_order_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  product_id BIGINT NOT NULL,
  location_id BIGINT NULL,
  unit_id BIGINT NOT NULL,
  batch_no VARCHAR(64) NULL,
  plan_qty DECIMAL(18,6) NOT NULL,
  picked_qty DECIMAL(18,6) NOT NULL DEFAULT 0,
  shipped_qty DECIMAL(18,6) NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_outbound_line (tenant_id, outbound_order_id, line_no),
  KEY idx_outbound_item_product (tenant_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE inbound_order_items ADD COLUMN location_id BIGINT NULL AFTER product_id',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'inbound_order_items'
    AND column_name = 'location_id'
);
PREPARE s1 FROM @stmt;
EXECUTE s1;
DEALLOCATE PREPARE s1;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE outbound_order_items ADD COLUMN location_id BIGINT NULL AFTER product_id',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'outbound_order_items'
    AND column_name = 'location_id'
);
PREPARE s2 FROM @stmt;
EXECUTE s2;
DEALLOCATE PREPARE s2;

CREATE TABLE IF NOT EXISTS transfer_orders (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  transfer_no VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  from_warehouse_id BIGINT NOT NULL,
  to_warehouse_id BIGINT NOT NULL,
  from_location_id BIGINT NULL,
  to_location_id BIGINT NULL,
  completed_at DATETIME(3) NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_transfer_no (tenant_id, transfer_no),
  KEY idx_transfer_status (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS transfer_order_items (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  transfer_order_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  product_id BIGINT NOT NULL,
  unit_id BIGINT NOT NULL,
  batch_no VARCHAR(64) NULL,
  plan_qty DECIMAL(18,6) NOT NULL,
  actual_qty DECIMAL(18,6) NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_transfer_line (tenant_id, transfer_order_id, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS stocktake_orders (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  stocktake_no VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  warehouse_id BIGINT NOT NULL,
  scope_type VARCHAR(32) NOT NULL,
  planned_at DATETIME(3) NULL,
  started_at DATETIME(3) NULL,
  finished_at DATETIME(3) NULL,
  diff_count INT NOT NULL DEFAULT 0,
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_stocktake_no (tenant_id, stocktake_no),
  KEY idx_stocktake_status (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS stocktake_order_items (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  stocktake_order_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  product_id BIGINT NOT NULL,
  location_id BIGINT NULL,
  system_qty DECIMAL(18,6) NOT NULL,
  counted_qty DECIMAL(18,6) NOT NULL,
  diff_qty DECIMAL(18,6) NOT NULL,
  result_type VARCHAR(32) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_stocktake_line (tenant_id, stocktake_order_id, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
