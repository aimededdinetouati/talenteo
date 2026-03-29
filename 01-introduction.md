# Talenteo HR System — Technical Documentation

## Project Overview

Talenteo is a backend REST API built as a technical exercise to demonstrate software engineering practices in a real-world HR domain. The system covers two core business areas:

- **Employee Management** — create, update, deactivate, and search employees with filtering and pagination
- **Payroll Processing** — calculate, review, recalculate, finalize, and bulk-process monthly payroll slips with configurable tax rules

The design emphasizes clean architecture, thoughtful decision-making, and production-grade engineering practices rather than just making things work.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.4 |
| Database | PostgreSQL |
| Schema migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| Object mapping | ModelMapper |
| Validation | Bean Validation (Jakarta) |
| API documentation | Springdoc OpenAPI (Swagger UI) |
| Error handling | RFC 7807 Problem Details |
| Testing | JUnit 5, Mockito, Testcontainers |
| Build | Maven |

---

## Architecture Highlights

- **Package-by-feature** — each feature (`employee`, `payroll`) is self-contained with its own controller, service, repository, domain, and DTO layers
- **Two-step payroll flow** — calculate (DRAFT) → review → recalculate if needed → finalize (FINALIZED), making finalization meaningful and immutability intentional
- **Config-driven tax rules** — rates externalized to `application.yml` via `@ConfigurationProperties`, no magic numbers in business logic
- **Salary snapshot pattern** — salary and bonus are copied into line items at calculation time, ensuring past payroll slips are never affected by future employee updates
- **Defense in depth** — business rules enforced at both service and database levels (unique constraints, check constraints)

---

## Document Structure

| Document | Content |
|---|---|
| **ER Diagram** | Entity-relationship diagram showing the data model |
| **Decision Records** | Architecture and use case decisions with rationale |
| **Requirements** | Functional use cases derived from the specification |
| **Package Structure** | Project layout and package-by-feature rationale |
| **Technical Decisions** | Technology choices and trade-offs |
| **API Contract** | All endpoints with request/response shapes |
| **User Stories** | 35 developer stories across 5 epics, with completion status |
