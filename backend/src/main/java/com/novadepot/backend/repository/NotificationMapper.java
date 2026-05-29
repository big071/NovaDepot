package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.NotificationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<NotificationEntity> {
    @Select("""
            SELECT *
            FROM notifications
            WHERE tenant_id = #{tenantId}
              AND receiver_user_id = #{receiverUserId}
              AND deleted = 0
              AND (#{unreadOnly} = false OR read_flag = 0)
            ORDER BY read_flag ASC, sent_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<NotificationEntity> selectMinePage(@Param("tenantId") Long tenantId,
                                            @Param("receiverUserId") Long receiverUserId,
                                            @Param("unreadOnly") boolean unreadOnly,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM notifications
            WHERE tenant_id = #{tenantId}
              AND receiver_user_id = #{receiverUserId}
              AND id = #{id}
              AND deleted = 0
            LIMIT 1
            """)
    NotificationEntity selectMineById(@Param("tenantId") Long tenantId,
                                      @Param("receiverUserId") Long receiverUserId,
                                      @Param("id") Long id);

    @Select("""
            SELECT *
            FROM notifications
            WHERE tenant_id = #{tenantId}
              AND receiver_user_id = #{receiverUserId}
              AND notify_type = #{notifyType}
              AND (#{bizType} IS NULL OR #{bizType} = '' OR biz_type = #{bizType})
              AND (#{bizNo} IS NULL OR #{bizNo} = '' OR biz_no = #{bizNo})
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            """)
    NotificationEntity selectExisting(@Param("tenantId") Long tenantId,
                                      @Param("receiverUserId") Long receiverUserId,
                                      @Param("notifyType") String notifyType,
                                      @Param("bizType") String bizType,
                                      @Param("bizNo") String bizNo);
}
