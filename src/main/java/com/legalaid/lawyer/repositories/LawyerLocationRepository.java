package com.legalaid.lawyer.repositories;

import com.legalaid.lawyer.LawyerLocation;
import com.legalaid.lawyer.LawyerPracticeArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LawyerLocationRepository extends JpaRepository<LawyerLocation, UUID> {
    List<LawyerLocation> findAllByLawyer_Id(UUID lawyerId);
    void deleteAllByLawyer_Id(UUID lawyerId);
}
