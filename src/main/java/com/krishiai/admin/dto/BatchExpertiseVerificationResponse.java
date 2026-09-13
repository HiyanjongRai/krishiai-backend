package com.krishiai.admin.dto;

import java.util.List;

public record BatchExpertiseVerificationResponse(
        int totalProcessed,
        int verifiedCount,
        int rejectedCount,
        int requestedInfoCount,
        List<Long> verifiedIds,
        List<Long> rejectedIds,
        List<Long> requestedInfoIds
) {}
