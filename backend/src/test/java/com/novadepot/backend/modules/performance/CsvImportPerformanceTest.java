package com.novadepot.backend.modules.performance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.cache.ReferenceDataCacheService;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.model.entity.ProductEntity;
import com.novadepot.backend.model.entity.WarehouseEntity;
import com.novadepot.backend.model.entity.WarehouseLocationEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.modules.inventory.InventoryService;
import com.novadepot.backend.modules.inventory.LowStockPolicyService;
import com.novadepot.backend.modules.partners.PartnersService;
import com.novadepot.backend.modules.products.ProductsService;
import com.novadepot.backend.repository.AuditLogMapper;
import com.novadepot.backend.repository.ImportErrorReportMapper;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.InventoryTransactionMapper;
import com.novadepot.backend.repository.PartnerMapper;
import com.novadepot.backend.repository.ProductMapper;
import com.novadepot.backend.repository.WarehouseLocationMapper;
import com.novadepot.backend.repository.WarehouseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CsvImportPerformanceTest {

    private final ImportErrorReportMapper importErrorReportMapper = mock(ImportErrorReportMapper.class);
    private final AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
    private final AuditLogRecordService auditLogRecordService = new AuditLogRecordService(auditLogMapper);
    private final ReferenceDataCacheService cacheService = mock(ReferenceDataCacheService.class);

    @BeforeEach
    void setUp() {
        RequestContext.setTenantId(1L);
        RequestContext.setUserId(100L);
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void productImport_preloadsExistingCodesOnceAndPreservesDuplicateSummary() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductsService service = new ProductsService(productMapper, importErrorReportMapper, auditLogRecordService, cacheService);
        ProductEntity existed = product(1L, "SKU-DEMO-001");
        when(productMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existed));

        Map<String, Object> summary = service.importCsv(service.importTemplateCsv());

        assertThat(summary).containsEntry("successRows", 0);
        assertThat(summary).containsEntry("failedRows", 1);
        assertThat(summary).containsEntry("skippedRows", 1);
        assertThat((List<?>) summary.get("errors")).hasSize(1);
        verify(productMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(productMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(productMapper, never()).insert(any(ProductEntity.class));
    }

    @Test
    void partnerImport_preloadsExistingCodesOnceAndPreservesSkippedRows() {
        PartnerMapper partnerMapper = mock(PartnerMapper.class);
        PartnersService service = new PartnersService(partnerMapper, auditLogRecordService, importErrorReportMapper, cacheService);
        var existed = new com.novadepot.backend.model.entity.PartnerEntity();
        existed.setId(1L);
        existed.setPartnerCode("P-DEMO-001");
        when(partnerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existed));

        Map<String, Object> summary = service.importCsv(service.importTemplateCsv());

        assertThat(summary).containsEntry("successRows", 0);
        assertThat(summary).containsEntry("failedRows", 1);
        assertThat(summary).containsEntry("skippedRows", 1);
        verify(partnerMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(partnerMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(partnerMapper, never()).insert(any(com.novadepot.backend.model.entity.PartnerEntity.class));
    }

    @Test
    void inventoryImport_preloadsReferencesAndInventoryOnce() {
        InventoryMapper inventoryMapper = mock(InventoryMapper.class);
        InventoryTransactionMapper transactionMapper = mock(InventoryTransactionMapper.class);
        WarehouseMapper warehouseMapper = mock(WarehouseMapper.class);
        WarehouseLocationMapper locationMapper = mock(WarehouseLocationMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        InventoryService service = new InventoryService(
                inventoryMapper,
                transactionMapper,
                mock(LowStockPolicyService.class),
                auditLogRecordService,
                importErrorReportMapper,
                warehouseMapper,
                locationMapper,
                productMapper
        );
        WarehouseEntity warehouse = warehouse(10L, "WH-SH-01");
        WarehouseLocationEntity location = location(20L, warehouse.getId(), "A-01-01");
        ProductEntity product = product(30L, "SKU-DEMO-001");
        InventoryEntity existed = inventory(40L, warehouse.getId(), location.getId(), product.getId(), BigDecimal.ONE);
        when(warehouseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(warehouse));
        when(locationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(location));
        when(productMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(product));
        when(inventoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existed));

        Map<String, Object> summary = service.importCsv(service.importTemplateCsv());

        assertThat(summary).containsEntry("successRows", 1);
        assertThat(summary).containsEntry("failedRows", 0);
        verify(warehouseMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(locationMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(productMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(inventoryMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(inventoryMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(inventoryMapper, times(1)).updateById(existed);
        verify(transactionMapper, times(1)).insert(any(InventoryTransactionEntity.class));
    }

    @Test
    void inventoryTransactions_useParameterizedRecentMapper() {
        InventoryTransactionMapper transactionMapper = mock(InventoryTransactionMapper.class);
        InventoryService service = new InventoryService(
                mock(InventoryMapper.class),
                transactionMapper,
                mock(LowStockPolicyService.class),
                auditLogRecordService,
                importErrorReportMapper,
                mock(WarehouseMapper.class),
                mock(WarehouseLocationMapper.class),
                mock(ProductMapper.class)
        );
        when(transactionMapper.selectRecent(eq(1L), eq(200))).thenReturn(List.of());

        assertThat(service.transactions()).isEmpty();

        verify(transactionMapper).selectRecent(1L, 200);
    }

    private ProductEntity product(Long id, String code) {
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setProductCode(code);
        entity.setProductName(code);
        return entity;
    }

    private WarehouseEntity warehouse(Long id, String code) {
        WarehouseEntity entity = new WarehouseEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setWarehouseCode(code);
        return entity;
    }

    private WarehouseLocationEntity location(Long id, Long warehouseId, String code) {
        WarehouseLocationEntity entity = new WarehouseLocationEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setWarehouseId(warehouseId);
        entity.setLocationCode(code);
        return entity;
    }

    private InventoryEntity inventory(Long id, Long warehouseId, Long locationId, Long productId, BigDecimal availableQty) {
        InventoryEntity entity = new InventoryEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setWarehouseId(warehouseId);
        entity.setLocationId(locationId);
        entity.setProductId(productId);
        entity.setAvailableQty(availableQty);
        return entity;
    }
}
