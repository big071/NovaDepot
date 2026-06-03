package com.novadepot.backend.modules.roles;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/roles")
public class RolesController {
    private final RolesService service;

    public RolesController(RolesService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("ROLE_READ")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(service.list(), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    @RequirePermission("ROLE_READ")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("ROLE_MANAGE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody RoleSaveRequest request) {
        return ApiResponse.success(service.create(request), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    @RequirePermission("ROLE_MANAGE")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id,
                                                   @Valid @RequestBody RoleSaveRequest request) {
        return ApiResponse.success(service.update(id, request), MDC.get("traceId"));
    }
}
