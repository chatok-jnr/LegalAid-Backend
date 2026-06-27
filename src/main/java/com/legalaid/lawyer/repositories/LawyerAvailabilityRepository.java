package com.legalaid.lawyer.repositories;

import com.legalaid.lawyer.LawyerAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LawyerAvailabilityRepository extends JpaRepository<LawyerAvailability, UUID> {

    List<LawyerAvailability> findAllByLawyer_Id(UUID lawyerId);
    void deleteAllByLawyer_Id(UUID lawyerId);
}