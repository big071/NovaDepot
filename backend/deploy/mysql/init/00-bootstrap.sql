-- NovaDepot backend scaffold init sql
-- 后续请在此目录补充正式 DDL / migration
CREATE TABLE IF NOT EXISTS _bootstrap_marker (
  id BIGINT PRIMARY KEY,
  note VARCHAR(128) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO _bootstrap_marker (id, note)
VALUES (1, 'novadepot backend init')
ON DUPLICATE KEY UPDATE note = VALUES(note);
