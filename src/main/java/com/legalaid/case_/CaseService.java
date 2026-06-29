package com.legalaid.case_;

import com.legalaid.case_.dto.*;
import com.legalaid.case_.enums.*;
import com.legalaid.case_.entitys.CaseAccess;
import com.legalaid.case_.entitys.CaseMilestone;
import com.legalaid.case_.entitys.CaseTag;
import com.legalaid.case_.entitys.LegalCase;
import com.legalaid.case_.enums.CaseAccessRole;
import com.legalaid.case_.repositories.CaseAccessRepository;
import com.legalaid.case_.repositories.CaseMilestoneRepository;
import com.legalaid.case_.repositories.CaseRepository;
import com.legalaid.case_.repositories.CaseTagRepository;
import com.legalaid.notification.NotificationService;
import com.legalaid.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {
    private final CaseRepository          caseRepository;
    private final CaseAccessRepository accessRepository;
    private final CaseMilestoneRepository milestoneRepository;
    private final CaseTagRepository tagRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // ── GET /api/cases — list own cases ──────────────────────
    // Returns all cases the user can see:
    // cases they own (clientId) + cases assigned to them (lawyerId)
    // + cases they were invited to via case_access
    public List<CaseSummaryResponse> getMyCases(UUID userId) {
        return caseRepository.findAllVisibleToUser(userId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    // ── GET /api/cases/:id — case detail ─────────────────────
    // Any user with access can view — owner, assigned lawyer, or invited
    public CaseResponse getCaseDetail(UUID caseId, UUID requesterId) {
        LegalCase legalCase = caseRepository.findByIdAndDeletedAtIsNull(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        assertHasAccess(legalCase, requesterId);

        return toFullResponse(legalCase);
    }

    // ── POST /api/cases — create case ────────────────────────
    // CLIENT or any lawyer with access can create
    @Transactional
    public CaseResponse createCase(UUID creatorId, CreateCaseRequest request) {
        LegalCase legalCase = LegalCase.builder()
                .clientId(creatorId)
                .title(request.getTitle())
                .category(request.getCategory())
                .court(request.getCourt())
                .description(request.getDescription())
                .status(CaseStatus.OPEN)
                .priority(request.getPriority() != null
                        ? request.getPriority() : CasePriority.MEDIUM)
                .nextHearingDate(request.getNextHearingDate())
                .filedDate(request.getFiledDate())
                .build();

        legalCase = caseRepository.save(legalCase);

        // Save tags
        if (request.getTags() != null) {
            saveTags(legalCase, request.getTags());
        }

        // Save milestones
        if (request.getMilestones() != null) {
            saveMilestones(legalCase, request.getMilestones());
        }

        return toFullResponse(legalCase);
    }

    // ── PUT /api/cases/:id — update case ─────────────────────
    // Owner or EDITOR access required
    @Transactional
    public CaseResponse updateCase(UUID caseId,
                                   UUID requesterId,
                                   UpdateCaseRequest request) {
        LegalCase legalCase = caseRepository.findByIdAndDeletedAtIsNull(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        assertHasEditAccess(legalCase, requesterId);

        // Update only fields that were sent
        if (request.getTitle()           != null) legalCase.setTitle(request.getTitle());
        if (request.getCategory()        != null) legalCase.setCategory(request.getCategory());
        if (request.getCourt()           != null) legalCase.setCourt(request.getCourt());
        if (request.getDescription()     != null) legalCase.setDescription(request.getDescription());
        if (request.getStatus()          != null) legalCase.setStatus(request.getStatus());
        if (request.getPriority()        != null) legalCase.setPriority(request.getPriority());
        if (request.getNextHearingDate() != null) legalCase.setNextHearingDate(request.getNextHearingDate());
        if (request.getFiledDate()       != null) legalCase.setFiledDate(request.getFiledDate());
        if (request.getLawyerId()        != null) legalCase.setLawyerId(request.getLawyerId());

        caseRepository.save(legalCase);

        // Replace tags if provided
        if (request.getTags() != null) {
            tagRepository.deleteAllByLegalCase_Id(caseId);
            saveTags(legalCase, request.getTags());
        }

        return toFullResponse(legalCase);
    }

    // ── DELETE /api/cases/:id — soft delete ──────────────────
    // Owner only
    @Transactional
    public void deleteCase(UUID caseId, UUID requesterId) {
        LegalCase legalCase = caseRepository.findByIdAndDeletedAtIsNull(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        assertIsOwner(legalCase, requesterId);

        legalCase.setDeletedAt(Instant.now());
        caseRepository.save(legalCase);
    }

    // ── POST /api/cases/:id/access — invite user ─────────────
    // Owner only can invite others
    @Transactional
    public void grantAccess(UUID caseId,
                            UUID requesterId,
                            GrantAccessRequest request) {
        LegalCase legalCase = caseRepository.findByIdAndDeletedAtIsNull(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        assertIsOwner(legalCase, requesterId);

        // Prevent duplicate access grants
        if (accessRepository.existsByLegalCase_IdAndUserId(caseId, request.getUserId())) {
            throw new RuntimeException("User already has access to this case");
        }

        // Owner cannot be re-invited
        if (legalCase.getClientId().equals(request.getUserId())) {
            throw new RuntimeException("Cannot invite the case owner");
        }

        CaseAccess access = CaseAccess.builder()
                .legalCase(legalCase)
                .userId(request.getUserId())
                .invitedBy(requesterId)
                .role(request.getRole() != null
                        ? request.getRole() : CaseAccessRole.VIEWER)
                .build();

        accessRepository.save(access);

        String inviter = userRepository.getUserNameById(requesterId)
                        .orElse("Someone");
        notificationService.notifyCaseInvite(
                request.getUserId(),
                caseId,
                inviter,
                legalCase.getTitle()
        );
    }

    // ── DELETE /api/cases/:id/access/:userId — remove user ───
    // Owner only
    @Transactional
    public void revokeAccess(UUID caseId, UUID requesterId, UUID targetUserId) {
        LegalCase legalCase = caseRepository.findByIdAndDeletedAtIsNull(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        assertIsOwner(legalCase, requesterId);

        if (legalCase.getClientId().equals(targetUserId)) {
            throw new RuntimeException("Cannot revoke access from the case owner");
        }

        accessRepository.deleteByLegalCase_IdAndUserId(caseId, targetUserId);
    }

    // ── PUT /api/cases/:id/milestones/:mId — mark done ───────
    // Owner or EDITOR access required
    @Transactional
    public CaseResponse.MilestoneDto toggleMilestone(UUID caseId,
                                                     UUID milestoneId,
                                                     UUID requesterId) {
        LegalCase legalCase = caseRepository.findByIdAndDeletedAtIsNull(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        assertHasEditAccess(legalCase, requesterId);

        CaseMilestone milestone = milestoneRepository
                .findByIdAndLegalCase_Id(milestoneId, caseId)
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        // Toggle done state
        boolean nowDone = !milestone.getIsDone();
        milestone.setIsDone(nowDone);
        milestone.setCompletedAt(nowDone ? Instant.now() : null);
        milestoneRepository.save(milestone);

        return toMilestoneDto(milestone);
    }

    // ── Access control helpers ───────────────────────────────

    // Any access - owner, assigned lawyer, or invited user
    private void assertHasAccess(LegalCase legalCase, UUID userId) {
        boolean isOwner = legalCase.getClientId().equals(userId);
        boolean isLawyer = userId.equals(legalCase.getLawyerId());
        boolean isInvited = accessRepository
                .existsByLegalCase_IdAndUserId(legalCase.getId(), userId);

        if(!isOwner && !isLawyer && !isInvited) {
            throw new RuntimeException("You don not have access to this case");
        }
    }

    // Edit access - owner, assigned lawyer with EDITOR role, or invited EDITOR
    private void assertHasEditAccess(LegalCase legalCase, UUID userId) {
        if(legalCase.getClientId().equals(userId)) return; // owner always can edit
        if(userId.equals(legalCase.getLawyerId())) return; // assigned lawyer can edit

        // Check if invited with EDITOR role
        accessRepository.findByLegalCase_IdAndUserId(legalCase.getId(), userId)
                .filter(a -> a.getRole() == CaseAccessRole.EDITOR)
                .orElseThrow(() -> new RuntimeException("You don not have access to this case"));
    }

    private void assertIsOwner(LegalCase legalCase, UUID userId) {
        if(!legalCase.getClientId().equals(userId)) {
            throw new RuntimeException("You don not have access to this case");
        }
    }

    // ── Sub-item helpers ─────────────────────────────────────

    private void saveTags(LegalCase legalCase, List<String> tags) {
        tags.forEach(tag -> tagRepository.save(
                CaseTag.builder()
                        .legalCase(legalCase)
                        .tag(tag)
                        .build()));
    }

    private void saveMilestones(LegalCase legalCase, List<CreateCaseRequest.MilestoneItem> items) {
        for(int i = 0; i < items.size(); i++) {
            CreateCaseRequest.MilestoneItem item = items.get(i);
            milestoneRepository.save(CaseMilestone.builder()
                    .legalCase(legalCase)
                    .label(item.getLabel())
                    .dueDate(item.getDueDate())
                    .sortOrder(item.getSortOrder() != null ? item.getSortOrder() : i)
                    .build()
            );
        }

    }

    // ── Mappers ──────────────────────────────────────────────

    private CaseSummaryResponse toSummaryResponse(LegalCase legalCase) {
        List<String> tags = tagRepository.findAllByLegalCase_Id(legalCase.getId())
                .stream().map(CaseTag::getTag).toList();

        return CaseSummaryResponse.builder()
                .id(legalCase.getId())
                .clientId(legalCase.getClientId())
                .lawyerId(legalCase.getLawyerId())
                .title(legalCase.getTitle())
                .category(legalCase.getCategory())
                .court(legalCase.getCourt())
                .status(legalCase.getStatus().name())
                .priority(legalCase.getPriority().name())
                .nextHearingDate(legalCase.getNextHearingDate())
                .filedDate(legalCase.getFiledDate())
                .tags(tags)
                .createdAt(legalCase.getCreatedAt())
                .updatedAt(legalCase.getUpdatedAt())
                .build();
    }

    private CaseResponse toFullResponse(LegalCase legalCase) {
        List<String> tags = tagRepository.findAllByLegalCase_Id(legalCase.getId())
                .stream().map(CaseTag::getTag).toList();

        List<CaseResponse.MilestoneDto> milestones =
                milestoneRepository.findAllByLegalCase_IdOrderBySortOrderAsc(legalCase.getId())
                        .stream().map(this::toMilestoneDto).toList();

        List<CaseResponse.AccessDto> accessList =
                accessRepository.findAllByLegalCase_Id(legalCase.getId())
                        .stream()
                        .map(a -> CaseResponse.AccessDto.builder()
                                .userId(a.getUserId())
                                .role(a.getRole().name())
                                .invitedBy(a.getInvitedBy())
                                .grantedAt(a.getGrantedAt())
                                .build())
                        .toList();

        return CaseResponse.builder()
                .id(legalCase.getId())
                .clientId(legalCase.getClientId())
                .lawyerId(legalCase.getLawyerId())
                .title(legalCase.getTitle())
                .category(legalCase.getCategory())
                .court(legalCase.getCourt())
                .description(legalCase.getDescription())
                .status(legalCase.getStatus().name())
                .priority(legalCase.getPriority().name())
                .nextHearingDate(legalCase.getNextHearingDate())
                .filedDate(legalCase.getFiledDate())
                .tags(tags)
                .milestones(milestones)
                .accessList(accessList)
                .createdAt(legalCase.getCreatedAt())
                .updatedAt(legalCase.getUpdatedAt())
                .build();
    }

    private CaseResponse.MilestoneDto toMilestoneDto(CaseMilestone m) {
        return CaseResponse.MilestoneDto.builder()
                .id(m.getId())
                .label(m.getLabel())
                .isDone(m.getIsDone())
                .dueDate(m.getDueDate())
                .completedAt(m.getCompletedAt())
                .sortOrder(m.getSortOrder())
                .build();
    }
}
