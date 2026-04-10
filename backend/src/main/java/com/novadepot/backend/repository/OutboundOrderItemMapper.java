package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.OutboundOrderItemEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboundOrderItemMapper extends BaseMapper<OutboundOrderItemEntity> {
}

