package com.legalaid.user;

import com.legalaid.lawyer.LawyerProfile;
import com.legalaid.lawyer.VerificationStatus;
import com.legalaid.lawyer.repositories.LawyerRepository;
import com.legalaid.user.dto.UpdateUserRequest;
import com.legalaid.user.dto.UserPrivateResponse;
import com.legalaid.user.dto.UserPublicResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final LawyerRepository lawyerProfileRepository;

    // ── GET /api/users/:id — public profile ─────────────────
    public UserPublicResponse getPublicProfile(UUID userId) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserPublicResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .build();
    }

    // ── GET /api/users/me — own full profile ─────────────────
    public UserPrivateResponse getMyProfile(UUID userId) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return toPrivateResponse(user);
    }

    // ── PUT /api/users/me — update own profile ───────────────
    @Transactional
    public UserPrivateResponse updateMyProfile(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // only update fields that were actually sent (not null)
        if(request.getName() != null) user.setName(request.getName());
        if(request.getPhone() != null) user.setPhone(request.getPhone());
        if(request.getAddress() != null) user.setAddress(request.getAddress());
        if(request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        userRepository.save(user);
        return toPrivateResponse(user);
    }

    // ── PUT /api/users/me/role — request lawyer role ─────────
    // Does NOT grant LAWYER role immediately.
    // Sets a flag so onboarding flow begins.
    // Actual role is granted only after admin approves docs.
    @Transactional
    public UserPrivateResponse requestLawyerRole(UUID userId) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if(user.getRole() == UserRole.LAWYER) {
            throw new RuntimeException("You are already a lawyer");
        }

        if(user.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("Admin cannot request lawyer role");
        }

        // check if a lawyer profile already exist(already applied before)
        boolean alreadyApplied = lawyerProfileRepository.existsByUserId(userId);
        if(alreadyApplied) {
            throw new RuntimeException("You have already submitted a lawyer application");
        }

        // Create and empty lawyer profile with UNSUBMITTED status
        // user will fil this in during onboarding
        LawyerProfile profile = LawyerProfile.builder()
                .userId(userId)
                .verificationStatus(VerificationStatus.UNSUBMITTED)
                .onboardingCompleted(false)
                .rating(BigDecimal.ZERO)
                .reviewCount(0)
                .experienceYears(0)
                .build();

        lawyerProfileRepository.save(profile);

        // Role stays client until admin approves
        return toPrivateResponse(user);
    }

    // ── DELETE /api/users/me — soft delete account ───────────
    @Transactional
    public void deleteMyAccount(UUID userId) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setDeletedAt(Instant.now());
        userRepository.save(user);
    }

    // ── Internal helper — used by other services ─────────────
    public User findActiveUserById(UUID userId) {
        return userRepository.findActiveById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Private mapper ───────────────────────────────────────
    private UserPrivateResponse toPrivateResponse(User user) {
        return UserPrivateResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
