package com.novadepot.backend.modules.warehouses;

import jakarta.validation.constraints.NotBlank;

public class WarehouseCreateRequest {
    @NotBlank(message = "warehouseCode 不能为空")
    private String warehouseCode;
    @NotBlank(message = "warehouseName 不能为空")
    private String warehouseName;
    private String warehouseType;
    private String address;

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getWarehouseType() { return warehouseType; }
    public void setWarehouseType(String warehouseType) { this.warehouseType = warehouseType; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
