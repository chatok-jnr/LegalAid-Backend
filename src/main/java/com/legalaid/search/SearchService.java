package com.legalaid.search;

import com.legalaid.lawyer.LawyerLocation;
import com.legalaid.lawyer.repositories.LawyerLocationRepository;
import com.legalaid.lawyer.LawyerPracticeArea;
import com.legalaid.lawyer.repositories.LawyerPracticeAreaRepository;
import com.legalaid.lawyer.LawyerProfile;
import com.legalaid.lawyer.repositories.LawyerRepository;
import com.legalaid.search.dto.SearchResponse;
import com.legalaid.search.dto.SearchResultResponse;
import com.legalaid.service.LegalService;
import com.legalaid.service.repositories.ServiceMediaRepository;
import com.legalaid.service.repositories.ServiceRepository;
import com.legalaid.service.ServiceMedia;
import com.legalaid.user.User;
import com.legalaid.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final LawyerRepository           lawyerRepository;
    private final LawyerPracticeAreaRepository practiceAreaRepository;
    private final LawyerLocationRepository   locationRepository;
    private final ServiceRepository          serviceRepository;
    private final ServiceMediaRepository     mediaRepository;
    private final UserRepository             userRepository;

    // ── GET /api/search ───────────────────────────────────────
    // Unified search across lawyers and services.
    // type param: ALL (default), LAWYER, SERVICE
    // Delegates to existing repository search methods —
    // no new tables needed.
    public SearchResponse search(String query,
                                 String type,
                                 String category,
                                 String location,
                                 BigDecimal priceMin,
                                 BigDecimal priceMax,
                                 Double minRating,
                                 Integer deliveryDays,
                                 String sort,
                                 int page,
                                 int size) {

        int cappedSize  = Math.min(size, 50);
        String typeUpper = type != null ? type.toUpperCase() : "ALL";

        List<SearchResultResponse> results = new ArrayList<>();
        long totalResults = 0;
        int  totalPages   = 0;

        Pageable pageable = PageRequest.of(page, cappedSize);

        if ("ALL".equals(typeUpper) || "LAWYER".equals(typeUpper)) {
            // Search lawyers using existing LawyerRepository.searchLawyers()
            // Use query as category filter when no category provided
            String categoryFilter = category != null ? category
                    : (query != null && !query.isBlank() ? query : null);

            Page<LawyerProfile> lawyerPage = lawyerRepository.searchLawyers(
                    categoryFilter, location, minRating, pageable);

            lawyerPage.getContent().forEach(profile -> {
                User user = userRepository.findActiveById(profile.getUserId())
                        .orElse(null);
                results.add(toLawyerResult(profile, user));
            });

            if ("LAWYER".equals(typeUpper)) {
                totalResults = lawyerPage.getTotalElements();
                totalPages   = lawyerPage.getTotalPages();
            } else {
                totalResults += lawyerPage.getTotalElements();
            }
        }

        if ("ALL".equals(typeUpper) || "SERVICE".equals(typeUpper)) {
            // Search services using existing ServiceRepository.browseServices()
            // Use query as category filter when no category provided
            String categoryFilter = category != null ? category
                    : (query != null && !query.isBlank() ? query : null);

            Page<LegalService> servicePage = serviceRepository.browseServices(
                    categoryFilter, priceMin, priceMax, deliveryDays, pageable);

            servicePage.getContent().forEach(service -> {
                LawyerProfile profile = lawyerRepository
                        .findById(service.getLawyerId()).orElse(null);
                User user = profile != null
                        ? userRepository.findActiveById(profile.getUserId()).orElse(null)
                        : null;
                results.add(toServiceResult(service, profile, user));
            });

            if ("SERVICE".equals(typeUpper)) {
                totalResults = servicePage.getTotalElements();
                totalPages   = servicePage.getTotalPages();
            } else {
                totalResults += servicePage.getTotalElements();
                totalPages    = Math.max(totalPages, servicePage.getTotalPages());
            }
        }

        return SearchResponse.builder()
                .query(query)
                .type(typeUpper)
                .totalResults(totalResults)
                .page(page)
                .size(cappedSize)
                .totalPages(totalPages)
                .results(results)
                .build();
    }

    // ── Mappers ──────────────────────────────────────────────

    private SearchResultResponse toLawyerResult(LawyerProfile profile, User user) {
        List<String> areas = practiceAreaRepository
                .findAllByLawyer_Id(profile.getId())
                .stream().map(LawyerPracticeArea::getArea).toList();

        String primaryCity = locationRepository
                .findAllByLawyer_Id(profile.getId())
                .stream()
                .filter(LawyerLocation::getIsPrimary)
                .findFirst()
                .map(LawyerLocation::getCity)
                .orElse(null);

        return SearchResultResponse.builder()
                .type("LAWYER")
                .id(profile.getId())
                .title(user != null ? user.getName() : null)
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .category(areas.isEmpty() ? null : areas.get(0))
                .location(primaryCity)
                .lawyerRating(profile.getRating())
                .lawyerReviewCount(profile.getReviewCount())
                .experienceYears(profile.getExperienceYears())
                .isVerified(true)   // only APPROVED lawyers in search results
                .practiceAreas(areas)
                .build();
    }

    private SearchResultResponse toServiceResult(LegalService service,
                                                 LawyerProfile profile,
                                                 User user) {
        // Get first image as thumbnail
        String thumbnail = mediaRepository
                .findAllByService_IdOrderBySortOrderAsc(service.getId())
                .stream()
                .filter(m -> m.getType() == ServiceMedia.MediaType.IMAGE)
                .findFirst()
                .map(ServiceMedia::getUrl)
                .orElse(null);

        return SearchResultResponse.builder()
                .type("SERVICE")
                .id(service.getId())
                .title(service.getTitle())
                .avatarUrl(thumbnail)
                .category(service.getCategory())
                .price(service.getPrice())
                .deliveryDays(service.getDeliveryDays())
                .revisions(service.getRevisions())
                .lawyerId(service.getLawyerId())
                .lawyerName(user != null ? user.getName() : null)
                .serviceRating(profile != null ? profile.getRating() : null)
                .isVerified(profile != null)
                .build();
    }
}