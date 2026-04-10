package com.novadepot.backend.modules.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.ProductEntity;
import com.novadepot.backend.repository.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProductsService {
    private final ProductMapper productMapper;

    public ProductsService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public List<ProductEntity> list() {
        return productMapper.selectList(new LambdaQueryWrapper<ProductEntity>()
                .eq(ProductEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(ProductEntity::getId));
    }

    public ProductEntity detail(Long id) {
        return productMapper.selectOne(new LambdaQueryWrapper<ProductEntity>()
                .eq(ProductEntity::getTenantId, RequestContext.tenantId())
                .eq(ProductEntity::getId, id));
    }

    public Map<String, Object> create(ProductCreateRequest req) {
        ProductEntity entity = new ProductEntity();
        entity.setTenantId(RequestContext.tenantId());
        entity.setProductCode(req.getProductCode());
        entity.setProductName(req.getProductName());
        entity.setCategoryId(req.getCategoryId());
        entity.setUnitId(req.getUnitId());
        entity.setBarcode(req.getBarcode());
        entity.setStatus("ACTIVE");
        entity.setBatchEnabled(0);
        productMapper.insert(entity);
        return Map.of("id", entity.getId(), "productCode", entity.getProductCode());
    }
}
