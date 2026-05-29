package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.InboundOrderEntity;
import com.novadepot.backend.modules.reports.PeriodCountMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InboundOrderMapper extends BaseMapper<InboundOrderEntity> {
    @Select("""
            SELECT *
            FROM inbound_orders
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND status IN ('SUBMITTED', 'APPROVED')
              AND created_at <= #{cutoff}
            ORDER BY created_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<InboundOrderEntity> selectOverdueForPatrol(@Param("tenantId") Long tenantId,
                                                    @Param("cutoff") LocalDateTime cutoff,
                                                    @Param("limit") int limit);

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS period,
                   COUNT(*) AS count
            FROM inbound_orders
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND created_at >= #{start}
              AND created_at < #{end}
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            ORDER BY period ASC
            """)
    List<PeriodCountMetric> countByDay(@Param("tenantId") Long tenantId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
}

