package com.talenteo.hr.payroll.dto;

import com.talenteo.hr.payroll.domain.PayrollStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollSlipDto {

    private Long id;
    private Long employeeId;
    private int periodYear;
    private int periodMonth;
    private BigDecimal netSalary;
    private PayrollStatus status;
    private LocalDateTime calculatedAt;
    private List<LineItemDto> lineItems;
}
