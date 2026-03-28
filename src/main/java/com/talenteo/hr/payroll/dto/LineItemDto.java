package com.talenteo.hr.payroll.dto;

import com.talenteo.hr.payroll.domain.LineItemCode;
import com.talenteo.hr.payroll.domain.LineItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineItemDto {

    private LineItemType type;
    private LineItemCode code;
    private String label;
    private BigDecimal amount;
}
