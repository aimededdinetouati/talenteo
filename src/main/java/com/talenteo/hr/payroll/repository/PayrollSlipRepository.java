package com.talenteo.hr.payroll.repository;

import com.talenteo.hr.payroll.domain.PayrollSlip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayrollSlipRepository extends JpaRepository<PayrollSlip, Long> {

    Optional<PayrollSlip> findByEmployeeIdAndPeriodYearAndPeriodMonth(
            Long employeeId, int periodYear, int periodMonth);

    boolean existsByEmployeeIdAndPeriodYearAndPeriodMonth(
            Long employeeId, int periodYear, int periodMonth);

    Page<PayrollSlip> findByEmployeeIdOrderByPeriodYearDescPeriodMonthDesc(
            Long employeeId, Pageable pageable);
}
