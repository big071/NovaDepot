package com.novadepot.backend.modules.partners;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.PartnerEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/partners")
public class PartnersController {
    private final PartnersService service;

    public PartnersController(PartnersService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("PARTNER_READ")
    public ApiResponse<List<PartnerEntity>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String partnerType) {
        return ApiResponse.success(service.list(keyword, partnerType), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    @RequirePermission("PARTNER_READ")
    public ApiResponse<PartnerEntity> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("PARTNER_CREATE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody PartnerRequest request) {
        return ApiResponse.success(service.create(request), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    @RequirePermission("PARTNER_UPDATE")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id,
                                                   @Valid @RequestBody PartnerRequest request) {
        return ApiResponse.success(service.update(id, request), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/disable")
    @RequirePermission("PARTNER_UPDATE")
    public ApiResponse<Map<String, Object>> disable(@PathVariable Long id) {
        return ApiResponse.success(service.setStatus(id, "DISABLED"), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/enable")
    @RequirePermission("PARTNER_UPDATE")
    public ApiResponse<Map<String, Object>> enable(@PathVariable Long id) {
        return ApiResponse.success(service.setStatus(id, "ACTIVE"), MDC.get("traceId"));
    }

    @GetMapping(value = "/import/template", produces = "text/csv;charset=UTF-8")
    @RequirePermission("PARTNER_TEMPLATE_EXPORT")
    public ResponseEntity<String> exportImportTemplate() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=partners-import-template.csv")
                .body(service.importTemplateCsv());
    }

    @PostMapping(value = "/import", consumes = {"text/csv", "text/plain"})
    @RequirePermission("PARTNER_IMPORT")
    public ApiResponse<Map<String, Object>> importCsv(@RequestBody String csvContent) {
        return ApiResponse.success(service.importCsv(csvContent), MDC.get("traceId"));
    }

    @GetMapping(value = "/import/errors/{reportId}", produces = "text/csv;charset=UTF-8")
    @RequirePermission("IMPORT_ERROR_REPORT_READ")
    public ResponseEntity<String> importErrors(@PathVariable String reportId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=partner-import-errors-" + reportId + ".csv")
                .body(service.importErrorReport(reportId));
    }
}
