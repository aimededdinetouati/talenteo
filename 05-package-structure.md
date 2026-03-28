# Package Structure

**Approach:** Package by Feature with internal layering.

Each feature is self-contained. Inside each feature, the standard Controller/Service/Repository layering is explicit and visible.

---

```
com.talenteo.hr
│
├── employee
│   ├── controller
│   │   └── EmployeeController
│   ├── service
│   │   └── EmployeeService
│   ├── repository
│   │   └── EmployeeRepository
│   ├── domain
│   │   └── Employee                    ← JPA entity
│   └── dto
│       ├── EmployeeRequest
│       └── EmployeeResponse
│
├── payroll
│   ├── controller
│   │   └── PayrollController
│   ├── service
│   │   ├── PayrollService              ← orchestration: save, retrieve, finalize
│   │   └── PayrollCalculator           ← pure math: computes line items from salary/bonus
│   ├── repository
│   │   ├── PayrollSlipRepository
│   │   └── PayrollLineItemRepository
│   ├── domain
│   │   ├── PayrollSlip                 ← JPA entity (header)
│   │   └── PayrollLineItem             ← JPA entity (line items)
│   └── dto
│       ├── PayrollCalculationRequest   ← single employee calculation
│       ├── BulkPayrollRequest          ← bulk calculation (period only)
│       ├── PayrollSlipResponse         ← slip header + line items + net salary
│       └── PayrollSummaryResponse      ← used in history list (no line items)
│
└── common
    ├── exception
    │   ├── ResourceNotFoundException
    │   ├── PayrollAlreadyExistsException
    │   ├── PayrollAlreadyFinalizedException
    │   └── GlobalExceptionHandler      ← @ControllerAdvice
    └── dto
        └── ErrorResponse
```

---

## Key Decisions

**Why package by feature?**
Opening `payroll/` gives you everything about payroll. No jumping between `controller/`, `service/`, `repository/` folders at the root level. Adding a new feature means adding a new folder — existing features are untouched.

**Why internal layering inside each feature?**
The evaluation explicitly checks Controller/Service/Repository separation. The internal `controller/`, `service/`, `repository/` sub-packages make this separation visible and unambiguous.

**Why `PayrollService` and `PayrollCalculator` as two separate classes?**
- `PayrollCalculator` — pure math, no Spring dependencies, no DB access. Takes a salary and bonus, returns a list of line items. Trivially unit testable without any mocks.
- `PayrollService` — orchestration: fetch the employee, call the calculator, enforce business rules (duplicate check, finalization guard), persist the result.

**Why no `department/` or `position/` feature packages?**
Department and position are inline `VARCHAR` fields on the Employee entity. They have no dedicated APIs, no separate tables, and no management lifecycle. They live entirely within the `employee` feature.

**What belongs in `common/`?**
Only two things:
- Exceptions — because `GlobalExceptionHandler` must see all of them
- `ErrorResponse` DTO — the unified error envelope returned by the handler

Nothing else goes in `common/`. It is not a utility dumping ground.
