package com.novadepot.backend.modules.products;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.ProductEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
public class ProductsController {
    private final ProductsService service;

    public ProductsController(ProductsService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("PRODUCT_READ")
    public ApiResponse<List<ProductEntity>> list() {
        return ApiResponse.success(service.list(), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    @RequirePermission("PRODUCT_READ")
    public ApiResponse<ProductEntity> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("PRODUCT_CREATE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody ProductCreateRequest request) {
        return ApiResponse.success(service.create(request), MDC.get("traceId"));
    }
}
