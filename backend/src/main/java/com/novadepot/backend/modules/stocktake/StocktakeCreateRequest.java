package com.novadepot.backend.modules.stocktake;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StocktakeCreateRequest {
    @NotNull
    private Long warehouseId;

    private String remark;
}
