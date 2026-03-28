# Technical Decisions

Cross-cutting technical decisions for error handling and validation.

---

## TD-001 — Error handling follows RFC 7807 (Problem Details)

**Decision:** All error responses follow RFC 7807 using Spring Boot's native `ProblemDetail` support. No external library (e.g. Zalando) needed.

**Standard fields:**
- `type` — URI identifying the problem type (defaults to `about:blank`)
- `title` — short human-readable summary of the problem
- `status` — HTTP status code
- `detail` — human-readable explanation specific to this occurrence
- `instance` — the request URI that triggered the error

**Extension:** For validation errors (400), a custom `errors` property is added via `ProblemDetail.setProperty()` containing a map of field → message:

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/employees",
  "errors": {
    "email": "must be a valid email address",
    "baseSalary": "must be greater than 0"
  }
}
```

**Rationale:** RFC 7807 is the industry standard for HTTP error responses. Spring Boot 3+ supports it natively. The `errors` extension is necessary for validation failures — without it, all field errors collapse into a single generic message which is not actionable for the client.

**Implementation:** A single `@ControllerAdvice` class (`GlobalExceptionHandler`) handles all exceptions and maps them to `ProblemDetail`.

**Status code mapping:**

| Scenario | Exception | Status |
|---|---|---|
| Employee not found | `ResourceNotFoundException` | 404 |
| Payroll slip not found | `ResourceNotFoundException` | 404 |
| DTO field validation failure | `MethodArgumentNotValidException` | 400 |
| Duplicate email on create | `DuplicateEmailException` | 409 |
| Payroll already exists for period | `PayrollAlreadyExistsException` | 409 |
| Payroll already finalized | `PayrollAlreadyFinalizedException` | 409 |
| Calculating payroll for INACTIVE employee | `InactiveEmployeeException` | 422 |

---

## TD-002 — Validation split between DTO annotations and service layer

**Decision:** Validation is applied at two levels depending on whether the rule requires DB context.

**Rule:**
- **DTO annotation** (`@NotBlank`, `@Email`, `@NotNull`, `@Min`, `@Max`, etc.) — for structural and format rules that are always true regardless of system state. Validated automatically by Spring via `@Valid` on the controller method parameter.
- **Service layer** — for rules that require querying the DB or checking business state.

**DTO annotation examples:**
- `first_name`, `last_name` — `@NotBlank`
- `email` — `@NotBlank`, `@Email`
- `hire_date` — `@NotNull`
- `department`, `position` — `@NotBlank`
- `base_salary` — `@NotNull`, `@DecimalMin("0.01")`
- `bonus` — `@DecimalMin("0.0")`
- `period_month` — `@Min(1)`, `@Max(12)`
- `period_year` — `@Min(2000)`

**Service validation examples:**
- Email uniqueness on create → query DB → throw `DuplicateEmailException` → 409
- Employee exists → query DB → throw `ResourceNotFoundException` → 404
- Employee is ACTIVE before payroll calculation → check status field → throw `InactiveEmployeeException` → 422
- Payroll slip already exists for period → query DB → throw `PayrollAlreadyExistsException` → 409
- Payroll slip is DRAFT before finalization → check status field → throw `PayrollAlreadyFinalizedException` → 409

**Rationale:** DTO annotations catch bad input early, before the service is even called. Service validation handles rules that require state awareness. Mixing the two in the wrong layer leads to either anemic services or bloated DTOs.

---

## TD-003 — Entity ↔ DTO mapping via ModelMapper singleton bean

**Decision:** ModelMapper is used for entity-to-DTO and DTO-to-entity conversion. A single `ModelMapper` instance is declared as a Spring `@Bean` in a `@Configuration` class and injected wherever mapping is needed.

**Rationale:** ModelMapper is expensive to instantiate — it scans and caches type mappings at creation time. A singleton bean pays that cost once at startup. Never instantiate with `new ModelMapper()` inside a service or method.

**Usage:** Services inject `ModelMapper` and call `modelMapper.map(source, TargetClass.class)`. Custom mappings (if needed) are configured once on the bean definition.

---

## TD-004 — Single DTO class per resource (no request/response split)

**Decision:** One DTO class per resource (e.g., `EmployeeDto`, `PayrollSlipDto`). No separate `EmployeeRequest` / `EmployeeResponse` classes.

**Rationale:** Keeps the number of classes low for the scope of this exercise. Fields like `id` and `createdAt` that only make sense in responses are simply nullable/ignored on input.

---

## TD-005 — Database: PostgreSQL with Flyway migrations

**Decision:** PostgreSQL is the database for both development and production. Schema is managed with Flyway — no `ddl-auto=create` or `ddl-auto=update`.

**Migration conventions:**
- Files in `src/main/resources/db/migration/`
- Named `V{version}__{description}.sql` (e.g., `V1__create_employee_table.sql`)
- Each table gets its own migration file
- Migrations are append-only — never edit an existing migration file

**Rationale:** Flyway migrations are version-controlled, repeatable, and match how schema changes are managed in production systems. Using `ddl-auto` is acceptable for prototypes but signals lack of production-readiness to evaluators.

---

## TD-006 — Testing: unit tests + integration tests with Testcontainers

**Two levels of testing:**

**Unit tests:**
- Target: `PayrollCalculator` and any pure business logic
- Tool: JUnit 5 + Mockito
- No Spring context loaded — plain Java instantiation
- Services are tested with mocked repositories
- Fast, no infrastructure needed

**Integration tests:**
- Target: full request-to-DB flow per feature
- Tool: `@SpringBootTest` + `MockMvc` + Testcontainers (PostgreSQL container)
- One test class per controller (e.g., `EmployeeControllerIT`, `PayrollControllerIT`)
- Tests make real HTTP calls through MockMvc, hit a real PostgreSQL instance, assert on response body and DB state
- Flyway migrations run automatically on the test container at startup

**Rationale:** Testcontainers with PostgreSQL catches issues that H2 would miss (type differences, constraint behavior, SQL dialect). `@SpringBootTest` + `MockMvc` tests the full stack in one shot without the fragility of splitting into web/data slices.

---

## TD-007 — Pagination via Spring's Page<T> envelope

**Decision:** All list endpoints return Spring's native `Page<T>` response envelope. No custom wrapper.

**Default parameters:** `page=0`, `size=20` if not provided by the caller.

**Response shape:**
```json
{
  "content": [...],
  "pageable": { "pageNumber": 0, "pageSize": 20 },
  "totalElements": 42,
  "totalPages": 3,
  "last": false,
  "first": true
}
```

**Rationale:** Spring's `Page<T>` is immediately familiar to any Spring developer and requires no additional code. The envelope provides all metadata a client needs to implement pagination UI.

---

## TD-008 — Bulk payroll response shape

**Decision:** The bulk payroll endpoint returns a summary object with two lists:

```json
{
  "created": [
    { "employeeId": 1, "employeeName": "John Doe", "period": "2026-03" },
    { "employeeId": 2, "employeeName": "Jane Smith", "period": "2026-03" }
  ],
  "skipped": [
    { "employeeId": 3, "employeeName": "Bob Martin", "reason": "Payroll already exists for this period" }
  ]
}
```

**Rationale:** HR needs to know exactly which employees were processed and which were skipped and why. A single count is not enough. The response is a flat summary — it does not embed full payroll slip details (those are retrievable individually via UC-PR-04).

---

## TD-010 — API documentation via Springdoc OpenAPI (Swagger)

**Decision:** API documentation is auto-generated from code using Springdoc OpenAPI. No manual API contract document maintained separately.

**Dependency:** `springdoc-openapi-starter-webmvc-ui`

**Access:** Swagger UI available at `/swagger-ui.html`, OpenAPI JSON at `/v3/api-docs`.

**Rationale:** Swagger generates documentation that is always in sync with the actual implementation. Manual API docs go stale. Annotations like `@Operation`, `@ApiResponse`, and `@Schema` on controllers and DTOs enrich the generated documentation without duplicating effort.

---

## TD-009 — Audit fields via Spring Data JPA Auditing

**Decision:** `createdAt` and `updatedAt` on `Employee` are managed automatically via Spring Data JPA Auditing (`@CreatedDate`, `@LastModifiedDate`, `@EnableJpaAuditing`).

**Implementation:**
- `@EnableJpaAuditing` on the main application class or a `@Configuration` class
- A `@MappedSuperclass` base entity (`BaseEntity`) holds the audit fields and is annotated with `@EntityListeners(AuditingEntityListener.class)`
- `Employee` extends `BaseEntity`
- `PayrollSlip` uses only `calculatedAt` set manually at calculation time — no auditing needed there

**Rationale:** Eliminates manual `LocalDateTime.now()` calls scattered across service methods. Audit fields are always populated consistently regardless of which service method persists the entity.
