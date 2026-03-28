# Architecture Decision Records

A living document of design decisions made during the conception phase.

---

## DM-001 — Department and Position as inline string fields on Employee

**Options:**
- A: Plain `VARCHAR` fields on the employee table
- B: FK references to dedicated `departments` and `positions` tables

**Decision:** Option A — inline `VARCHAR` fields on the employee table. No separate tables, no dedicated APIs.

**Rationale:** Keeping department and position as simple string fields reduces scope and complexity without losing meaningful information. No management endpoints are needed for them — they are set inline when creating or updating an employee.

---

## DM-002 — Employee deletion is a soft delete

**Options:**
- A: Hard delete — `DELETE FROM employees WHERE id = ?`
- B: Soft delete — `status` field (`ACTIVE`, `INACTIVE`), never remove the row

**Decision:** Option B — soft delete via `status` enum.

**Rationale:** HR systems must preserve history. A terminated employee's payroll records must remain queryable. Hard delete would orphan or cascade-delete payroll data.

---

## DM-003 — Payroll modeled as Header + Line Items, not a flat table

**Options:**
- A: Flat table — one row per payroll with fixed columns
- B: `payroll_slip` as header + `payroll_line_item` rows for each earning/deduction

**Decision:** Option B — header + line items.

**Rationale:** Flat table is rigid — adding a new deduction type requires a schema change. Line items support variable structure, mirror real payroll systems, and make immutability natural.

---

## DM-004 — Net salary stored as denormalized column on payroll_slip

**Options:**
- A: Compute at query time: `SUM(EARNING) - SUM(DEDUCTION)` — no storage needed
- B: Denormalized column `net_salary` on `payroll_slip` — computed once at calculation time and stored

**Decision:** Option B — stored in DB.

**Rationale:** Payroll is immutable once finalized, so the risk of inconsistency is eliminated. Storing net salary avoids an aggregation query on every read, and makes the value immediately available in list responses without joining line items.

---

## DM-005 — Payroll immutability enforced at two levels

**Decision:**
1. **Database level** — unique constraint on `(employee_id, period_year, period_month)` prevents duplicate slips
2. **Service level** — once `status = FINALIZED`, the service rejects modifications

**Rationale:** Defense in depth. DB constraint is the last line of defense; service check gives a meaningful error to the API consumer.

---

## DM-006 — Salary and bonus snapshotted as line items at calculation time

**Decision:** At calculation time, `base_salary` and `bonus` are copied from the employee record into `EARNING` line items. They are never referenced by FK.

**Rationale:** If the employee's salary changes later, past payroll slips are untouched. The `label` field also snapshots the rate (e.g., `"Income Tax (20%)"`) so rule changes don't affect historical records.

---

## UC-001 — Payroll calculation and finalization are two separate steps

**Options:**
- A: Calculate and finalize in one step — payroll is immediately immutable
- B: Two steps — calculate (DRAFT), then explicitly finalize (FINALIZED)

**Decision:** Option B — two separate steps.

**Rationale:** HR needs to review the payroll slip before committing it. The DRAFT state allows verification. Only after HR validates does the slip become FINALIZED and immutable.

---

## UC-002 — Payroll can be calculated for past and future periods

**Decision:** No restriction on the period date. HR can calculate payroll for any month/year combination.

**Rationale:** HR may need to process backdated payroll (late hires, corrections) or prepare payroll in advance.

---

## UC-003 — Duplicate payroll returns 409

**Decision:** If a payroll slip already exists for a given `(employee_id, period_year, period_month)`, the API returns `409 Conflict`.

**Rationale:** Silent overwrite would be dangerous in a financial context. The HR must be explicit — if a recalculation is needed, the existing slip must be handled first.

---

## UC-004 — Bulk payroll calculation is in scope

**Decision:** The system supports calculating payroll for all active employees for a given period in a single request.

**Rationale:** In practice, HR runs payroll for the entire company at once, not one employee at a time. Individual calculation remains available for corrections or new hires.

**Conflict behavior:** Employees who already have a slip for that period are silently skipped. The rest are processed normally. The response reports which were created and which were skipped.

---

## DM-007 — Tax and deduction rules hardcoded in service

**Decision:** Rules are externalized to `application.yml` via `@ConfigurationProperties`. The calculator reads rates from an injected config object — no magic numbers in code.

**Config structure (`application.yml`):**
```yaml
payroll:
  rules:
    income-tax-rate: 0.20
    social-contribution-rate: 0.05
    high-salary-threshold: 5000.00
    high-salary-surcharge-rate: 0.05
```

**Rules applied:**

| Rule | Code | Type | Calculation |
|------|------|------|-------------|
| Base salary | `BASE_SALARY` | EARNING | Employee's current base salary |
| Bonus | `BONUS` | EARNING | Employee's bonus (if > 0) |
| Income tax | `INCOME_TAX` | DEDUCTION | `baseSalary × income-tax-rate` |
| Social contribution | `SOCIAL_CONTRIBUTION` | DEDUCTION | `baseSalary × social-contribution-rate` |
| High salary surcharge | `HIGH_SALARY_SURCHARGE` | DEDUCTION | `baseSalary × high-salary-surcharge-rate`, only if `baseSalary > high-salary-threshold` |

**Rationale:** Tax rates change in real systems without code changes. Externalizing rates to config means a rate update requires only a config change and restart — not a recompile and redeploy. `PayrollCalculator` remains a pure service injected with a config bean, keeping it fully testable. A DB rules table (Option C) would be more flexible but is over-engineering for this exercise scope.
