package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sop_knowledge")
public class SopKnowledgeEntity extends BaseEntity {
    private String sopCode;
    private String title;
    private String scene;
    private String steps;
    private String risks;
    private String reviewChecks;
    private String tags;
    private Integer priority;
    private Integer enabled;
    private String reviewStatus;
    private String sourceType;
    private String sourceRefId;
}
