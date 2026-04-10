package com.novadepot.backend.modules.products;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProductCreateRequest {
    @NotBlank(message = "productCode 不能为空")
    private String productCode;
    @NotBlank(message = "productName 不能为空")
    private String productName;
    @NotNull(message = "categoryId 不能为空")
    private Long categoryId;
    @NotNull(message = "unitId 不能为空")
    private Long unitId;
    private String barcode;

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
}
