package com.talenteo.hr.employee.service;

import com.talenteo.hr.common.exception.DuplicateEmailException;
import com.talenteo.hr.common.exception.ResourceNotFoundException;
import com.talenteo.hr.employee.domain.Employee;
import com.talenteo.hr.employee.domain.EmployeeStatus;
import com.talenteo.hr.employee.dto.EmployeeDto;
import com.talenteo.hr.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    EmployeeRepository employeeRepository;
    @Mock
    ModelMapper modelMapper;
    @InjectMocks
    EmployeeService employeeService;

    @Test
    void createEmployee_newEmail_savesAndReturnsDto() {
        EmployeeDto dto = new EmployeeDto();
        dto.setEmail("new@example.com");
        dto.setBonus(new BigDecimal("100.00"));

        Employee entity = new Employee();
        EmployeeDto expected = new EmployeeDto();

        when(employeeRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(modelMapper.map(dto, Employee.class)).thenReturn(entity);
        when(employeeRepository.save(entity)).thenReturn(entity);
        when(modelMapper.map(entity, EmployeeDto.class)).thenReturn(expected);

        EmployeeDto result = employeeService.createEmployee(dto);

        assertThat(result).isEqualTo(expected);
        verify(employeeRepository).save(entity);
    }

    @Test
    void createEmployee_duplicateEmail_throwsDuplicateEmailException() {
        EmployeeDto dto = new EmployeeDto();
        dto.setEmail("dup@example.com");

        when(employeeRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(dto))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void createEmployee_nullBonus_defaultsToZero() {
        EmployeeDto dto = new EmployeeDto();
        dto.setEmail("test@example.com");
        dto.setBonus(null);

        Employee entity = new Employee();

        when(employeeRepository.existsByEmail(any())).thenReturn(false);
        when(modelMapper.map(dto, Employee.class)).thenReturn(entity);
        when(employeeRepository.save(any())).thenReturn(entity);
        when(modelMapper.map(entity, EmployeeDto.class)).thenReturn(new EmployeeDto());

        employeeService.createEmployee(dto);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getBonus()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getEmployeeById_exists_returnsDto() {
        Employee employee = new Employee();
        EmployeeDto expected = new EmployeeDto();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(modelMapper.map(employee, EmployeeDto.class)).thenReturn(expected);

        assertThat(employeeService.getEmployeeById(1L)).isEqualTo(expected);
    }

    @Test
    void getEmployeeById_notFound_throwsResourceNotFoundException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateEmployee_emailTakenByOther_throwsDuplicateEmailException() {
        EmployeeDto dto = new EmployeeDto();
        dto.setEmail("taken@example.com");

        Employee existing = new Employee();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByEmailAndIdNot("taken@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.updateEmployee(1L, dto))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void deactivateEmployee_exists_setsInactive() {
        Employee employee = new Employee();
        employee.setStatus(EmployeeStatus.ACTIVE);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);

        employeeService.deactivateEmployee(1L);

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
        verify(employeeRepository).save(employee);
    }
}
