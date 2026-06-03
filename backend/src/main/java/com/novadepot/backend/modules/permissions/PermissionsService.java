package com.novadepot.backend.modules.permissions;

import com.novadepot.backend.model.entity.PermissionEntity;
import com.novadepot.backend.repository.PermissionMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PermissionsService {
    private final PermissionMapper permissionMapper;

    public PermissionsService(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public List<Map<String, Object>> list() {
        return permissionMapper.selectActivePermissions()
                .stream()
                .map(this::toMap)
                .toList();
    }

    public Map<String, Object> detail(Long id) {
        PermissionEntity permission = permissionMapper.selectActiveByIds(List.of(id)).stream().findFirst().orElse(null);
        return permission == null ? Map.of() : toMap(permission);
    }

    private Map<String, Object> toMap(PermissionEntity permission) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", permission.getId());
        map.put("permCode", permission.getPermCode());
        map.put("permName", permission.getPermName());
        map.put("resource", permission.getResource());
        map.put("action", permission.getAction());
        map.put("status", permission.getStatus());
        return map;
    }
}
