CREATE TABLE payroll_line_item
(
    id             BIGSERIAL      PRIMARY KEY,
    payroll_slip_id BIGINT        NOT NULL,
    type           VARCHAR(20)    NOT NULL,
    code           VARCHAR(50)    NOT NULL,
    label          VARCHAR(150)   NOT NULL,
    amount         DECIMAL(12, 2) NOT NULL,

    CONSTRAINT fk_line_item_payroll_slip FOREIGN KEY (payroll_slip_id) REFERENCES payroll_slip (id),
    CONSTRAINT chk_line_item_type CHECK (type IN ('EARNING', 'DEDUCTION'))
);
