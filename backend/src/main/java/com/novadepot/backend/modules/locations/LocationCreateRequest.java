package com.novadepot.backend.modules.locations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class LocationCreateRequest {
    @NotNull(message = "warehouseId 不能为空")
    private Long warehouseId;
    @NotBlank(message = "locationCode 不能为空")
    private String locationCode;
    @NotBlank(message = "locationName 不能为空")
    private String locationName;
    private String locationType;
    private BigDecimal capacityQty;

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }
    public BigDecimal getCapacityQty() { return capacityQty; }
    public void setCapacityQty(BigDecimal capacityQty) { this.capacityQty = capacityQty; }
}
