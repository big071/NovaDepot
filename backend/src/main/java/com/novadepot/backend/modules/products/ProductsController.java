package com.novadepot.backend.modules.products;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.ProductEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/code/{productCode}")
    @RequirePermission("PRODUCT_READ")
    public ApiResponse<ProductEntity> detailByCode(@PathVariable String productCode) {
        return ApiResponse.success(service.detailByCode(productCode), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("PRODUCT_CREATE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody ProductCreateRequest request) {
        return ApiResponse.success(service.create(request), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    @RequirePermission("PRODUCT_UPDATE")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id,
                                                   @Valid @RequestBody ProductCreateRequest request) {
        return ApiResponse.success(service.update(id, request), MDC.get("traceId"));
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    @RequirePermission("PRODUCT_EXPORT")
    public ResponseEntity<String> exportCsv() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=products.csv")
                .body(service.exportCsv());
    }

    @GetMapping(value = "/import/template", produces = "text/csv;charset=UTF-8")
    @RequirePermission("PRODUCT_TEMPLATE_EXPORT")
    public ResponseEntity<String> exportImportTemplate() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=products-import-template.csv")
                .body(service.importTemplateCsv());
    }

    @PostMapping(value = "/import", consumes = {"text/csv", "text/plain"})
    @RequirePermission("PRODUCT_IMPORT")
    public ApiResponse<Map<String, Object>> importCsv(@RequestBody String csvContent) {
        return ApiResponse.success(service.importCsv(csvContent), MDC.get("traceId"));
    }

    @GetMapping(value = "/import/errors/{reportId}", produces = "text/csv;charset=UTF-8")
    @RequirePermission("IMPORT_ERROR_REPORT_READ")
    public ResponseEntity<String> importErrors(@PathVariable String reportId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=product-import-errors-" + reportId + ".csv")
                .body(service.importErrorReport(reportId));
    }
}
