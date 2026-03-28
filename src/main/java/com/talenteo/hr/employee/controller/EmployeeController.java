package com.talenteo.hr.employee.controller;

import com.talenteo.hr.employee.domain.EmployeeStatus;
import com.talenteo.hr.employee.dto.EmployeeDto;
import com.talenteo.hr.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Validated
@Tag(name = "Employees", description = "Employee management operations")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @Operation(summary = "Create a new employee")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employee created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    public ResponseEntity<EmployeeDto> createEmployee(@Valid @RequestBody EmployeeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(dto));
    }

    @GetMapping
    @Operation(summary = "List employees with optional filters")
    @ApiResponse(responseCode = "200", description = "Paginated list of employees")
    public ResponseEntity<Page<EmployeeDto>> listEmployees(
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String department,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(employeeService.listEmployees(status, department, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an employee by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee found"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an employee")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Employee not found"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    public ResponseEntity<EmployeeDto> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeDto dto) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate an employee (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Employee deactivated"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<Void> deactivateEmployee(@PathVariable Long id) {
        employeeService.deactivateEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
