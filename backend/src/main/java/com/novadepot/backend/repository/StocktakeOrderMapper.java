package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.StocktakeOrderEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StocktakeOrderMapper extends BaseMapper<StocktakeOrderEntity> {
}
