package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.AIConversationEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AIConversationMapper extends BaseMapper<AIConversationEntity> {
}
