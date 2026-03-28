package com.talenteo.hr.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkPayrollResult {

    private List<BulkCreatedEntry> created;
    private List<BulkSkippedEntry> skipped;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkCreatedEntry {
        private Long employeeId;
        private String employeeName;
        private String period;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkSkippedEntry {
        private Long employeeId;
        private String employeeName;
        private String reason;
    }
}
