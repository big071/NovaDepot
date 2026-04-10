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
@TableName("customer_service_rules")
public class CustomerServiceRuleEntity extends BaseEntity {
    private String ruleCode;
    private String ruleName;
    private String triggerType;
    private String triggerExpr;
    private String actionType;
    private String actionConfig;
    private Integer enabled;
}
