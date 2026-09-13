package com.krishiai.admin.service;

import com.krishiai.admin.dto.RejectDocumentRequest;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.common.response.PageResponse;
import com.krishiai.expert.dto.ExpertDocumentResponse;
import com.krishiai.expert.entity.ExpertDocument;
import com.krishiai.expert.entity.ExpertDocumentStatus;
import com.krishiai.expert.repository.ExpertDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDocumentServiceImpl implements AdminDocumentService {

    private final ExpertDocumentRepository documentRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ExpertDocumentResponse> getDocuments(ExpertDocumentStatus status, Pageable pageable) {
        Page<ExpertDocument> page = (status != null)
                ? documentRepository.findByStatus(status, pageable)
                : documentRepository.findAll(pageable);
        return PageResponse.from(page.map(ExpertDocumentResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public ExpertDocumentResponse getDocumentById(Long documentId) {
        ExpertDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Expert document not found with id: " + documentId));
        return ExpertDocumentResponse.from(doc);
    }

    @Override
    @Transactional
    public ExpertDocumentResponse approveDocument(Long documentId, Long adminId) {
        log.info("Admin {} approving expert document id={}", adminId, documentId);
        ExpertDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Expert document not found with id: " + documentId));

        doc.setStatus(ExpertDocumentStatus.APPROVED);
        doc.setRejectionReason(null);
        doc.setVerifiedAt(LocalDateTime.now());
        doc.setReviewedByAdminId(adminId);

        ExpertDocument saved = documentRepository.save(doc);
        log.info("Successfully approved expert document id={}", documentId);
        return ExpertDocumentResponse.from(saved);
    }

    @Override
    @Transactional
    public ExpertDocumentResponse rejectDocument(Long documentId, RejectDocumentRequest request, Long adminId) {
        log.info("Admin {} rejecting expert document id={}, reason={}", adminId, documentId, request.reason());
        ExpertDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Expert document not found with id: " + documentId));

        doc.setStatus(ExpertDocumentStatus.REJECTED);
        doc.setRejectionReason(request.reason().strip());
        doc.setVerifiedAt(LocalDateTime.now());
        doc.setReviewedByAdminId(adminId);

        ExpertDocument saved = documentRepository.save(doc);
        log.info("Successfully rejected expert document id={}", documentId);
        return ExpertDocumentResponse.from(saved);
    }
}
