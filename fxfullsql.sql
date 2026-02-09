/* =========================================================
   FLEET SERVICE DATABASE MANAGEMENT SYSTEM (SaaS + QR)
   SINGLE SCRIPT - FINAL (v3 tenant-strict FKs everywhere)
   ========================================================= */

-- 1) DB
DROP DATABASE IF EXISTS fleet_service_db;
CREATE DATABASE fleet_service_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE fleet_service_db;

SET sql_safe_updates = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 2) SAAS CORE
-- =========================================================

-- 2.1 Tenants (Services)
CREATE TABLE tenants (
  tenant_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  phone VARCHAR(25) NULL,
  email VARCHAR(120) NULL,
  city  VARCHAR(80) NULL,
  address VARCHAR(255) NULL,
  whatsapp_phone VARCHAR(25) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_tenant_name (name)
) ENGINE=InnoDB;

-- 2.2 Users (Service staff + customers who login)
CREATE TABLE users (
  user_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  role ENUM('SERVICE_ADMIN','SERVICE_STAFF','CUSTOMER') NOT NULL,
  status ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE',

  email VARCHAR(120) NULL,
  phone VARCHAR(25) NULL,

  password_hash VARCHAR(255) NULL,

  otp_hash VARCHAR(255) NULL,
  otp_expires_at DATETIME NULL,
  otp_attempts SMALLINT UNSIGNED NOT NULL DEFAULT 0,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at DATETIME NULL,

  KEY ix_users_tenant (tenant_id),
  KEY ix_users_email (email),
  KEY ix_users_phone (phone),

  UNIQUE KEY uq_users_tenant_email (tenant_id, email),
  UNIQUE KEY uq_users_tenant_phone (tenant_id, phone),

  CONSTRAINT fk_users_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 3) BUSINESS ENTITIES
-- =========================================================

-- 3.1 Customers
CREATE TABLE customers (
  customer_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,

  first_name VARCHAR(50) NOT NULL,
  last_name  VARCHAR(50) NOT NULL,
  phone      VARCHAR(25) NULL,
  email      VARCHAR(120) NULL,

  user_id BIGINT UNSIGNED NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY ix_customers_tenant (tenant_id),
  KEY ix_customers_phone  (phone),
  KEY ix_customers_email  (email),

  -- ✅ tenant-safe composite reference target
  UNIQUE KEY uq_customers_tenant_customer (tenant_id, customer_id),

  CONSTRAINT fk_customers_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_customers_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE SET NULL
) ENGINE=InnoDB;

-- 3.2 Drivers (fleet module)
CREATE TABLE drivers (
  driver_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,

  first_name VARCHAR(50) NOT NULL,
  last_name  VARCHAR(50) NOT NULL,
  license_no VARCHAR(30) NOT NULL,
  phone      VARCHAR(25) NULL,

  KEY ix_driver_tenant (tenant_id),
  UNIQUE KEY uq_driver_tenant_license (tenant_id, license_no),

  -- ✅ tenant-safe composite reference target
  UNIQUE KEY uq_drivers_tenant_driver (tenant_id, driver_id),

  CONSTRAINT fk_drivers_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3.3 Vehicle (VIN OPTIONAL)
CREATE TABLE vehicle (
  vehicle_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,

  customer_id BIGINT UNSIGNED NULL,

  -- public id for future (viewer / deep-link / api)
  public_id CHAR(36) NOT NULL,

  plate_no VARCHAR(15) NOT NULL,
  vin_no   VARCHAR(32) NULL,                 -- ✅ VIN optional
  make     VARCHAR(50) NOT NULL,
  model    VARCHAR(50) NOT NULL,
  model_year SMALLINT NOT NULL,
  colour   VARCHAR(30) NULL,
  current_km INT UNSIGNED NOT NULL DEFAULT 0,

  status ENUM('ACTIVE','IN_SERVICE','ASSIGNED','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  notes VARCHAR(500) NULL,
  service_entry_date DATE NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY ix_vehicle_tenant (tenant_id),
  KEY ix_vehicle_customer (customer_id),

  UNIQUE KEY uq_vehicle_tenant_plate (tenant_id, plate_no),
  UNIQUE KEY uq_vehicle_tenant_vin   (tenant_id, vin_no),  -- ✅ vin NULL ise problem yok
  UNIQUE KEY uq_vehicle_public_id (public_id),

  -- ✅ tenant-safe composite reference target
  UNIQUE KEY uq_vehicle_tenant_vehicle (tenant_id, vehicle_id),

  CONSTRAINT chk_vehicle_year CHECK (model_year BETWEEN 1980 AND 2100),

  CONSTRAINT fk_vehicle_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
    ON DELETE CASCADE,

  -- ✅ tenant-safe: (tenant_id, customer_id) must exist in customers
  -- MySQL rule: customer_id NULL ise FK kontrol edilmez (sahipsiz araç ok)
  CONSTRAINT fk_vehicle_customer_tenant
    FOREIGN KEY (tenant_id, customer_id) REFERENCES customers(tenant_id, customer_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 3.4 Maintenance
CREATE TABLE maintenance (
  maint_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,

  maint_date DATE NOT NULL,
  maint_type VARCHAR(50) NOT NULL,
  odometer_km INT UNSIGNED NOT NULL,
  description VARCHAR(500) NULL,
  cost DECIMAL(10,2) NOT NULL DEFAULT 0.00,

  vehicle_id INT UNSIGNED NOT NULL,

  KEY ix_maintenance_vehicle (vehicle_id),
  KEY ix_maintenance_date (maint_date),
  KEY ix_maintenance_tenant (tenant_id),

  CONSTRAINT fk_maint_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
    ON DELETE CASCADE,

  -- ✅ tenant-safe: maintenance.tenant_id + vehicle_id must match vehicle row
  CONSTRAINT fk_maint_vehicle_tenant
    FOREIGN KEY (tenant_id, vehicle_id) REFERENCES vehicle(tenant_id, vehicle_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 3.5 Assignment (tenant-aware)
CREATE TABLE assignment (
  assignment_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,

  start_km INT UNSIGNED NOT NULL,
  end_km   INT UNSIGNED NULL,
  start_datetime DATETIME NOT NULL,
  end_datetime   DATETIME NULL,

  driver_id  INT UNSIGNED NOT NULL,
  vehicle_id INT UNSIGNED NOT NULL,

  active_vehicle_id INT UNSIGNED
    GENERATED ALWAYS AS (IF(end_datetime IS NULL, vehicle_id, NULL)) STORED,
  active_driver_id  INT UNSIGNED
    GENERATED ALWAYS AS (IF(end_datetime IS NULL, driver_id, NULL)) STORED,

  KEY ix_asg_tenant  (tenant_id),
  KEY ix_asg_driver  (driver_id),
  KEY ix_asg_vehicle (vehicle_id),
  KEY ix_asg_startdt (start_datetime),

  UNIQUE KEY uq_active_vehicle_tenant (tenant_id, active_vehicle_id),
  UNIQUE KEY uq_active_driver_tenant  (tenant_id, active_driver_id),

  CONSTRAINT chk_asg_end_after_start
    CHECK (end_datetime IS NULL OR end_datetime >= start_datetime),
  CONSTRAINT chk_asg_endkm_after_startkm
    CHECK (end_km IS NULL OR end_km >= start_km),

  CONSTRAINT fk_asg_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
    ON DELETE CASCADE,

  -- ✅ tenant-safe driver FK
  CONSTRAINT fk_asg_driver_tenant
    FOREIGN KEY (tenant_id, driver_id) REFERENCES drivers(tenant_id, driver_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

  -- ✅ tenant-safe vehicle FK
  CONSTRAINT fk_asg_vehicle_tenant
    FOREIGN KEY (tenant_id, vehicle_id) REFERENCES vehicle(tenant_id, vehicle_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 4) QR TAGS
-- =========================================================

CREATE TABLE qr_tags (
  qr_tag_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,

  token_hash VARCHAR(255) NOT NULL,
  display_code VARCHAR(16) NULL,

  status ENUM('NEW','ACTIVATED','REVOKED') NOT NULL DEFAULT 'NEW',

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  activated_at DATETIME NULL,

  vehicle_id INT UNSIGNED NULL,

  KEY ix_qr_tenant (tenant_id),
  KEY ix_qr_vehicle (vehicle_id),
  KEY ix_qr_status (status),
  UNIQUE KEY uq_qr_tenant_tokenhash (tenant_id, token_hash),
  UNIQUE KEY uq_qr_tenant_vehicle (tenant_id, vehicle_id),

  CONSTRAINT fk_qr_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
    ON DELETE CASCADE,

  -- ✅ tenant-safe vehicle ref
  CONSTRAINT fk_qr_vehicle_tenant
    FOREIGN KEY (tenant_id, vehicle_id) REFERENCES vehicle(tenant_id, vehicle_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE vehicle_public_tokens (
  token_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  vehicle_id INT UNSIGNED NOT NULL,
  tenant_id BIGINT UNSIGNED NOT NULL,

  token_hash VARCHAR(255) NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  rotated_at DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY ix_vpt_vehicle (vehicle_id),
  KEY ix_vpt_tenant  (tenant_id),
  KEY ix_vpt_active  (is_active),

  -- ✅ tenant-safe vehicle ref
  CONSTRAINT fk_vpt_vehicle_tenant
    FOREIGN KEY (tenant_id, vehicle_id) REFERENCES vehicle(tenant_id, vehicle_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  CONSTRAINT fk_vpt_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 5) REMINDERS
-- =========================================================
CREATE TABLE reminders (
  reminder_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  vehicle_id INT UNSIGNED NOT NULL,
  customer_id BIGINT UNSIGNED NULL,

  remind_at DATETIME NOT NULL,
  title VARCHAR(120) NOT NULL,
  message VARCHAR(500) NULL,

  channel_email TINYINT(1) NOT NULL DEFAULT 1,
  channel_sms   TINYINT(1) NOT NULL DEFAULT 0,

  status ENUM('PENDING','SENT','CANCELLED','FAILED') NOT NULL DEFAULT 'PENDING',
  last_error VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY ix_rem_tenant (tenant_id),
  KEY ix_rem_vehicle (vehicle_id),
  KEY ix_rem_due (remind_at),
  KEY ix_rem_status (status),

  CONSTRAINT fk_rem_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
    ON DELETE CASCADE,

  -- ✅ tenant-safe vehicle ref
  CONSTRAINT fk_rem_vehicle_tenant
    FOREIGN KEY (tenant_id, vehicle_id) REFERENCES vehicle(tenant_id, vehicle_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  -- ✅ tenant-safe customer ref (customer_id NULL serbest)
  CONSTRAINT fk_rem_customer_tenant
    FOREIGN KEY (tenant_id, customer_id) REFERENCES customers(tenant_id, customer_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 6) VIEWS (tenant-safe joins)
-- =========================================================

CREATE OR REPLACE VIEW vw_active_assignments_detailed AS
SELECT
  a.tenant_id,
  a.assignment_id,
  a.start_datetime,
  a.start_km,
  TIMESTAMPDIFF(MINUTE, a.start_datetime, NOW()) AS active_minutes,
  a.driver_id,
  CONCAT(d.first_name, ' ', d.last_name) AS driver_name,
  d.license_no,
  a.vehicle_id,
  v.plate_no,
  v.vin_no,
  v.make,
  v.model,
  v.status
FROM assignment a
JOIN drivers d
  ON d.driver_id = a.driver_id
 AND d.tenant_id = a.tenant_id
JOIN vehicle v
  ON v.vehicle_id = a.vehicle_id
 AND v.tenant_id = a.tenant_id
WHERE a.end_datetime IS NULL;

CREATE OR REPLACE VIEW vw_vehicle_maintenance_summary AS
SELECT
  v.tenant_id,
  v.vehicle_id,
  v.plate_no,
  v.make,
  v.model,
  COUNT(m.maint_id) AS maint_count,
  COALESCE(SUM(m.cost), 0.00) AS total_cost,
  MAX(m.maint_date) AS last_maint_date
FROM vehicle v
LEFT JOIN maintenance m
  ON m.vehicle_id = v.vehicle_id
 AND m.tenant_id = v.tenant_id
GROUP BY v.tenant_id, v.vehicle_id, v.plate_no, v.make, v.model;

CREATE OR REPLACE VIEW vw_maintenance_history AS
SELECT
  m.tenant_id,
  m.maint_id,
  m.maint_date,
  m.maint_type,
  m.odometer_km,
  m.cost,
  m.description,
  v.vehicle_id,
  v.plate_no,
  v.make,
  v.model
FROM maintenance m
JOIN vehicle v
  ON v.vehicle_id = m.vehicle_id
 AND v.tenant_id = m.tenant_id
ORDER BY m.maint_date DESC;

CREATE OR REPLACE VIEW vw_available_vehicles AS
SELECT v.tenant_id, v.vehicle_id, v.plate_no, v.make, v.model, v.current_km
FROM vehicle v
WHERE v.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1 FROM assignment a
    WHERE a.vehicle_id = v.vehicle_id
      AND a.tenant_id = v.tenant_id
      AND a.end_datetime IS NULL
  );

CREATE OR REPLACE VIEW vw_available_drivers AS
SELECT d.tenant_id, d.driver_id, d.first_name, d.last_name, d.license_no
FROM drivers d
WHERE NOT EXISTS (
    SELECT 1 FROM assignment a
    WHERE a.driver_id = d.driver_id
      AND a.tenant_id = d.tenant_id
      AND a.end_datetime IS NULL
);

CREATE OR REPLACE VIEW vw_driver_active_vehicle AS
SELECT
  d.tenant_id,
  d.driver_id, d.first_name, d.last_name, d.license_no,
  CASE
    WHEN a.assignment_id IS NULL THEN NULL
    ELSE CONCAT(v.plate_no, ' - ', v.make, ' ', v.model)
  END AS active_vehicle
FROM drivers d
LEFT JOIN assignment a
  ON a.driver_id = d.driver_id
 AND a.tenant_id = d.tenant_id
 AND a.end_datetime IS NULL
LEFT JOIN vehicle v
  ON v.vehicle_id = a.vehicle_id
 AND v.tenant_id = a.tenant_id;

CREATE OR REPLACE VIEW vw_public_vehicle_by_tokenhash AS
SELECT
  t.tenant_id,
  t.qr_tag_id,
  t.status,
  t.activated_at,
  t.display_code,
  v.vehicle_id,
  v.plate_no,
  v.make,
  v.model,
  v.model_year,
  v.colour,
  v.current_km
FROM qr_tags t
LEFT JOIN vehicle v
  ON v.vehicle_id = t.vehicle_id
 AND v.tenant_id = t.tenant_id
WHERE t.status IN ('NEW','ACTIVATED');

-- =========================================================
-- 7) TRIGGERS & PROCEDURES
-- =========================================================
DELIMITER $$

CREATE TRIGGER trg_maintenance_km_check
BEFORE INSERT ON maintenance
FOR EACH ROW
BEGIN
  DECLARE v_km INT UNSIGNED;
  DECLARE v_tid BIGINT UNSIGNED;

  SELECT current_km, tenant_id INTO v_km, v_tid
  FROM vehicle
  WHERE vehicle_id = NEW.vehicle_id;

  IF v_tid IS NULL OR v_tid <> NEW.tenant_id THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tenant mismatch for vehicle/maintenance';
  END IF;

  IF v_km IS NOT NULL AND NEW.odometer_km < v_km THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Maintenance odometer cannot be less than vehicle current KM';
  END IF;
END $$

CREATE TRIGGER trg_maintenance_sync_vehicle_km
AFTER INSERT ON maintenance
FOR EACH ROW
BEGIN
  UPDATE vehicle
  SET current_km = GREATEST(current_km, NEW.odometer_km)
  WHERE vehicle_id = NEW.vehicle_id
    AND tenant_id = NEW.tenant_id;
END $$

CREATE PROCEDURE sp_create_assignment(
  IN p_tenant_id BIGINT UNSIGNED,
  IN p_driver_id INT UNSIGNED,
  IN p_vehicle_id INT UNSIGNED,
  IN p_start_km INT UNSIGNED,
  IN p_start_datetime DATETIME
)
BEGIN
  DECLARE v_vehicle_km INT UNSIGNED;
  DECLARE v_status VARCHAR(20);
  DECLARE v_vehicle_tid BIGINT UNSIGNED;
  DECLARE v_driver_tid BIGINT UNSIGNED;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT tenant_id INTO v_driver_tid
  FROM drivers
  WHERE driver_id = p_driver_id
  FOR UPDATE;

  IF v_driver_tid IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Driver not found';
  END IF;

  IF v_driver_tid <> p_tenant_id THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tenant mismatch: driver';
  END IF;

  SELECT current_km, status, tenant_id INTO v_vehicle_km, v_status, v_vehicle_tid
  FROM vehicle
  WHERE vehicle_id = p_vehicle_id
  FOR UPDATE;

  IF v_vehicle_tid IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vehicle not found';
  END IF;

  IF v_vehicle_tid <> p_tenant_id THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tenant mismatch: vehicle';
  END IF;

  IF EXISTS (
    SELECT 1 FROM assignment
    WHERE tenant_id = p_tenant_id
      AND driver_id = p_driver_id
      AND end_datetime IS NULL
    LIMIT 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Driver already has an active assignment';
  END IF;

  IF EXISTS (
    SELECT 1 FROM assignment
    WHERE tenant_id = p_tenant_id
      AND vehicle_id = p_vehicle_id
      AND end_datetime IS NULL
    LIMIT 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vehicle already has an active assignment';
  END IF;

  IF v_status <> 'ACTIVE' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vehicle is not ACTIVE; cannot create assignment';
  END IF;

  IF p_start_km < v_vehicle_km THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Start KM cannot be less than vehicle current KM';
  END IF;

  INSERT INTO assignment (tenant_id, driver_id, vehicle_id, start_km, start_datetime, end_km, end_datetime)
  VALUES (p_tenant_id, p_driver_id, p_vehicle_id, p_start_km, p_start_datetime, NULL, NULL);

  UPDATE vehicle
  SET status = 'ASSIGNED', current_km = p_start_km
  WHERE vehicle_id = p_vehicle_id
    AND tenant_id = p_tenant_id;

  COMMIT;
END $$

CREATE PROCEDURE sp_close_assignment(
  IN p_tenant_id BIGINT UNSIGNED,
  IN p_assignment_id INT UNSIGNED,
  IN p_end_km INT UNSIGNED,
  IN p_end_datetime DATETIME
)
BEGIN
  DECLARE v_vehicle_id INT UNSIGNED;
  DECLARE v_start_km INT UNSIGNED;
  DECLARE v_end_dt DATETIME;
  DECLARE v_asg_tid BIGINT UNSIGNED;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT tenant_id, vehicle_id, start_km, end_datetime
    INTO v_asg_tid, v_vehicle_id, v_start_km, v_end_dt
  FROM assignment
  WHERE assignment_id = p_assignment_id
  FOR UPDATE;

  IF v_asg_tid IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assignment not found';
  END IF;

  IF v_asg_tid <> p_tenant_id THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tenant mismatch: assignment';
  END IF;

  IF v_end_dt IS NOT NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assignment already closed';
  END IF;

  IF p_end_km < v_start_km THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'End KM cannot be less than Start KM';
  END IF;

  UPDATE assignment
  SET end_km = p_end_km, end_datetime = p_end_datetime
  WHERE assignment_id = p_assignment_id
    AND tenant_id = p_tenant_id;

  UPDATE vehicle
  SET status = 'ACTIVE', current_km = p_end_km
  WHERE vehicle_id = v_vehicle_id
    AND tenant_id = p_tenant_id;

  COMMIT;
END $$

CREATE PROCEDURE sp_create_qr_tag(
  IN p_tenant_id BIGINT UNSIGNED,
  IN p_token_hash VARCHAR(255),
  IN p_display_code VARCHAR(16)
)
BEGIN
  IF (SELECT COUNT(*) FROM tenants WHERE tenant_id = p_tenant_id) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tenant not found';
  END IF;

  INSERT INTO qr_tags (tenant_id, token_hash, display_code, status)
  VALUES (p_tenant_id, p_token_hash, p_display_code, 'NEW');
END $$

CREATE PROCEDURE sp_activate_qr_tag(
  IN p_tenant_id BIGINT UNSIGNED,
  IN p_token_hash VARCHAR(255),
  IN p_vehicle_id INT UNSIGNED
)
BEGIN
  DECLARE v_tag_id BIGINT UNSIGNED;
  DECLARE v_status VARCHAR(20);
  DECLARE v_vehicle_tid BIGINT UNSIGNED;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT qr_tag_id, status INTO v_tag_id, v_status
  FROM qr_tags
  WHERE tenant_id = p_tenant_id AND token_hash = p_token_hash
  FOR UPDATE;

  IF v_tag_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'QR tag not found';
  END IF;

  IF v_status <> 'NEW' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'QR tag already activated or revoked';
  END IF;

  SELECT tenant_id INTO v_vehicle_tid
  FROM vehicle
  WHERE vehicle_id = p_vehicle_id
  FOR UPDATE;

  IF v_vehicle_tid IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vehicle not found';
  END IF;

  IF v_vehicle_tid <> p_tenant_id THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tenant mismatch: vehicle';
  END IF;

  IF EXISTS (
    SELECT 1 FROM qr_tags
    WHERE tenant_id = p_tenant_id
      AND vehicle_id = p_vehicle_id
      AND status = 'ACTIVATED'
    LIMIT 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vehicle already has an activated QR tag';
  END IF;

  UPDATE qr_tags
  SET status='ACTIVATED', activated_at=NOW(), vehicle_id=p_vehicle_id
  WHERE qr_tag_id=v_tag_id;

  UPDATE vehicle_public_tokens
  SET is_active = 0, rotated_at = NOW()
  WHERE tenant_id = p_tenant_id
    AND vehicle_id = p_vehicle_id
    AND is_active = 1;

  INSERT INTO vehicle_public_tokens (vehicle_id, tenant_id, token_hash, is_active, rotated_at)
  VALUES (p_vehicle_id, p_tenant_id, p_token_hash, 1, NOW());

  COMMIT;
END $$

CREATE PROCEDURE sp_revoke_qr_tag(
  IN p_tenant_id BIGINT UNSIGNED,
  IN p_token_hash VARCHAR(255)
)
BEGIN
  UPDATE qr_tags
  SET status='REVOKED'
  WHERE tenant_id = p_tenant_id
    AND token_hash = p_token_hash;
END $$

DELIMITER ;

-- =========================================================
-- 8) SEED (minimal demo)
-- =========================================================

INSERT INTO tenants (name, phone, email, city, address, whatsapp_phone)
VALUES ('DemoService', '5550000000', 'demo@service.com', 'Istanbul', 'Demo Address', '5550000000');

-- DEV seed: login olur (şifre: 1234) - projede şu an düz karşılaştırma var diye böyle bıraktım
INSERT INTO users (tenant_id, role, status, email, password_hash)
VALUES (1, 'SERVICE_ADMIN', 'ACTIVE', 'admin@demo.com', '1234');

INSERT INTO customers (tenant_id, first_name, last_name, phone, email)
VALUES
(1, 'Osman', 'Korkmaz', '5551112233', 'osman@example.com'),
(1, 'Ali', 'Yilmaz',  '5552223344', 'ali@example.com');

INSERT INTO drivers (tenant_id, first_name, last_name, license_no, phone)
VALUES (1, 'Veli', 'Ozturk', 'TR002', '5554445566');

INSERT INTO vehicle (tenant_id, customer_id, public_id, plate_no, vin_no, make, model, model_year, colour, current_km, status, notes)
VALUES
(1, 1, '11111111-1111-1111-1111-111111111111', '34ABC123', 'VIN001', 'BMW', '320i', 2018, 'Black', 119000, 'ACTIVE', 'Demo Car 1'),
(1, 2, '22222222-2222-2222-2222-222222222222', '06XYZ789', NULL,     'BMW', '520d', 2016, 'White', 183000, 'IN_SERVICE', 'Demo Car 2');

INSERT INTO maintenance (tenant_id, maint_date, maint_type, odometer_km, description, cost, vehicle_id)
VALUES (1, '2025-12-01', 'Oil Change', 119500, 'Regular maintenance', 2500.00, 1);

CALL sp_create_qr_tag(1, 'HASHED_TOKEN_1', 'AB12CD');
CALL sp_create_qr_tag(1, 'HASHED_TOKEN_2', 'EF34GH');

CALL sp_activate_qr_tag(1, 'HASHED_TOKEN_1', 1);

CALL sp_create_assignment(1, 1, 1, 120000, NOW());

-- DONE
