package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.CustomerServiceMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerServiceMessageMapper extends BaseMapper<CustomerServiceMessageEntity> {
    @Select("""
            SELECT *
            FROM customer_service_messages
            WHERE tenant_id = #{tenantId}
              AND session_id = #{sessionId}
              AND sender_type = 'CUSTOMER'
              AND deleted = 0
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """)
    CustomerServiceMessageEntity selectLatestCustomerMessage(@Param("tenantId") Long tenantId,
                                                             @Param("sessionId") Long sessionId);
}
