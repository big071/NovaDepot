package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.RolePermissionEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermissionEntity> {
    @Insert("""
            INSERT INTO role_permissions (id, tenant_id, role_id, permission_id, created_at, updated_at, deleted)
            VALUES (#{id}, #{tenantId}, #{roleId}, #{permissionId}, NOW(3), NOW(3), 0)
            ON DUPLICATE KEY UPDATE deleted = 0, updated_at = NOW(3)
            """)
    int grant(@Param("id") Long id,
              @Param("tenantId") Long tenantId,
              @Param("roleId") Long roleId,
              @Param("permissionId") Long permissionId);
}
