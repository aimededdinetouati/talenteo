package com.talenteo.hr.employee.controller;

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

class EmployeeControllerIT extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/employees";

    // --- POST /api/v1/employees ---

    @Test
    void createEmployee_validPayload_returns201() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidDto("john@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void createEmployee_missingFirstName_returns400WithErrors() throws Exception {
        EmployeeDto dto = buildValidDto("missing@example.com");
        dto.setFirstName(null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").isNotEmpty());
    }

    @Test
    void createEmployee_invalidEmail_returns400WithErrors() throws Exception {
        EmployeeDto dto = buildValidDto("notanemail");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").isNotEmpty());
    }

    @Test
    void createEmployee_duplicateEmail_returns409() throws Exception {
        String body = objectMapper.writeValueAsString(buildValidDto("dup@example.com"));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    // --- GET /api/v1/employees/{id} ---

    @Test
    void getEmployeeById_exists_returns200() throws Exception {
        long id = createEmployee(buildValidDto("get@example.com"));

        mockMvc.perform(get(BASE_URL + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value("get@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void getEmployeeById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", 9999))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/v1/employees ---

    @Test
    void listEmployees_noFilters_returnsAll() throws Exception {
        createEmployee(buildValidDto("list1@example.com"));
        createEmployee(buildValidDto("list2@example.com"));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listEmployees_filterByStatus_returnsOnlyActive() throws Exception {
        long id = createEmployee(buildValidDto("inactive@example.com"));
        createEmployee(buildValidDto("active@example.com"));

        mockMvc.perform(delete(BASE_URL + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL).param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("active@example.com"));
    }

    // --- PUT /api/v1/employees/{id} ---

    @Test
    void updateEmployee_validPayload_returns200() throws Exception {
        long id = createEmployee(buildValidDto("update@example.com"));

        EmployeeDto updated = buildValidDto("update@example.com");
        updated.setFirstName("UpdatedName");

        mockMvc.perform(put(BASE_URL + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("UpdatedName"));
    }

    @Test
    void updateEmployee_emailTakenByAnother_returns409() throws Exception {
        createEmployee(buildValidDto("first@example.com"));
        long secondId = createEmployee(buildValidDto("second@example.com"));

        EmployeeDto dto = buildValidDto("first@example.com");

        mockMvc.perform(put(BASE_URL + "/{id}", secondId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    // --- DELETE /api/v1/employees/{id} ---

    @Test
    void deactivateEmployee_exists_returns204AndStatusInactive() throws Exception {
        long id = createEmployee(buildValidDto("deactivate@example.com"));

        mockMvc.perform(delete(BASE_URL + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void deactivateEmployee_notFound_returns404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", 9999))
                .andExpect(status().isNotFound());
    }

    // --- Helpers ---

    private EmployeeDto buildValidDto(String email) {
        return EmployeeDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .hireDate(LocalDate.of(2024, 1, 15))
                .department("Engineering")
                .position("Backend Developer")
                .baseSalary(new BigDecimal("5000.00"))
                .bonus(new BigDecimal("500.00"))
                .build();
    }

    private long createEmployee(EmployeeDto dto) throws Exception {
        String response = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}
