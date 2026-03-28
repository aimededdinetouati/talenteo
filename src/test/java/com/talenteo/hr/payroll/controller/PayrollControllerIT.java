package com.talenteo.hr.payroll.controller;

import com.talenteo.hr.BaseIntegrationTest;
import com.talenteo.hr.employee.dto.EmployeeDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PayrollControllerIT extends BaseIntegrationTest {

    private static final String EMPLOYEE_URL = "/api/v1/employees";
    private static final int YEAR = 2026;
    private static final int MONTH = 3;

    // --- POST /{id}/payroll/{year}/{month} — calculate ---

    @Test
    void calculatePayroll_validEmployee_returns201WithDraftSlip() throws Exception {
        long id = createEmployee();

        mockMvc.perform(post(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}", id, YEAR, MONTH))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.netSalary").isNotEmpty())
                .andExpect(jsonPath("$.lineItems").isNotEmpty());
    }

    @Test
    void calculatePayroll_unknownEmployee_returns404() throws Exception {
        mockMvc.perform(post(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}", 9999, YEAR, MONTH))
                .andExpect(status().isNotFound());
    }

    @Test
    void calculatePayroll_inactiveEmployee_returns422() throws Exception {
        long id = createEmployee();

        mockMvc.perform(delete(EMPLOYEE_URL + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}", id, YEAR, MONTH))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void calculatePayroll_duplicatePeriod_returns409() throws Exception {
        long id = createEmployee();

        mockMvc.perform(post(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}", id, YEAR, MONTH))
                .andExpect(status().isCreated());

        mockMvc.perform(post(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}", id, YEAR, MONTH))
                .andExpect(status().isConflict());
    }

    @Test
    void calculatePayroll_verifyNetSalaryAndLineItems() throws Exception {
        // baseSalary=3000, bonus=500 → gross=3500
        // incomeTax=3500×0.15=525.00, social=3500×0.09=315.00, surcharge=0 (gross<=5000)
        // net=3500-525-315=2660.00
        long id = createEmployee();

        mockMvc.perform(post(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}", id, YEAR, MONTH))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.netSalary").value(2660.0))
                .andExpect(jsonPath("$.lineItems.length()").value(5))
                .andExpect(jsonPath("$.lineItems[?(@.code == 'BASE_SALARY')].amount").value(3000.0))
                .andExpect(jsonPath("$.lineItems[?(@.code == 'INCOME_TAX')].amount").value(525.0));
    }

    // --- PUT /{id}/payroll/{year}/{month}/recalculate ---

    @Test
    void recalculatePayroll_draftSlip_returns200WithDraftStatus() throws Exception {
        long id = createEmployee();
        calculatePayroll(id, YEAR, MONTH);

        mockMvc.perform(put(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}/recalculate", id, YEAR, MONTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.lineItems").isNotEmpty());
    }

    @Test
    void recalculatePayroll_notFound_returns404() throws Exception {
        long id = createEmployee();

        mockMvc.perform(put(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}/recalculate", id, YEAR, MONTH))
                .andExpect(status().isNotFound());
    }

    @Test
    void recalculatePayroll_finalizedSlip_returns409() throws Exception {
        long id = createEmployee();
        calculatePayroll(id, YEAR, MONTH);

        mockMvc.perform(post(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}/finalize", id, YEAR, MONTH))
                .andExpect(status().isOk());

        mockMvc.perform(put(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}/recalculate", id, YEAR, MONTH))
                .andExpect(status().isConflict());
    }

    // --- POST /{id}/payroll/{year}/{month}/finalize ---

    @Test
    void finalizePayroll_draftSlip_returns200WithFinalizedStatus() throws Exception {
        long id = createEmployee();
        calculatePayroll(id, YEAR, MONTH);

        mockMvc.perform(post(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}/finalize", id, YEAR, MONTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZED"));
    }

    @Test
    void finalizePayroll_notFound_returns404() throws Exception {
        long id = createEmployee();

        mockMvc.perform(post(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}/finalize", id, YEAR, MONTH))
                .andExpect(status().isNotFound());
    }

    @Test
    void finalizePayroll_alreadyFinalized_returns409() throws Exception {
        long id = createEmployee();
        calculatePayroll(id, YEAR, MONTH);

        mockMvc.perform(post(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}/finalize", id, YEAR, MONTH))
                .andExpect(status().isOk());

        mockMvc.perform(post(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}/finalize", id, YEAR, MONTH))
                .andExpect(status().isConflict());
    }

    // --- GET /{id}/payroll/{year}/{month} ---

    @Test
    void getPayrollSlip_exists_returns200WithLineItems() throws Exception {
        long id = createEmployee();
        calculatePayroll(id, YEAR, MONTH);

        mockMvc.perform(get(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}", id, YEAR, MONTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodYear").value(YEAR))
                .andExpect(jsonPath("$.periodMonth").value(MONTH))
                .andExpect(jsonPath("$.lineItems").isNotEmpty());
    }

    @Test
    void getPayrollSlip_notFound_returns404() throws Exception {
        long id = createEmployee();

        mockMvc.perform(get(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}", id, YEAR, MONTH))
                .andExpect(status().isNotFound());
    }

    // --- GET /{id}/payroll ---

    @Test
    void listPayrollHistory_multipleSlips_returnsPaginatedOrderedByPeriodDesc() throws Exception {
        long id = createEmployee();
        calculatePayroll(id, 2026, 2);
        calculatePayroll(id, 2026, 3);

        mockMvc.perform(get(EMPLOYEE_URL + "/{id}/payroll", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].periodMonth").value(3))
                .andExpect(jsonPath("$.content[1].periodMonth").value(2));
    }

    @Test
    void listPayrollHistory_unknownEmployee_returns404() throws Exception {
        mockMvc.perform(get(EMPLOYEE_URL + "/{id}/payroll", 9999))
                .andExpect(status().isNotFound());
    }

    // --- Helpers ---

    private long createEmployee() throws Exception {
        EmployeeDto dto = EmployeeDto.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith." + System.nanoTime() + "@example.com")
                .hireDate(LocalDate.of(2024, 1, 15))
                .department("Engineering")
                .position("Backend Developer")
                .baseSalary(new BigDecimal("3000.00"))
                .bonus(new BigDecimal("500.00"))
                .build();

        String response = mockMvc.perform(post(EMPLOYEE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createHighSalaryEmployee() throws Exception {
        EmployeeDto dto = EmployeeDto.builder()
                .firstName("Senior")
                .lastName("Dev")
                .email("senior.dev." + System.nanoTime() + "@example.com")
                .hireDate(LocalDate.of(2024, 1, 15))
                .department("Engineering")
                .position("Principal Engineer")
                .baseSalary(new BigDecimal("6000.00"))
                .bonus(new BigDecimal("500.00"))
                .build();

        String response = mockMvc.perform(post(EMPLOYEE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private void calculatePayroll(long employeeId, int year, int month) throws Exception {
        mockMvc.perform(post(EMPLOYEE_URL + "/{id}/payroll/{year}/{month}", employeeId, year, month))
                .andExpect(status().isCreated());
    }
}
