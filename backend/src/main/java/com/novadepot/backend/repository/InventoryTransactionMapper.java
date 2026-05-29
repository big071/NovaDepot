package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.modules.reports.InventoryTurnoverMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InventoryTransactionMapper extends BaseMapper<InventoryTransactionEntity> {
    @Select("""
            SELECT *
            FROM inventory_transactions
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
            ORDER BY occurred_at DESC
            LIMIT #{limit}
            """)
    List<InventoryTransactionEntity> selectRecent(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM inventory_transactions
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND occurred_at >= #{since}
            ORDER BY occurred_at DESC
            LIMIT #{limit}
            """)
    List<InventoryTransactionEntity> selectRecentSince(@Param("tenantId") Long tenantId,
                                                       @Param("since") LocalDateTime since,
                                                       @Param("limit") int limit);

    @Select("""
            SELECT product_id AS productId,
                   COALESCE(SUM(ABS(change_qty)), 0) AS outboundQty,
                   CAST(0 AS DECIMAL(18, 4)) AS availableQty
            FROM inventory_transactions
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND occurred_at >= #{start}
              AND occurred_at < #{end}
              AND (#{warehouseId} IS NULL OR warehouse_id = #{warehouseId})
              AND (UPPER(biz_type) = 'OUTBOUND_SHIP' OR change_qty < 0)
            GROUP BY product_id
            """)
    List<InventoryTurnoverMetric> sumOutboundByProduct(@Param("tenantId") Long tenantId,
                                                       @Param("warehouseId") Long warehouseId,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);
}

