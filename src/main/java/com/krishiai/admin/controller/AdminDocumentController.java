package com.krishiai.admin.controller;

import com.krishiai.admin.dto.RejectDocumentRequest;
import com.krishiai.admin.service.AdminDocumentService;
import com.krishiai.common.response.ApiResponse;
import com.krishiai.common.response.PageResponse;
import com.krishiai.expert.dto.ExpertDocumentResponse;
import com.krishiai.expert.entity.ExpertDocumentStatus;
import com.krishiai.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/documents")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final AdminDocumentService adminDocumentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExpertDocumentResponse>>> getDocuments(
            @RequestParam(required = false) ExpertDocumentStatus status,
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ExpertDocumentResponse> response = adminDocumentService.getDocuments(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", response));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<ExpertDocumentResponse>> getDocumentById(
            @PathVariable Long documentId) {
        ExpertDocumentResponse response = adminDocumentService.getDocumentById(documentId);
        return ResponseEntity.ok(ApiResponse.success("Document retrieved successfully", response));
    }

    @PostMapping("/{documentId}/approve")
    public ResponseEntity<ApiResponse<ExpertDocumentResponse>> approveDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long adminId = (principal != null) ? principal.getUserId() : null;
        ExpertDocumentResponse response = adminDocumentService.approveDocument(documentId, adminId);
        return ResponseEntity.ok(ApiResponse.success("Document approved successfully", response));
    }

    @PostMapping("/{documentId}/reject")
    public ResponseEntity<ApiResponse<ExpertDocumentResponse>> rejectDocument(
            @PathVariable Long documentId,
            @Valid @RequestBody RejectDocumentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long adminId = (principal != null) ? principal.getUserId() : null;
        ExpertDocumentResponse response = adminDocumentService.rejectDocument(documentId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success("Document rejected successfully", response));
    }
}
