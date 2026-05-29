package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.SopKnowledgeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SopKnowledgeMapper extends BaseMapper<SopKnowledgeEntity> {
    @Select("""
            SELECT *
            FROM sop_knowledge
            WHERE tenant_id = #{tenantId}
              AND enabled = 1
              AND review_status = 'APPROVED'
              AND deleted = 0
              AND (#{scene} IS NULL OR #{scene} = '' OR scene = #{scene} OR tags LIKE CONCAT('%', #{scene}, '%'))
            ORDER BY priority DESC, id DESC
            LIMIT #{limit}
            """)
    List<SopKnowledgeEntity> selectActiveForMatch(@Param("tenantId") Long tenantId,
                                                  @Param("scene") String scene,
                                                  @Param("limit") int limit);
}
