package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("partners")
public class PartnerEntity extends BaseEntity {
    private String partnerCode;
    private String partnerName;
    private String partnerType;
    private String contactName;
    private String phone;
    private String address;
    private String status;
    private String remark;
}
