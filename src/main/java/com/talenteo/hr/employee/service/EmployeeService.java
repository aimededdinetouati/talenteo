package com.talenteo.hr.employee.service;

import com.talenteo.hr.common.exception.DuplicateEmailException;
import com.talenteo.hr.common.exception.ResourceNotFoundException;
import com.talenteo.hr.employee.domain.Employee;
import com.talenteo.hr.employee.domain.EmployeeStatus;
import com.talenteo.hr.employee.dto.EmployeeDto;
import com.talenteo.hr.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public EmployeeDto createEmployee(EmployeeDto dto) {
        if (employeeRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateEmailException("Email already in use: " + dto.getEmail());
        }

        Employee employee = modelMapper.map(dto, Employee.class);
        employee.setStatus(EmployeeStatus.ACTIVE);
        if (employee.getBonus() == null) {
            employee.setBonus(BigDecimal.ZERO);
        }

        return modelMapper.map(employeeRepository.save(employee), EmployeeDto.class);
    }

    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeById(Long id) {
        Employee employee = findByIdOrThrow(id);
        return modelMapper.map(employee, EmployeeDto.class);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeDto> listEmployees(EmployeeStatus status, String department, Pageable pageable) {
        return employeeRepository
                .findWithFilters(status, department, pageable)
                .map(e -> modelMapper.map(e, EmployeeDto.class));
    }

    @Transactional
    public EmployeeDto updateEmployee(Long id, EmployeeDto dto) {
        Employee employee = findByIdOrThrow(id);

        if (employeeRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new DuplicateEmailException("Email already in use: " + dto.getEmail());
        }

        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setEmail(dto.getEmail());
        employee.setHireDate(dto.getHireDate());
        employee.setDepartment(dto.getDepartment());
        employee.setPosition(dto.getPosition());
        employee.setBaseSalary(dto.getBaseSalary());
        employee.setBonus(dto.getBonus() != null ? dto.getBonus() : BigDecimal.ZERO);

        return modelMapper.map(employeeRepository.save(employee), EmployeeDto.class);
    }

    @Transactional
    public void deactivateEmployee(Long id) {
        Employee employee = findByIdOrThrow(id);
        employee.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.save(employee);
    }

    private Employee findByIdOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }
}
