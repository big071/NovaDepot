package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("rule_configs")
public class RuleConfigEntity extends BaseEntity {
    private String configKey;
    private String configName;
    private String configValue;
    private String valueType;
    private String scene;
    private String remark;
    private Integer enabled;
}
