package com.legalaid.lawyer.repositories;

import com.legalaid.lawyer.LawyerPracticeArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LawyerPracticeAreaRepository extends JpaRepository<LawyerPracticeArea, UUID> {
    List<LawyerPracticeArea> findAllByLawyer_Id(UUID lawyerId);
    void deleteAllByLawyer_Id(UUID lawyerId);
}
