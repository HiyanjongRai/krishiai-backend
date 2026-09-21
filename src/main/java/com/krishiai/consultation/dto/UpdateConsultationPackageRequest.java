package com.krishiai.consultation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class UpdateConsultationPackageRequest {

    @Size(max = 150, message = "Package name must not exceed 150 characters")
    private String name;

    private String description;

    @DecimalMin(value = "100.00", message = "Minimum price is NPR 100.00")
    private BigDecimal price;

    private Integer durationHours;

    private Boolean active;
}
