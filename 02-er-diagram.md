```mermaid
erDiagram
    EMPLOYEE {
        bigint id PK
        varchar first_name "NOT NULL"
        varchar last_name "NOT NULL"
        varchar email "UNIQUE, NOT NULL"
        date hire_date "NOT NULL"
        varchar department "NOT NULL"
        varchar position "NOT NULL"
        decimal base_salary "NOT NULL, > 0"
        decimal bonus "DEFAULT 0"
        enum status "ACTIVE | INACTIVE"
        timestamp created_at
        timestamp updated_at
    }

    PAYROLL_SLIP {
        bigint id PK
        bigint employee_id FK
        smallint period_year "NOT NULL"
        tinyint period_month "NOT NULL, 1-12"
        decimal net_salary "NOT NULL, computed at calculation time"
        enum status "DRAFT | FINALIZED"
        timestamp calculated_at
    }

    PAYROLL_LINE_ITEM {
        bigint id PK
        bigint payroll_slip_id FK
        enum type "EARNING | DEDUCTION"
        enum code "BASE_SALARY | BONUS | INCOME_TAX | SOCIAL_CONTRIBUTION | HIGH_SALARY_SURCHARGE"
        varchar label "human-readable snapshot"
        decimal amount "NOT NULL"
    }

    EMPLOYEE ||--o{ PAYROLL_SLIP : "has many"
    PAYROLL_SLIP ||--o{ PAYROLL_LINE_ITEM : "has many"
```

> **Unique constraint:** `PAYROLL_SLIP(employee_id, period_year, period_month)` — enforced at DB level.
>
> **Net salary** is stored as a denormalized column on `PAYROLL_SLIP`, computed once at calculation time (`SUM(EARNING) - SUM(DEDUCTION)`) and never recalculated.
>
> **Immutability:** once `PAYROLL_SLIP.status = FINALIZED`, no modifications allowed — enforced in the service layer.
>
> **Department and Position** are plain `VARCHAR` fields on `EMPLOYEE` — no separate tables or management APIs.
