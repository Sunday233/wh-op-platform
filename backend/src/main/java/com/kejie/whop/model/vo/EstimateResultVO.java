package com.kejie.whop.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EstimateResultVO {

    private Integer estimatedHeadcount;
    private BigDecimal estimatedTotalHours;
    private BigDecimal weightedUnitPrice;
    private BigDecimal monthlyFee;
    private BigDecimal costPerOrder;
    private BigDecimal costPerItem;
}
