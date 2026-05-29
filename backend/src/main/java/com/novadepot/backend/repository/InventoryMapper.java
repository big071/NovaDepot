package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.modules.reports.InventoryTurnoverMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface InventoryMapper extends BaseMapper<InventoryEntity> {
    @Select("""
            SELECT *
            FROM inventory
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
            ORDER BY available_qty ASC, id ASC
            LIMIT #{limit}
            """)
    List<InventoryEntity> selectLowestAvailable(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM inventory
            WHERE tenant_id = #{tenantId}
              AND available_qty <= #{threshold}
              AND deleted = 0
            ORDER BY available_qty ASC, id ASC
            LIMIT #{limit}
            """)
    List<InventoryEntity> selectLowAvailable(@Param("tenantId") Long tenantId,
                                             @Param("threshold") BigDecimal threshold,
                                             @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM inventory
            WHERE tenant_id = #{tenantId}
              AND available_qty < 0
              AND deleted = 0
            ORDER BY available_qty ASC, id ASC
            LIMIT #{limit}
            """)
    List<InventoryEntity> selectNegativeAvailable(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM inventory
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
            LIMIT #{limit}
            """)
    List<InventoryEntity> selectFirstRows(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    @Select("""
            SELECT product_id AS productId,
                   CAST(0 AS DECIMAL(18, 4)) AS outboundQty,
                   COALESCE(SUM(available_qty), 0) AS availableQty
            FROM inventory
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND (#{warehouseId} IS NULL OR warehouse_id = #{warehouseId})
            GROUP BY product_id
            """)
    List<InventoryTurnoverMetric> sumAvailableByProduct(@Param("tenantId") Long tenantId,
                                                        @Param("warehouseId") Long warehouseId);
}

