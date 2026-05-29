package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.AIPromptTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AIPromptTemplateMapper extends BaseMapper<AIPromptTemplateEntity> {
    @Select("""
            SELECT *
            FROM ai_prompt_templates
            WHERE tenant_id = #{tenantId}
              AND scene = #{scene}
              AND enabled = 1
              AND deleted = 0
            ORDER BY version_no DESC, id DESC
            LIMIT 1
            """)
    AIPromptTemplateEntity selectLatestEnabled(@Param("tenantId") Long tenantId, @Param("scene") String scene);
}
