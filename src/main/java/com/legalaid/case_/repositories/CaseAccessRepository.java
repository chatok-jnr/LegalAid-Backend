package com.legalaid.case_.repositories;

import com.legalaid.case_.entitys.CaseAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseAccessRepository extends JpaRepository<CaseAccess, UUID> {

    List<CaseAccess> findAllByLegalCase_Id(UUID caseId);

    Optional<CaseAccess> findByLegalCase_IdAndUserId(UUID caseId, UUID userId);

    boolean existsByLegalCase_IdAndUserId(UUID caseId, UUID userId);

    void deleteByLegalCase_IdAndUserId(UUID caseId, UUID userId);
}