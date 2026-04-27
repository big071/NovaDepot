package com.novadepot.backend.modules.agent;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent")
@Validated
public class AgentCenterController {
    private final AgentCenterService service;

    public AgentCenterController(AgentCenterService service) {
        this.service = service;
    }

    @GetMapping("/tasks")
    @RequirePermission("AGENT_TASK_READ")
    public ApiResponse<List<Map<String, Object>>> tasks() {
        return ApiResponse.success(service.listTasks(), MDC.get("traceId"));
    }

    @PostMapping("/tasks/{taskCode}/execute")
    @RequirePermission("AGENT_TASK_EXECUTE")
    public ApiResponse<Map<String, Object>> execute(@PathVariable String taskCode,
                                                    @Valid @RequestBody AgentTaskExecuteRequest request) {
        return ApiResponse.success(service.executeTask(taskCode, request.getTarget()), MDC.get("traceId"));
    }

    @GetMapping("/runs")
    @RequirePermission("AGENT_TASK_READ")
    public ApiResponse<Map<String, Object>> runs(@RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "20") Integer pageSize,
                                                 @RequestParam(required = false) String taskCode,
                                                 @RequestParam(required = false) String status) {
        return ApiResponse.success(service.listRuns(pageNo, pageSize, taskCode, status), MDC.get("traceId"));
    }

    @GetMapping("/runs/{id}")
    @RequirePermission("AGENT_TASK_READ")
    public ApiResponse<Map<String, Object>> runDetail(@PathVariable Long id) {
        return ApiResponse.success(service.runDetail(id), MDC.get("traceId"));
    }
}
