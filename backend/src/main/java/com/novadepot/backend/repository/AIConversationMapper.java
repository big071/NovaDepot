package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.AIConversationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AIConversationMapper extends BaseMapper<AIConversationEntity> {
    @Select("""
            SELECT *
            FROM ai_conversations
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
            ORDER BY started_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<AIConversationEntity> selectRecentConversations(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM ai_conversations
            WHERE tenant_id = #{tenantId}
              AND conversation_no = #{conversationNo}
              AND deleted = 0
            LIMIT 1
            """)
    AIConversationEntity selectByConversationNo(@Param("tenantId") Long tenantId,
                                                @Param("conversationNo") String conversationNo);

    @Select("""
            SELECT *
            FROM ai_conversations
            WHERE status = 'ACTIVE'
              AND last_active_at < #{cutoff}
              AND deleted = 0
            LIMIT #{limit}
            """)
    List<AIConversationEntity> selectInactiveActive(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);
}
