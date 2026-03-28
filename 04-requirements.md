# Requirements & Use Cases

**Actor:** HR Manager (the only actor in scope)

---

## Employee Management

### UC-EM-01 — Create an employee
The HR manager can create a new employee by providing their personal information, employment details, and compensation data.

**Preconditions:** The email must not already exist in the system.

**Outcome:** A new employee is created with status `ACTIVE`.

---

### UC-EM-02 — View an employee
The HR manager can retrieve the full profile of an employee by their ID.

**Outcome:** Returns personal info, employment details, compensation data, and current status.

---

### UC-EM-03 — List employees
The HR manager can retrieve a list of all employees.

- By default, returns all employees including `INACTIVE` ones
- HR can filter by status (`ACTIVE` / `INACTIVE`)
- HR can filter by department (plain string match)

---

### UC-EM-04 — Update an employee
The HR manager can update an employee's information (e.g., salary raise, department change, name correction).

**Preconditions:** Employee must exist.

**Note:** Updating salary does not affect past payroll slips — those are already snapshotted.

---

### UC-EM-05 — Deactivate an employee
The HR manager can deactivate an employee when they leave the company.

**Outcome:** Employee status changes to `INACTIVE`. The employee record and all payroll history are preserved.

---

## Payroll

### UC-PR-01 — Calculate payroll for one employee
The HR manager can trigger a payroll calculation for a specific employee and a given period (month + year).

**Preconditions:**
- Employee must exist and be `ACTIVE`
- No payroll slip must already exist for that employee and period — otherwise `409 Conflict`

**Outcome:** A `DRAFT` payroll slip is created with all earning and deduction line items snapshotted.

**Note:** Past and future periods are allowed.

---

### UC-PR-02 — Calculate payroll in bulk
The HR manager can trigger payroll calculation for all `ACTIVE` employees for a given period in a single request.

**Preconditions:** No payroll slip must already exist for any of the targeted employees for that period.

**Behavior:** If some employees already have a slip for that period, those are skipped — the rest are calculated normally.

**Outcome:** A `DRAFT` payroll slip is created for each eligible active employee.

---

### UC-PR-03 — Finalize a payroll slip
The HR manager can finalize a `DRAFT` payroll slip after reviewing it.

**Preconditions:** The payroll slip must exist and be in `DRAFT` status.

**Outcome:** Slip status changes to `FINALIZED`. No further modifications are possible.

---

### UC-PR-04 — View a payroll slip
The HR manager can retrieve a specific payroll slip for an employee and period.

**Outcome:** Returns the slip header (period, status, calculated_at) and all line items. The net salary is computed from the line items.

---

### UC-PR-05 — List payroll history for an employee
The HR manager can retrieve all payroll slips for a given employee, ordered by period descending.

**Outcome:** Returns a list of slips with their period, status, and net salary.
