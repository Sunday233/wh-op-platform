package com.kejie.whop.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EstimateRequest {

    @NotNull
    @Positive
    private BigDecimal dailyOrders;

    @NotNull
    @Positive
    private BigDecimal itemsPerOrder;

    @NotNull
    @Positive
    private Integer workDays;

    @NotNull
    @Positive
    private BigDecimal laborEfficiency;

    @NotNull
    @Positive
    private BigDecimal fixedLaborPrice;

    @NotNull
    @Positive
    private BigDecimal tempLaborPrice;

    @NotNull
    private BigDecimal fixedLaborRatio;

    private BigDecimal taxRate = new BigDecimal("0.06");
}
