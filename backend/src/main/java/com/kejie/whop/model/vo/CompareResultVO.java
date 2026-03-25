package com.kejie.whop.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompareResultVO {

    private String warehouseCode;
    private String warehouseName;
    private BigDecimal dailyAvgFee;
    private BigDecimal costPerOrder;
    private BigDecimal costPerItem;
    private BigDecimal laborEfficiency;
    private BigDecimal totalWorkHours;
}
