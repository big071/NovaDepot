package com.novadepot.backend.modules.roles;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RoleSaveRequest {
    @NotBlank
    private String roleCode;
    @NotBlank
    private String roleName;
    private String dataScope = "ALL";
    private String status = "ACTIVE";
    private List<Long> permissionIds = new ArrayList<>();
}

