package com.krishiai.expert.dto;

import java.time.LocalDateTime;

public record FarmerInquiryDto(
        Long id,
        Long farmerId,
        String farmerName,
        String farmerPhone,
        String farmerLocation,
        String farmerAvatar,
        String cropName,
        String cropEmoji,
        String category,
        String issueTitle,
        String issueDescription,
        String severity, // "CRITICAL", "HIGH", "MEDIUM", "LOW"
        String status,   // "PENDING_REVIEW", "IN_PROGRESS", "RESOLVED"
        LocalDateTime submittedAt,
        String aiDiagnosisHint,
        String expertNotes
) {}
