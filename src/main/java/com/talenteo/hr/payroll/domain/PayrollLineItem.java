package com.talenteo.hr.payroll.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payroll_line_item")
public class PayrollLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_slip_id", nullable = false)
    private PayrollSlip payrollSlip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LineItemType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LineItemCode code;

    @Column(nullable = false, length = 150)
    private String label;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}
