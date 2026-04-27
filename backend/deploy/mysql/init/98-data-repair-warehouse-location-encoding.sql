SET NAMES utf8mb4;

-- Repeatable local-dev repair:
-- Fix historical mojibake rows where names were persisted as question marks.
UPDATE warehouses
SET warehouse_name = CONCAT('仓库-', warehouse_code),
    updated_at = NOW(3)
WHERE warehouse_name LIKE '%?%'
  AND deleted = 0;

UPDATE warehouse_locations
SET location_name = CONCAT('库位-', location_code),
    updated_at = NOW(3)
WHERE location_name LIKE '%?%'
  AND deleted = 0;
