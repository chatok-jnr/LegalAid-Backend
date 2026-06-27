package com.legalaid.service.repositories;

import com.legalaid.service.ServiceFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceFaqRepository extends JpaRepository<ServiceFaq, UUID> {

    List<ServiceFaq> findAllByService_IdOrderBySortOrderAsc(UUID serviceId);
    void deleteAllByService_Id(UUID serviceId);
}