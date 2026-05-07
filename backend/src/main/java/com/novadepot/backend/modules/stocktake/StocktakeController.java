package com.novadepot.backend.modules.stocktake;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.StocktakeOrderEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stocktakes")
public class StocktakeController {
    private final StocktakeService service;

    public StocktakeController(StocktakeService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("STOCKTAKE_READ")
    public ApiResponse<List<StocktakeOrderEntity>> list(@RequestParam(required = false) String status) {
        return ApiResponse.success(service.list(status), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    @RequirePermission("STOCKTAKE_READ")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("STOCKTAKE_CREATE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody StocktakeCreateRequest request) {
        return ApiResponse.success(service.create(request), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/start")
    @RequirePermission("STOCKTAKE_COUNT")
    public ApiResponse<Map<String, Object>> start(@PathVariable Long id) {
        return ApiResponse.success(service.start(id), MDC.get("traceId"));
    }

    @PutMapping("/{id}/items/{itemId}/count")
    @RequirePermission("STOCKTAKE_COUNT")
    public ApiResponse<Map<String, Object>> updateCount(@PathVariable Long id,
                                                        @PathVariable Long itemId,
                                                        @Valid @RequestBody StocktakeCountRequest request) {
        return ApiResponse.success(service.updateCount(id, itemId, request), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/submit-review")
    @RequirePermission("STOCKTAKE_COUNT")
    public ApiResponse<Map<String, Object>> submitReview(@PathVariable Long id) {
        return ApiResponse.success(service.submitReview(id), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/confirm")
    @RequirePermission("STOCKTAKE_CONFIRM")
    public ApiResponse<Map<String, Object>> confirm(@PathVariable Long id) {
        return ApiResponse.success(service.confirm(id), MDC.get("traceId"));
    }
}
