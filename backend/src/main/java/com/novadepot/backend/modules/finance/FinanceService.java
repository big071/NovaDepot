package com.novadepot.backend.modules.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.PayableEntity;
import com.novadepot.backend.model.entity.PaymentEntity;
import com.novadepot.backend.model.entity.PurchaseOrderEntity;
import com.novadepot.backend.model.entity.ReceivableEntity;
import com.novadepot.backend.model.entity.SalesOrderEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.PayableMapper;
import com.novadepot.backend.repository.PaymentMapper;
import com.novadepot.backend.repository.ReceivableMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class FinanceService {
    private static final String MODULE = "ERP_FINANCE";

    private final PayableMapper payableMapper;
    private final ReceivableMapper receivableMapper;
    private final PaymentMapper paymentMapper;
    private final AuditLogRecordService auditLogRecordService;

    public FinanceService(PayableMapper payableMapper,
                          ReceivableMapper receivableMapper,
                          PaymentMapper paymentMapper,
                          AuditLogRecordService auditLogRecordService) {
        this.payableMapper = payableMapper;
        this.receivableMapper = receivableMapper;
        this.paymentMapper = paymentMapper;
        this.auditLogRecordService = auditLogRecordService;
    }

    public List<PayableEntity> listPayables(String status) {
        LambdaQueryWrapper<PayableEntity> wrapper = new LambdaQueryWrapper<PayableEntity>()
                .eq(PayableEntity::getTenantId, RequestContext.tenantId());
        if (StringUtils.hasText(status)) {
            wrapper.eq(PayableEntity::getStatus, status.trim().toUpperCase());
        }
        return payableMapper.selectList(wrapper.orderByDesc(PayableEntity::getId));
    }

    public List<ReceivableEntity> listReceivables(String status) {
        LambdaQueryWrapper<ReceivableEntity> wrapper = new LambdaQueryWrapper<ReceivableEntity>()
                .eq(ReceivableEntity::getTenantId, RequestContext.tenantId());
        if (StringUtils.hasText(status)) {
            wrapper.eq(ReceivableEntity::getStatus, status.trim().toUpperCase());
        }
        return receivableMapper.selectList(wrapper.orderByDesc(ReceivableEntity::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void ensurePayableForPurchase(PurchaseOrderEntity order) {
        PayableEntity existed = payableMapper.selectOne(new LambdaQueryWrapper<PayableEntity>()
                .eq(PayableEntity::getTenantId, RequestContext.tenantId())
                .eq(PayableEntity::getSourceType, "PURCHASE_ORDER")
                .eq(PayableEntity::getSourceOrderId, order.getId()));
        if (existed != null) {
            return;
        }
        PayableEntity payable = new PayableEntity();
        payable.setTenantId(RequestContext.tenantId());
        payable.setPayableNo(NoGenerator.next("AP"));
        payable.setSourceType("PURCHASE_ORDER");
        payable.setSourceOrderId(order.getId());
        payable.setSourceOrderNo(order.getPurchaseNo());
        payable.setPartnerId(order.getPartnerId());
        payable.setWarehouseId(order.getWarehouseId());
        payable.setTotalAmount(nullToZero(order.getTotalAmount()));
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setBalanceAmount(nullToZero(order.getTotalAmount()));
        payable.setStatus(statusFor(BigDecimal.ZERO, nullToZero(order.getTotalAmount())));
        payable.setRemark("Created from purchase confirmation");
        payable.setCreatedBy(RequestContext.userId());
        payable.setUpdatedBy(RequestContext.userId());
        payableMapper.insert(payable);
        record(payable.getId(), payable.getPayableNo(), "PAYABLE_CREATE", null, payable.getStatus(),
                "{\"sourceOrderNo\":\"" + safe(order.getPurchaseNo()) + "\",\"totalAmount\":\"" + payable.getTotalAmount() + "\"}");
    }

    @Transactional(rollbackFor = Exception.class)
    public void ensureReceivableForSales(SalesOrderEntity order) {
        ReceivableEntity existed = receivableMapper.selectOne(new LambdaQueryWrapper<ReceivableEntity>()
                .eq(ReceivableEntity::getTenantId, RequestContext.tenantId())
                .eq(ReceivableEntity::getSourceType, "SALES_ORDER")
                .eq(ReceivableEntity::getSourceOrderId, order.getId()));
        if (existed != null) {
            return;
        }
        ReceivableEntity receivable = new ReceivableEntity();
        receivable.setTenantId(RequestContext.tenantId());
        receivable.setReceivableNo(NoGenerator.next("AR"));
        receivable.setSourceType("SALES_ORDER");
        receivable.setSourceOrderId(order.getId());
        receivable.setSourceOrderNo(order.getSalesNo());
        receivable.setPartnerId(order.getPartnerId());
        receivable.setWarehouseId(order.getWarehouseId());
        receivable.setTotalAmount(nullToZero(order.getTotalAmount()));
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setBalanceAmount(nullToZero(order.getTotalAmount()));
        receivable.setStatus(statusFor(BigDecimal.ZERO, nullToZero(order.getTotalAmount())));
        receivable.setRemark("Created from sales confirmation");
        receivable.setCreatedBy(RequestContext.userId());
        receivable.setUpdatedBy(RequestContext.userId());
        receivableMapper.insert(receivable);
        record(receivable.getId(), receivable.getReceivableNo(), "RECEIVABLE_CREATE", null, receivable.getStatus(),
                "{\"sourceOrderNo\":\"" + safe(order.getSalesNo()) + "\",\"totalAmount\":\"" + receivable.getTotalAmount() + "\"}");
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelPayableForPurchase(PurchaseOrderEntity order) {
        PayableEntity payable = payableMapper.selectOne(new LambdaQueryWrapper<PayableEntity>()
                .eq(PayableEntity::getTenantId, RequestContext.tenantId())
                .eq(PayableEntity::getSourceType, "PURCHASE_ORDER")
                .eq(PayableEntity::getSourceOrderId, order.getId()));
        if (payable == null || nullToZero(payable.getPaidAmount()).compareTo(BigDecimal.ZERO) > 0) {
            return;
        }
        String before = payable.getStatus();
        payable.setStatus("CANCELLED");
        payable.setBalanceAmount(BigDecimal.ZERO);
        payable.setUpdatedBy(RequestContext.userId());
        payableMapper.updateById(payable);
        record(payable.getId(), payable.getPayableNo(), "LEDGER_STATUS_SYNC", before, "CANCELLED",
                "{\"sourceOrderNo\":\"" + safe(order.getPurchaseNo()) + "\"}");
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelReceivableForSales(SalesOrderEntity order) {
        ReceivableEntity receivable = receivableMapper.selectOne(new LambdaQueryWrapper<ReceivableEntity>()
                .eq(ReceivableEntity::getTenantId, RequestContext.tenantId())
                .eq(ReceivableEntity::getSourceType, "SALES_ORDER")
                .eq(ReceivableEntity::getSourceOrderId, order.getId()));
        if (receivable == null || nullToZero(receivable.getReceivedAmount()).compareTo(BigDecimal.ZERO) > 0) {
            return;
        }
        String before = receivable.getStatus();
        receivable.setStatus("CANCELLED");
        receivable.setBalanceAmount(BigDecimal.ZERO);
        receivable.setUpdatedBy(RequestContext.userId());
        receivableMapper.updateById(receivable);
        record(receivable.getId(), receivable.getReceivableNo(), "LEDGER_STATUS_SYNC", before, "CANCELLED",
                "{\"sourceOrderNo\":\"" + safe(order.getSalesNo()) + "\"}");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> registerPayment(Long id, FinanceRegistrationRequest request) {
        PayableEntity payable = mustGetPayable(id);
        ensureRegisterable(payable.getStatus());
        BigDecimal amount = nullToZero(request.getAmount());
        ensureAmountWithinBalance(amount, payable.getBalanceAmount());

        PaymentEntity payment = newPayment("PAYABLE", payable.getId(), payable.getPayableNo(), payable.getPartnerId(), request);
        paymentMapper.insert(payment);

        String before = payable.getStatus();
        BigDecimal paid = nullToZero(payable.getPaidAmount()).add(amount);
        payable.setPaidAmount(paid);
        payable.setBalanceAmount(nullToZero(payable.getTotalAmount()).subtract(paid));
        payable.setStatus(statusFor(paid, payable.getTotalAmount()));
        payable.setUpdatedBy(RequestContext.userId());
        payableMapper.updateById(payable);
        record(payable.getId(), payable.getPayableNo(), "PAYMENT_REGISTER", before, payable.getStatus(),
                "{\"paymentNo\":\"" + payment.getPaymentNo() + "\",\"amount\":\"" + amount + "\",\"balanceAmount\":\"" + payable.getBalanceAmount() + "\"}");
        return Map.of("id", String.valueOf(payable.getId()), "status", payable.getStatus(), "balanceAmount", payable.getBalanceAmount());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> registerReceipt(Long id, FinanceRegistrationRequest request) {
        ReceivableEntity receivable = mustGetReceivable(id);
        ensureRegisterable(receivable.getStatus());
        BigDecimal amount = nullToZero(request.getAmount());
        ensureAmountWithinBalance(amount, receivable.getBalanceAmount());

        PaymentEntity payment = newPayment("RECEIVABLE", receivable.getId(), receivable.getReceivableNo(), receivable.getPartnerId(), request);
        paymentMapper.insert(payment);

        String before = receivable.getStatus();
        BigDecimal received = nullToZero(receivable.getReceivedAmount()).add(amount);
        receivable.setReceivedAmount(received);
        receivable.setBalanceAmount(nullToZero(receivable.getTotalAmount()).subtract(received));
        receivable.setStatus(statusFor(received, receivable.getTotalAmount()));
        receivable.setUpdatedBy(RequestContext.userId());
        receivableMapper.updateById(receivable);
        record(receivable.getId(), receivable.getReceivableNo(), "RECEIPT_REGISTER", before, receivable.getStatus(),
                "{\"paymentNo\":\"" + payment.getPaymentNo() + "\",\"amount\":\"" + amount + "\",\"balanceAmount\":\"" + receivable.getBalanceAmount() + "\"}");
        return Map.of("id", String.valueOf(receivable.getId()), "status", receivable.getStatus(), "balanceAmount", receivable.getBalanceAmount());
    }

    private PaymentEntity newPayment(String direction, Long ledgerId, String ledgerNo, Long partnerId, FinanceRegistrationRequest request) {
        PaymentEntity payment = new PaymentEntity();
        payment.setTenantId(RequestContext.tenantId());
        payment.setPaymentNo(NoGenerator.next("PAY"));
        payment.setDirection(direction);
        payment.setLedgerId(ledgerId);
        payment.setLedgerNo(ledgerNo);
        payment.setPartnerId(partnerId);
        payment.setAmount(request.getAmount());
        payment.setPaidAt(request.getPaidAt() == null ? LocalDate.now() : request.getPaidAt());
        payment.setMethod(StringUtils.hasText(request.getMethod()) ? request.getMethod().trim() : "MANUAL");
        payment.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
        payment.setCreatedBy(RequestContext.userId());
        payment.setUpdatedBy(RequestContext.userId());
        return payment;
    }

    private PayableEntity mustGetPayable(Long id) {
        PayableEntity payable = payableMapper.selectOne(new LambdaQueryWrapper<PayableEntity>()
                .eq(PayableEntity::getTenantId, RequestContext.tenantId())
                .eq(PayableEntity::getId, id));
        if (payable == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Payable ledger does not exist");
        }
        return payable;
    }

    private ReceivableEntity mustGetReceivable(Long id) {
        ReceivableEntity receivable = receivableMapper.selectOne(new LambdaQueryWrapper<ReceivableEntity>()
                .eq(ReceivableEntity::getTenantId, RequestContext.tenantId())
                .eq(ReceivableEntity::getId, id));
        if (receivable == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Receivable ledger does not exist");
        }
        return receivable;
    }

    private void ensureRegisterable(String status) {
        if (List.of("PAID", "CANCELLED").contains(status)) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "This ledger cannot accept more registrations");
        }
    }

    private void ensureAmountWithinBalance(BigDecimal amount, BigDecimal balance) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Amount must be greater than 0");
        }
        if (amount.compareTo(nullToZero(balance)) > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Amount cannot exceed current balance");
        }
    }

    private String statusFor(BigDecimal paidOrReceived, BigDecimal total) {
        BigDecimal paid = nullToZero(paidOrReceived);
        BigDecimal amount = nullToZero(total);
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            return "UNPAID";
        }
        if (paid.compareTo(amount) >= 0) {
            return "PAID";
        }
        return "PARTIALLY_PAID";
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void record(Long id, String no, String action, String beforeStatus, String afterStatus, String extraJson) {
        String beforeJson = beforeStatus == null ? null : "{\"status\":\"" + beforeStatus + "\"}";
        String afterJson = "{\"status\":\"" + afterStatus + "\",\"ledgerNo\":\"" + safe(no) + "\",\"extra\":" + extraJson + "}";
        auditLogRecordService.record(MODULE, action, "FINANCE_LEDGER", String.valueOf(id), no, beforeJson, afterJson);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
