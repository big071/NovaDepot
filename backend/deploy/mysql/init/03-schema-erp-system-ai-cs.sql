SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS purchase_orders (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  purchase_no VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  supplier_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  expected_arrival_date DATE NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_purchase_no (tenant_id, purchase_no),
  KEY idx_purchase_status (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS purchase_order_items (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  purchase_order_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  product_id BIGINT NOT NULL,
  unit_price DECIMAL(18,2) NOT NULL,
  order_qty DECIMAL(18,6) NOT NULL,
  received_qty DECIMAL(18,6) NOT NULL DEFAULT 0,
  tax_rate DECIMAL(8,4) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_purchase_line (tenant_id, purchase_order_id, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sales_orders (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  sales_no VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  customer_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  delivery_date DATE NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_sales_no (tenant_id, sales_no),
  KEY idx_sales_status (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sales_order_items (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  sales_order_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  product_id BIGINT NOT NULL,
  unit_price DECIMAL(18,2) NOT NULL,
  order_qty DECIMAL(18,6) NOT NULL,
  shipped_qty DECIMAL(18,6) NOT NULL DEFAULT 0,
  tax_rate DECIMAL(8,4) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_sales_line (tenant_id, sales_order_id, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  notify_type VARCHAR(32) NOT NULL,
  biz_type VARCHAR(32) NULL,
  biz_no VARCHAR(64) NULL,
  receiver_user_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  read_flag TINYINT(1) NOT NULL DEFAULT 0,
  sent_at DATETIME(3) NOT NULL,
  read_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_notify_receiver (tenant_id, receiver_user_id, read_flag),
  KEY idx_notify_sent (tenant_id, sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  module VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id VARCHAR(64) NULL,
  biz_no VARCHAR(64) NULL,
  operator_id BIGINT NULL,
  operator_name VARCHAR(64) NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  ip VARCHAR(64) NULL,
  user_agent VARCHAR(255) NULL,
  occurred_at DATETIME(3) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_audit_tenant_module_time (tenant_id, module, occurred_at),
  KEY idx_audit_resource (resource_type, resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS file_assets (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  biz_type VARCHAR(32) NULL,
  biz_no VARCHAR(64) NULL,
  file_name VARCHAR(255) NOT NULL,
  file_ext VARCHAR(32) NULL,
  mime_type VARCHAR(64) NULL,
  file_size BIGINT NOT NULL DEFAULT 0,
  storage_provider VARCHAR(32) NOT NULL,
  bucket VARCHAR(64) NULL,
  object_key VARCHAR(255) NOT NULL,
  url VARCHAR(500) NULL,
  uploaded_by BIGINT NULL,
  uploaded_at DATETIME(3) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_file_biz (tenant_id, biz_type, biz_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_conversations (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  conversation_no VARCHAR(64) NOT NULL,
  scene VARCHAR(32) NOT NULL,
  biz_type VARCHAR(32) NULL,
  biz_no VARCHAR(64) NULL,
  provider_type VARCHAR(32) NOT NULL,
  model_name VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL,
  started_at DATETIME(3) NOT NULL,
  ended_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_conversation_no (tenant_id, conversation_no),
  KEY idx_ai_scene_time (tenant_id, scene, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_messages (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  conversation_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL,
  content TEXT NOT NULL,
  tokens INT NULL,
  latency_ms INT NULL,
  confidence DECIMAL(5,4) NULL,
  error_code VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_ai_msg_conv (tenant_id, conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_prompt_templates (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  template_name VARCHAR(128) NOT NULL,
  scene VARCHAR(32) NOT NULL,
  template_content TEXT NOT NULL,
  version_no INT NOT NULL DEFAULT 1,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_template_code (tenant_id, template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS customer_service_sessions (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  session_no VARCHAR(64) NOT NULL,
  channel VARCHAR(32) NOT NULL,
  customer_id BIGINT NULL,
  status VARCHAR(32) NOT NULL,
  assigned_user_id BIGINT NULL,
  priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
  first_response_at DATETIME(3) NULL,
  closed_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_cs_session_no (tenant_id, session_no),
  KEY idx_cs_assigned_status (tenant_id, assigned_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS customer_service_messages (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  sender_type VARCHAR(16) NOT NULL,
  sender_id BIGINT NULL,
  content TEXT NOT NULL,
  msg_type VARCHAR(16) NOT NULL DEFAULT 'TEXT',
  ai_suggested TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_cs_msg_session (tenant_id, session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS customer_service_rules (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  trigger_type VARCHAR(32) NOT NULL,
  trigger_expr TEXT NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  action_config JSON NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_cs_rule_code (tenant_id, rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS faq_knowledge (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  faq_code VARCHAR(64) NOT NULL,
  question VARCHAR(500) NOT NULL,
  answer TEXT NOT NULL,
  tags VARCHAR(255) NULL,
  scene VARCHAR(32) NULL,
  priority INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  version_no INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_faq_code (tenant_id, faq_code),
  KEY idx_faq_scene (tenant_id, scene, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
