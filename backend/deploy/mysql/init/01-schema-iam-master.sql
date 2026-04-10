SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS tenants (
  id BIGINT PRIMARY KEY,
  tenant_code VARCHAR(64) NOT NULL,
  tenant_name VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  expire_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_code (tenant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(64) NOT NULL,
  phone VARCHAR(32) NULL,
  email VARCHAR(128) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_username (tenant_id, username),
  KEY idx_users_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(64) NOT NULL,
  data_scope VARCHAR(32) NOT NULL DEFAULT 'ALL',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_role_code (tenant_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS permissions (
  id BIGINT PRIMARY KEY,
  perm_code VARCHAR(128) NOT NULL,
  perm_name VARCHAR(64) NOT NULL,
  resource VARCHAR(128) NOT NULL,
  action VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_perm_code (perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_roles (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_user_role (tenant_id, user_id, role_id),
  KEY idx_user_roles_user (user_id),
  KEY idx_user_roles_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS role_permissions (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_role_perm (tenant_id, role_id, permission_id),
  KEY idx_role_permissions_role (role_id),
  KEY idx_role_permissions_perm (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS product_categories (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  parent_id BIGINT NULL,
  category_code VARCHAR(64) NOT NULL,
  category_name VARCHAR(128) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_category_code (tenant_id, category_code),
  KEY idx_category_parent (tenant_id, parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS product_units (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  unit_code VARCHAR(32) NOT NULL,
  unit_name VARCHAR(32) NOT NULL,
  precision_scale INT NOT NULL DEFAULT 2,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_unit_code (tenant_id, unit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS products (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  product_code VARCHAR(64) NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  category_id BIGINT NOT NULL,
  unit_id BIGINT NOT NULL,
  barcode VARCHAR(64) NULL,
  spec VARCHAR(255) NULL,
  batch_enabled TINYINT(1) NOT NULL DEFAULT 0,
  shelf_life_days INT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_product_code (tenant_id, product_code),
  KEY idx_product_barcode (tenant_id, barcode),
  KEY idx_product_category (tenant_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS warehouses (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  warehouse_code VARCHAR(64) NOT NULL,
  warehouse_name VARCHAR(128) NOT NULL,
  warehouse_type VARCHAR(32) NOT NULL DEFAULT 'STANDARD',
  address VARCHAR(255) NULL,
  manager_user_id BIGINT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_wh_code (tenant_id, warehouse_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS warehouse_locations (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  location_code VARCHAR(64) NOT NULL,
  location_name VARCHAR(128) NOT NULL,
  location_type VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  capacity_qty DECIMAL(18,6) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_wh_loc_code (tenant_id, warehouse_id, location_code),
  KEY idx_locations_wh (tenant_id, warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS suppliers (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  supplier_code VARCHAR(64) NOT NULL,
  supplier_name VARCHAR(128) NOT NULL,
  contact_name VARCHAR(64) NULL,
  phone VARCHAR(32) NULL,
  credit_level VARCHAR(16) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_supplier_code (tenant_id, supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS customers (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  customer_code VARCHAR(64) NOT NULL,
  customer_name VARCHAR(128) NOT NULL,
  contact_name VARCHAR(64) NULL,
  phone VARCHAR(32) NULL,
  customer_level VARCHAR(16) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_customer_code (tenant_id, customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
