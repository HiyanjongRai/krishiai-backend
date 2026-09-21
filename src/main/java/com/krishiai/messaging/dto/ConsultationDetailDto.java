package com.krishiai.messaging.dto;

import com.krishiai.consultation.entity.Consultation;
import com.krishiai.consultation.entity.ConsultationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationDetailDto {
    private Long id;
    private UserPublicSummaryDto farmer;
    private UserPublicSummaryDto expert;
    private String cropName;
    private Long cropId;
    private ConsultationStatus status;
    private String subject;
    private String description;
    private Long conversationId;
    private Long packageId;
    private java.math.BigDecimal priceAtPurchase;
    private String currency;
    private Integer durationHours;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime paymentVerifiedAt;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    public static ConsultationDetailDto fromEntity(Consultation c, Long conversationId,
                                                    boolean farmerOnline, boolean expertOnline) {
        return ConsultationDetailDto.builder()
                .id(c.getId())
                .farmer(UserPublicSummaryDto.fromUser(c.getFarmer(), farmerOnline))
                .expert(c.getExpert() != null ? UserPublicSummaryDto.fromUser(c.getExpert(), expertOnline) : null)
                .cropName(c.getCrop() != null ? c.getCrop().getName() : null)
                .cropId(c.getCrop() != null ? c.getCrop().getId() : null)
                .packageId(c.getPackageEntity() != null ? c.getPackageEntity().getId() : null)
                .priceAtPurchase(c.getPriceAtPurchase())
                .currency(c.getCurrency())
                .durationHours(c.getPackageEntity() != null ? c.getPackageEntity().getDurationHours() : null)
                .status(c.getStatus())
                .subject(c.getDisplaySubject())
                .description(c.getDescription())
                .conversationId(conversationId)
                .createdAt(c.getCreatedAt())
                .acceptedAt(c.getAcceptedAt())
                .paymentVerifiedAt(c.getPaymentVerifiedAt())
                .startedAt(c.getStartedAt())
                .expiresAt(c.getExpiresAt())
                .completedAt(c.getCompletedAt())
                .cancelledAt(c.getCancelledAt())
                .build();
    }
}

