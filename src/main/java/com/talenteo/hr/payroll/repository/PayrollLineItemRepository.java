package com.talenteo.hr.payroll.repository;

import com.talenteo.hr.payroll.domain.PayrollLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollLineItemRepository extends JpaRepository<PayrollLineItem, Long> {
}
