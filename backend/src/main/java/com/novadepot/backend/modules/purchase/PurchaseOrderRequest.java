package com.novadepot.backend.modules.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class PurchaseOrderRequest {
    @NotNull
    private Long partnerId;
    @NotNull
    private Long warehouseId;
    private LocalDate expectedArrivalDate;
    private String remark;
    @Valid
    @NotEmpty
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        @NotNull
        private Long productId;
        @NotNull
        @DecimalMin(value = "0.000001")
        private BigDecimal orderQty;
        @NotNull
        @DecimalMin(value = "0.00")
        private BigDecimal unitPrice;
    }
}
