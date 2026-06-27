package com.legalaid.lawyer;

import com.cloudinary.Cloudinary;
import com.legalaid.lawyer.dto.*;
import com.legalaid.lawyer.repositories.*;
import com.legalaid.lawyer.verification.LawyerVerificationDoc;
import com.legalaid.lawyer.verification.LawyerVerificationDocRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class LawyerService {

    private final LawyerRepository                lawyerRepository;
    private final LawyerVerificationDocRepository verificationDocRepository;
    private final UserRepository userRepository;
    private final LawyerPracticeAreaRepository practiceAreaRepository;
    private final LawyerLocationRepository        locationRepository;
    private final LawyerCourtRepository courtRepository;
    private final LawyerLanguageRepository languageRepository;
    private final LawyerAvailabilityRepository availabilityRepository;
    private final Cloudinary cloudinary;

    // ── GET /api/lawyers — search verified lawyers ───────────
    public Page<LawyerPublicResponse> searchLawyers(String category,
                                                    String city,
                                                    Double minRating,
                                                    Pageable pageable) {
        return lawyerRepository
                .searchLawyers(category, city, minRating, pageable)
                .map(profile -> {
                    User user = userRepository.findActiveById(profile.getUserId())
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    return toPublicResponse(profile, user);
                });
    }

    // ── GET /api/lawyers/:id — public profile ────────────────
    public LawyerPublicResponse getPublicProfile(UUID lawyerProfileId) {
        LawyerProfile profile = lawyerRepository.findById(lawyerProfileId)
                .orElseThrow(() -> new RuntimeException("Lawyer not found"));

        User user = userRepository.findActiveById(profile.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return toPublicResponse(profile, user);
    }

    // ── GET /api/lawyers/me — own profile ───────────────────
    public LawyerProfileResponse getMyProfile(UUID userId) {
        LawyerProfile profile = lawyerRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Lawyer not found"));
        return toPrivateResponse(profile);
    }

    // ── GET /api/lawyers/me/stats ────────────────────────────
    // Earnings + case counts wired up after payment/case packages are built
    public LawyerStatsResponse getMyStats(UUID userId) {
        LawyerProfile profile = lawyerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Lawyer profile not found"));

        return LawyerStatsResponse.builder()
                .rating(profile.getRating())
                .reviewCount(profile.getReviewCount())
                .totalEarnings(BigDecimal.ZERO)
                .pendingPayout(BigDecimal.ZERO)
                .totalCases(0)
                .activeCases(0)
                .completedContracts(0)
                .build();
    }

    // ── PUT /api/lawyers/me/onboarding — step 1 ─────────────
    // Fills profile + all sub-tables.
    // Does NOT submit for verification yet.
    // Can be re-done if rejected — clears and re-saves sub-tables.
    @Transactional
    public LawyerProfileResponse completeOnboarding(
            UUID userId,
            OnboardingRequest request
    ) {
        LawyerProfile profile = lawyerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No lawyer profile found - request lawyer role first"));

        if(profile.getVerificationStatus() == VerificationStatus.APPROVED) {
            throw new RuntimeException("Already verified - onboarding not needed");
        }

        // Update core profile fields
        profile.setTitle(request.getTitle());
        profile.setBarId(request.getBarId());
        profile.setExperienceYears(request.getExperienceYears());
        profile.setBio(request.getBio());
        profile.setOfficeAddress(request.getOfficeAddress());
        profile.setFeeMin(request.getFeeMin());
        profile.setFeeMax(request.getFeeMax());
        profile.setOnboardingCompleted(true);
        lawyerRepository.save(profile);

        // clear all sub-table first so re-doing onboardin works clearly
        practiceAreaRepository.deleteAllByLawyer_Id(profile.getId());
        courtRepository.deleteAllByLawyer_Id(profile.getId());
        languageRepository.deleteAllByLawyer_Id(profile.getId());
        availabilityRepository.deleteAllByLawyer_Id(profile.getId());
        locationRepository.deleteAllByLawyer_Id(profile.getId());

        // Save practice areas
        if(request.getPracticeAreas() != null) {
            request.getPracticeAreas().forEach(area -> practiceAreaRepository.save(
                    LawyerPracticeArea.builder()
                            .lawyer(profile)
                            .area(area)
                            .build()
            ));
        }

        // Save courts
        if(request.getLanguages() != null) {
            request.getLanguages().forEach(lang ->
                languageRepository.save(
                        LawyerLanguage.builder()
                                .lawyer(profile)
                                .language(lang)
                                .build()
                )
            );
        }

        // Save availability slots
        if (request.getAvailability() != null) {
            request.getAvailability().forEach(slot ->
                    availabilityRepository.save(
                            LawyerAvailability.builder()
                                    .lawyer(profile)
                                    .slot(slot)
                                    .build()));
        }

        // Save primary location
        locationRepository.save(
                LawyerLocation.builder()
                        .lawyer(profile)
                        .city(request.getCity())
                        .division(request.getDivision())
                        .isPrimary(true)
                        .build());

        return toPrivateResponse(profile);
    }

    // ── POST /api/lawyers/me/verify — step 2 ────────────────
    // Upload bar cert + NID docs → sets verification_status = PENDING
    // Admin then reviews and approves/rejects in admin package
    @Transactional
    public void submitVerificationDocs(
            UUID userId,
            List<VerificationDocRequest> docs
    ) {
        LawyerProfile profile = lawyerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Lawyer profile not found"));

        if(!profile.getOnboardingCompleted()) {
            throw new RuntimeException("Complete onboarding before submitting verification documnets");
        }

        if(profile.getVerificationStatus() == VerificationStatus.PENDING) {
            throw new RuntimeException("Verification already submitted - awaiting admin review");
        }

        if(profile.getVerificationStatus() == VerificationStatus.APPROVED) {
            throw new RuntimeException("Already verified");
        }

        // Clear previous rejected docs before submitting
        verificationDocRepository.deleteAllByLawyerId(profile.getId());
        docs.forEach(doc ->
                verificationDocRepository.save(
                        LawyerVerificationDoc.builder()
                                .lawyerId(profile.getId())
                                .docUrl(doc.getDocUrl())
                                .cloudinaryId(doc.getCloudinaryId())
                                .docType(LawyerVerificationDoc.DocType.valueOf(doc.getDocType()))
                                .uploadedAt(Instant.now())
                                .build()
                        )
                );
        profile.setVerificationStatus(VerificationStatus.PENDING);
        lawyerRepository.save(profile);
    }

    // ── Private mapper — own profile (all fields) ────────────
    private LawyerProfileResponse toPrivateResponse(LawyerProfile profile) {
        return LawyerProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .title(profile.getTitle())
                .barId(profile.getBarId())
                .experienceYears(profile.getExperienceYears())
                .bio(profile.getBio())
                .officeAddress(profile.getOfficeAddress())
                .feeMin(profile.getFeeMin())
                .feeMax(profile.getFeeMax())
                .verificationStatus(profile.getVerificationStatus().name())
                .onboardingCompleted(profile.getOnboardingCompleted())
                .rating(profile.getRating())
                .reviewCount(profile.getReviewCount())
                .verifiedAt(profile.getVerifiedAt())
                .practiceAreas(
                        practiceAreaRepository.findAllByLawyer_Id(profile.getId())
                                .stream().map(LawyerPracticeArea::getArea).toList()
                )
                .courts(
                        courtRepository.findAllByLawyer_Id(profile.getId())
                                .stream().map(LawyerCourt::getCourtName).toList())

                .languages(
                        languageRepository.findAllByLawyer_Id(profile.getId())
                                .stream().map(LawyerLanguage::getLanguage).toList()
                )
                .availability(
                        availabilityRepository.findAllByLawyer_Id(profile.getId())
                                .stream().map(LawyerAvailability::getSlot).toList()
                )
                .locations(
                        locationRepository.findAllByLawyer_Id(profile.getId())
                                .stream()
                                .map(
                                        l -> LawyerProfileResponse.LocationDto.builder()
                                        .city(l.getCity())
                                        .division(l.getDivision())
                                        .isPrimary(l.getIsPrimary())
                                        .build()
                                )
                                .toList()
                )
                .build();
    }

    // ── Public mapper — limited fields only ──────────────────
    private LawyerPublicResponse toPublicResponse(LawyerProfile profile, User user) {
        return LawyerPublicResponse.builder()
                .id(profile.getId())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .title(profile.getTitle())
                .experienceYears(profile.getExperienceYears())
                .bio(profile.getBio())
                .feeMin(profile.getFeeMin())
                .feeMax(profile.getFeeMax())
                .rating(profile.getRating())
                .reviewCount(profile.getReviewCount())
                .isVerified(profile.getVerificationStatus() == VerificationStatus.APPROVED)
                .verifiedAt(profile.getVerifiedAt())
                .practiceAreas(
                        practiceAreaRepository.findAllByLawyer_Id(profile.getId())
                                .stream().map(LawyerPracticeArea::getArea).toList()
                )
                .courts(
                        courtRepository.findAllByLawyer_Id(profile.getId())
                                .stream().map(LawyerCourt::getCourtName).toList()
                )
                .languages(
                        languageRepository.findAllByLawyer_Id(profile.getId())
                                .stream().map(LawyerLanguage::getLanguage).toList()
                )
                .availability(
                        availabilityRepository.findAllByLawyer_Id(profile.getId())
                                .stream().map(LawyerAvailability::getSlot).toList()
                )
                .locations(
                        locationRepository.findAllByLawyer_Id(profile.getId())
                                .stream().map(LawyerLocation::getCity).toList()
                )
                .build();
    }
}