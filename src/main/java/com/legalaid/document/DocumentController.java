package com.legalaid.document;

import com.legalaid.common.response.ApiResponse;
import com.legalaid.document.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // ── POST /api/documents/upload ────────────────────────────
    // Multipart form upload — file goes through backend to Cloudinary
    // Form fields: file (required), caseId, contractId, folderName (all optional)
    @PostMapping(
            value    = "/api/documents/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @AuthenticationPrincipal UUID userId,
            @RequestPart("file")                         MultipartFile file,
            @RequestParam(required = false) UUID         caseId,
            @RequestParam(required = false) UUID         contractId,
            @RequestParam(required = false) String       folderName) {

        ApiResponse<DocumentResponse> body = ApiResponse.success(
                documentService.uploadDocument(
                        userId, file, caseId, contractId, folderName)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ── GET /api/documents ────────────────────────────────────
    // All documents for the authenticated user
    // Optional filters: folder=<name>, starred=true
    @GetMapping("/api/documents")
    public ResponseEntity<ApiResponse<DocumentListResponse>> getMyDocuments(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) String  folder,
            @RequestParam(required = false) Boolean starred) {

        ApiResponse<DocumentListResponse> body = ApiResponse.success(
                documentService.getMyDocuments(userId, folder, starred)
        );
        return ResponseEntity.ok(body);
    }

    // ── GET /api/cases/:id/documents ─────────────────────────
    // All documents linked to a case
    @GetMapping("/api/cases/{caseId}/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByCase(
            @PathVariable UUID caseId) {

        ApiResponse<List<DocumentResponse>> body = ApiResponse.success(
                documentService.getDocumentsByCase(caseId)
        );
        return ResponseEntity.ok(body);
    }

    // ── GET /api/contracts/:id/documents ─────────────────────
    // All documents linked to a contract
    @GetMapping("/api/contracts/{contractId}/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByContract(
            @PathVariable UUID contractId) {

        ApiResponse<List<DocumentResponse>> body = ApiResponse.success(
                documentService.getDocumentsByContract(contractId)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/documents/:id ────────────────────────────────
    // Update metadata — name, folder, starred, shared
    @PutMapping("/api/documents/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateDocumentRequest request) {

        ApiResponse<DocumentResponse> body = ApiResponse.success(
                documentService.updateDocument(id, userId, request)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/documents/:id/star ───────────────────────────
    // Toggle starred status
    @PutMapping("/api/documents/{id}/star")
    public ResponseEntity<ApiResponse<DocumentResponse>> toggleStar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<DocumentResponse> body = ApiResponse.success(
                documentService.toggleStar(id, userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── DELETE /api/documents/:id ─────────────────────────────
    // Soft deletes DB record + removes from Cloudinary
    @DeleteMapping("/api/documents/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        documentService.deleteDocument(id, userId);
        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.ok(body);
    }
}