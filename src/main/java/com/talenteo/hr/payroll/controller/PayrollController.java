package com.talenteo.hr.payroll.controller;

import com.talenteo.hr.payroll.dto.PayrollSlipDto;
import com.talenteo.hr.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Validated
@Tag(name = "Payroll", description = "Payroll management operations")
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/{id}/payroll/{year}/{month}")
    @Operation(summary = "Calculate payroll for an employee")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payroll slip created"),
            @ApiResponse(responseCode = "404", description = "Employee not found"),
            @ApiResponse(responseCode = "409", description = "Payroll already exists for this period"),
            @ApiResponse(responseCode = "422", description = "Employee is not active")
    })
    public ResponseEntity<PayrollSlipDto> calculatePayroll(
            @PathVariable Long id,
            @PathVariable @Min(2000) int year,
            @PathVariable @Min(1) @Max(12) int month) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payrollService.calculatePayroll(id, year, month));
    }

    @PutMapping("/{id}/payroll/{year}/{month}/recalculate")
    @Operation(summary = "Recalculate a DRAFT payroll slip")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payroll slip recalculated"),
            @ApiResponse(responseCode = "404", description = "Payroll slip not found"),
            @ApiResponse(responseCode = "409", description = "Payroll slip is already finalized")
    })
    public ResponseEntity<PayrollSlipDto> recalculatePayroll(
            @PathVariable Long id,
            @PathVariable @Min(2000) int year,
            @PathVariable @Min(1) @Max(12) int month) {
        return ResponseEntity.ok(payrollService.recalculatePayroll(id, year, month));
    }

    @PostMapping("/{id}/payroll/{year}/{month}/finalize")
    @Operation(summary = "Finalize a DRAFT payroll slip")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payroll slip finalized"),
            @ApiResponse(responseCode = "404", description = "Payroll slip not found"),
            @ApiResponse(responseCode = "409", description = "Payroll slip is already finalized")
    })
    public ResponseEntity<PayrollSlipDto> finalizePayroll(
            @PathVariable Long id,
            @PathVariable @Min(2000) int year,
            @PathVariable @Min(1) @Max(12) int month) {
        return ResponseEntity.ok(payrollService.finalizePayroll(id, year, month));
    }

    @GetMapping("/{id}/payroll/{year}/{month}")
    @Operation(summary = "Get a payroll slip by employee and period")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payroll slip found"),
            @ApiResponse(responseCode = "404", description = "Payroll slip not found")
    })
    public ResponseEntity<PayrollSlipDto> getPayrollSlip(
            @PathVariable Long id,
            @PathVariable @Min(2000) int year,
            @PathVariable @Min(1) @Max(12) int month) {
        return ResponseEntity.ok(payrollService.getPayrollSlip(id, year, month));
    }

    @GetMapping("/{id}/payroll")
    @Operation(summary = "List payroll history for an employee")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated payroll history"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<Page<PayrollSlipDto>> listPayrollHistory(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(payrollService.listPayrollHistory(id, pageable));
    }
}
