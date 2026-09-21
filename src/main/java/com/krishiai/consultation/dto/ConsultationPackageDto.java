package com.krishiai.consultation.dto;

import com.krishiai.consultation.entity.ConsultationPackage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationPackageDto {
    private Long id;
    private Long expertId;
    private String expertName;
    private Long cropId;
    private String cropName;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private Integer durationHours;
    private Boolean active;
    private LocalDateTime createdAt;

    public static ConsultationPackageDto fromEntity(ConsultationPackage pkg) {
        return ConsultationPackageDto.builder()
                .id(pkg.getId())
                .expertId(pkg.getExpert().getId())
                .expertName(pkg.getExpert().getFullName())
                .cropId(pkg.getCrop() != null ? pkg.getCrop().getId() : null)
                .cropName(pkg.getCrop() != null ? pkg.getCrop().getName() : null)
                .name(pkg.getName())
                .description(pkg.getDescription())
                .price(pkg.getPrice())
                .currency(pkg.getCurrency())
                .durationHours(pkg.getDurationHours())
                .active(pkg.getActive())
                .createdAt(pkg.getCreatedAt())
                .build();
    }
}
