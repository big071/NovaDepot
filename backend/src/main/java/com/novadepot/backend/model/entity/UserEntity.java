package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("users")
public class UserEntity extends BaseEntity {
    private String username;
    private String passwordHash;
    private String realName;
    private String phone;
    private String email;
    private String status;
}
