package com.legalaid.case_.dto;

import com.legalaid.case_.enums.CasePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;


@Setter
@Getter
public class CreateCaseRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 chracters")
    private String title;

    @Size(max = 100)
    private String category;

    @Size(max = 150)
    private String court;

    @Size(max = 5000)
    private String description;

    private CasePriority priority; // default to medium if null

    private LocalDate nextHearingDate;

    private LocalDate filedDate;

    private List<String> tags; // e.g. ["Land", "Boundary", "Civil"]

    private List<MilestoneItem> milestones;

    @Getter
    @Setter
    public static class MilestoneItem{
        @NotBlank(message = "Milestone label is required")
        @Size(max = 200, message = "Milestone label must not exceed 200 characters")
        private String label;
        private LocalDate dueDate;
        private Integer sortOrder;
    }
}
