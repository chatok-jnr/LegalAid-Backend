package com.legalaid.lawyer.repositories;

import com.legalaid.lawyer.LawyerCourt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LawyerCourtRepository extends JpaRepository<LawyerCourt, UUID> {
    List<LawyerCourt> findAllByLawyer_Id(UUID lawyerId);
    void deleteAllByLawyer_Id(UUID lawyerId);
}
