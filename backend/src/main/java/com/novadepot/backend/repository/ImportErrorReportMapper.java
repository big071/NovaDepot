package com.novadepot.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novadepot.backend.model.entity.ImportErrorReportEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImportErrorReportMapper extends BaseMapper<ImportErrorReportEntity> {
}
