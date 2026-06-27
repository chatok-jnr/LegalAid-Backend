package com.legalaid.lawyer.repositories;

import com.legalaid.lawyer.LawyerLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LawyerLanguageRepository extends JpaRepository<LawyerLanguage, UUID> {
    List<LawyerLanguage> findAllByLawyer_Id(UUID lawyerId);
    void deleteAllByLawyer_Id(UUID lawyerId);
}


