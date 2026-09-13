package com.krishiai.admin.service;

import com.krishiai.admin.dto.RejectDocumentRequest;
import com.krishiai.common.response.PageResponse;
import com.krishiai.expert.dto.ExpertDocumentResponse;
import com.krishiai.expert.entity.ExpertDocumentStatus;
import org.springframework.data.domain.Pageable;

public interface AdminDocumentService {

    PageResponse<ExpertDocumentResponse> getDocuments(ExpertDocumentStatus status, Pageable pageable);

    ExpertDocumentResponse getDocumentById(Long documentId);

    ExpertDocumentResponse approveDocument(Long documentId, Long adminId);

    ExpertDocumentResponse rejectDocument(Long documentId, RejectDocumentRequest request, Long adminId);
}
