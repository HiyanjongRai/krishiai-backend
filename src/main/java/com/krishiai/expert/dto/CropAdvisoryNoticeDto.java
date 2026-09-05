package com.krishiai.expert.dto;

public record CropAdvisoryNoticeDto(
        Long id,
        String cropName,
        String cropEmoji,
        String alertTitle,
        String severity, // "ALERT", "WARNING", "INFO"
        String description,
        String recommendedAction,
        String issuedDate
) {}
