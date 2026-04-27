package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("agent_task_runs")
public class AgentTaskRunEntity extends BaseEntity {
    private String taskCode;
    private String taskName;
    private String status;
    private String targetJson;
    private String stepsJson;
    private String resultJson;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
