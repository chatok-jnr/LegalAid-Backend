package com.legalaid.service.repositories;

import com.legalaid.service.ServiceFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceFeatureRepository extends JpaRepository<ServiceFeature, UUID> {

    List<ServiceFeature> findAllByService_IdOrderBySortOrderAsc(UUID serviceId);
    void deleteAllByService_Id(UUID serviceId);
}