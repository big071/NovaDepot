package com.novadepot.backend.modules.stocktake;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StocktakeCountRequest {
    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal countedQty;
}
