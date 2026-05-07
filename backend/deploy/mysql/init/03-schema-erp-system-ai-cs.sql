SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS partners (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  partner_code VARCHAR(64) NOT NULL,
  partner_name VARCHAR(128) NOT NULL,
  partner_type VARCHAR(32) NOT NULL,
  contact_name VARCHAR(64) NULL,
  phone VARCHAR(32) NULL,
  address VARCHAR(255) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_partner_code (tenant_id, partner_code),
  KEY idx_partner_type_status (tenant_id, partner_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS purchase_orders (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  purchase_no VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  partner_id BIGINT NOT NULL,
  supplier_id BIGINT NULL,
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
  partner_id BIGINT NOT NULL,
  customer_id BIGINT NULL,
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

SET @stmt = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE purchase_orders ADD COLUMN partner_id BIGINT NULL AFTER status',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'purchase_orders'
    AND column_name = 'partner_id'
);
PREPARE s_erp_po_partner FROM @stmt;
EXECUTE s_erp_po_partner;
DEALLOCATE PREPARE s_erp_po_partner;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE sales_orders ADD COLUMN partner_id BIGINT NULL AFTER status',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sales_orders'
    AND column_name = 'partner_id'
);
PREPARE s_erp_so_partner FROM @stmt;
EXECUTE s_erp_so_partner;
DEALLOCATE PREPARE s_erp_so_partner;

UPDATE purchase_orders SET partner_id = supplier_id WHERE partner_id IS NULL AND supplier_id IS NOT NULL;
UPDATE sales_orders SET partner_id = customer_id WHERE partner_id IS NULL AND customer_id IS NOT NULL;

SET @stmt = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE inbound_orders ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT ''MANUAL'' AFTER status',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'inbound_orders' AND column_name = 'source_type'
);
PREPARE s_s2_in_src_type FROM @stmt;
EXECUTE s_s2_in_src_type;
DEALLOCATE PREPARE s_s2_in_src_type;

SET @stmt = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE inbound_orders ADD COLUMN source_order_id BIGINT NULL AFTER source_type',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'inbound_orders' AND column_name = 'source_order_id'
);
PREPARE s_s2_in_src_id FROM @stmt;
EXECUTE s_s2_in_src_id;
DEALLOCATE PREPARE s_s2_in_src_id;

SET @stmt = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE inbound_orders ADD COLUMN source_order_no VARCHAR(64) NULL AFTER source_order_id',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'inbound_orders' AND column_name = 'source_order_no'
);
PREPARE s_s2_in_src_no FROM @stmt;
EXECUTE s_s2_in_src_no;
DEALLOCATE PREPARE s_s2_in_src_no;

SET @stmt = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE inbound_order_items ADD COLUMN source_order_item_id BIGINT NULL AFTER line_no',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'inbound_order_items' AND column_name = 'source_order_item_id'
);
PREPARE s_s2_in_item_src_id FROM @stmt;
EXECUTE s_s2_in_item_src_id;
DEALLOCATE PREPARE s_s2_in_item_src_id;

SET @stmt = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE inbound_order_items ADD COLUMN source_line_no INT NULL AFTER source_order_item_id',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'inbound_order_items' AND column_name = 'source_line_no'
);
PREPARE s_s2_in_item_src_line FROM @stmt;
EXECUTE s_s2_in_item_src_line;
DEALLOCATE PREPARE s_s2_in_item_src_line;

SET @stmt = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE outbound_orders ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT ''MANUAL'' AFTER status',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'outbound_orders' AND column_name = 'source_type'
);
PREPARE s_s2_out_src_type FROM @stmt;
EXECUTE s_s2_out_src_type;
DEALLOCATE PREPARE s_s2_out_src_type;

SET @stmt = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE outbound_orders ADD COLUMN source_order_id BIGINT NULL AFTER source_type',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'outbound_orders' AND column_name = 'source_order_id'
);
PREPARE s_s2_out_src_id FROM @stmt;
EXECUTE s_s2_out_src_id;
DEALLOCATE PREPARE s_s2_out_src_id;

SET @stmt = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE outbound_orders ADD COLUMN source_order_no VARCHAR(64) NULL AFTER source_order_id',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'outbound_orders' AND column_name = 'source_order_no'
);
PREPARE s_s2_out_src_no FROM @stmt;
EXECUTE s_s2_out_src_no;
DEALLOCATE PREPARE s_s2_out_src_no;

SET @stmt = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE outbound_order_items ADD COLUMN source_order_item_id BIGINT NULL AFTER line_no',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'outbound_order_items' AND column_name = 'source_order_item_id'
);
PREPARE s_s2_out_item_src_id FROM @stmt;
EXECUTE s_s2_out_item_src_id;
DEALLOCATE PREPARE s_s2_out_item_src_id;

SET @stmt = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE outbound_order_items ADD COLUMN source_line_no INT NULL AFTER source_order_item_id',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'outbound_order_items' AND column_name = 'source_line_no'
);
PREPARE s_s2_out_item_src_line FROM @stmt;
EXECUTE s_s2_out_item_src_line;
DEALLOCATE PREPARE s_s2_out_item_src_line;

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

CREATE TABLE IF NOT EXISTS business_history_events (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id VARCHAR(64) NOT NULL,
  biz_no VARCHAR(64) NULL,
  action VARCHAR(64) NOT NULL,
  action_label VARCHAR(128) NOT NULL,
  status_from VARCHAR(32) NULL,
  status_to VARCHAR(32) NULL,
  note VARCHAR(500) NULL,
  operator_id BIGINT NULL,
  operator_name VARCHAR(64) NULL,
  occurred_at DATETIME(3) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_bhe_resource_time (tenant_id, resource_type, resource_id, occurred_at),
  KEY idx_bhe_biz_no (tenant_id, biz_no, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS customer_service_tickets (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  ticket_no VARCHAR(64) NOT NULL,
  session_id BIGINT NOT NULL,
  priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
  content TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  assignee_user_id BIGINT NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_cs_ticket_no (tenant_id, ticket_no),
  KEY idx_cs_ticket_session (tenant_id, session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE customer_service_tickets ADD COLUMN assignee_user_id BIGINT NULL AFTER status',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'customer_service_tickets'
    AND column_name = 'assignee_user_id'
);
PREPARE s3 FROM @stmt;
EXECUTE s3;
DEALLOCATE PREPARE s3;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE customer_service_tickets ADD COLUMN remark VARCHAR(500) NULL AFTER assignee_user_id',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'customer_service_tickets'
    AND column_name = 'remark'
);
PREPARE s4 FROM @stmt;
EXECUTE s4;
DEALLOCATE PREPARE s4;

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

SET @stmt = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE faq_knowledge ADD COLUMN review_status VARCHAR(16) NOT NULL DEFAULT ''APPROVED'' AFTER enabled',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'faq_knowledge'
    AND column_name = 'review_status'
);
PREPARE s5 FROM @stmt;
EXECUTE s5;
DEALLOCATE PREPARE s5;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE faq_knowledge ADD COLUMN source_type VARCHAR(32) NULL AFTER version_no',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'faq_knowledge'
    AND column_name = 'source_type'
);
PREPARE s6 FROM @stmt;
EXECUTE s6;
DEALLOCATE PREPARE s6;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE faq_knowledge ADD COLUMN source_ref_id VARCHAR(64) NULL AFTER source_type',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'faq_knowledge'
    AND column_name = 'source_ref_id'
);
PREPARE s7 FROM @stmt;
EXECUTE s7;
DEALLOCATE PREPARE s7;

CREATE TABLE IF NOT EXISTS sop_knowledge (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  sop_code VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  scene VARCHAR(64) NULL,
  steps TEXT NOT NULL,
  risks TEXT NULL,
  review_checks TEXT NULL,
  tags VARCHAR(255) NULL,
  priority INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  review_status VARCHAR(16) NOT NULL DEFAULT 'APPROVED',
  source_type VARCHAR(32) NULL,
  source_ref_id VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_sop_code (tenant_id, sop_code),
  KEY idx_sop_scene (tenant_id, scene, enabled, review_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rule_configs (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  config_key VARCHAR(64) NOT NULL,
  config_name VARCHAR(128) NOT NULL,
  config_value TEXT NOT NULL,
  value_type VARCHAR(16) NOT NULL DEFAULT 'TEXT',
  scene VARCHAR(64) NULL,
  remark VARCHAR(500) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_rule_config_key (tenant_id, config_key),
  KEY idx_rule_config_scene (tenant_id, scene, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS import_error_reports (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  module VARCHAR(64) NOT NULL,
  report_id VARCHAR(64) NOT NULL,
  content LONGTEXT NOT NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_module_report (tenant_id, module, report_id),
  KEY idx_report_created (tenant_id, module, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS agent_task_runs (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  task_code VARCHAR(64) NOT NULL,
  task_name VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  target_json JSON NULL,
  steps_json JSON NULL,
  result_json JSON NULL,
  error_message VARCHAR(1000) NULL,
  started_at DATETIME(3) NOT NULL,
  finished_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_agent_task_runs_tenant_task_time (tenant_id, task_code, started_at),
  KEY idx_agent_task_runs_tenant_status_time (tenant_id, status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS payables (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  payable_no VARCHAR(64) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_order_id BIGINT NOT NULL,
  source_order_no VARCHAR(64) NOT NULL,
  partner_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  paid_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  balance_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_payable_no (tenant_id, payable_no),
  UNIQUE KEY uk_tenant_payable_source (tenant_id, source_type, source_order_id),
  KEY idx_payable_status (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS receivables (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  receivable_no VARCHAR(64) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_order_id BIGINT NOT NULL,
  source_order_no VARCHAR(64) NOT NULL,
  partner_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  received_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  balance_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_receivable_no (tenant_id, receivable_no),
  UNIQUE KEY uk_tenant_receivable_source (tenant_id, source_type, source_order_id),
  KEY idx_receivable_status (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS payments (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  payment_no VARCHAR(64) NOT NULL,
  direction VARCHAR(32) NOT NULL,
  ledger_id BIGINT NOT NULL,
  ledger_no VARCHAR(64) NOT NULL,
  partner_id BIGINT NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  paid_at DATE NOT NULL,
  method VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_payment_no (tenant_id, payment_no),
  KEY idx_payment_ledger (tenant_id, direction, ledger_id),
  KEY idx_payment_paid_at (tenant_id, paid_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS backup_records (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  backup_no VARCHAR(64) NOT NULL,
  file_name VARCHAR(255) NULL,
  file_path VARCHAR(1000) NULL,
  file_size BIGINT NULL,
  checksum VARCHAR(128) NULL,
  status VARCHAR(32) NOT NULL,
  started_at DATETIME(3) NOT NULL,
  finished_at DATETIME(3) NULL,
  error_message VARCHAR(1000) NULL,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_backup_no (tenant_id, backup_no),
  KEY idx_backup_status_time (tenant_id, status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
