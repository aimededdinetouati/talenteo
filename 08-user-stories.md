# Developer User Stories

**Project:** Talenteo HR System
**Stack:** Spring Boot 3, PostgreSQL, Flyway, ModelMapper, Springdoc OpenAPI, Testcontainers

Each story is atomic and ordered by implementation dependency. The sum of all stories = the complete implementation.

---

## EPIC-1: Project Setup and Infrastructure

> Goal: A runnable Spring Boot application with database connectivity, Flyway, shared configuration, and audit infrastructure. No feature code yet.

---

### US-01 — Initialize Spring Boot project with dependencies

**As a developer, I want to** initialize the Spring Boot project with all required dependencies so the skeleton compiles and runs.

**Description:**
Create a Maven Spring Boot 3 project with the following dependencies:
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `postgresql` (runtime)
- `flyway-core`
- `modelmapper`
- `springdoc-openapi-starter-webmvc-ui`
- `lombok`
- `spring-boot-starter-test`
- `testcontainers` (postgresql)

**Acceptance Criteria:**
- [x] Project compiles with `mvn clean package`
- [x] All dependencies resolve without conflicts
- [x] Main application class exists with `@SpringBootApplication`
- [x] Base package is `com.talenteo.hr`

**Technical Notes:**
- Spring Boot version: 3.x
- Java version: 17+
- Group: `com.talenteo`, Artifact: `hr`

---

### US-02 — Configure application properties

**As a developer, I want to** configure `application.yml` with datasource, JPA, Flyway, and Springdoc settings so the application connects to PostgreSQL and serves the Swagger UI.

**Description:**
Create `src/main/resources/application.yml` with:
- PostgreSQL datasource (url, username, password)
- `spring.jpa.hibernate.ddl-auto=validate` (Flyway manages schema)
- `spring.flyway.enabled=true`
- `springdoc.swagger-ui.path=/swagger-ui.html`
- `springdoc.api-docs.path=/v3/api-docs`

Create `src/test/resources/application-test.yml` (Testcontainers overrides datasource at runtime — minimal config needed).

**Acceptance Criteria:**
- [x] Application starts without errors against a running PostgreSQL instance
- [x] `ddl-auto=validate` is set (Flyway owns the schema, not Hibernate)
- [x] Swagger UI accessible at `/swagger-ui.html` when app is running

**Technical Notes:**
- TD-005: Flyway manages schema, never `ddl-auto=create` or `update`
- TD-010: Springdoc OpenAPI

---

### US-03 — Flyway migration V1: create employees table

**As a developer, I want to** write the first Flyway migration that creates the `employees` table so the schema is version-controlled from day one.

**Description:**
Create `src/main/resources/db/migration/V1__create_employee_table.sql`:
```sql
CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    hire_date DATE NOT NULL,
    department VARCHAR(100) NOT NULL,
    position VARCHAR(100) NOT NULL,
    base_salary DECIMAL(12,2) NOT NULL,
    bonus DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT chk_base_salary CHECK (base_salary > 0),
    CONSTRAINT chk_bonus CHECK (bonus >= 0)
);
```

**Acceptance Criteria:**
- [x] Migration runs successfully on a clean PostgreSQL instance
- [x] Unique constraint on `email` exists
- [x] Check constraints on `base_salary` and `bonus` exist
- [x] Migration is named exactly `V1__create_employee_table.sql`

**Technical Notes:**
- TD-005: append-only migrations, never edit after applying
- DM-001: department and position are plain VARCHAR, no FK
- DM-002: status is VARCHAR storing enum values

---

### US-04 — Flyway migration V2: create payroll_slip table

**As a developer, I want to** write the second Flyway migration that creates the `payroll_slip` table with its unique constraint so duplicate payrolls are prevented at the DB level.

**Description:**
Create `src/main/resources/db/migration/V2__create_payroll_slip_table.sql`:
```sql
CREATE TABLE payroll_slip (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    period_year SMALLINT NOT NULL,
    period_month SMALLINT NOT NULL,
    net_salary DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    calculated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payroll_slip_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT uk_payroll_slip_period UNIQUE (employee_id, period_year, period_month),
    CONSTRAINT chk_period_month CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT chk_period_year CHECK (period_year >= 2000)
);
```

**Acceptance Criteria:**
- [x] Migration runs successfully after V1
- [x] Unique constraint on `(employee_id, period_year, period_month)` exists
- [x] FK to `employees.id` exists
- [x] Check constraints on year and month exist

**Technical Notes:**
- DM-004: `net_salary` stored (denormalized), computed at calculation time
- DM-005: immutability enforced at DB level via unique constraint
- UC-003: duplicate returns 409

---

### US-05 — Flyway migration V3: create payroll_line_item table

**As a developer, I want to** write the third Flyway migration that creates the `payroll_line_item` table so each payroll breakdown can be stored as immutable rows.

**Description:**
Create `src/main/resources/db/migration/V3__create_payroll_line_item_table.sql`:
```sql
CREATE TABLE payroll_line_item (
    id BIGSERIAL PRIMARY KEY,
    payroll_slip_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    code VARCHAR(50) NOT NULL,
    label VARCHAR(150) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_line_item_slip FOREIGN KEY (payroll_slip_id) REFERENCES payroll_slip(id)
);
```

**Acceptance Criteria:**
- [x] Migration runs successfully after V2
- [x] FK to `payroll_slip.id` exists
- [x] `amount`, `label`, `type`, `code` are all NOT NULL

**Technical Notes:**
- DM-003: line items are the backbone of the header + items model
- DM-006: label is a snapshot (e.g., "Income Tax (20%)") — not derived at query time

---

### US-06 — BaseEntity with JPA Auditing

**As a developer, I want to** implement a `BaseEntity` superclass with automatic audit fields so `createdAt` and `updatedAt` are populated consistently without manual code.

**Description:**
Create `com.talenteo.hr.common.config.BaseEntity`:
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

Enable auditing on the main application class or a `@Configuration` class with `@EnableJpaAuditing`.

**Acceptance Criteria:**
- [x] `BaseEntity` is a `@MappedSuperclass` with `@EntityListeners(AuditingEntityListener.class)`
- [x] `createdAt` is non-updatable
- [x] `@EnableJpaAuditing` is present
- [x] `Employee` will extend `BaseEntity` (verified in US-11)

**Technical Notes:**
- TD-009: JPA Auditing eliminates scattered `LocalDateTime.now()` calls
- Package: `com.talenteo.hr.common.config`

---

### US-07 — ModelMapper singleton bean

**As a developer, I want to** declare a singleton `ModelMapper` bean so all entity-DTO conversions share one pre-initialized instance.

**Description:**
Create `com.talenteo.hr.common.config.AppConfig`:
```java
@Configuration
public class AppConfig {
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
```

**Acceptance Criteria:**
- [x] `ModelMapper` is declared as a `@Bean` in a `@Configuration` class
- [x] Only one `ModelMapper` bean exists in the application context
- [x] No `new ModelMapper()` calls appear outside this class

**Technical Notes:**
- TD-003: ModelMapper is expensive to instantiate — singleton only
- Package: `com.talenteo.hr.common.config`

---

### US-08 — Springdoc OpenAPI configuration

**As a developer, I want to** configure Springdoc OpenAPI with project metadata so the Swagger UI displays meaningful information about the API.

**Description:**
Create `com.talenteo.hr.common.config.OpenApiConfig`:
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI hrApiOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Talenteo HR API")
                .description("Employee Management and Payroll Processing")
                .version("1.0.0"));
    }
}
```

**Acceptance Criteria:**
- [x] Swagger UI accessible at `/swagger-ui.html`
- [x] OpenAPI JSON accessible at `/v3/api-docs`
- [x] API title, description, and version are displayed in the UI

**Technical Notes:**
- TD-010: Springdoc generates docs from code — no manual API doc maintenance
- Package: `com.talenteo.hr.common.config`

---

## EPIC-2: Common / Cross-Cutting Infrastructure

> Goal: Exception classes and global error handler in place before any feature code. Every service story depends on these.

---

### US-09 — Custom exception hierarchy

**As a developer, I want to** define all custom exceptions so services can throw meaningful, typed errors that the global handler maps to correct HTTP responses.

**Description:**
Create the following in `com.talenteo.hr.common.exception`:

```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) { super(message); }
}

public class PayrollAlreadyExistsException extends RuntimeException {
    public PayrollAlreadyExistsException(String message) { super(message); }
}

public class PayrollAlreadyFinalizedException extends RuntimeException {
    public PayrollAlreadyFinalizedException(String message) { super(message); }
}

public class InactiveEmployeeException extends RuntimeException {
    public InactiveEmployeeException(String message) { super(message); }
}
```

**Acceptance Criteria:**
- [x] All 5 exception classes exist in `com.talenteo.hr.common.exception`
- [x] All extend `RuntimeException`
- [x] All accept a `String message` constructor parameter

**Technical Notes:**
- TD-001: status code mapping — ResourceNotFoundException→404, DuplicateEmail/PayrollAlreadyExists/PayrollAlreadyFinalized→409, InactiveEmployee→422

---

### US-10 — GlobalExceptionHandler

**As a developer, I want to** implement a `@ControllerAdvice` that maps all exceptions to RFC 7807 `ProblemDetail` responses so every error follows a consistent structure.

**Description:**
Create `com.talenteo.hr.common.exception.GlobalExceptionHandler` annotated with `@ControllerAdvice`.

Handle:
- `ResourceNotFoundException` → `ProblemDetail` with status 404
- `DuplicateEmailException` → 409
- `PayrollAlreadyExistsException` → 409
- `PayrollAlreadyFinalizedException` → 409
- `InactiveEmployeeException` → 422
- `MethodArgumentNotValidException` → 400 with `errors` property (map of field → message) added via `problemDetail.setProperty("errors", fieldErrors)`

**Acceptance Criteria:**
- [x] All 5 custom exceptions are handled and return correct status codes
- [x] `MethodArgumentNotValidException` returns 400 with `errors` map in the response body
- [x] All responses use Spring's `ProblemDetail` (not a custom class)
- [x] No exception leaks as a 500 for the mapped scenarios

**Technical Notes:**
- TD-001: RFC 7807, `ProblemDetail.forStatusAndDetail(HttpStatus, String)`
- Package: `com.talenteo.hr.common.exception`

---

## EPIC-3: Employee Management

> Goal: Full employee feature end-to-end covering all 5 use cases (UC-EM-01 to UC-EM-05) and their error paths.

---

### US-11 — Employee JPA entity

**As a developer, I want to** implement the `Employee` JPA entity extending `BaseEntity` so it maps correctly to the `employees` table.

**Description:**
Create `com.talenteo.hr.employee.domain.Employee` extending `BaseEntity`:
- `@Entity`, `@Table(name = "employees")`
- Fields: `id` (BIGSERIAL), `firstName`, `lastName`, `email`, `hireDate`, `department`, `position`, `baseSalary`, `bonus`, `status`
- `EmployeeStatus` enum: `ACTIVE`, `INACTIVE` stored as `@Enumerated(EnumType.STRING)`
- `baseSalary` and `bonus` as `BigDecimal`

**Acceptance Criteria:**
- [x] Entity maps to `employees` table without Hibernate errors
- [x] `status` is stored as string (`ACTIVE`/`INACTIVE`)
- [x] `baseSalary` and `bonus` are `BigDecimal` (never `double` or `float`)
- [x] Extends `BaseEntity` (inherits `createdAt`, `updatedAt`)

**Technical Notes:**
- Package: `com.talenteo.hr.employee.domain`
- DM-002: soft delete via status field
- Always use `BigDecimal` for money — never `double`

---

### US-12 — EmployeeRepository

**As a developer, I want to** implement `EmployeeRepository` with query methods for filtered, paginated listing so UC-EM-03 can filter by status and department.

**Description:**
Create `com.talenteo.hr.employee.repository.EmployeeRepository` extending `JpaRepository<Employee, Long>`.

Add a query method supporting optional filters:
```java
Page<Employee> findByStatusAndDepartmentContainingIgnoreCase(
    EmployeeStatus status, String department, Pageable pageable);
```

Or use a `@Query` with `JPQL` for more flexible optional filtering:
```java
@Query("SELECT e FROM Employee e WHERE (:status IS NULL OR e.status = :status) AND (:department IS NULL OR LOWER(e.department) LIKE LOWER(CONCAT('%', :department, '%')))")
Page<Employee> findWithFilters(@Param("status") EmployeeStatus status, @Param("department") String department, Pageable pageable);
```

**Acceptance Criteria:**
- [x] Repository extends `JpaRepository<Employee, Long>`
- [x] Supports paginated listing with optional status filter
- [x] Supports optional department filter (case-insensitive partial match)
- [x] No business logic in the repository

**Technical Notes:**
- Package: `com.talenteo.hr.employee.repository`
- UC-EM-03: filter by status and department, paginated

---

### US-13 — EmployeeDto

**As a developer, I want to** define `EmployeeDto` with Bean Validation annotations so input is validated automatically before reaching the service.

**Description:**
Create `com.talenteo.hr.employee.dto.EmployeeDto` with all fields and annotations:

```java
public class EmployeeDto {
    private Long id;                         // response only, null on input

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank @Email
    private String email;

    @NotNull
    private LocalDate hireDate;

    @NotBlank
    private String department;

    @NotBlank
    private String position;

    @NotNull @DecimalMin("0.01")
    private BigDecimal baseSalary;

    @DecimalMin("0.0")
    private BigDecimal bonus;

    private EmployeeStatus status;           // response only, ignored on input
    private LocalDateTime createdAt;         // response only
    private LocalDateTime updatedAt;         // response only
}
```

**Acceptance Criteria:**
- [x] All input fields have appropriate validation annotations
- [x] `id`, `status`, `createdAt`, `updatedAt` are present but not validated (response fields)
- [x] `baseSalary` minimum is `0.01` (strictly positive)
- [x] `bonus` minimum is `0.0` (zero allowed)

**Technical Notes:**
- TD-002: DTO annotations for structural/format rules
- TD-004: single DTO class — no request/response split

---

### US-14 — EmployeeService: create

**As a developer, I want to** implement the `createEmployee` method in `EmployeeService` so UC-EM-01 is fulfilled with email uniqueness enforcement.

**Description:**
Implement in `com.talenteo.hr.employee.service.EmployeeService`:
```
createEmployee(EmployeeDto dto) → EmployeeDto
```
Steps:
1. Check email uniqueness via repository — throw `DuplicateEmailException` if taken
2. Map DTO to entity via `ModelMapper`
3. Set `status = ACTIVE`
4. Save and return mapped response DTO

**Acceptance Criteria:**
- [x] Returns created employee DTO with `id`, `status=ACTIVE`, `createdAt`, `updatedAt` populated
- [x] Throws `DuplicateEmailException` if email already exists
- [x] Does not set `id` manually (auto-generated)
- [x] `bonus` defaults to `BigDecimal.ZERO` if null

**Technical Notes:**
- Package: `com.talenteo.hr.employee.service`
- UC-EM-01 precondition: email must be unique

---

### US-15 — EmployeeService: read and list

**As a developer, I want to** implement `getEmployeeById` and `listEmployees` in `EmployeeService` so UC-EM-02 and UC-EM-03 are fulfilled.

**Description:**
Implement:
```
getEmployeeById(Long id) → EmployeeDto
listEmployees(EmployeeStatus status, String department, Pageable pageable) → Page<EmployeeDto>
```

`getEmployeeById`: fetch by id, throw `ResourceNotFoundException` if absent, return mapped DTO.

`listEmployees`: delegate to repository with optional filters, map each entity to DTO.

**Acceptance Criteria:**
- [x] `getEmployeeById` throws `ResourceNotFoundException` for unknown id
- [x] `listEmployees` returns paginated results
- [x] Both `status` and `department` filters are optional (null = no filter)
- [x] `department` filter is case-insensitive

**Technical Notes:**
- UC-EM-02, UC-EM-03
- TD-007: `Page<T>` returned directly

---

### US-16 — EmployeeService: update

**As a developer, I want to** implement `updateEmployee` in `EmployeeService` so UC-EM-04 is fulfilled with correct email uniqueness validation.

**Description:**
Implement:
```
updateEmployee(Long id, EmployeeDto dto) → EmployeeDto
```
Steps:
1. Fetch existing employee — throw `ResourceNotFoundException` if absent
2. If email changed: check new email is not taken by *another* employee — throw `DuplicateEmailException` if so
3. Map all fields from DTO onto the existing entity
4. Save and return updated DTO

**Acceptance Criteria:**
- [x] Throws `ResourceNotFoundException` if employee not found
- [x] Throws `DuplicateEmailException` if email is taken by a *different* employee
- [x] Does NOT throw on email uniqueness if the email belongs to the same employee (unchanged email)
- [x] Updating salary does not affect past payroll slips (they are already snapshotted)

**Technical Notes:**
- UC-EM-04
- Key edge case: email uniqueness check must exclude the current employee's own id

---

### US-17 — EmployeeService: deactivate

**As a developer, I want to** implement `deactivateEmployee` in `EmployeeService` so UC-EM-05 is fulfilled with soft delete.

**Description:**
Implement:
```
deactivateEmployee(Long id) → void
```
Steps:
1. Fetch employee — throw `ResourceNotFoundException` if absent
2. Set `status = INACTIVE`
3. Save

**Acceptance Criteria:**
- [x] Throws `ResourceNotFoundException` if employee not found
- [x] Sets `status = INACTIVE` — does not delete the record
- [x] Payroll history is unaffected
- [x] `updatedAt` is updated (handled by JPA Auditing)

**Technical Notes:**
- DM-002: soft delete
- UC-EM-05

---

### US-18 — EmployeeController

**As a developer, I want to** implement `EmployeeController` with all 5 endpoints wired to `EmployeeService` so the employee API is complete and documented via Swagger.

**Description:**
Create `com.talenteo.hr.employee.controller.EmployeeController` with `@RestController`, `@RequestMapping("/api/v1/employees")`:

| Method | Path | Service call | Status |
|---|---|---|---|
| POST | `/` | `createEmployee` | 201 |
| GET | `/` | `listEmployees` | 200 |
| GET | `/{id}` | `getEmployeeById` | 200 |
| PUT | `/{id}` | `updateEmployee` | 200 |
| DELETE | `/{id}` | `deactivateEmployee` | 204 |

- Use `@Valid` on request body parameters
- Use `ResponseEntity` to set correct status codes
- Annotate with `@Operation` and `@ApiResponse` for Swagger

**Acceptance Criteria:**
- [x] All 5 endpoints are present and return correct HTTP status codes
- [x] `@Valid` is applied on POST and PUT request bodies
- [x] List endpoint accepts `status`, `department`, `page`, `size` query params
- [x] DELETE returns `204 No Content` (no body)
- [x] Swagger UI displays all 5 endpoints with descriptions

**Technical Notes:**
- Package: `com.talenteo.hr.employee.controller`
- TD-010: `@Operation`, `@ApiResponse` Swagger annotations

---

## EPIC-4: Payroll Feature

> Goal: Full payroll feature end-to-end covering all 5 use cases (UC-PR-01 to UC-PR-05).

---

### US-19 — PayrollSlip and PayrollLineItem JPA entities

**As a developer, I want to** implement the `PayrollSlip` and `PayrollLineItem` JPA entities with their enums and relationship so the payroll data model is correctly mapped.

**Description:**
Create `com.talenteo.hr.payroll.domain.PayrollSlip`:
- `@Entity`, `@Table(name = "payroll_slip")`
- Fields: `id`, `employee` (ManyToOne), `periodYear`, `periodMonth`, `netSalary` (BigDecimal), `status` (PayrollStatus enum), `calculatedAt`
- `PayrollStatus` enum: `DRAFT`, `FINALIZED`
- `@OneToMany(mappedBy = "payrollSlip", cascade = CascadeType.ALL, fetch = FetchType.EAGER)` for `lineItems`

Create `com.talenteo.hr.payroll.domain.PayrollLineItem`:
- `@Entity`, `@Table(name = "payroll_line_item")`
- Fields: `id`, `payrollSlip` (ManyToOne), `type` (LineItemType enum), `code` (LineItemCode enum), `label`, `amount` (BigDecimal)
- `LineItemType` enum: `EARNING`, `DEDUCTION`
- `LineItemCode` enum: `BASE_SALARY`, `BONUS`, `INCOME_TAX`, `SOCIAL_CONTRIBUTION`, `HIGH_SALARY_SURCHARGE`

**Acceptance Criteria:**
- [ ] Both entities map to their tables without Hibernate errors
- [ ] `@OneToMany` on `PayrollSlip.lineItems` uses `CascadeType.ALL`
- [ ] All monetary fields are `BigDecimal`
- [ ] Enums are stored as `STRING`

**Technical Notes:**
- Package: `com.talenteo.hr.payroll.domain`
- DM-003: header + line items model
- `PayrollSlip` does NOT extend `BaseEntity` — uses `calculatedAt` set manually

---

### US-20 — PayrollSlipRepository

**As a developer, I want to** implement `PayrollSlipRepository` with query methods for duplicate detection and paginated history so all payroll read/write use cases are supported.

**Description:**
Create `com.talenteo.hr.payroll.repository.PayrollSlipRepository` extending `JpaRepository<PayrollSlip, Long>`:

```java
Optional<PayrollSlip> findByEmployeeIdAndPeriodYearAndPeriodMonth(
    Long employeeId, int year, int month);

Page<PayrollSlip> findByEmployeeIdOrderByPeriodYearDescPeriodMonthDesc(
    Long employeeId, Pageable pageable);
```

**Acceptance Criteria:**
- [ ] `findByEmployeeIdAndPeriodYearAndPeriodMonth` returns `Optional` for duplicate detection
- [ ] `findByEmployeeId...` supports paginated history ordered by period descending
- [ ] No business logic in the repository

**Technical Notes:**
- Package: `com.talenteo.hr.payroll.repository`
- UC-PR-01: duplicate guard, UC-PR-04: view slip, UC-PR-05: history

---

### US-21 — PayrollLineItemRepository

**As a developer, I want to** implement `PayrollLineItemRepository` so line items can be persisted independently if needed.

**Description:**
Create `com.talenteo.hr.payroll.repository.PayrollLineItemRepository` extending `JpaRepository<PayrollLineItem, Long>`. No custom query methods needed — line items are read through the `PayrollSlip.lineItems` collection.

**Acceptance Criteria:**
- [ ] Repository extends `JpaRepository<PayrollLineItem, Long>`
- [ ] No custom methods needed at this stage

**Technical Notes:**
- Package: `com.talenteo.hr.payroll.repository`

---

### US-22 — Payroll DTOs

**As a developer, I want to** define all payroll DTOs so requests and responses are cleanly typed and validated.

**Description:**
Create in `com.talenteo.hr.payroll.dto`:

`BulkPayrollRequest`:
- `@Min(2000) int year`
- `@Min(1) @Max(12) int month`

`LineItemDto`:
- `LineItemType type`, `LineItemCode code`, `String label`, `BigDecimal amount`

`PayrollSlipDto`:
- `Long id`, `Long employeeId`, `int periodYear`, `int periodMonth`
- `BigDecimal netSalary`, `PayrollStatus status`, `LocalDateTime calculatedAt`
- `List<LineItemDto> lineItems`

`BulkPayrollResult`:
- `List<BulkCreatedEntry> created` — each: `employeeId`, `employeeName`, `String period` (e.g., "2026-03")
- `List<BulkSkippedEntry> skipped` — each: `employeeId`, `employeeName`, `String reason`

**Acceptance Criteria:**
- [ ] `BulkPayrollRequest` has validation annotations on `year` and `month`
- [ ] `PayrollSlipDto` includes `lineItems` list
- [ ] `BulkPayrollResult` has both `created` and `skipped` lists

**Technical Notes:**
- TD-004: single DTO class per resource
- TD-008: bulk response shape

---

### US-23 — PayrollCalculator

**As a developer, I want to** implement `PayrollCalculator` as a pure calculation component that reads rates from config so business rules are isolated, testable, and changeable without code modifications.

**Description:**
Create `com.talenteo.hr.payroll.config.PayrollProperties` as a `@ConfigurationProperties(prefix = "payroll.rules")` bean:
- `incomeTaxRate` (BigDecimal)
- `socialContributionRate` (BigDecimal)
- `highSalaryThreshold` (BigDecimal)
- `highSalaryRatesSurchargeRate` (BigDecimal)

Add to `application.yml`:
```yaml
payroll:
  rules:
    income-tax-rate: 0.20
    social-contribution-rate: 0.05
    high-salary-threshold: 5000.00
    high-salary-surcharge-rate: 0.05
```

Create `com.talenteo.hr.payroll.service.PayrollCalculator` as a `@Component` injecting `PayrollProperties`.

Method signature:
```java
public List<PayrollLineItem> calculate(BigDecimal baseSalary, BigDecimal bonus)
```

Applies 5 rules using rates from `PayrollProperties`:
1. `BASE_SALARY` (EARNING) = `baseSalary`
2. `BONUS` (EARNING) = `bonus` — only if `bonus > 0`
3. `INCOME_TAX` (DEDUCTION) = `baseSalary × incomeTaxRate`
4. `SOCIAL_CONTRIBUTION` (DEDUCTION) = `baseSalary × socialContributionRate`
5. `HIGH_SALARY_SURCHARGE` (DEDUCTION) = `baseSalary × highSalaryRatesSurchargeRate` — only if `baseSalary > highSalaryThreshold`

Returns detached `PayrollLineItem` objects — no `id` or `payrollSlip` set. `PayrollService` sets the slip reference before persisting.

**Acceptance Criteria:**
- [ ] No repository, no DB access in this class
- [ ] Rates injected via `PayrollProperties` — no magic numbers in the calculator
- [ ] `BONUS` line item excluded when `bonus` is zero or null
- [ ] `HIGH_SALARY_SURCHARGE` only included when `baseSalary > highSalaryThreshold`
- [ ] All `BigDecimal` calculations use `setScale(2, RoundingMode.HALF_UP)`
- [ ] Label built dynamically from config value (e.g., `"Income Tax (20%)"`)

**Technical Notes:**
- Package: `com.talenteo.hr.payroll.service` (calculator), `com.talenteo.hr.payroll.config` (properties)
- DM-007 updated: rates from `application.yml` via `@ConfigurationProperties`, not hardcoded
- This class gets its own dedicated unit test in US-29

---

### US-24 — PayrollService: calculate single

**As a developer, I want to** implement `calculatePayroll` in `PayrollService` so UC-PR-01 is fulfilled with all guards and the snapshot pattern.

**Description:**
Implement in `com.talenteo.hr.payroll.service.PayrollService`:
```
calculatePayroll(Long employeeId, int year, int month) → PayrollSlipDto
```
Steps:
1. Fetch employee — throw `ResourceNotFoundException` if absent
2. Guard: employee must be `ACTIVE` — throw `InactiveEmployeeException` if not
3. Guard: no existing slip for period — throw `PayrollAlreadyExistsException` if exists
4. Call `PayrollCalculator.calculate(employee.baseSalary, employee.bonus)` — snapshots values
5. Compute `netSalary = SUM(EARNINGS) - SUM(DEDUCTIONS)`
6. Build and persist `PayrollSlip` with status `DRAFT`, `calculatedAt = now()`
7. Associate and persist line items
8. Return mapped `PayrollSlipDto`

**Acceptance Criteria:**
- [ ] Throws `ResourceNotFoundException` for unknown employee
- [ ] Throws `InactiveEmployeeException` for `INACTIVE` employee (422)
- [ ] Throws `PayrollAlreadyExistsException` for duplicate period (409)
- [ ] `netSalary` is correctly computed and stored on the slip
- [ ] Line items are persisted with correct type, code, label, and amount
- [ ] Past and future periods are accepted (no date restriction)

**Technical Notes:**
- UC-PR-01, DM-004, DM-006
- Package: `com.talenteo.hr.payroll.service`

---

### US-25 — PayrollService: bulk calculate

**As a developer, I want to** implement `bulkCalculatePayroll` in `PayrollService` so UC-PR-02 is fulfilled with correct skip behavior.

**Description:**
Implement:
```
bulkCalculatePayroll(int year, int month) → BulkPayrollResult
```
Steps:
1. Fetch all `ACTIVE` employees
2. For each employee:
   - If slip already exists for period → add to `skipped` list with reason
   - Otherwise → calculate payroll (reuse core logic from US-24), add to `created` list
3. Return `BulkPayrollResult`

**Acceptance Criteria:**
- [ ] Only processes `ACTIVE` employees
- [ ] Employees with existing slips are added to `skipped` — processing continues for the rest
- [ ] `created` list contains `employeeId`, `employeeName`, `period` (formatted as "YYYY-MM")
- [ ] `skipped` list contains `employeeId`, `employeeName`, `reason`
- [ ] Returns 200 even if all employees were skipped

**Technical Notes:**
- UC-PR-02, UC-004 conflict behavior

---

### US-26 — PayrollService: finalize

**As a developer, I want to** implement `finalizePayroll` in `PayrollService` so UC-PR-03 is fulfilled with immutability enforcement.

**Description:**
Implement:
```
finalizePayroll(Long employeeId, int year, int month) → PayrollSlipDto
```
Steps:
1. Fetch slip by `(employeeId, year, month)` — throw `ResourceNotFoundException` if absent
2. Guard: status must be `DRAFT` — throw `PayrollAlreadyFinalizedException` if already `FINALIZED`
3. Set `status = FINALIZED`
4. Save and return updated `PayrollSlipDto`

**Acceptance Criteria:**
- [ ] Throws `ResourceNotFoundException` if slip not found (404)
- [ ] Throws `PayrollAlreadyFinalizedException` if already finalized (409)
- [ ] Returns slip with `status = FINALIZED`
- [ ] Line items are unchanged after finalization

**Technical Notes:**
- UC-PR-03, DM-005, UC-001

---

### US-27 — PayrollService: read and list history

**As a developer, I want to** implement `getPayrollSlip` and `listPayrollHistory` in `PayrollService` so UC-PR-04 and UC-PR-05 are fulfilled.

**Description:**
Implement:
```
getPayrollSlip(Long employeeId, int year, int month) → PayrollSlipDto
listPayrollHistory(Long employeeId, Pageable pageable) → Page<PayrollSlipDto>
```

`getPayrollSlip`: fetch by `(employeeId, year, month)` — throw `ResourceNotFoundException` if absent. Return full DTO with line items.

`listPayrollHistory`: verify employee exists (throw `ResourceNotFoundException` if not), fetch paginated slips ordered by period descending.

**Acceptance Criteria:**
- [ ] `getPayrollSlip` throws `ResourceNotFoundException` if slip not found
- [ ] `listPayrollHistory` throws `ResourceNotFoundException` if employee not found
- [ ] History is ordered by `periodYear DESC`, `periodMonth DESC`
- [ ] Both return `lineItems` in the DTO

**Technical Notes:**
- UC-PR-04, UC-PR-05

---

### US-28 — PayrollController

**As a developer, I want to** implement `PayrollController` with all 5 payroll endpoints wired to `PayrollService` so the payroll API is complete and documented via Swagger.

**Description:**
Create `com.talenteo.hr.payroll.controller.PayrollController` with `@RestController`:

| Method | Path | Service call | Status |
|---|---|---|---|
| POST | `/api/v1/employees/{id}/payroll/{year}/{month}` | `calculatePayroll` | 201 |
| POST | `/api/v1/payroll/bulk` | `bulkCalculatePayroll` | 200 |
| POST | `/api/v1/employees/{id}/payroll/{year}/{month}/finalize` | `finalizePayroll` | 200 |
| GET | `/api/v1/employees/{id}/payroll/{year}/{month}` | `getPayrollSlip` | 200 |
| GET | `/api/v1/employees/{id}/payroll` | `listPayrollHistory` | 200 |

- `@Valid` on `BulkPayrollRequest`
- `@Min(2000)` on `{year}` path variable, `@Min(1) @Max(12)` on `{month}`
- `@Operation` and `@ApiResponse` Swagger annotations on each endpoint

**Acceptance Criteria:**
- [ ] All 5 endpoints present with correct HTTP status codes
- [ ] Bulk endpoint uses `@Valid` on request body
- [ ] Path variable constraints are validated (`@Validated` on controller class)
- [ ] Swagger UI displays all 5 payroll endpoints

**Technical Notes:**
- Package: `com.talenteo.hr.payroll.controller`
- TD-010: Swagger annotations

---

## EPIC-5: Testing

> Goal: Full test coverage across all layers. Unit tests first, then integration tests.

---

### US-29 — Unit test: PayrollCalculator

**As a developer, I want to** unit test `PayrollCalculator` in isolation so every calculation rule and edge case is verified without infrastructure.

**Description:**
Create `com.talenteo.hr.payroll.service.PayrollCalculatorTest` using JUnit 5. No Spring context.

Test cases:
1. Base salary only (no bonus) below 5000 → BASE_SALARY + INCOME_TAX + SOCIAL_CONTRIBUTION (3 items)
2. Base salary + bonus below 5000 → BASE_SALARY + BONUS + INCOME_TAX + SOCIAL_CONTRIBUTION (4 items)
3. Base salary above 5000, no bonus → includes HIGH_SALARY_SURCHARGE (4 items)
4. Base salary above 5000 + bonus → all 5 line items
5. Bonus = 0 → BONUS line item excluded
6. Exact amounts: salary=6000 → INCOME_TAX=1200, SOCIAL_CONTRIBUTION=300, SURCHARGE=300, net=(6000-1800)=4200
7. Salary exactly 5000 → no surcharge (boundary: > 5000, not >=)

**Acceptance Criteria:**
- [ ] All 7 test cases pass
- [ ] `PayrollProperties` instantiated directly with test values — no Spring context needed
- [ ] Amounts verified with exact `BigDecimal` equality
- [ ] Boundary condition at salary=5000 is explicitly tested

**Technical Notes:**
- DM-007: rates from `PayrollProperties` (config), not magic numbers
- Instantiate `PayrollCalculator` with a manually built `PayrollProperties` object in tests
- Use `assertThat(result).hasSize(n)` and verify each line item

---

### US-30 — Unit test: EmployeeService

**As a developer, I want to** unit test `EmployeeService` with mocked dependencies so all service-layer business rules are verified in isolation.

**Description:**
Create `com.talenteo.hr.employee.service.EmployeeServiceTest` using JUnit 5 + Mockito.

Test cases:
1. `createEmployee` — happy path: repository saves and returns mapped DTO with status ACTIVE
2. `createEmployee` — duplicate email: `findByEmail` returns existing → throws `DuplicateEmailException`
3. `getEmployeeById` — not found: repository returns empty → throws `ResourceNotFoundException`
4. `updateEmployee` — email taken by another employee → throws `DuplicateEmailException`
5. `updateEmployee` — same email as own employee → succeeds (no false positive)
6. `deactivateEmployee` — sets status to INACTIVE
7. `deactivateEmployee` — not found → throws `ResourceNotFoundException`

**Acceptance Criteria:**
- [ ] All 7 test cases pass
- [ ] `EmployeeRepository` and `ModelMapper` are mocked
- [ ] No Spring context loaded
- [ ] Exception types are exactly the expected custom exceptions

---

### US-31 — Unit test: PayrollService

**As a developer, I want to** unit test `PayrollService` with mocked dependencies so all orchestration logic and guards are verified in isolation.

**Description:**
Create `com.talenteo.hr.payroll.service.PayrollServiceTest` using JUnit 5 + Mockito.

Test cases:
1. `calculatePayroll` — employee not found → throws `ResourceNotFoundException`
2. `calculatePayroll` — employee is INACTIVE → throws `InactiveEmployeeException`
3. `calculatePayroll` — slip already exists → throws `PayrollAlreadyExistsException`
4. `calculatePayroll` — happy path: slip created with correct netSalary and line items
5. `finalizePayroll` — slip not found → throws `ResourceNotFoundException`
6. `finalizePayroll` — already finalized → throws `PayrollAlreadyFinalizedException`
7. `bulkCalculatePayroll` — mixed: 2 created, 1 skipped → result lists correctly populated

**Acceptance Criteria:**
- [ ] All 7 test cases pass
- [ ] All dependencies mocked (EmployeeRepository, PayrollSlipRepository, PayrollCalculator, ModelMapper)
- [ ] No Spring context loaded

---

### US-32 — Testcontainers base configuration

**As a developer, I want to** set up a shared Testcontainers configuration so all integration test classes use the same PostgreSQL container and Flyway migrations run once.

**Description:**
Create `com.talenteo.hr.BaseIntegrationTest` (in `src/test`):
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("talenteo_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
```

**Acceptance Criteria:**
- [x] PostgreSQL container starts before tests
- [x] Flyway migrations run automatically at startup
- [x] `MockMvc` and `ObjectMapper` are available to all subclasses
- [x] Container is shared (static) — not restarted per test class

**Technical Notes:**
- TD-006: Testcontainers with real PostgreSQL
- Both `EmployeeControllerIT` and `PayrollControllerIT` extend this class

---

### US-33 — Integration test: EmployeeControllerIT

**As a developer, I want to** integration test all employee endpoints against a real PostgreSQL container so the full request-to-DB flow is verified.

**Description:**
Create `com.talenteo.hr.employee.controller.EmployeeControllerIT` extending `BaseIntegrationTest`.

Test cases:
1. `POST /api/v1/employees` — valid payload → 201, response has id and status=ACTIVE
2. `POST /api/v1/employees` — missing required field → 400 with `errors` map
3. `POST /api/v1/employees` — duplicate email → 409
4. `GET /api/v1/employees/{id}` — existing id → 200 with full employee
5. `GET /api/v1/employees/{id}` — unknown id → 404
6. `GET /api/v1/employees` — no filters → 200 paginated list
7. `GET /api/v1/employees?status=ACTIVE` — only active employees returned
8. `PUT /api/v1/employees/{id}` — valid update → 200 with updated fields
9. `PUT /api/v1/employees/{id}` — email taken by another → 409
10. `DELETE /api/v1/employees/{id}` — existing → 204, employee status becomes INACTIVE
11. `DELETE /api/v1/employees/{id}` — unknown id → 404

**Acceptance Criteria:**
- [x] All 11 test cases pass against real PostgreSQL
- [x] Each test is independent (clean state or uses unique emails)
- [x] Response body shape is asserted (not just status code)
- [x] 400 errors include the `errors` map field

---

### US-34 — Integration test: PayrollControllerIT

**As a developer, I want to** integration test all payroll endpoints against a real PostgreSQL container so the full payroll flow including edge cases is verified.

**Description:**
Create `com.talenteo.hr.payroll.controller.PayrollControllerIT` extending `BaseIntegrationTest`.

Test cases:
1. `POST .../payroll/2026/3` — active employee → 201, DRAFT slip with correct line items and netSalary
2. `POST .../payroll/2026/3` — inactive employee → 422
3. `POST .../payroll/2026/3` — duplicate period → 409
4. `POST .../payroll/2026/3` — unknown employee → 404
5. `POST /api/v1/payroll/bulk` — all active, no existing slips → 200, all in `created`
6. `POST /api/v1/payroll/bulk` — some have existing slips → skipped entries in result
7. `POST .../payroll/2026/3/finalize` — DRAFT slip → 200, status=FINALIZED
8. `POST .../payroll/2026/3/finalize` — already finalized → 409
9. `POST .../payroll/2026/3/finalize` — slip not found → 404
10. `GET .../payroll/2026/3` — existing slip → 200 with line items
11. `GET .../payroll/2026/3` — not found → 404
12. `GET .../payroll` — existing history → 200 paginated, ordered by period descending

**Acceptance Criteria:**
- [ ] All 12 test cases pass against real PostgreSQL
- [ ] Line item amounts are verified for at least one happy path test
- [ ] `netSalary` is verified against expected calculation
- [ ] Bulk result `created` and `skipped` lists are asserted

---

## Story Summary

| Epic | Stories | IDs |
|---|---|---|
| EPIC-1: Project Setup | 8 | US-01 to US-08 |
| EPIC-2: Common Infrastructure | 2 | US-09 to US-10 |
| EPIC-3: Employee Management | 8 | US-11 to US-18 |
| EPIC-4: Payroll Feature | 10 | US-19 to US-28 |
| EPIC-5: Testing | 6 | US-29 to US-34 |
| **Total** | **34** | |
