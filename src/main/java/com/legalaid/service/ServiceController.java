package com.legalaid.service;

import com.legalaid.common.response.ApiResponse;
import com.legalaid.service.dto.CreateServiceRequest;
import com.legalaid.service.dto.ServiceDetailResponse;
import com.legalaid.service.dto.ServiceSummaryResponse;
import com.legalaid.service.dto.UpdateServiceRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {
    private final ServiceService serviceService;

    // ── GET /api/services ─────────────────────────────────────
    // Public — no auth needed
    // Filters: category, minPrice, maxPrice, deliveryDays
    // Pagination: page, size, sort
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ServiceSummaryResponse>>> browseService(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer deliveryDays,
            @PageableDefault(size = 12, sort = "createdAt") Pageable pageable
            ) {
        ApiResponse<Page<ServiceSummaryResponse>> body = ApiResponse.success(serviceService.browseServices(category, minPrice, maxPrice, deliveryDays, pageable));
        return ResponseEntity.ok(body);
    }

    // ── GET /api/services/mine ────────────────────────────────
    // Lawyer sees all their own services including inactive
    // NOTE: /mine must be before /{id} to avoid UUID parse error
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<ServiceSummaryResponse>>> getMyService(
            @AuthenticationPrincipal UUID userId
    ) {
        ApiResponse<List<ServiceSummaryResponse>> body = ApiResponse.success(serviceService.getMyServices(userId));
        return ResponseEntity.ok(body);
    }

    // ── GET /api/services/:id ─────────────────────────────────
    // Public — returns full detail of a single active service
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceDetailResponse>> getServiceDetail(
            @PathVariable UUID id) {

        ApiResponse<ServiceDetailResponse> body = ApiResponse.success(
                serviceService.getServiceDetail(id)
        );
        return ResponseEntity.ok(body);
    }

    // ── POST /api/services ────────────────────────────────────
    // Lawyer only — must be verified
    @PostMapping
    @PreAuthorize("hasAuthority('LAWYER')")
    public ResponseEntity<ApiResponse<ServiceDetailResponse>> createService(
            @AuthenticationPrincipal
            UUID userId,
            @Valid @RequestBody
            CreateServiceRequest request
    ) {
        ApiResponse<ServiceDetailResponse> body = ApiResponse.success(serviceService.createService(userId, request));
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/services/:id ─────────────────────────────────
    // Owner only — partial update
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LAWYER')")
    public ResponseEntity<ApiResponse<ServiceDetailResponse>> updateService(
            @AuthenticationPrincipal
            UUID userId,
            @RequestBody
            @Valid
            UpdateServiceRequest request,
            @PathVariable
            UUID id
    ) {
        ApiResponse<ServiceDetailResponse> body = ApiResponse.success(serviceService.updateService(userId, id, request));
        return ResponseEntity.ok(body);
    }

    // ── DELETE /api/services/:id ──────────────────────────────
    // Owner only — soft delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LAWYER')")
    public ResponseEntity<ApiResponse<Void>> deleteService(
            @AuthenticationPrincipal
            UUID userId,
            @PathVariable
            UUID id
    ) {
        serviceService.deleteService(userId, id);
        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.ok(body);
    }

    // ── GET /api/lawyers/:id/services ────────────────────────
    // Public — active services by a specific lawyer
    // Registered here instead of LawyerController to keep
    // service logic in the service package
    @GetMapping("/by-lawyer/{lawyerProfileId}")
    public ResponseEntity<ApiResponse<List<ServiceSummaryResponse>>> getServicesByLawyer(
            @PathVariable
            UUID lawyerProfileId
    ) {
        ApiResponse<List<ServiceSummaryResponse>> body = ApiResponse.success(serviceService.getServiceByLawyer(lawyerProfileId));
        return ResponseEntity.ok(body);
    }
}
