package com.legalaid.service.repositories;

import com.legalaid.service.ServiceMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceMediaRepository extends JpaRepository<ServiceMedia, UUID> {
    List<ServiceMedia> findAllByService_IdOrderBySortOrderAsc(UUID serviceId);
    void deleteAllByService_Id(UUID serviceId);
}
