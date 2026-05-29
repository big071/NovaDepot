package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.CustomerServiceTicketEntity;
import com.novadepot.backend.modules.reports.TicketEfficiencyMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CustomerServiceTicketMapper extends BaseMapper<CustomerServiceTicketEntity> {
    @Select("""
            SELECT *
            FROM customer_service_tickets
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND status IN ('OPEN', 'PROCESSING')
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<CustomerServiceTicketEntity> selectOpenForAgent(@Param("tenantId") Long tenantId,
                                                         @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM customer_service_tickets
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND (#{sessionId} IS NULL OR session_id = #{sessionId})
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<CustomerServiceTicketEntity> selectTicketsPage(@Param("tenantId") Long tenantId,
                                                        @Param("sessionId") Long sessionId,
                                                        @Param("offset") int offset,
                                                        @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM customer_service_tickets
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND status IN ('OPEN', 'PROCESSING')
              AND created_at <= #{cutoff}
            ORDER BY created_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<CustomerServiceTicketEntity> selectOverdueForPatrol(@Param("tenantId") Long tenantId,
                                                             @Param("cutoff") LocalDateTime cutoff,
                                                             @Param("limit") int limit);

    @Select("""
            SELECT COALESCE(assignee_user_id, 0) AS assigneeId,
                   COUNT(*) AS ticketCount,
                   SUM(CASE WHEN status IN ('CLOSED', 'RESOLVED') THEN 1 ELSE 0 END) AS closedCount
            FROM customer_service_tickets
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND created_at >= #{start}
              AND created_at < #{end}
              AND (#{assigneeId} IS NULL OR assignee_user_id = #{assigneeId})
            GROUP BY COALESCE(assignee_user_id, 0)
            ORDER BY assigneeId ASC
            """)
    List<TicketEfficiencyMetric> selectEfficiencyByAssignee(@Param("tenantId") Long tenantId,
                                                            @Param("assigneeId") Long assigneeId,
                                                            @Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);
}
