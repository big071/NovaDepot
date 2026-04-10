package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.AIMessageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AIMessageMapper extends BaseMapper<AIMessageEntity> {
}
