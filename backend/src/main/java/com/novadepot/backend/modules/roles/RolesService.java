package com.novadepot.backend.modules.roles;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.model.entity.PermissionEntity;
import com.novadepot.backend.model.entity.RoleEntity;
import com.novadepot.backend.model.entity.RolePermissionEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.PermissionMapper;
import com.novadepot.backend.repository.RoleMapper;
import com.novadepot.backend.repository.RolePermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RolesService {
    private static final Set<String> BUILT_IN_ROLE_CODES = Set.of(
            "TENANT_ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_OPERATOR", "CS_AGENT", "DATA_VIEWER"
    );

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AuditLogRecordService auditLogRecordService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RolesService(RoleMapper roleMapper,
                        PermissionMapper permissionMapper,
                        RolePermissionMapper rolePermissionMapper,
                        AuditLogRecordService auditLogRecordService) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.auditLogRecordService = auditLogRecordService;
    }

    public List<Map<String, Object>> list() {
        List<RoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getTenantId, RequestContext.tenantId())
                .orderByAsc(RoleEntity::getRoleCode));
        List<Map<String, Object>> result = new ArrayList<>(roles.size());
        for (RoleEntity role : roles) {
            Map<String, Object> map = toMap(role);
            map.put("permissionCount", permissionIds(role.getId()).size());
            result.add(map);
        }
        return result;
    }

    public Map<String, Object> detail(Long id) {
        RoleEntity role = findRole(id);
        Map<String, Object> map = toMap(role);
        List<Long> ids = permissionIds(role.getId());
        map.put("permissionIds", ids);
        map.put("permissions", permissions(ids));
        return map;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(RoleSaveRequest request) {
        String roleCode = cleanCode(request.getRoleCode());
        validateRoleCodeAvailable(roleCode, null);
        RoleEntity role = new RoleEntity();
        role.setTenantId(RequestContext.tenantId());
        role.setRoleCode(roleCode);
        role.setRoleName(cleanText(request.getRoleName()));
        role.setDataScope(cleanDataScope(request.getDataScope()));
        role.setStatus(cleanStatus(request.getStatus()));
        roleMapper.insert(role);
        replacePermissions(role.getId(), request.getPermissionIds());
        Map<String, Object> after = detail(role.getId());
        auditLogRecordService.record("SYSTEM_RBAC", "ROLE_CREATE", "ROLE", String.valueOf(role.getId()),
                role.getRoleCode(), null, toJson(after));
        return Map.of("id", String.valueOf(role.getId()), "roleCode", role.getRoleCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, RoleSaveRequest request) {
        RoleEntity role = findRole(id);
        if (BUILT_IN_ROLE_CODES.contains(role.getRoleCode())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Built-in roles cannot be edited in v1.4");
        }
        Map<String, Object> before = detail(id);
        String roleCode = cleanCode(request.getRoleCode());
        validateRoleCodeAvailable(roleCode, id);
        role.setRoleCode(roleCode);
        role.setRoleName(cleanText(request.getRoleName()));
        role.setDataScope(cleanDataScope(request.getDataScope()));
        role.setStatus(cleanStatus(request.getStatus()));
        roleMapper.updateById(role);
        replacePermissions(role.getId(), request.getPermissionIds());
        Map<String, Object> after = detail(role.getId());
        auditLogRecordService.record("SYSTEM_RBAC", "ROLE_UPDATE", "ROLE", String.valueOf(role.getId()),
                role.getRoleCode(), toJson(before), toJson(after));
        return Map.of("id", String.valueOf(role.getId()), "roleCode", role.getRoleCode());
    }

    private RoleEntity findRole(Long id) {
        RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getTenantId, RequestContext.tenantId())
                .eq(RoleEntity::getId, id));
        if (role == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Role not found");
        }
        return role;
    }

    private Map<String, Object> toMap(RoleEntity role) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", String.valueOf(role.getId()));
        map.put("tenantId", String.valueOf(role.getTenantId()));
        map.put("roleCode", role.getRoleCode());
        map.put("roleName", role.getRoleName());
        map.put("dataScope", role.getDataScope());
        map.put("status", role.getStatus());
        map.put("builtIn", BUILT_IN_ROLE_CODES.contains(role.getRoleCode()));
        return map;
    }

    private List<Long> permissionIds(Long roleId) {
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermissionEntity>()
                        .eq(RolePermissionEntity::getTenantId, RequestContext.tenantId())
                        .eq(RolePermissionEntity::getRoleId, roleId))
                .stream()
                .map(RolePermissionEntity::getPermissionId)
                .toList();
    }

    private List<Map<String, Object>> permissions(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectActiveByIds(ids)
                .stream()
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", item.getId());
                    map.put("permCode", item.getPermCode());
                    map.put("permName", item.getPermName());
                    return map;
                })
                .toList();
    }

    private void replacePermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermissionEntity>()
                .eq(RolePermissionEntity::getTenantId, RequestContext.tenantId())
                .eq(RolePermissionEntity::getRoleId, roleId));
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        List<PermissionEntity> validPermissions = permissionMapper.selectActiveByIds(permissionIds);
        for (PermissionEntity permission : validPermissions) {
            rolePermissionMapper.grant(nextRelationId(), RequestContext.tenantId(), roleId, permission.getId());
        }
    }

    private Long nextRelationId() {
        return IdWorker.getId();
    }

    private void validateRoleCodeAvailable(String roleCode, Long selfId) {
        RoleEntity same = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getTenantId, RequestContext.tenantId())
                .eq(RoleEntity::getRoleCode, roleCode));
        if (same == null || (selfId != null && selfId.equals(same.getId()))) {
            return;
        }
        throw new BizException(ErrorCode.BIZ_ERROR.code(), "roleCode already exists");
    }

    private String cleanCode(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "roleCode cannot be empty");
        }
        return value.trim().toUpperCase();
    }

    private String cleanText(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "roleName cannot be empty");
        }
        return value.trim();
    }

    private String cleanDataScope(String value) {
        String scope = value == null || value.isBlank() ? "ALL" : value.trim().toUpperCase();
        return Set.of("ALL", "WAREHOUSE", "SELF").contains(scope) ? scope : "ALL";
    }

    private String cleanStatus(String value) {
        String status = value == null || value.isBlank() ? "ACTIVE" : value.trim().toUpperCase();
        return Set.of("ACTIVE", "DISABLED").contains(status) ? status : "ACTIVE";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
