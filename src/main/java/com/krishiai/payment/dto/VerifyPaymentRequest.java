package com.krishiai.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VerifyPaymentRequest {

    /** Base64-encoded response data from eSewa redirect. */
    @NotBlank(message = "Payment data payload is required")
    private String data;
}
