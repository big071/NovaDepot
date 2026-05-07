package com.novadepot.backend.modules.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class FinanceRegistrationRequest {
    @NotNull
    @DecimalMin(value = "0.000001")
    private BigDecimal amount;

    private LocalDate paidAt;

    private String method;

    private String remark;
}
