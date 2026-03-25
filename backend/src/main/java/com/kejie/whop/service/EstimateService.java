package com.kejie.whop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kejie.whop.mapper.AttendanceStatisticsMapper;
import com.kejie.whop.mapper.OutboundOrderMapper;
import com.kejie.whop.mapper.QuotationInfoMapper;
import com.kejie.whop.model.dto.EstimateRequest;
import com.kejie.whop.model.entity.AttendanceStatistics;
import com.kejie.whop.model.entity.OutboundOrder;
import com.kejie.whop.model.entity.QuotationInfo;
import com.kejie.whop.model.vo.EstimateResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static com.kejie.whop.service.DashboardService.*;

@Service
@RequiredArgsConstructor
public class EstimateService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final AttendanceStatisticsMapper attendanceStatisticsMapper;
    private final QuotationInfoMapper quotationInfoMapper;
    private final WarehouseService warehouseService;

    private static final BigDecimal HOURS_PER_DAY = new BigDecimal("8");

    public EstimateResultVO calculate(EstimateRequest req) {
        BigDecimal dailyOrders = req.getDailyOrders();
        BigDecimal itemsPerOrder = req.getItemsPerOrder();
        int workDays = req.getWorkDays();
        BigDecimal laborEfficiency = req.getLaborEfficiency();
        BigDecimal fixedLaborPrice = req.getFixedLaborPrice();
        BigDecimal tempLaborPrice = req.getTempLaborPrice();
        BigDecimal fixedLaborRatio = req.getFixedLaborRatio();
        BigDecimal taxRate = req.getTaxRate() != null ? req.getTaxRate() : new BigDecimal("0.06");

        // estimatedHeadcount = ceil(dailyOrders / laborEfficiency)
        int estimatedHeadcount = dailyOrders
                .divide(laborEfficiency, 0, RoundingMode.CEILING).intValue();

        // dailyHours = estimatedHeadcount × 8
        BigDecimal dailyHours = BigDecimal.valueOf(estimatedHeadcount).multiply(HOURS_PER_DAY);

        // estimatedTotalHours = dailyHours × workDays
        BigDecimal estimatedTotalHours = dailyHours.multiply(BigDecimal.valueOf(workDays));

        // weightedUnitPrice = fixedLaborRatio × fixedLaborPrice + (1 - fixedLaborRatio) × tempLaborPrice
        BigDecimal weightedUnitPrice = fixedLaborRatio.multiply(fixedLaborPrice)
                .add(BigDecimal.ONE.subtract(fixedLaborRatio).multiply(tempLaborPrice))
                .setScale(2, RoundingMode.HALF_UP);

        // monthlyFee = estimatedTotalHours × weightedUnitPrice × (1 + taxRate)
        BigDecimal monthlyFee = estimatedTotalHours
                .multiply(weightedUnitPrice)
                .multiply(BigDecimal.ONE.add(taxRate))
                .setScale(2, RoundingMode.HALF_UP);

        // costPerOrder = monthlyFee / (dailyOrders × workDays)
        BigDecimal totalOrders = dailyOrders.multiply(BigDecimal.valueOf(workDays));
        BigDecimal costPerOrder = totalOrders.compareTo(BigDecimal.ZERO) > 0
                ? monthlyFee.divide(totalOrders, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // costPerItem = monthlyFee / (dailyItems × workDays)
        BigDecimal dailyItems = dailyOrders.multiply(itemsPerOrder);
        BigDecimal totalItems = dailyItems.multiply(BigDecimal.valueOf(workDays));
        BigDecimal costPerItem = totalItems.compareTo(BigDecimal.ZERO) > 0
                ? monthlyFee.divide(totalItems, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        EstimateResultVO result = new EstimateResultVO();
        result.setEstimatedHeadcount(estimatedHeadcount);
        result.setEstimatedTotalHours(estimatedTotalHours);
        result.setWeightedUnitPrice(weightedUnitPrice);
        result.setMonthlyFee(monthlyFee);
        result.setCostPerOrder(costPerOrder);
        result.setCostPerItem(costPerItem);
        return result;
    }

    public EstimateRequest getDefaults(String warehouseCode) {
        String warehouseName = warehouseService.getWarehouseName(warehouseCode);
        EstimateRequest defaults = new EstimateRequest();

        // 默认值
        defaults.setDailyOrders(new BigDecimal("500"));
        defaults.setItemsPerOrder(new BigDecimal("4.25"));
        defaults.setWorkDays(26);
        defaults.setLaborEfficiency(new BigDecimal("50"));
        defaults.setFixedLaborPrice(new BigDecimal("22.00"));
        defaults.setTempLaborPrice(new BigDecimal("18.00"));
        defaults.setFixedLaborRatio(new BigDecimal("0.67"));
        defaults.setTaxRate(new BigDecimal("0.06"));

        if (warehouseName == null) {
            return defaults;
        }

        // 从出库单表计算日均单量和件单比
        QueryWrapper<OutboundOrder> orderQw = new QueryWrapper<>();
        orderQw.select("COUNT(*) as totalOrders",
                "SUM(CAST(物料总数量 AS DECIMAL(20,2))) as totalItems",
                "COUNT(DISTINCT DATE(创建时间)) as workDays")
                .eq("库房编码", warehouseCode);
        List<Map<String, Object>> orderRows = outboundOrderMapper.selectMaps(orderQw);

        if (!orderRows.isEmpty() && orderRows.get(0).get("totalOrders") != null) {
            Map<String, Object> row = orderRows.get(0);
            long totalOrders = toLong(row.get("totalOrders"));
            long totalItems = toLong(row.get("totalItems"));
            int workDays = Math.max(1, toInt(row.get("workDays")));

            if (totalOrders > 0) {
                defaults.setDailyOrders(BigDecimal.valueOf(totalOrders)
                        .divide(BigDecimal.valueOf(workDays), 1, RoundingMode.HALF_UP));
                defaults.setItemsPerOrder(BigDecimal.valueOf(totalItems)
                        .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP));
                defaults.setWorkDays(workDays);
            }
        }

        // 从出勤统计表计算人效
        QueryWrapper<AttendanceStatistics> attQw = new QueryWrapper<>();
        attQw.select("COUNT(DISTINCT 员工编码) as distinctEmployees",
                "COUNT(DISTINCT 考勤日期) as attDays")
                .eq("库房", warehouseName);
        List<Map<String, Object>> attRows = attendanceStatisticsMapper.selectMaps(attQw);

        if (!attRows.isEmpty() && attRows.get(0).get("distinctEmployees") != null) {
            int employees = toInt(attRows.get(0).get("distinctEmployees"));
            int attDays = Math.max(1, toInt(attRows.get(0).get("attDays")));
            if (employees > 0 && defaults.getDailyOrders().compareTo(BigDecimal.ZERO) > 0) {
                defaults.setLaborEfficiency(defaults.getDailyOrders()
                        .divide(BigDecimal.valueOf(employees), 1, RoundingMode.HALF_UP));
            }
        }

        // 从报价信息表获取劳务单价
        QueryWrapper<QuotationInfo> priceQw = new QueryWrapper<>();
        priceQw.select("AVG(供应商结算单价) as avgPrice")
                .eq("库房名称", warehouseName)
                .isNotNull("供应商结算单价");
        List<Map<String, Object>> priceRows = quotationInfoMapper.selectMaps(priceQw);

        if (!priceRows.isEmpty() && priceRows.get(0).get("avgPrice") != null) {
            BigDecimal avgPrice = toBigDecimal(priceRows.get(0).get("avgPrice")).setScale(2, RoundingMode.HALF_UP);
            defaults.setFixedLaborPrice(avgPrice);
            defaults.setTempLaborPrice(avgPrice.multiply(new BigDecimal("0.82")).setScale(2, RoundingMode.HALF_UP));
        }

        return defaults;
    }
}
