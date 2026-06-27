package com.legalaid.lawyer.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LawyerVerificationDocRepository extends JpaRepository<LawyerVerificationDoc, UUID> {

    List<LawyerVerificationDoc> findAllByLawyerId(UUID lawyerId);
    void deleteAllByLawyerId(UUID lawyerId);
}