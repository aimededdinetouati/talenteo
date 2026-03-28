package com.talenteo.hr.payroll.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "payroll.rules")
@Getter
@Setter
public class PayrollProperties {

    private BigDecimal incomeTaxRate;
    private BigDecimal socialContributionRate;
    private BigDecimal highSalaryThreshold;
    private BigDecimal highSalarySurchargeRate;
}
