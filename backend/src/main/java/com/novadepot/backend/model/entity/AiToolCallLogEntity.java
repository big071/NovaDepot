package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("ai_tool_call_logs")
public class AiToolCallLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long conversationId;
    private Long messageId;
    private String requestId;
    private String toolName;
    private String argumentsSummary;
    private Integer success;
    private String permissionResult;
    private Integer durationMs;
    private Integer resultCount;
    private String errorCode;
    private String errorMessage;
    private Long createdBy;
    private LocalDateTime createdAt;
}
