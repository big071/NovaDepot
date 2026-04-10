package com.novadepot.backend.modules.outboundorders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class OutboundCreateRequest {
    @NotNull(message = "warehouseId 不能为空")
    private Long warehouseId;
    private Long customerId;
    @Valid
    @NotNull(message = "items 不能为空")
    private List<Item> items;

    public static class Item {
        @NotNull(message = "productId 不能为空")
        private Long productId;
        @NotNull(message = "locationId 不能为空")
        private Long locationId;
        @DecimalMin(value = "0.000001", message = "qty 必须大于 0")
        private BigDecimal qty;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Long getLocationId() { return locationId; }
        public void setLocationId(Long locationId) { this.locationId = locationId; }
        public BigDecimal getQty() { return qty; }
        public void setQty(BigDecimal qty) { this.qty = qty; }
    }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}
