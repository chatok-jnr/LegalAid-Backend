package com.legalaid.case_.dto;

import com.legalaid.case_.enums.CasePriority;
import com.legalaid.case_.enums.CaseStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UpdateCaseRequest {
    @Size(max = 200)
    private String title;

    @Size(max = 100)
    private String category;

    @Size(max = 150)
    private String court;

    @Size(max = 5000)
    private String description;

    private CaseStatus status;

    private CasePriority priority;

    private LocalDate nextHearingDate;

    private LocalDate filedDate;

    private UUID lawyerId; // assign/change lawyer

    // When provided, replaces all existing tags
    private List<String> tags;
}
