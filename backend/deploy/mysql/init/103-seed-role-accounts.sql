SET NAMES utf8mb4;

-- Keep local demo login accounts aligned with v1.0 delivery docs.
UPDATE users
SET username = 'warehouse01',
    password_hash = 'pass123',
    real_name = 'Warehouse Operator',
    email = 'warehouse01@novadepot.local',
    status = 'ACTIVE',
    failed_login_count = 0,
    lock_until = NULL,
    deleted = 0,
    updated_at = NOW(3)
WHERE tenant_id = 1 AND id = 2;

UPDATE users
SET username = 'cs01',
    password_hash = 'pass123',
    real_name = 'Customer Service Operator',
    email = 'cs01@novadepot.local',
    status = 'ACTIVE',
    failed_login_count = 0,
    lock_until = NULL,
    deleted = 0,
    updated_at = NOW(3)
WHERE tenant_id = 1 AND id = 4;

UPDATE users
SET username = 'observer01',
    password_hash = 'pass123',
    real_name = 'Observer',
    email = 'observer01@novadepot.local',
    status = 'ACTIVE',
    failed_login_count = 0,
    lock_until = NULL,
    deleted = 0,
    updated_at = NOW(3)
WHERE tenant_id = 1 AND id = 5;
