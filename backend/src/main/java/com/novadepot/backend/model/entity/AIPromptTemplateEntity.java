package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("ai_prompt_templates")
public class AIPromptTemplateEntity extends BaseEntity {
    private String templateCode;
    private String templateName;
    private String scene;
    private String templateContent;
    private Integer versionNo;
    private Integer enabled;
    private String remark;
}
