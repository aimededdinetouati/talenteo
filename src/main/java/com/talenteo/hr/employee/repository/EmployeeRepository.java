package com.talenteo.hr.employee.repository;

import com.talenteo.hr.employee.domain.Employee;
import com.talenteo.hr.employee.domain.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("""
            SELECT e FROM Employee e
            WHERE (:status IS NULL OR e.status = :status)
            AND (:department IS NULL OR LOWER(e.department) LIKE LOWER(CONCAT('%', CAST(:department AS string), '%')))
            """)
    Page<Employee> findWithFilters(
            @Param("status") EmployeeStatus status,
            @Param("department") String department,
            Pageable pageable);
}
