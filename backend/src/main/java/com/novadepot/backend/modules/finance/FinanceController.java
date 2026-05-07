package com.novadepot.backend.modules.finance;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.PayableEntity;
import com.novadepot.backend.model.entity.ReceivableEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {
    private final FinanceService service;

    public FinanceController(FinanceService service) {
        this.service = service;
    }

    @GetMapping("/payables")
    @RequirePermission("FINANCE_PAYABLE_READ")
    public ApiResponse<List<PayableEntity>> listPayables(@RequestParam(required = false) String status) {
        return ApiResponse.success(service.listPayables(status), MDC.get("traceId"));
    }

    @GetMapping("/receivables")
    @RequirePermission("FINANCE_RECEIVABLE_READ")
    public ApiResponse<List<ReceivableEntity>> listReceivables(@RequestParam(required = false) String status) {
        return ApiResponse.success(service.listReceivables(status), MDC.get("traceId"));
    }

    @PostMapping("/payables/{id}/payments")
    @RequirePermission("FINANCE_PAYMENT_REGISTER")
    public ApiResponse<Map<String, Object>> registerPayment(@PathVariable Long id,
                                                            @Valid @RequestBody FinanceRegistrationRequest request) {
        return ApiResponse.success(service.registerPayment(id, request), MDC.get("traceId"));
    }

    @PostMapping("/receivables/{id}/receipts")
    @RequirePermission("FINANCE_RECEIPT_REGISTER")
    public ApiResponse<Map<String, Object>> registerReceipt(@PathVariable Long id,
                                                            @Valid @RequestBody FinanceRegistrationRequest request) {
        return ApiResponse.success(service.registerReceipt(id, request), MDC.get("traceId"));
    }
}
