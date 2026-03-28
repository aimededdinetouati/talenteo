# API Contract

Base path: `/api/v1`

All error responses follow RFC 7807 (see `06-technical-decisions.md` TD-001).
All list endpoints return Spring's `Page<T>` envelope (see TD-007).

---

## Employee

### POST /api/v1/employees
Create a new employee.

**Request body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@company.com",
  "hireDate": "2024-01-15",
  "department": "Engineering",
  "position": "Backend Developer",
  "baseSalary": 5000.00,
  "bonus": 500.00
}
```

| Field | Required | Constraint |
|---|---|---|
| firstName | yes | not blank |
| lastName | yes | not blank |
| email | yes | valid email format, unique |
| hireDate | yes | not null |
| department | yes | not blank |
| position | yes | not blank |
| baseSalary | yes | > 0 |
| bonus | no | >= 0, defaults to 0 |

**Response: `201 Created`**
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@company.com",
  "hireDate": "2024-01-15",
  "department": "Engineering",
  "position": "Backend Developer",
  "baseSalary": 5000.00,
  "bonus": 500.00,
  "status": "ACTIVE",
  "createdAt": "2026-03-27T10:00:00Z",
  "updatedAt": "2026-03-27T10:00:00Z"
}
```

**Error responses:**
- `400` — validation failure (invalid field format or missing required field)
- `409` — email already exists

---

### GET /api/v1/employees
List all employees with optional filters. Paginated.

**Query parameters:**

| Parameter | Required | Description |
|---|---|---|
| status | no | Filter by `ACTIVE` or `INACTIVE` |
| department | no | Filter by department (plain string match) |
| page | no | Page index, default `0` |
| size | no | Page size, default `20` |

**Response: `200 OK`** — `Page<EmployeeDto>`

---

### GET /api/v1/employees/{id}
Retrieve a single employee by ID.

**Response: `200 OK`** — same shape as POST response.

**Error responses:**
- `404` — employee not found

---

### PUT /api/v1/employees/{id}
Full update of an employee's information.

**Request body:** same fields as POST (all fields required).

**Response: `200 OK`** — updated employee, same shape as POST response.

**Error responses:**
- `400` — validation failure
- `404` — employee not found
- `409` — email already taken by another employee

---

### DELETE /api/v1/employees/{id}
Deactivate an employee (soft delete). Sets status to `INACTIVE`.

**Response: `204 No Content`**

**Error responses:**
- `404` — employee not found

---

## Payroll

### POST /api/v1/employees/{id}/payroll/{year}/{month}
Calculate payroll for a specific employee and period. Creates a `DRAFT` slip.

**Path variables:**

| Variable | Constraint |
|---|---|
| id | existing employee id |
| year | >= 2000 |
| month | 1–12 |

**Request body:** none.

**Response: `201 Created`**
```json
{
  "id": 10,
  "employeeId": 1,
  "periodYear": 2026,
  "periodMonth": 3,
  "status": "DRAFT",
  "calculatedAt": "2026-03-27T10:00:00Z",
  "lineItems": [
    { "type": "EARNING",   "code": "BASE_SALARY",           "label": "Base Salary",              "amount": 5000.00 },
    { "type": "EARNING",   "code": "BONUS",                 "label": "Monthly Bonus",             "amount": 500.00  },
    { "type": "DEDUCTION", "code": "INCOME_TAX",            "label": "Income Tax (20%)",          "amount": 1000.00 },
    { "type": "DEDUCTION", "code": "SOCIAL_CONTRIBUTION",   "label": "Social Contribution (5%)",  "amount": 250.00  }
  ],
  "netSalary": 4250.00
}
```

**Error responses:**
- `404` — employee not found
- `409` — payroll already exists for this period
- `422` — employee is INACTIVE

---

### POST /api/v1/payroll/bulk
Calculate payroll for all `ACTIVE` employees for a given period.
Employees who already have a slip for the period are skipped.

**Request body:**
```json
{
  "year": 2026,
  "month": 3
}
```

| Field | Required | Constraint |
|---|---|---|
| year | yes | >= 2000 |
| month | yes | 1–12 |

**Response: `200 OK`**
```json
{
  "created": [
    { "employeeId": 1, "employeeName": "John Doe",    "period": "2026-03" },
    { "employeeId": 2, "employeeName": "Jane Smith",  "period": "2026-03" }
  ],
  "skipped": [
    { "employeeId": 3, "employeeName": "Bob Martin",  "reason": "Payroll already exists for this period" }
  ]
}
```

**Error responses:**
- `400` — invalid year or month

---

### POST /api/v1/employees/{id}/payroll/{year}/{month}/finalize
Finalize a `DRAFT` payroll slip. Once finalized, the slip is immutable.

**Request body:** none.

**Response: `200 OK`** — finalized slip, same shape as payroll calculation response with `status: "FINALIZED"`.

**Error responses:**
- `404` — payroll slip not found
- `409` — payroll slip is already finalized

---

### GET /api/v1/employees/{id}/payroll/{year}/{month}
Retrieve a specific payroll slip with full line items breakdown.

**Response: `200 OK`** — same shape as payroll calculation response.

**Error responses:**
- `404` — employee not found or no payroll slip for this period

---

### GET /api/v1/employees/{id}/payroll
List payroll history for an employee. Paginated, ordered by period descending.

**Query parameters:**

| Parameter | Required | Description |
|---|---|---|
| page | no | Page index, default `0` |
| size | no | Page size, default `20` |

**Response: `200 OK`** — `Page<PayrollSlipDto>` (each slip includes line items and computed net salary).

**Error responses:**
- `404` — employee not found

---

## Endpoint Summary

| Method | Path | Use Case | Status |
|---|---|---|---|
| POST | `/api/v1/employees` | Create employee | 201 |
| GET | `/api/v1/employees` | List employees | 200 |
| GET | `/api/v1/employees/{id}` | View employee | 200 |
| PUT | `/api/v1/employees/{id}` | Update employee | 200 |
| DELETE | `/api/v1/employees/{id}` | Deactivate employee | 204 |
| POST | `/api/v1/employees/{id}/payroll/{year}/{month}` | Calculate payroll | 201 |
| POST | `/api/v1/payroll/bulk` | Bulk calculate payroll | 200 |
| POST | `/api/v1/employees/{id}/payroll/{year}/{month}/finalize` | Finalize payroll slip | 200 |
| GET | `/api/v1/employees/{id}/payroll/{year}/{month}` | View payroll slip | 200 |
| GET | `/api/v1/employees/{id}/payroll` | List payroll history | 200 |
