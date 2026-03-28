package com.talenteo.hr.payroll.service;

import com.talenteo.hr.common.exception.InactiveEmployeeException;
import com.talenteo.hr.common.exception.PayrollAlreadyExistsException;
import com.talenteo.hr.common.exception.PayrollAlreadyFinalizedException;
import com.talenteo.hr.common.exception.ResourceNotFoundException;
import com.talenteo.hr.employee.domain.Employee;
import com.talenteo.hr.employee.domain.EmployeeStatus;
import com.talenteo.hr.employee.repository.EmployeeRepository;
import com.talenteo.hr.payroll.domain.LineItemCode;
import com.talenteo.hr.payroll.domain.LineItemType;
import com.talenteo.hr.payroll.domain.PayrollLineItem;
import com.talenteo.hr.payroll.domain.PayrollSlip;
import com.talenteo.hr.payroll.domain.PayrollStatus;
import com.talenteo.hr.payroll.dto.PayrollSlipDto;
import com.talenteo.hr.payroll.repository.PayrollSlipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock EmployeeRepository employeeRepository;
    @Mock PayrollSlipRepository payrollSlipRepository;
    @Mock PayrollCalculator payrollCalculator;
    @Mock ModelMapper modelMapper;
    @InjectMocks PayrollService payrollService;

    // --- calculatePayroll ---

    @Test
    void calculatePayroll_happyPath_savesSlipAndReturnsDto() {
        Employee employee = activeEmployee();
        PayrollSlip savedSlip = new PayrollSlip();
        PayrollSlipDto expectedDto = new PayrollSlipDto();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(payrollSlipRepository.existsByEmployeeIdAndPeriodYearAndPeriodMonth(1L, 2026, 3)).thenReturn(false);
        when(payrollCalculator.calculate(any(), any())).thenReturn(sampleLineItems());
        when(payrollSlipRepository.save(any())).thenReturn(savedSlip);
        when(modelMapper.map(savedSlip, PayrollSlipDto.class)).thenReturn(expectedDto);

        PayrollSlipDto result = payrollService.calculatePayroll(1L, 2026, 3);

        assertThat(result).isEqualTo(expectedDto);
        verify(payrollSlipRepository).save(any(PayrollSlip.class));
    }

    @Test
    void calculatePayroll_unknownEmployee_throwsResourceNotFoundException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payrollService.calculatePayroll(99L, 2026, 3))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void calculatePayroll_inactiveEmployee_throwsInactiveEmployeeException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(inactiveEmployee()));

        assertThatThrownBy(() -> payrollService.calculatePayroll(1L, 2026, 3))
                .isInstanceOf(InactiveEmployeeException.class);
    }

    @Test
    void calculatePayroll_duplicatePeriod_throwsPayrollAlreadyExistsException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(activeEmployee()));
        when(payrollSlipRepository.existsByEmployeeIdAndPeriodYearAndPeriodMonth(1L, 2026, 3)).thenReturn(true);

        assertThatThrownBy(() -> payrollService.calculatePayroll(1L, 2026, 3))
                .isInstanceOf(PayrollAlreadyExistsException.class);
    }

    // --- finalizePayroll ---

    @Test
    void finalizePayroll_draftSlip_setsFinalizedStatus() {
        PayrollSlip slip = draftSlip();
        PayrollSlipDto expectedDto = new PayrollSlipDto();

        when(payrollSlipRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(1L, 2026, 3))
                .thenReturn(Optional.of(slip));
        when(payrollSlipRepository.save(slip)).thenReturn(slip);
        when(modelMapper.map(slip, PayrollSlipDto.class)).thenReturn(expectedDto);

        payrollService.finalizePayroll(1L, 2026, 3);

        assertThat(slip.getStatus()).isEqualTo(PayrollStatus.FINALIZED);
        verify(payrollSlipRepository).save(slip);
    }

    @Test
    void finalizePayroll_notFound_throwsResourceNotFoundException() {
        when(payrollSlipRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(1L, 2026, 3))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> payrollService.finalizePayroll(1L, 2026, 3))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void finalizePayroll_alreadyFinalized_throwsPayrollAlreadyFinalizedException() {
        when(payrollSlipRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(1L, 2026, 3))
                .thenReturn(Optional.of(finalizedSlip()));

        assertThatThrownBy(() -> payrollService.finalizePayroll(1L, 2026, 3))
                .isInstanceOf(PayrollAlreadyFinalizedException.class);
    }

    // --- getPayrollSlip ---

    @Test
    void getPayrollSlip_exists_returnsDto() {
        PayrollSlip slip = draftSlip();
        PayrollSlipDto expectedDto = new PayrollSlipDto();

        when(payrollSlipRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(1L, 2026, 3))
                .thenReturn(Optional.of(slip));
        when(modelMapper.map(slip, PayrollSlipDto.class)).thenReturn(expectedDto);

        assertThat(payrollService.getPayrollSlip(1L, 2026, 3)).isEqualTo(expectedDto);
    }

    @Test
    void getPayrollSlip_notFound_throwsResourceNotFoundException() {
        when(payrollSlipRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(1L, 2026, 3))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> payrollService.getPayrollSlip(1L, 2026, 3))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- listPayrollHistory ---

    @Test
    void listPayrollHistory_unknownEmployee_throwsResourceNotFoundException() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> payrollService.listPayrollHistory(99L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Helpers ---

    private Employee activeEmployee() {
        Employee e = new Employee();
        e.setStatus(EmployeeStatus.ACTIVE);
        e.setBaseSalary(new BigDecimal("3000.00"));
        e.setBonus(new BigDecimal("500.00"));
        return e;
    }

    private Employee inactiveEmployee() {
        Employee e = new Employee();
        e.setStatus(EmployeeStatus.INACTIVE);
        return e;
    }

    private PayrollSlip draftSlip() {
        PayrollSlip slip = new PayrollSlip();
        slip.setStatus(PayrollStatus.DRAFT);
        return slip;
    }

    private PayrollSlip finalizedSlip() {
        PayrollSlip slip = new PayrollSlip();
        slip.setStatus(PayrollStatus.FINALIZED);
        return slip;
    }

    private List<PayrollLineItem> sampleLineItems() {
        PayrollLineItem earning = new PayrollLineItem();
        earning.setType(LineItemType.EARNING);
        earning.setCode(LineItemCode.BASE_SALARY);
        earning.setLabel("Base Salary");
        earning.setAmount(new BigDecimal("3000.00"));

        PayrollLineItem deduction = new PayrollLineItem();
        deduction.setType(LineItemType.DEDUCTION);
        deduction.setCode(LineItemCode.INCOME_TAX);
        deduction.setLabel("Income Tax");
        deduction.setAmount(new BigDecimal("450.00"));

        return List.of(earning, deduction);
    }
}
