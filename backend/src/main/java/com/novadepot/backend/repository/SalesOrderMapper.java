package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.SalesOrderEntity;
import com.novadepot.backend.modules.reports.AmountSummaryMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrderEntity> {
    @Select("""
            SELECT *
            FROM sales_orders
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND status IN ('DRAFT', 'CONFIRMED', 'PARTIAL_SHIPPED')
              AND created_at <= #{cutoff}
            ORDER BY created_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<SalesOrderEntity> selectOverdueForPatrol(@Param("tenantId") Long tenantId,
                                                  @Param("cutoff") LocalDateTime cutoff,
                                                  @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*) AS count,
                   COALESCE(SUM(total_amount), 0) AS amount
            FROM sales_orders
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND created_at >= #{start}
              AND created_at < #{end}
              AND (#{partnerId} IS NULL OR partner_id = #{partnerId})
            """)
    AmountSummaryMetric selectAmountSummary(@Param("tenantId") Long tenantId,
                                            @Param("partnerId") Long partnerId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}
