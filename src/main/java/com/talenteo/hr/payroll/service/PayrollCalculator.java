package com.talenteo.hr.payroll.service;

import com.talenteo.hr.payroll.config.PayrollProperties;
import com.talenteo.hr.payroll.domain.LineItemCode;
import com.talenteo.hr.payroll.domain.LineItemType;
import com.talenteo.hr.payroll.domain.PayrollLineItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PayrollCalculator {

    private final PayrollProperties properties;

    public List<PayrollLineItem> calculate(BigDecimal baseSalary, BigDecimal bonus) {
        BigDecimal gross = baseSalary.add(bonus);

        BigDecimal incomeTax = gross.multiply(properties.getIncomeTaxRate())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal socialContribution = gross.multiply(properties.getSocialContributionRate())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal surcharge = gross.compareTo(properties.getHighSalaryThreshold()) > 0
                ? gross.multiply(properties.getHighSalarySurchargeRate()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return List.of(
                buildItem(LineItemType.EARNING, LineItemCode.BASE_SALARY, "Base Salary",
                        baseSalary.setScale(2, RoundingMode.HALF_UP)),
                buildItem(LineItemType.EARNING, LineItemCode.BONUS, "Bonus",
                        bonus.setScale(2, RoundingMode.HALF_UP)),
                buildItem(LineItemType.DEDUCTION, LineItemCode.INCOME_TAX,
                        "Income Tax (" + rateLabel(properties.getIncomeTaxRate()) + ")", incomeTax),
                buildItem(LineItemType.DEDUCTION, LineItemCode.SOCIAL_CONTRIBUTION,
                        "Social Contribution (" + rateLabel(properties.getSocialContributionRate()) + ")", socialContribution),
                buildItem(LineItemType.DEDUCTION, LineItemCode.HIGH_SALARY_SURCHARGE,
                        "High Salary Surcharge (" + rateLabel(properties.getHighSalarySurchargeRate()) + ")", surcharge)
        );
    }

    private PayrollLineItem buildItem(LineItemType type, LineItemCode code, String label, BigDecimal amount) {
        PayrollLineItem item = new PayrollLineItem();
        item.setType(type);
        item.setCode(code);
        item.setLabel(label);
        item.setAmount(amount);
        return item;
    }

    private String rateLabel(BigDecimal rate) {
        return rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }
}
