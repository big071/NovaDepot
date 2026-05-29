package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.FAQKnowledgeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FAQKnowledgeMapper extends BaseMapper<FAQKnowledgeEntity> {
    @Select("""
            SELECT *
            FROM faq_knowledge
            WHERE tenant_id = #{tenantId}
              AND enabled = 1
              AND deleted = 0
            ORDER BY priority DESC, id DESC
            LIMIT #{limit}
            """)
    List<FAQKnowledgeEntity> selectEnabledByPriority(@Param("tenantId") Long tenantId,
                                                     @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM faq_knowledge
            WHERE tenant_id = #{tenantId}
              AND enabled = 1
              AND review_status = 'APPROVED'
              AND deleted = 0
              AND (#{scene} IS NULL OR #{scene} = '' OR scene = #{scene} OR tags LIKE CONCAT('%', #{scene}, '%'))
            ORDER BY priority DESC, id DESC
            LIMIT #{limit}
            """)
    List<FAQKnowledgeEntity> selectActiveForMatch(@Param("tenantId") Long tenantId,
                                                  @Param("scene") String scene,
                                                  @Param("limit") int limit);
}
