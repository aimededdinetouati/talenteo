CREATE TABLE employees
(
    id         BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100)   NOT NULL,
    last_name  VARCHAR(100)   NOT NULL,
    email      VARCHAR(150)   NOT NULL,
    hire_date  DATE           NOT NULL,
    department VARCHAR(100)   NOT NULL,
    position   VARCHAR(100)   NOT NULL,
    base_salary DECIMAL(12, 2) NOT NULL,
    bonus      DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status     VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP      NOT NULL,
    updated_at TIMESTAMP      NOT NULL,

    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT chk_employees_base_salary CHECK (base_salary > 0),
    CONSTRAINT chk_employees_bonus CHECK (bonus >= 0),
    CONSTRAINT chk_employees_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
