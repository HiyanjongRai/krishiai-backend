package com.krishiai.consultation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CreateConsultationPackageRequest {

    private Long cropId;

    @NotBlank(message = "Package name is required")
    @Size(max = 150, message = "Package name must not exceed 150 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "100.00", message = "Minimum price is NPR 100.00")
    private BigDecimal price;

    @NotNull(message = "Duration in hours is required")
    private Integer durationHours;
}
