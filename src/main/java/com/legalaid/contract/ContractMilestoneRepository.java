package com.legalaid.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractMilestoneRepository extends JpaRepository<ContractMilestone, UUID> {

    List<ContractMilestone> findAllByContract_IdOrderBySortOrderAsc(UUID contractId);

    Optional<ContractMilestone> findByIdAndContract_Id(UUID milestoneId, UUID contractId);

    void deleteAllByContract_Id(UUID contractId);
}