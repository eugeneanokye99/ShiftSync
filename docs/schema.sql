-- ============================================================================
-- ShiftSync Database Schema
-- PostgreSQL 12+
-- ============================================================================

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

CREATE TYPE user_role AS ENUM ('EMPLOYEE', 'MANAGER', 'HR_ADMIN');

CREATE TYPE employment_type AS ENUM ('FULL_TIME', 'PART_TIME', 'CONTRACT');

CREATE TYPE leave_type AS ENUM ('ANNUAL', 'SICK', 'UNPAID');

CREATE TYPE leave_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED');

CREATE TYPE shift_status AS ENUM ('OPEN', 'CANCELLED');

CREATE TYPE swap_status AS ENUM ('PENDING_MANAGER_APPROVAL', 'APPROVED', 'REJECTED');

CREATE TYPE notification_type AS ENUM (
    'SHIFT_ASSIGNED',
    'SHIFT_REMOVED',
    'SHIFT_CANCELLED',
    'SHIFT_CHANGED',
    'LEAVE_UPDATED',
    'SWAP_OUTCOME',
    'LEAVE_CONFLICT'
);

CREATE TYPE audit_action AS ENUM ('CREATE', 'UPDATE', 'DELETE');

CREATE TYPE day_of_week AS ENUM (
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
    'SUNDAY'
);

-- ============================================================================
-- CORE TABLES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Users Table (Authentication)
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role user_role NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- ----------------------------------------------------------------------------
-- Locations Table
-- ----------------------------------------------------------------------------
CREATE TABLE locations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    address TEXT NOT NULL,
    max_headcount_per_shift INTEGER NOT NULL DEFAULT 50,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_locations_active ON locations(active);

-- ----------------------------------------------------------------------------
-- Departments Table
-- ----------------------------------------------------------------------------
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    location_id BIGINT NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(location_id, name)
);

CREATE INDEX idx_departments_location ON departments(location_id);

-- ----------------------------------------------------------------------------
-- Employees Table
-- ----------------------------------------------------------------------------
CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    phone VARCHAR(50),
    employment_type employment_type NOT NULL,
    department_id BIGINT NOT NULL REFERENCES departments(id),
    location_id BIGINT NOT NULL REFERENCES locations(id),
    skills TEXT[], -- Array of skill tags
    contracted_weekly_hours DECIMAL(5,2) NOT NULL,
    hire_date DATE NOT NULL DEFAULT CURRENT_DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deactivated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employees_user ON employees(user_id);
CREATE INDEX idx_employees_department ON employees(department_id);
CREATE INDEX idx_employees_location ON employees(location_id);
CREATE INDEX idx_employees_active ON employees(active);
CREATE INDEX idx_employees_employment_type ON employees(employment_type);

-- ----------------------------------------------------------------------------
-- Manager Location Assignments
-- ----------------------------------------------------------------------------
CREATE TABLE manager_locations (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    location_id BIGINT NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(manager_id, location_id)
);

CREATE INDEX idx_manager_locations_manager ON manager_locations(manager_id);
CREATE INDEX idx_manager_locations_location ON manager_locations(location_id);

-- ----------------------------------------------------------------------------
-- Recurring Availability (Weekly Pattern)
-- ----------------------------------------------------------------------------
CREATE TABLE recurring_availability (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    day_of_week day_of_week NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_time_order CHECK (end_time > start_time)
);

CREATE INDEX idx_recurring_availability_employee ON recurring_availability(employee_id);
CREATE INDEX idx_recurring_availability_day ON recurring_availability(day_of_week);

-- ----------------------------------------------------------------------------
-- Availability Overrides (One-off blocks)
-- ----------------------------------------------------------------------------
CREATE TABLE availability_overrides (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_date_order CHECK (end_date >= start_date)
);

CREATE INDEX idx_availability_overrides_employee ON availability_overrides(employee_id);
CREATE INDEX idx_availability_overrides_dates ON availability_overrides(start_date, end_date);

-- ----------------------------------------------------------------------------
-- Leave Requests
-- ----------------------------------------------------------------------------
CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    leave_type leave_type NOT NULL,
    reason TEXT,
    status leave_status NOT NULL DEFAULT 'PENDING',
    hr_note TEXT,
    approved_by BIGINT REFERENCES users(id),
    approved_at TIMESTAMP,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_leave_date_order CHECK (end_date >= start_date)
);

CREATE INDEX idx_leave_requests_employee ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_leave_requests_dates ON leave_requests(start_date, end_date);
CREATE INDEX idx_leave_requests_submitted ON leave_requests(submitted_at);

-- ----------------------------------------------------------------------------
-- Shifts
-- ----------------------------------------------------------------------------
CREATE TABLE shifts (
    id BIGSERIAL PRIMARY KEY,
    location_id BIGINT NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    department_id BIGINT NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    shift_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    required_skill VARCHAR(255),
    minimum_headcount INTEGER NOT NULL DEFAULT 1,
    status shift_status NOT NULL DEFAULT 'OPEN',
    created_by BIGINT NOT NULL REFERENCES users(id),
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_shift_time_order CHECK (end_time > start_time)
);

CREATE INDEX idx_shifts_location ON shifts(location_id);
CREATE INDEX idx_shifts_department ON shifts(department_id);
CREATE INDEX idx_shifts_date ON shifts(shift_date);
CREATE INDEX idx_shifts_status ON shifts(status);
CREATE INDEX idx_shifts_location_date ON shifts(location_id, shift_date);

-- ----------------------------------------------------------------------------
-- Shift Assignments
-- ----------------------------------------------------------------------------
CREATE TABLE shift_assignments (
    id BIGSERIAL PRIMARY KEY,
    shift_id BIGINT NOT NULL REFERENCES shifts(id) ON DELETE CASCADE,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    assigned_by BIGINT NOT NULL REFERENCES users(id),
    override_applied BOOLEAN NOT NULL DEFAULT FALSE,
    override_reason TEXT,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(shift_id, employee_id)
);

CREATE INDEX idx_shift_assignments_shift ON shift_assignments(shift_id);
CREATE INDEX idx_shift_assignments_employee ON shift_assignments(employee_id);
CREATE INDEX idx_shift_assignments_assigned_at ON shift_assignments(assigned_at);

-- ----------------------------------------------------------------------------
-- Shift Swap Requests
-- ----------------------------------------------------------------------------
CREATE TABLE swap_requests (
    id BIGSERIAL PRIMARY KEY,
    requesting_employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    requesting_assignment_id BIGINT NOT NULL REFERENCES shift_assignments(id) ON DELETE CASCADE,
    target_employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    target_assignment_id BIGINT REFERENCES shift_assignments(id) ON DELETE CASCADE,
    reason TEXT,
    status swap_status NOT NULL DEFAULT 'PENDING_MANAGER_APPROVAL',
    manager_note TEXT,
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_swap_requests_requesting_employee ON swap_requests(requesting_employee_id);
CREATE INDEX idx_swap_requests_target_employee ON swap_requests(target_employee_id);
CREATE INDEX idx_swap_requests_status ON swap_requests(status);

-- ----------------------------------------------------------------------------
-- Notifications
-- ----------------------------------------------------------------------------
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type notification_type NOT NULL,
    message TEXT NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(user_id, read);
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);

-- ----------------------------------------------------------------------------
-- Audit Log
-- ----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    action audit_action NOT NULL,
    actor_id BIGINT NOT NULL REFERENCES users(id),
    actor_role user_role NOT NULL,
    before_state JSONB,
    after_state JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);

-- ============================================================================
-- TRIGGERS FOR UPDATED_AT
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_locations_updated_at BEFORE UPDATE ON locations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_departments_updated_at BEFORE UPDATE ON departments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_employees_updated_at BEFORE UPDATE ON employees
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_recurring_availability_updated_at BEFORE UPDATE ON recurring_availability
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_availability_overrides_updated_at BEFORE UPDATE ON availability_overrides
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_leave_requests_updated_at BEFORE UPDATE ON leave_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_shifts_updated_at BEFORE UPDATE ON shifts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_shift_assignments_updated_at BEFORE UPDATE ON shift_assignments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_swap_requests_updated_at BEFORE UPDATE ON swap_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE users IS 'Authentication and user account information';
COMMENT ON TABLE employees IS 'Employee profiles with soft delete support';
COMMENT ON TABLE locations IS 'Physical branch locations operated by HHG';
COMMENT ON TABLE departments IS 'Work areas within locations';
COMMENT ON TABLE manager_locations IS 'Manager-to-location assignment mapping';
COMMENT ON TABLE recurring_availability IS 'Weekly recurring availability patterns';
COMMENT ON TABLE availability_overrides IS 'One-off unavailability blocks';
COMMENT ON TABLE leave_requests IS 'Formal time-off requests with approval workflow';
COMMENT ON TABLE shifts IS 'Scheduled work blocks';
COMMENT ON TABLE shift_assignments IS 'Employee-to-shift assignments with conflict override tracking';
COMMENT ON TABLE swap_requests IS 'Shift swap proposals between employees';
COMMENT ON TABLE notifications IS 'In-system notification inbox';
COMMENT ON TABLE audit_logs IS 'Immutable audit trail of all write operations';
