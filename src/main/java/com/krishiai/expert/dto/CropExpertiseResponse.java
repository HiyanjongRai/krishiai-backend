package com.krishiai.expert.dto;

import com.krishiai.expert.entity.CropExpertiseType;
import com.krishiai.expert.entity.CropExpertiseVerificationStatus;
import com.krishiai.expert.entity.ExpertCropExpertise;

import java.time.LocalDateTime;

public record CropExpertiseResponse(
        Long id,
        Long cropId,
        String cropName,
        String cropEmoji,
        String categoryName,
        CropExpertiseType expertiseType,
        CropExpertiseVerificationStatus verificationStatus,
        LocalDateTime verifiedAt
) {
    public static CropExpertiseResponse from(ExpertCropExpertise ece) {
        return new CropExpertiseResponse(
                ece.getId(),
                ece.getCrop().getId(),
                ece.getCrop().getName(),
                ece.getCrop().getEmoji(),
                ece.getCrop().getCategory() != null ? ece.getCrop().getCategory().getName() : null,
                ece.getExpertiseType(),
                ece.getVerificationStatus() != null ? ece.getVerificationStatus() : CropExpertiseVerificationStatus.PENDING,
                ece.getVerifiedAt()
        );
    }
}
