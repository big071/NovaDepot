package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.RuleConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RuleConfigMapper extends BaseMapper<RuleConfigEntity> {
    @Select("""
            SELECT *
            FROM rule_configs
            WHERE tenant_id = #{tenantId}
              AND config_key = #{configKey}
              AND deleted = 0
            LIMIT 1
            """)
    RuleConfigEntity selectByConfigKey(@Param("tenantId") Long tenantId, @Param("configKey") String configKey);

    @Select("""
            SELECT *
            FROM rule_configs
            WHERE tenant_id = #{tenantId}
              AND config_key = #{configKey}
              AND enabled = 1
              AND deleted = 0
            LIMIT 1
            """)
    RuleConfigEntity selectActiveByConfigKey(@Param("tenantId") Long tenantId, @Param("configKey") String configKey);
}
