package com.legalaid.service;

import com.legalaid.lawyer.LawyerProfile;
import com.legalaid.lawyer.VerificationStatus;
import com.legalaid.lawyer.repositories.LawyerRepository;
import com.legalaid.service.dto.CreateServiceRequest;
import com.legalaid.service.dto.ServiceDetailResponse;
import com.legalaid.service.dto.ServiceSummaryResponse;
import com.legalaid.service.dto.UpdateServiceRequest;
import com.legalaid.service.repositories.*;
import com.legalaid.user.User;
import com.legalaid.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceService {
    private final ServiceRepository serviceRepository;
    private final ServiceFeatureRepository featureRepository;
    private final ServiceMediaRepository mediaRepository;
    private final ServiceHighlightRepository highlightRepository;
    private final ServiceFaqRepository faqRepository;

    private final UserRepository userRepository;
    private final LawyerRepository lawyerRepository;

    // ── GET /api/services — browse paginated ─────────────────
    public Page<ServiceSummaryResponse> browseServices(String category,
                                                       BigDecimal minPrice,
                                                       BigDecimal maxPrice,
                                                       Integer deliveryDays,
                                                       Pageable pageable) {
        return serviceRepository
                .browseServices(category, minPrice, maxPrice, deliveryDays, pageable)
                .map(this::toSummaryResponse);
    }

    // ── GET /api/services/:id — service detail ───────────────
    public ServiceDetailResponse getServiceDetail(UUID serviceId) {
        LegalService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        return toDetailResponse(service);
    }

    // ── GET /api/lawyers/:id/services — by lawyer (public) ───
    // Returns only active non-deleted services
    public List<ServiceSummaryResponse> getServiceByLawyer(UUID lawyerId) {
        return serviceRepository
                .findAllByLawyerIdAndIsActiveTrueAndDeletedAtIsNull(lawyerId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    // ── GET /api/services/mine — own services (lawyer) ───────
    // Returns all statuses including inactive — lawyer sees everything
    public List<ServiceSummaryResponse> getMyServices(UUID userId) {
        LawyerProfile profile = lawyerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Lawyer not found"));
        return serviceRepository
                .findAllByLawyerIdAndDeletedAtIsNull(profile.getId())
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    // ── POST /api/services — create service (lawyer only) ────
    @Transactional
    public ServiceDetailResponse createService(UUID userId, CreateServiceRequest request) {
        LawyerProfile profile = lawyerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Lawyer profile not found"));
        if(profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new RuntimeException("Only verified lawyer can create service. " +
                    "Current status: " + profile.getVerificationStatus());
        }

        LegalService service = LegalService.builder()
                .lawyerId(profile.getId())
                .title(request.getTitle())
                .category(request.getCategory())
                .price(request.getPrice())
                .deliveryDays(request.getDeliveryDays())
                .revisions(request.getRevisions() != null ? request.getRevisions() : 1)
                .description(request.getDescription())
                .isActive(true)
                .build();

        service = serviceRepository.save(service);

        saveSubItems(service, request.getMedia(), request.getHighlights(), request.getFeatures(), request.getFaqs());

        return toDetailResponse(service);
    }

    // ── PUT /api/services/:id — update service (owner only) ──
    @Transactional
    public ServiceDetailResponse updateService(UUID userId, UUID serviceId, UpdateServiceRequest request) {
        LawyerProfile profile = lawyerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Lawyer profile not found"));

        LegalService service = serviceRepository
                .findByIdAndLawyerIdAndDeletedAtIsNull(serviceId, profile.getId())
                .orElseThrow(() -> new RuntimeException("Service not found or you don't own this service"));

        // Update only fields that were sent
        if(request.getTitle() != null) service.setTitle(request.getTitle());
        if(request.getCategory() != null) service.setCategory(request.getCategory());
        if(request.getPrice() != null) service.setPrice(request.getPrice());
        if(request.getDeliveryDays() != null) service.setDeliveryDays(request.getDeliveryDays());
        if(request.getRevisions() != null) service.setRevisions(request.getRevisions());
        if(request.getDescription() != null) service.setDescription(request.getDescription());
        if(request.getIsActive() != null) service.setIsActive(request.getIsActive());

        serviceRepository.save(service);

        // Replace sub-items only if they were included in the request
        if (request.getMedia()       != null) replaceMedia(service, request.getMedia());
        if (request.getHighlights()  != null) replaceHighlights(service, request.getHighlights());
        if (request.getFeatures()    != null) replaceFeatures(service, request.getFeatures());
        if (request.getFaqs()        != null) replaceFaqs(service, request.getFaqs());

        return toDetailResponse(service);
    }

    // ── DELETE /api/services/:id — soft delete (owner only) ──
    @Transactional
    public void deleteService(UUID userId, UUID serviceId) {
        LawyerProfile profile = lawyerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Lawyer profile not found"));

        LegalService service = serviceRepository.findByIdAndLawyerIdAndDeletedAtIsNull(serviceId, profile.getId())
                .orElseThrow(() -> new RuntimeException("Service not found or you don't own this service"));

        service.setDeletedAt(Instant.now());
        service.setIsActive(false);
        serviceRepository.save(service);
    }

    // ── Sub-item helpers ─────────────────────────────────────
    private void saveSubItems(LegalService service,
                              List<CreateServiceRequest.MediaItem> media,
                              List<CreateServiceRequest.TextItem> highlights,
                              List<CreateServiceRequest.TextItem> features,
                              List<CreateServiceRequest.FaqItem> faqs) {
        if (media != null) {
            media.forEach(m -> mediaRepository.save(ServiceMedia.builder()
                    .service(service)
                    .url(m.getUrl())
                    .cloudinaryId(m.getCloudinaryId())
                    .type(ServiceMedia.MediaType.valueOf(m.getType()))
                    .sortOrder(m.getSortOrder() != null ? m.getSortOrder() : 0)
                    .build()));
        }

        if (highlights != null) {
            highlights.forEach(h -> highlightRepository.save(ServiceHighlight.builder()
                    .service(service)
                    .text(h.getText())
                    .sortOrder(h.getSortOrder() != null ? h.getSortOrder() : 0)
                    .build()));
        }

        if (features != null) {
            features.forEach(f -> featureRepository.save(ServiceFeature.builder()
                    .service(service)
                    .text(f.getText())
                    .sortOrder(f.getSortOrder() != null ? f.getSortOrder() : 0)
                    .build()));
        }

        if (faqs != null) {
            faqs.forEach(f -> faqRepository.save(ServiceFaq.builder()
                    .service(service)
                    .question(f.getQuestion())
                    .answer(f.getAnswer())
                    .sortOrder(f.getSortOrder() != null ? f.getSortOrder() : 0)
                    .build()));
        }
    }

    private void replaceMedia(LegalService service,
                              List<CreateServiceRequest.MediaItem> media) {
        mediaRepository.deleteAllByService_Id(service.getId());
        media.forEach(m -> mediaRepository.save(ServiceMedia.builder()
                .service(service)
                .url(m.getUrl())
                .cloudinaryId(m.getCloudinaryId())
                .type(ServiceMedia.MediaType.valueOf(m.getType()))
                .sortOrder(m.getSortOrder() != null ? m.getSortOrder() : 0)
                .build()));
    }

    private void replaceHighlights(LegalService service,
                                   List<CreateServiceRequest.TextItem> highlights) {
        highlightRepository.deleteAllByService_Id(service.getId());
        highlights.forEach(h -> highlightRepository.save(ServiceHighlight.builder()
                .service(service)
                .text(h.getText())
                .sortOrder(h.getSortOrder() != null ? h.getSortOrder() : 0)
                .build()));
    }

    private void replaceFeatures(LegalService service,
                                 List<CreateServiceRequest.TextItem> features) {
        featureRepository.deleteAllByService_Id(service.getId());
        features.forEach(f -> featureRepository.save(ServiceFeature.builder()
                .service(service)
                .text(f.getText())
                .sortOrder(f.getSortOrder() != null ? f.getSortOrder() : 0)
                .build()));
    }

    private void replaceFaqs(LegalService service,
                             List<CreateServiceRequest.FaqItem> faqs) {
        faqRepository.deleteAllByService_Id(service.getId());
        faqs.forEach(f -> faqRepository.save(ServiceFaq.builder()
                .service(service)
                .question(f.getQuestion())
                .answer(f.getAnswer())
                .sortOrder(f.getSortOrder() != null ? f.getSortOrder() : 0)
                .build()));
    }

    // ── Mappers ──────────────────────────────────────────────

    private ServiceSummaryResponse toSummaryResponse(LegalService service) {
        // Fetch lawyer profile and user for name/avatar/rating
        LawyerProfile profile = lawyerRepository.findById(service.getLawyerId())
                .orElse(null);
        User user = profile != null
                ? userRepository.findActiveById(profile.getUserId()).orElse(null)
                : null;

        // Get first image as thumbnail
        String thumbnail = mediaRepository
                .findAllByService_IdOrderBySortOrderAsc(service.getId())
                .stream()
                .filter(m -> m.getType() == ServiceMedia.MediaType.IMAGE)
                .findFirst()
                .map(ServiceMedia::getUrl)
                .orElse(null);

        return ServiceSummaryResponse.builder()
                .id(service.getId())
                .lawyerId(service.getLawyerId())
                .lawyerName(user != null ? user.getName() : null)
                .lawyerAvatarUrl(user != null ? user.getAvatarUrl() : null)
                .lawyerRating(profile != null ? profile.getRating() : null)
                .lawyerVerified(profile != null &&
                        profile.getVerificationStatus() == VerificationStatus.APPROVED)
                .title(service.getTitle())
                .category(service.getCategory())
                .price(service.getPrice())
                .deliveryDays(service.getDeliveryDays())
                .revisions(service.getRevisions())
                .thumbnailUrl(thumbnail)
                .build();
    }

    private ServiceDetailResponse toDetailResponse(LegalService service) {
        LawyerProfile profile = lawyerRepository.findById(service.getLawyerId())
                .orElse(null);
        User user = profile != null
                ? userRepository.findActiveById(profile.getUserId()).orElse(null)
                : null;

        List<ServiceDetailResponse.MediaDto> media = mediaRepository
                .findAllByService_IdOrderBySortOrderAsc(service.getId())
                .stream()
                .map(m -> ServiceDetailResponse.MediaDto.builder()
                        .url(m.getUrl())
                        .cloudinaryId(m.getCloudinaryId())
                        .type(m.getType().name())
                        .sortOrder(m.getSortOrder())
                        .build())
                .toList();

        List<String> highlights = highlightRepository
                .findAllByService_IdOrderBySortOrderAsc(service.getId())
                .stream().map(ServiceHighlight::getText).toList();

        List<String> features = featureRepository
                .findAllByService_IdOrderBySortOrderAsc(service.getId())
                .stream().map(ServiceFeature::getText).toList();

        List<ServiceDetailResponse.FaqDto> faqs = faqRepository
                .findAllByService_IdOrderBySortOrderAsc(service.getId())
                .stream()
                .map(f -> ServiceDetailResponse.FaqDto.builder()
                        .question(f.getQuestion())
                        .answer(f.getAnswer())
                        .sortOrder(f.getSortOrder())
                        .build())
                .toList();

        return ServiceDetailResponse.builder()
                .id(service.getId())
                .lawyerId(service.getLawyerId())
                .lawyerName(user != null ? user.getName() : null)
                .lawyerAvatarUrl(user != null ? user.getAvatarUrl() : null)
                .lawyerTitle(profile != null ? profile.getTitle() : null)
                .lawyerRating(profile != null ? profile.getRating() : null)
                .lawyerReviewCount(profile != null ? profile.getReviewCount() : null)
                .lawyerVerified(profile != null &&
                        profile.getVerificationStatus() == VerificationStatus.APPROVED)
                .lawyerExperienceYears(profile != null ? profile.getExperienceYears() : null)
                .title(service.getTitle())
                .category(service.getCategory())
                .price(service.getPrice())
                .deliveryDays(service.getDeliveryDays())
                .revisions(service.getRevisions())
                .description(service.getDescription())
                .isActive(service.getIsActive())
                .createdAt(service.getCreatedAt())
                .media(media)
                .highlights(highlights)
                .features(features)
                .faqs(faqs)
                .build();
    }
}
