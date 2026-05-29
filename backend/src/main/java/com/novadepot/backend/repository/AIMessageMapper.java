package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.AIMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AIMessageMapper extends BaseMapper<AIMessageEntity> {
    @Select("""
            SELECT *
            FROM ai_messages
            WHERE tenant_id = #{tenantId}
              AND conversation_id = #{conversationId}
              AND role IN ('USER', 'ASSISTANT')
              AND status IN ('COMPLETED', 'STOPPED')
              AND deleted = 0
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<AIMessageEntity> selectRecentContext(@Param("tenantId") Long tenantId,
                                              @Param("conversationId") Long conversationId,
                                              @Param("limit") int limit);
}
