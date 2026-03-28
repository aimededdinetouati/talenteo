package com.talenteo.hr.payroll.service;

import com.talenteo.hr.payroll.config.PayrollProperties;
import com.talenteo.hr.payroll.domain.LineItemCode;
import com.talenteo.hr.payroll.domain.LineItemType;
import com.talenteo.hr.payroll.domain.PayrollLineItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PayrollCalculatorTest {

    private PayrollCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PayrollCalculator(buildProperties());
    }

    @Test
    void calculate_normalSalaryNoBonus_returnsCorrectAmounts() {
        List<PayrollLineItem> items = calculator.calculate(
                new BigDecimal("3000.00"), BigDecimal.ZERO);

        assertThat(items).hasSize(5);
        assertThat(amount(items, LineItemCode.INCOME_TAX)).isEqualByComparingTo("450.00");
        assertThat(amount(items, LineItemCode.SOCIAL_CONTRIBUTION)).isEqualByComparingTo("270.00");
        assertThat(amount(items, LineItemCode.HIGH_SALARY_SURCHARGE)).isEqualByComparingTo("0.00");
    }

    @Test
    void calculate_normalSalaryWithBonus_grossUsedForTax() {
        List<PayrollLineItem> items = calculator.calculate(
                new BigDecimal("3000.00"), new BigDecimal("500.00"));

        // gross = 3500
        assertThat(amount(items, LineItemCode.INCOME_TAX)).isEqualByComparingTo("525.00");
        assertThat(amount(items, LineItemCode.SOCIAL_CONTRIBUTION)).isEqualByComparingTo("315.00");
        assertThat(amount(items, LineItemCode.HIGH_SALARY_SURCHARGE)).isEqualByComparingTo("0.00");
    }

    @Test
    void calculate_highSalaryNoBonus_surchargeApplied() {
        List<PayrollLineItem> items = calculator.calculate(
                new BigDecimal("6000.00"), BigDecimal.ZERO);

        // gross = 6000 > 5000
        assertThat(amount(items, LineItemCode.HIGH_SALARY_SURCHARGE)).isEqualByComparingTo("300.00");
    }

    @Test
    void calculate_highSalaryWithBonus_allAmountsCorrect() {
        List<PayrollLineItem> items = calculator.calculate(
                new BigDecimal("6000.00"), new BigDecimal("500.00"));

        // gross = 6500
        assertThat(amount(items, LineItemCode.INCOME_TAX)).isEqualByComparingTo("975.00");
        assertThat(amount(items, LineItemCode.SOCIAL_CONTRIBUTION)).isEqualByComparingTo("585.00");
        assertThat(amount(items, LineItemCode.HIGH_SALARY_SURCHARGE)).isEqualByComparingTo("325.00");
    }

    @Test
    void calculate_exactThreshold_noSurcharge() {
        // gross = exactly 5000 — condition is > not >=
        List<PayrollLineItem> items = calculator.calculate(
                new BigDecimal("5000.00"), BigDecimal.ZERO);

        assertThat(amount(items, LineItemCode.HIGH_SALARY_SURCHARGE)).isEqualByComparingTo("0.00");
    }

    @Test
    void calculate_justAboveThreshold_surchargeApplied() {
        // gross = 5001
        List<PayrollLineItem> items = calculator.calculate(
                new BigDecimal("5000.00"), new BigDecimal("1.00"));

        assertThat(amount(items, LineItemCode.HIGH_SALARY_SURCHARGE)).isEqualByComparingTo("250.05");
    }

    @Test
    void calculate_alwaysReturnsFiveItems() {
        assertThat(calculator.calculate(new BigDecimal("1000"), BigDecimal.ZERO)).hasSize(5);
        assertThat(calculator.calculate(new BigDecimal("6000"), new BigDecimal("500"))).hasSize(5);
    }

    @Test
    void calculate_labelContainsConfiguredRate() {
        List<PayrollLineItem> items = calculator.calculate(
                new BigDecimal("3000.00"), BigDecimal.ZERO);

        String taxLabel = label(items, LineItemCode.INCOME_TAX);
        String socialLabel = label(items, LineItemCode.SOCIAL_CONTRIBUTION);
        String surchargeLabel = label(items, LineItemCode.HIGH_SALARY_SURCHARGE);

        assertThat(taxLabel).contains("15%");
        assertThat(socialLabel).contains("9%");
        assertThat(surchargeLabel).contains("5%");
    }

    @Test
    void calculate_earningsHaveCorrectType() {
        List<PayrollLineItem> items = calculator.calculate(
                new BigDecimal("3000.00"), new BigDecimal("500.00"));

        assertThat(type(items, LineItemCode.BASE_SALARY)).isEqualTo(LineItemType.EARNING);
        assertThat(type(items, LineItemCode.BONUS)).isEqualTo(LineItemType.EARNING);
        assertThat(type(items, LineItemCode.INCOME_TAX)).isEqualTo(LineItemType.DEDUCTION);
        assertThat(type(items, LineItemCode.SOCIAL_CONTRIBUTION)).isEqualTo(LineItemType.DEDUCTION);
        assertThat(type(items, LineItemCode.HIGH_SALARY_SURCHARGE)).isEqualTo(LineItemType.DEDUCTION);
    }

    // --- Helpers ---

    private PayrollProperties buildProperties() {
        PayrollProperties p = new PayrollProperties();
        p.setIncomeTaxRate(new BigDecimal("0.15"));
        p.setSocialContributionRate(new BigDecimal("0.09"));
        p.setHighSalaryThreshold(new BigDecimal("5000.00"));
        p.setHighSalarySurchargeRate(new BigDecimal("0.05"));
        return p;
    }

    private BigDecimal amount(List<PayrollLineItem> items, LineItemCode code) {
        return items.stream()
                .filter(i -> i.getCode() == code)
                .findFirst()
                .orElseThrow()
                .getAmount();
    }

    private String label(List<PayrollLineItem> items, LineItemCode code) {
        return items.stream()
                .filter(i -> i.getCode() == code)
                .findFirst()
                .orElseThrow()
                .getLabel();
    }

    private LineItemType type(List<PayrollLineItem> items, LineItemCode code) {
        return items.stream()
                .filter(i -> i.getCode() == code)
                .findFirst()
                .orElseThrow()
                .getType();
    }
}
