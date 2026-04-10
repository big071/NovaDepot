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
@TableName("file_assets")
public class FileAssetEntity extends BaseEntity {
    private String bizType;
    private String bizNo;
    private String fileName;
    private String fileExt;
    private String mimeType;
    private Long fileSize;
    private String storageProvider;
    private String bucket;
    private String objectKey;
    private String url;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;
}
