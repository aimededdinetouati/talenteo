CREATE TABLE payroll_slip
(
    id           BIGSERIAL      PRIMARY KEY,
    employee_id  BIGINT         NOT NULL,
    period_year  SMALLINT       NOT NULL,
    period_month SMALLINT       NOT NULL,
    net_salary   DECIMAL(12, 2) NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    calculated_at TIMESTAMP     NOT NULL,

    CONSTRAINT fk_payroll_slip_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT uk_payroll_slip_period UNIQUE (employee_id, period_year, period_month),
    CONSTRAINT chk_payroll_slip_month CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT chk_payroll_slip_year CHECK (period_year >= 2000),
    CONSTRAINT chk_payroll_slip_status CHECK (status IN ('DRAFT', 'FINALIZED'))
);
