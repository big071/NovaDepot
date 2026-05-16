package com.novadepot.backend.modules.ai.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiToolResult {
    private String toolName;
    private String displayName;
    private String argumentsSummary;
    private boolean success = true;
    private String permissionResult = "ALLOWED";
    private boolean empty;
    private String summary;
    private String message;
    private List<Map<String, Object>> sources = List.of();
    private List<Map<String, Object>> rows = List.of();
    private int durationMs;
    private String errorCode;
    private String errorMessage;

    public static AiToolResult denied(AiToolDefinition definition, String args) {
        AiToolResult result = new AiToolResult();
        result.toolName = definition.name();
        result.displayName = definition.displayName();
        result.argumentsSummary = args;
        result.success = false;
        result.permissionResult = "DENIED";
        result.empty = true;
        result.summary = "当前账号无权限查询该类数据";
        result.message = result.summary;
        result.errorCode = "AI_TOOL_FORBIDDEN";
        return result;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("toolName", toolName);
        map.put("displayName", displayName);
        map.put("argumentsSummary", argumentsSummary);
        map.put("success", success);
        map.put("permissionResult", permissionResult);
        map.put("empty", empty);
        map.put("summary", summary);
        map.put("message", message);
        map.put("sources", sources);
        map.put("rows", rows);
        map.put("durationMs", durationMs);
        map.put("errorCode", errorCode);
        map.put("errorMessage", errorMessage);
        return map;
    }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getArgumentsSummary() { return argumentsSummary; }
    public void setArgumentsSummary(String argumentsSummary) { this.argumentsSummary = argumentsSummary; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getPermissionResult() { return permissionResult; }
    public void setPermissionResult(String permissionResult) { this.permissionResult = permissionResult; }
    public boolean isEmpty() { return empty; }
    public void setEmpty(boolean empty) { this.empty = empty; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<Map<String, Object>> getSources() { return sources; }
    public void setSources(List<Map<String, Object>> sources) { this.sources = sources; }
    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
    public int getDurationMs() { return durationMs; }
    public void setDurationMs(int durationMs) { this.durationMs = durationMs; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
