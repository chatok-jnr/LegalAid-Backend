package com.legalaid.service.repositories;

import com.legalaid.service.ServiceHighlight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceHighlightRepository extends JpaRepository<ServiceHighlight, UUID> {

    List<ServiceHighlight> findAllByService_IdOrderBySortOrderAsc(UUID serviceId);
    void deleteAllByService_Id(UUID serviceId);
}