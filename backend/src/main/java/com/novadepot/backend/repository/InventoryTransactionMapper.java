package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}

