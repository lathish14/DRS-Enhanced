-- =====================================================================
--  DRS-Enhanced - MySQL Database Script
--  COIT20258 Software Engineering - Assessment 3
--
--  Disaster Response System (Enhanced, three-tier)
--
--  NOTE: The server creates this schema and all tables PROGRAMMATICALLY
--  at startup (see drs.server.db.DatabaseInitialiser), as required by the
--  assignment. This standalone script is provided as well, so the database
--  can be created and populated manually if preferred, and to document the
--  schema. Running the server after this script is harmless: it only seeds
--  default rows when a table is empty.
--
--  Usage:
--    mysql -u root -p < drs_enhanced_schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS drs_enhanced
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE drs_enhanced;

-- ---------------------------------------------------------------------
--  Drop existing tables (children first, for clean re-creation)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS dispatches;
DROP TABLE IF EXISTS resources;
DROP TABLE IF EXISTS response_logs;
DROP TABLE IF EXISTS disasters;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS users;

-- ---------------------------------------------------------------------
--  users - authentication and role-based access control
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id     INT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    role        VARCHAR(30)  NOT NULL,
    department  VARCHAR(80),
    active      TINYINT(1)   NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
--  departments - external responding organisations
-- ---------------------------------------------------------------------
CREATE TABLE departments (
    department_id   INT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(80) NOT NULL UNIQUE,
    contact_number  VARCHAR(30),
    contact_email   VARCHAR(120),
    specialization  VARCHAR(60),
    response_status VARCHAR(30) NOT NULL DEFAULT 'STANDBY'
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
--  disasters - core reported events
-- ---------------------------------------------------------------------
CREATE TABLE disasters (
    disaster_id          INT AUTO_INCREMENT PRIMARY KEY,
    disaster_type        VARCHAR(50)  NOT NULL,
    location             VARCHAR(150) NOT NULL,
    description          TEXT         NOT NULL,
    severity_score       INT          NOT NULL DEFAULT 0,
    priority_level       VARCHAR(10)  NOT NULL DEFAULT 'LOW',
    status               VARCHAR(30)  NOT NULL DEFAULT 'REPORTED',
    reported_at          DATETIME     NOT NULL,
    reported_by          VARCHAR(100) NOT NULL,
    notified_departments VARCHAR(400) DEFAULT ''
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
--  response_logs - audit trail for accountability
-- ---------------------------------------------------------------------
CREATE TABLE response_logs (
    log_id             INT AUTO_INCREMENT PRIMARY KEY,
    disaster_id        INT NOT NULL,
    action_by          VARCHAR(50)  NOT NULL,
    action_description VARCHAR(400) NOT NULL,
    action_type        VARCHAR(40)  NOT NULL,
    log_time           DATETIME     NOT NULL,
    INDEX idx_logs_disaster (disaster_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
--  resources - Enhanced Feature 1: deployable units per department
-- ---------------------------------------------------------------------
CREATE TABLE resources (
    resource_id   INT AUTO_INCREMENT PRIMARY KEY,
    department_id INT NOT NULL,
    resource_name VARCHAR(80) NOT NULL,
    resource_type VARCHAR(40) NOT NULL,
    total_units   INT NOT NULL DEFAULT 0,
    deployed_units INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_resource_dept FOREIGN KEY (department_id)
        REFERENCES departments(department_id),
    INDEX idx_resource_dept (department_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
--  dispatches - Enhanced Feature 1: resource allocations to disasters
-- ---------------------------------------------------------------------
CREATE TABLE dispatches (
    dispatch_id      INT AUTO_INCREMENT PRIMARY KEY,
    disaster_id      INT NOT NULL,
    resource_id      INT NOT NULL,
    units_dispatched INT NOT NULL,
    dispatched_by    VARCHAR(50) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'DEPLOYED',
    dispatched_at    DATETIME    NOT NULL,
    CONSTRAINT fk_dispatch_disaster FOREIGN KEY (disaster_id)
        REFERENCES disasters(disaster_id),
    CONSTRAINT fk_dispatch_resource FOREIGN KEY (resource_id)
        REFERENCES resources(resource_id),
    INDEX idx_dispatch_disaster (disaster_id)
) ENGINE=InnoDB;

-- =====================================================================
--  SEED DATA
-- =====================================================================

-- Default departments (the eight standard DRS departments)
INSERT INTO departments
    (department_name, contact_number, contact_email, specialization, response_status)
VALUES
    ('Fire & Emergency','000','fire@drs.gov.au','Fire','STANDBY'),
    ('Hospital','131450','hospital@drs.gov.au','Medical','STANDBY'),
    ('Electricity','132051','electricity@drs.gov.au','Infrastructure','STANDBY'),
    ('Transportation','131500','transport@drs.gov.au','Transport','STANDBY'),
    ('Waste Management','1300123456','waste@drs.gov.au','Environmental','STANDBY'),
    ('Water Supply','132525','water@drs.gov.au','Infrastructure','STANDBY'),
    ('Law Enforcement','000','police@drs.gov.au','Security','STANDBY'),
    ('Schools','131872','schools@drs.gov.au','Community','STANDBY');

-- Default user accounts (one per role)
INSERT INTO users
    (full_name, username, password, role, department, active)
VALUES
    ('Admin User','admin','admin123','ADMIN','Administration',1),
    ('John Coordinator','coordinator','coord123','COORDINATOR','Emergency Management',1),
    ('Jane Public','public','pub123','PUBLIC','General Public',1),
    ('Sam Staff','staff','staff123','DEPARTMENT_STAFF','Fire & Emergency',1);

-- Default resources for Feature 1 (department IDs resolved by name)
INSERT INTO resources (department_id, resource_name, resource_type, total_units, deployed_units)
SELECT department_id, 'Fire Truck',       'Vehicle',   8, 0 FROM departments WHERE department_name='Fire & Emergency';
INSERT INTO resources (department_id, resource_name, resource_type, total_units, deployed_units)
SELECT department_id, 'Rescue Team',      'Personnel',12, 0 FROM departments WHERE department_name='Fire & Emergency';
INSERT INTO resources (department_id, resource_name, resource_type, total_units, deployed_units)
SELECT department_id, 'Ambulance',        'Vehicle',  10, 0 FROM departments WHERE department_name='Hospital';
INSERT INTO resources (department_id, resource_name, resource_type, total_units, deployed_units)
SELECT department_id, 'Medical Team',     'Personnel',15, 0 FROM departments WHERE department_name='Hospital';
INSERT INTO resources (department_id, resource_name, resource_type, total_units, deployed_units)
SELECT department_id, 'Repair Crew',      'Personnel', 6, 0 FROM departments WHERE department_name='Electricity';
INSERT INTO resources (department_id, resource_name, resource_type, total_units, deployed_units)
SELECT department_id, 'Mobile Generator', 'Equipment', 5, 0 FROM departments WHERE department_name='Electricity';
INSERT INTO resources (department_id, resource_name, resource_type, total_units, deployed_units)
SELECT department_id, 'Evacuation Bus',   'Vehicle',   7, 0 FROM departments WHERE department_name='Transportation';
INSERT INTO resources (department_id, resource_name, resource_type, total_units, deployed_units)
SELECT department_id, 'Water Tanker',     'Vehicle',   4, 0 FROM departments WHERE department_name='Water Supply';
INSERT INTO resources (department_id, resource_name, resource_type, total_units, deployed_units)
SELECT department_id, 'Patrol Unit',      'Vehicle',  14, 0 FROM departments WHERE department_name='Law Enforcement';
INSERT INTO resources (department_id, resource_name, resource_type, total_units, deployed_units)
SELECT department_id, 'Debris Truck',     'Vehicle',   6, 0 FROM departments WHERE department_name='Waste Management';

-- =====================================================================
--  Verification queries (optional)
-- =====================================================================
SELECT 'departments' AS table_name, COUNT(*) AS row_count FROM departments
UNION ALL SELECT 'users', COUNT(*) FROM users
UNION ALL SELECT 'resources', COUNT(*) FROM resources;
