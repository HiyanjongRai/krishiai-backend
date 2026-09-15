package com.krishiai.expert.dto;

import com.krishiai.expert.entity.CropExpertiseVerificationStatus;
import com.krishiai.expert.entity.CropExpertiseType;
import com.krishiai.expert.entity.ExpertCropExpertise;
import com.krishiai.expert.entity.ExpertiseLevel;

public record PublicExpertiseResponse(
        Long cropId,
        String cropName,
        String cropEmoji,
        String categoryName,
        CropExpertiseType expertiseType,
        CropExpertiseVerificationStatus verificationStatus,
        String expertiseArea,
        ExpertiseLevel expertiseLevel,
        Integer yearsOfExperience
) {
    public static PublicExpertiseResponse from(ExpertCropExpertise ece) {
        return new PublicExpertiseResponse(
                ece.getCrop() != null ? ece.getCrop().getId() : null,
                ece.getCrop() != null ? ece.getCrop().getName() : ece.getExpertiseArea(),
                ece.getCrop() != null ? ece.getCrop().getEmoji() : null,
                ece.getCrop() != null && ece.getCrop().getCategory() != null
                        ? ece.getCrop().getCategory().getName()
                        : null,
                ece.getExpertiseType(),
                ece.getVerificationStatus() != null
                        ? ece.getVerificationStatus()
                        : CropExpertiseVerificationStatus.SELF_DECLARED,
                ece.getExpertiseArea(),
                ece.getExpertiseLevel(),
                ece.getYearsOfExperience()
        );
    }
}
