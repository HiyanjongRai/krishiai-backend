package com.krishiai.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationRequestDto {

    @NotNull(message = "Expert ID is required")
    @Positive(message = "Expert ID must be positive")
    private Long expertId;

    @Positive(message = "Crop ID must be positive")
    private Long cropId;

    @Positive(message = "Package ID must be positive")
    private Long packageId;

    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject cannot exceed 255 characters")
    private String subject;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;
}

