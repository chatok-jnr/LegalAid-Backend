package com.legalaid.case_.repositories;

import com.legalaid.case_.entitys.CaseTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseTagRepository extends JpaRepository<CaseTag, UUID> {

    List<CaseTag> findAllByLegalCase_Id(UUID caseId);

    void deleteAllByLegalCase_Id(UUID caseId);
}