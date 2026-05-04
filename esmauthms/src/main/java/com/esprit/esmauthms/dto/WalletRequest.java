package com.esprit.esmauthms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WalletRequest {
    @NotNull
    @DecimalMin("0.01")
    private Double amount;
}
