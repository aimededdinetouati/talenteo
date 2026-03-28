package com.talenteo.hr.payroll.service;

import com.talenteo.hr.common.exception.InactiveEmployeeException;
import com.talenteo.hr.common.exception.PayrollAlreadyExistsException;
import com.talenteo.hr.common.exception.PayrollAlreadyFinalizedException;
import com.talenteo.hr.common.exception.ResourceNotFoundException;
import com.talenteo.hr.employee.domain.Employee;
import com.talenteo.hr.employee.domain.EmployeeStatus;
import com.talenteo.hr.employee.repository.EmployeeRepository;
import com.talenteo.hr.payroll.domain.LineItemType;
import com.talenteo.hr.payroll.domain.PayrollLineItem;
import com.talenteo.hr.payroll.domain.PayrollSlip;
import com.talenteo.hr.payroll.domain.PayrollStatus;
import com.talenteo.hr.payroll.dto.PayrollSlipDto;
import com.talenteo.hr.payroll.repository.PayrollSlipRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final PayrollSlipRepository payrollSlipRepository;
    private final PayrollCalculator payrollCalculator;
    private final ModelMapper modelMapper;

    public PayrollSlipDto calculatePayroll(Long employeeId, int year, int month) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new InactiveEmployeeException("Employee " + employeeId + " is not active");
        }

        if (payrollSlipRepository.existsByEmployeeIdAndPeriodYearAndPeriodMonth(employeeId, year, month)) {
            throw new PayrollAlreadyExistsException(
                    "Payroll already exists for employee " + employeeId + " period " + year + "/" + month);
        }

        List<PayrollLineItem> lineItems = payrollCalculator.calculate(employee.getBaseSalary(), employee.getBonus());

        BigDecimal netSalary = computeNetSalary(lineItems);

        PayrollSlip slip = new PayrollSlip();
        slip.setEmployee(employee);
        slip.setPeriodYear(year);
        slip.setPeriodMonth(month);
        slip.setNetSalary(netSalary);
        slip.setStatus(PayrollStatus.DRAFT);
        slip.setCalculatedAt(LocalDateTime.now());

        for (PayrollLineItem item : lineItems) {
            item.setPayrollSlip(slip);
            slip.getLineItems().add(item);
        }

        PayrollSlip saved = payrollSlipRepository.save(slip);
        return modelMapper.map(saved, PayrollSlipDto.class);
    }

    public PayrollSlipDto recalculatePayroll(Long employeeId, int year, int month) {
        PayrollSlip slip = payrollSlipRepository
                .findByEmployeeIdAndPeriodYearAndPeriodMonth(employeeId, year, month)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payroll not found for employee " + employeeId + " period " + year + "/" + month));

        if (slip.getStatus() == PayrollStatus.FINALIZED) {
            throw new PayrollAlreadyFinalizedException(
                    "Payroll for employee " + employeeId + " period " + year + "/" + month + " is already finalized");
        }

        payrollSlipRepository.delete(slip);
        payrollSlipRepository.flush();

        return calculatePayroll(employeeId, year, month);
    }

    public PayrollSlipDto finalizePayroll(Long employeeId, int year, int month) {
        PayrollSlip slip = payrollSlipRepository
                .findByEmployeeIdAndPeriodYearAndPeriodMonth(employeeId, year, month)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payroll not found for employee " + employeeId + " period " + year + "/" + month));

        if (slip.getStatus() == PayrollStatus.FINALIZED) {
            throw new PayrollAlreadyFinalizedException(
                    "Payroll for employee " + employeeId + " period " + year + "/" + month + " is already finalized");
        }

        slip.setStatus(PayrollStatus.FINALIZED);
        PayrollSlip saved = payrollSlipRepository.save(slip);
        return modelMapper.map(saved, PayrollSlipDto.class);
    }

    public PayrollSlipDto getPayrollSlip(Long employeeId, int year, int month) {
        PayrollSlip slip = payrollSlipRepository
                .findByEmployeeIdAndPeriodYearAndPeriodMonth(employeeId, year, month)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payroll not found for employee " + employeeId + " period " + year + "/" + month));

        return modelMapper.map(slip, PayrollSlipDto.class);
    }

    public Page<PayrollSlipDto> listPayrollHistory(Long employeeId, Pageable pageable) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }

        return payrollSlipRepository
                .findByEmployeeIdOrderByPeriodYearDescPeriodMonthDesc(employeeId, pageable)
                .map(slip -> modelMapper.map(slip, PayrollSlipDto.class));
    }

    private BigDecimal computeNetSalary(List<PayrollLineItem> lineItems) {
        BigDecimal earnings = lineItems.stream()
                .filter(i -> i.getType() == LineItemType.EARNING)
                .map(PayrollLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deductions = lineItems.stream()
                .filter(i -> i.getType() == LineItemType.DEDUCTION)
                .map(PayrollLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return earnings.subtract(deductions);
    }
}
