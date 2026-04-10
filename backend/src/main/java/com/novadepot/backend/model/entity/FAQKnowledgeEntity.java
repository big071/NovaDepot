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
@TableName("faq_knowledge")
public class FAQKnowledgeEntity extends BaseEntity {
    private String faqCode;
    private String question;
    private String answer;
    private String tags;
    private String scene;
    private Integer priority;
    private Integer enabled;
    private Integer versionNo;
}
