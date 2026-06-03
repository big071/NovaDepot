package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.AuditLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {
    @Select("""
            SELECT *
            FROM audit_logs
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND id >= #{lower}
              AND id <= #{upper}
            ORDER BY occurred_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<AuditLogEntity> selectNearestCandidates(@Param("tenantId") Long tenantId,
                                                 @Param("lower") long lower,
                                                 @Param("upper") long upper,
                                                 @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM audit_logs
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND (#{module} IS NULL OR module LIKE CONCAT('%', #{module}, '%'))
              AND (#{action} IS NULL OR action LIKE CONCAT('%', #{action}, '%'))
              AND (#{resourceType} IS NULL OR resource_type LIKE CONCAT('%', #{resourceType}, '%'))
              AND (#{resourceId} IS NULL OR resource_id LIKE CONCAT('%', #{resourceId}, '%'))
              AND (#{bizNo} IS NULL OR biz_no LIKE CONCAT('%', #{bizNo}, '%'))
              AND (#{operatorId} IS NULL OR operator_id = #{operatorId})
              AND (#{operatorKeyword} IS NULL OR operator_name LIKE CONCAT('%', #{operatorKeyword}, '%') OR CAST(operator_id AS CHAR) LIKE CONCAT('%', #{operatorKeyword}, '%'))
              AND (#{onlyFailed} = false OR action LIKE '%FAIL%')
              AND (#{dateFrom} IS NULL OR occurred_at >= #{dateFrom})
              AND (#{dateTo} IS NULL OR occurred_at <= #{dateTo})
            ORDER BY occurred_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<AuditLogEntity> selectAuditPage(@Param("tenantId") Long tenantId,
                                         @Param("module") String module,
                                         @Param("action") String action,
                                         @Param("resourceType") String resourceType,
                                         @Param("resourceId") String resourceId,
                                         @Param("bizNo") String bizNo,
                                         @Param("operatorId") Long operatorId,
                                         @Param("operatorKeyword") String operatorKeyword,
                                         @Param("onlyFailed") boolean onlyFailed,
                                         @Param("dateFrom") LocalDateTime dateFrom,
                                         @Param("dateTo") LocalDateTime dateTo,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM audit_logs
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND (#{module} IS NULL OR module LIKE CONCAT('%', #{module}, '%'))
              AND (#{action} IS NULL OR action LIKE CONCAT('%', #{action}, '%'))
              AND (#{resourceType} IS NULL OR resource_type LIKE CONCAT('%', #{resourceType}, '%'))
              AND (#{resourceId} IS NULL OR resource_id LIKE CONCAT('%', #{resourceId}, '%'))
              AND (#{bizNo} IS NULL OR biz_no LIKE CONCAT('%', #{bizNo}, '%'))
              AND (#{operatorId} IS NULL OR operator_id = #{operatorId})
              AND (#{operatorKeyword} IS NULL OR operator_name LIKE CONCAT('%', #{operatorKeyword}, '%') OR CAST(operator_id AS CHAR) LIKE CONCAT('%', #{operatorKeyword}, '%'))
              AND (#{onlyFailed} = false OR action LIKE '%FAIL%')
              AND (#{dateFrom} IS NULL OR occurred_at >= #{dateFrom})
              AND (#{dateTo} IS NULL OR occurred_at <= #{dateTo})
            ORDER BY occurred_at DESC, id DESC
            """)
    List<AuditLogEntity> selectAuditExport(@Param("tenantId") Long tenantId,
                                           @Param("module") String module,
                                           @Param("action") String action,
                                           @Param("resourceType") String resourceType,
                                           @Param("resourceId") String resourceId,
                                           @Param("bizNo") String bizNo,
                                           @Param("operatorId") Long operatorId,
                                           @Param("operatorKeyword") String operatorKeyword,
                                           @Param("onlyFailed") boolean onlyFailed,
                                           @Param("dateFrom") LocalDateTime dateFrom,
                                           @Param("dateTo") LocalDateTime dateTo);
}
