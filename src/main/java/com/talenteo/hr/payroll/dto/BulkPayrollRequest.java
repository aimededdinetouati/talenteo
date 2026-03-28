package com.talenteo.hr.payroll.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BulkPayrollRequest {

    @NotNull
    @Min(2000)
    private Integer year;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;
}
