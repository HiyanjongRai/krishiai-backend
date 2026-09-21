package com.krishiai.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CreateWithdrawalRequestDto {

    @NotNull(message = "Withdrawal amount is required")
    @DecimalMin(value = "100.00", message = "Minimum withdrawal amount is NPR 100")
    private BigDecimal amount;

    @NotBlank(message = "Bank or digital wallet (eSewa/Khalti) account details are required")
    private String accountDetails;
}
