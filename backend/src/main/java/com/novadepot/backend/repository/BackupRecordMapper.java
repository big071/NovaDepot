package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.BackupRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BackupRecordMapper extends BaseMapper<BackupRecordEntity> {
    @Select("""
            SELECT *
            FROM backup_records
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
            ORDER BY started_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<BackupRecordEntity> selectRecent(@Param("tenantId") Long tenantId, @Param("limit") int limit);
}
