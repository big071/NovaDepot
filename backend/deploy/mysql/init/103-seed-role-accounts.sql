SET NAMES utf8mb4;

-- Ensure one login account per business role for local testing
INSERT INTO users (id, tenant_id, username, password_hash, real_name, phone, email, status, force_password_change, failed_login_count, lock_until, pwd_updated_at, created_at, updated_at, deleted)
VALUES
(6,1,'warehouse_ops','123456','仓储运营账号','13800000006','warehouse_ops@novadepot.local','ACTIVE',0,0,NULL,NOW(3),NOW(3),NOW(3),0),
(7,1,'cs_ops','123456','客服运营账号','13800000007','cs_ops@novadepot.local','ACTIVE',0,0,NULL,NOW(3),NOW(3),NOW(3),0),
(8,1,'observer','123456','观察员账号','13800000008','observer@novadepot.local','ACTIVE',0,0,NULL,NOW(3),NOW(3),NOW(3),0)
ON DUPLICATE KEY UPDATE
password_hash=VALUES(password_hash),
real_name=VALUES(real_name),
phone=VALUES(phone),
email=VALUES(email),
status='ACTIVE',
failed_login_count=0,
lock_until=NULL,
deleted=0,
updated_at=NOW(3);

INSERT INTO user_roles (id, tenant_id, user_id, role_id, created_at, updated_at, deleted)
VALUES
(2006,1,6,1002,NOW(3),NOW(3),0),
(2007,1,7,1004,NOW(3),NOW(3),0),
(2008,1,8,1005,NOW(3),NOW(3),0)
ON DUPLICATE KEY UPDATE deleted=0, updated_at=NOW(3);