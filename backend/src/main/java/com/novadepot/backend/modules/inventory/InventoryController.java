package com.novadepot.backend.modules.inventory;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("INVENTORY_READ")
    public ApiResponse<List<InventoryEntity>> list() {
        return ApiResponse.success(service.list(), MDC.get("traceId"));
    }

    @GetMapping("/transactions")
    @RequirePermission("INVENTORY_TXN_READ")
    public ApiResponse<List<InventoryTransactionEntity>> transactions() {
        return ApiResponse.success(service.transactions(), MDC.get("traceId"));
    }

    @GetMapping("/alerts/low-stock")
    @RequirePermission("INVENTORY_ALERT_READ")
    public ApiResponse<List<InventoryEntity>> lowStockAlerts() {
        return ApiResponse.success(service.lowStockAlerts(), MDC.get("traceId"));
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    @RequirePermission("INVENTORY_EXPORT")
    public ResponseEntity<StreamingResponseBody> exportCsv() {
        StreamingResponseBody body = outputStream -> service.writeExportCsv(outputStream);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=inventory.csv")
                .body(body);
    }

    @GetMapping(value = "/import/template", produces = "text/csv;charset=UTF-8")
    @RequirePermission("INVENTORY_TEMPLATE_EXPORT")
    public ResponseEntity<String> exportImportTemplate() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=inventory-import-template.csv")
                .body(service.importTemplateCsv());
    }

    @GetMapping("/export/fields")
    @RequirePermission("INVENTORY_EXPORT")
    public ApiResponse<List<String>> exportFields() {
        return ApiResponse.success(service.exportFieldDescriptions(), MDC.get("traceId"));
    }

    @PostMapping(value = "/import", consumes = {"text/csv", "text/plain"})
    @RequirePermission("INVENTORY_IMPORT")
    public ApiResponse<java.util.Map<String, Object>> importCsv(@RequestBody String csvContent) {
        return ApiResponse.success(service.importCsv(csvContent), MDC.get("traceId"));
    }

    @GetMapping(value = "/import/errors/{reportId}", produces = "text/csv;charset=UTF-8")
    @RequirePermission("IMPORT_ERROR_REPORT_READ")
    public ResponseEntity<String> importErrors(@org.springframework.web.bind.annotation.PathVariable String reportId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=inventory-import-errors-" + reportId + ".csv")
                .body(service.importErrorReport(reportId));
    }
}
