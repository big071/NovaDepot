package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("import_error_reports")
public class ImportErrorReportEntity extends BaseEntity {
    private String module;
    private String reportId;
    private String content;
}
