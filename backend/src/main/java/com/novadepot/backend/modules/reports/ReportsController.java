package com.novadepot.backend.modules.reports;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportsController {
    private final ReportsService service;

    public ReportsController(ReportsService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    @RequirePermission("REPORT_DASHBOARD_READ")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.success(service.dashboard(), MDC.get("traceId"));
    }

    @GetMapping("/dashboard/todos")
    public ApiResponse<Map<String, Object>> dashboardTodos() {
        return ApiResponse.success(service.workbenchTodos(), MDC.get("traceId"));
    }

    @GetMapping("/inventory-turnover")
    @RequirePermission("REPORT_CENTER_READ")
    public ApiResponse<Map<String, Object>> inventoryTurnover(@RequestParam(required = false) String dateFrom,
                                                              @RequestParam(required = false) String dateTo,
                                                              @RequestParam(required = false) Long warehouseId) {
        return ApiResponse.success(service.inventoryTurnover(dateFrom, dateTo, warehouseId), MDC.get("traceId"));
    }

    @GetMapping(value = "/inventory-turnover/export", produces = "text/csv;charset=UTF-8")
    @RequirePermission("REPORT_EXPORT")
    public ResponseEntity<String> inventoryTurnoverExport(@RequestParam(required = false) String dateFrom,
                                                          @RequestParam(required = false) String dateTo,
                                                          @RequestParam(required = false) Long warehouseId) {
        return csv("inventory-turnover.csv", service.inventoryTurnoverCsv(dateFrom, dateTo, warehouseId));
    }

    @GetMapping("/inout-summary")
    @RequirePermission("REPORT_CENTER_READ")
    public ApiResponse<Map<String, Object>> inoutSummary(@RequestParam(required = false) String dateFrom,
                                                         @RequestParam(required = false) String dateTo,
                                                         @RequestParam(required = false, defaultValue = "DAY") String grain) {
        return ApiResponse.success(service.inoutSummary(dateFrom, dateTo, grain), MDC.get("traceId"));
    }

    @GetMapping(value = "/inout-summary/export", produces = "text/csv;charset=UTF-8")
    @RequirePermission("REPORT_EXPORT")
    public ResponseEntity<String> inoutSummaryExport(@RequestParam(required = false) String dateFrom,
                                                     @RequestParam(required = false) String dateTo,
                                                     @RequestParam(required = false, defaultValue = "DAY") String grain) {
        return csv("inout-summary.csv", service.inoutSummaryCsv(dateFrom, dateTo, grain));
    }

    @GetMapping("/purchase-sales-summary")
    @RequirePermission("REPORT_CENTER_READ")
    public ApiResponse<Map<String, Object>> purchaseSalesSummary(@RequestParam(required = false) String dateFrom,
                                                                 @RequestParam(required = false) String dateTo,
                                                                 @RequestParam(required = false) Long partnerId) {
        return ApiResponse.success(service.purchaseSalesSummary(dateFrom, dateTo, partnerId), MDC.get("traceId"));
    }

    @GetMapping(value = "/purchase-sales-summary/export", produces = "text/csv;charset=UTF-8")
    @RequirePermission("REPORT_EXPORT")
    public ResponseEntity<String> purchaseSalesSummaryExport(@RequestParam(required = false) String dateFrom,
                                                             @RequestParam(required = false) String dateTo,
                                                             @RequestParam(required = false) Long partnerId) {
        return csv("purchase-sales-summary.csv", service.purchaseSalesSummaryCsv(dateFrom, dateTo, partnerId));
    }

    @GetMapping("/ticket-efficiency")
    @RequirePermission("REPORT_CENTER_READ")
    public ApiResponse<Map<String, Object>> ticketEfficiency(@RequestParam(required = false) String dateFrom,
                                                             @RequestParam(required = false) String dateTo,
                                                             @RequestParam(required = false) Long assigneeId) {
        return ApiResponse.success(service.ticketEfficiency(dateFrom, dateTo, assigneeId), MDC.get("traceId"));
    }

    @GetMapping(value = "/ticket-efficiency/export", produces = "text/csv;charset=UTF-8")
    @RequirePermission("REPORT_EXPORT")
    public ResponseEntity<String> ticketEfficiencyExport(@RequestParam(required = false) String dateFrom,
                                                         @RequestParam(required = false) String dateTo,
                                                         @RequestParam(required = false) Long assigneeId) {
        return csv("ticket-efficiency.csv", service.ticketEfficiencyCsv(dateFrom, dateTo, assigneeId));
    }

    private ResponseEntity<String> csv(String filename, String body) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=" + filename)
                .body(body);
    }
}
