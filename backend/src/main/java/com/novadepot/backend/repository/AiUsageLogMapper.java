package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.AiUsageLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiUsageLogMapper extends BaseMapper<AiUsageLogEntity> {
    @Select("""
            SELECT *
            FROM ai_usage_logs
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND (#{conversationId} IS NULL OR conversation_id = #{conversationId})
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<AiUsageLogEntity> selectRecentUsage(@Param("tenantId") Long tenantId,
                                             @Param("conversationId") Long conversationId,
                                             @Param("limit") int limit);
}
