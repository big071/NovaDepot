package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.AgentTaskRunEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentTaskRunMapper extends BaseMapper<AgentTaskRunEntity> {
    @Select("""
            SELECT *
            FROM agent_task_runs
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND (#{taskCode} IS NULL OR task_code = #{taskCode})
              AND (#{status} IS NULL OR status = #{status})
            ORDER BY started_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<AgentTaskRunEntity> selectRunsPage(@Param("tenantId") Long tenantId,
                                            @Param("taskCode") String taskCode,
                                            @Param("status") String status,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);
}
