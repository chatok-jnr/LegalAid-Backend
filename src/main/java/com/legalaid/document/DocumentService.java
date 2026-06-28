package com.legalaid.document;

import com.legalaid.document.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CloudinaryService  cloudinaryService;

    // ── POST /api/documents/upload ────────────────────────────
    // Uploads file to Cloudinary then saves metadata to DB.
    // Optional query params: caseId, contractId, folderName
    @Transactional
    public DocumentResponse uploadDocument(UUID userId,
                                           MultipartFile file,
                                           UUID caseId,
                                           UUID contractId,
                                           String folderName) {
        // Upload to Cloudinary
        Map<String, Object> uploadResult;
        try {
            uploadResult = cloudinaryService.upload(file, folderName, userId);
        } catch (IllegalArgumentException e) {
            // Validation error (wrong type, too large)
            throw new RuntimeException(e.getMessage());
        } catch (IOException e) {
            log.error("Cloudinary upload failed for user: {}", userId, e);
            throw new RuntimeException("File upload failed. Please try again.");
        }

        // Save metadata to DB
        Document document = Document.builder()
                .ownerId(userId)
                .uploadedBy(userId)
                .caseId(caseId)
                .contractId(contractId)
                .name(file.getOriginalFilename())
                .url((String) uploadResult.get("secure_url"))
                .cloudinaryId((String) uploadResult.get("public_id"))
                .fileSizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .folderName(folderName)
                .isStarred(false)
                .isShared(false)
                .build();

        document = documentRepository.save(document);
        return toResponse(document);
    }

    // ── GET /api/documents ────────────────────────────────────
    // All documents for the authenticated user.
    // Optional filter: folder, starred
    public DocumentListResponse getMyDocuments(UUID userId,
                                               String folder,
                                               Boolean starred) {
        List<Document> documents;

        if (starred != null && starred) {
            documents = documentRepository
                    .findAllByOwnerIdAndIsStarredTrueAndDeletedAtIsNull(userId);
        } else if (folder != null && !folder.isBlank()) {
            documents = documentRepository
                    .findAllByOwnerIdAndFolderNameAndDeletedAtIsNull(userId, folder);
        } else {
            documents = documentRepository
                    .findAllByOwnerIdAndDeletedAtIsNull(userId);
        }

        List<String> folders = documentRepository
                .findDistinctFoldersByOwnerId(userId);

        List<DocumentResponse> responses = documents.stream()
                .map(this::toResponse)
                .toList();

        return DocumentListResponse.builder()
                .documents(responses)
                .folders(folders)
                .totalCount(responses.size())
                .build();
    }

    // ── GET /api/cases/:id/documents ─────────────────────────
    public List<DocumentResponse> getDocumentsByCase(UUID caseId) {
        return documentRepository.findAllByCaseIdAndDeletedAtIsNull(caseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── GET /api/contracts/:id/documents ─────────────────────
    public List<DocumentResponse> getDocumentsByContract(UUID contractId) {
        return documentRepository.findAllByContractIdAndDeletedAtIsNull(contractId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── PUT /api/documents/:id ────────────────────────────────
    // Update name, folder, starred, shared — owner only
    @Transactional
    public DocumentResponse updateDocument(UUID documentId,
                                           UUID userId,
                                           UpdateDocumentRequest request) {
        Document document = documentRepository
                .findByIdAndOwnerIdAndDeletedAtIsNull(documentId, userId)
                .orElseThrow(() -> new RuntimeException(
                        "Document not found or you don't own it"));

        if (request.getName()       != null) document.setName(request.getName());
        if (request.getFolderName() != null) document.setFolderName(request.getFolderName());
        if (request.getIsStarred()  != null) document.setIsStarred(request.getIsStarred());
        if (request.getIsShared()   != null) document.setIsShared(request.getIsShared());

        document = documentRepository.save(document);
        return toResponse(document);
    }

    // ── PUT /api/documents/:id/star — toggle starred ──────────
    @Transactional
    public DocumentResponse toggleStar(UUID documentId, UUID userId) {
        Document document = documentRepository
                .findByIdAndOwnerIdAndDeletedAtIsNull(documentId, userId)
                .orElseThrow(() -> new RuntimeException(
                        "Document not found or you don't own it"));

        document.setIsStarred(!document.getIsStarred());
        document = documentRepository.save(document);
        return toResponse(document);
    }

    // ── DELETE /api/documents/:id ─────────────────────────────
    // Soft deletes DB record + deletes from Cloudinary
    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        Document document = documentRepository
                .findByIdAndOwnerIdAndDeletedAtIsNull(documentId, userId)
                .orElseThrow(() -> new RuntimeException(
                        "Document not found or you don't own it"));

        // Delete from Cloudinary first
        if (document.getCloudinaryId() != null) {
            cloudinaryService.delete(document.getCloudinaryId(),
                    document.getMimeType());
        }

        // Soft delete DB record
        document.setDeletedAt(Instant.now());
        documentRepository.save(document);
    }

    // ── Mapper ───────────────────────────────────────────────
    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .ownerId(document.getOwnerId())
                .uploadedBy(document.getUploadedBy())
                .caseId(document.getCaseId())
                .contractId(document.getContractId())
                .name(document.getName())
                .url(document.getUrl())
                .cloudinaryId(document.getCloudinaryId())
                .fileSizeBytes(document.getFileSizeBytes())
                .mimeType(document.getMimeType())
                .folderName(document.getFolderName())
                .isStarred(document.getIsStarred())
                .isShared(document.getIsShared())
                .createdAt(document.getCreatedAt())
                .build();
    }
}