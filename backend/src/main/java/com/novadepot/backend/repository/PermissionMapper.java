package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.PermissionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<PermissionEntity> {
    @Select("""
            SELECT id, perm_code, perm_name, resource, action, status, created_at, created_by, updated_at, updated_by, deleted
            FROM permissions
            WHERE deleted = 0
              AND status = 'ACTIVE'
            ORDER BY perm_code ASC
            """)
    List<PermissionEntity> selectActivePermissions();

    @Select("""
            <script>
            SELECT id, perm_code, perm_name, resource, action, status, created_at, created_by, updated_at, updated_by, deleted
            FROM permissions
            WHERE deleted = 0
              AND status = 'ACTIVE'
              AND id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            ORDER BY perm_code ASC
            </script>
            """)
    List<PermissionEntity> selectActiveByIds(@Param("ids") List<Long> ids);
}
