package com.talenteo.hr.payroll.controller;

import com.talenteo.hr.payroll.dto.BulkPayrollRequest;
import com.talenteo.hr.payroll.dto.BulkPayrollResult;
import com.talenteo.hr.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
@Validated
@Tag(name = "Payroll", description = "Payroll management operations")
public class BulkPayrollController {

    private final PayrollService payrollService;

    @PostMapping("/bulk")
    @Operation(summary = "Bulk calculate payroll for all active employees")
    @ApiResponse(responseCode = "200", description = "Bulk result with created and skipped entries")
    public ResponseEntity<BulkPayrollResult> bulkCalculatePayroll(
            @Valid @RequestBody BulkPayrollRequest request) {
        return ResponseEntity.ok(
                payrollService.bulkCalculatePayroll(request.getYear(), request.getMonth()));
    }
}
