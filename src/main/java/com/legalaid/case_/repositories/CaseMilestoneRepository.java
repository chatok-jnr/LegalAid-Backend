package com.legalaid.case_.repositories;

import com.legalaid.case_.entitys.CaseMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseMilestoneRepository extends JpaRepository<CaseMilestone, UUID> {

    List<CaseMilestone> findAllByLegalCase_IdOrderBySortOrderAsc(UUID caseId);

    Optional<CaseMilestone> findByIdAndLegalCase_Id(UUID milestoneId, UUID caseId);

    void deleteAllByLegalCase_Id(UUID caseId);
}