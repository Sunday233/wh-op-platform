package com.kejie.whop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kejie.whop.mapper.AttendanceStatisticsMapper;
import com.kejie.whop.mapper.OutboundOrderMapper;
import com.kejie.whop.mapper.WorkloadStatisticsInfoMapper;
import com.kejie.whop.model.entity.AttendanceStatistics;
import com.kejie.whop.model.entity.OutboundOrder;
import com.kejie.whop.model.entity.WorkloadStatisticsInfo;
import com.kejie.whop.model.vo.CompareResultVO;
import com.kejie.whop.model.vo.MonthlyBaselineVO;
import com.kejie.whop.model.vo.WarehouseDetailVO;
import com.kejie.whop.model.vo.WarehouseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.kejie.whop.service.DashboardService.*;

@Service
@RequiredArgsConstructor
public class BaselineService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final AttendanceStatisticsMapper attendanceStatisticsMapper;
    private final WorkloadStatisticsInfoMapper workloadStatisticsInfoMapper;
    private final WarehouseService warehouseService;
    private final DashboardService dashboardService;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.06");

    public List<MonthlyBaselineVO> getMonthlyBaseline(String warehouseCode, Integer year, Integer month) {
        String monthStr = String.format("%d-%02d", year, month);

        // 确定要查询的仓库列表
        List<WarehouseVO> warehouses;
        if (warehouseCode != null && !warehouseCode.isEmpty()) {
            WarehouseVO wh = new WarehouseVO();
            wh.setWarehouseCode(warehouseCode);
            wh.setWarehouseName(warehouseService.getWarehouseName(warehouseCode));
            warehouses = List.of(wh);
        } else {
            warehouses = warehouseService.list();
        }

        List<MonthlyBaselineVO> results = new ArrayList<>();
        for (WarehouseVO wh : warehouses) {
            MonthlyBaselineVO vo = buildMonthlyBaseline(wh.getWarehouseCode(), wh.getWarehouseName(), year, month, monthStr);
            if (vo != null) {
                results.add(vo);
            }
        }
        return results;
    }

    public WarehouseDetailVO getWarehouseDetail(String warehouseCode, Integer year, Integer month) {
        String monthStr = String.format("%d-%02d", year, month);
        String warehouseName = warehouseService.getWarehouseName(warehouseCode);
        if (warehouseName == null) {
            return null;
        }

        MonthlyBaselineVO baseline = buildMonthlyBaseline(warehouseCode, warehouseName, year, month, monthStr);
        if (baseline == null) {
            return null;
        }

        WarehouseDetailVO vo = new WarehouseDetailVO();
        vo.setWarehouseCode(warehouseCode);
        vo.setWarehouseName(warehouseName);
        vo.setYear(year);
        vo.setMonth(month);
        vo.setTotalFee(baseline.getTotalFee());
        vo.setDailyAvgFee(baseline.getDailyAvgFee());
        vo.setTotalOrders(baseline.getTotalOrders());
        vo.setTotalItems(baseline.getTotalItems());
        vo.setCostPerOrder(baseline.getCostPerOrder());
        vo.setCostPerItem(baseline.getCostPerItem());
        vo.setAvgHeadcount(baseline.getAvgHeadcount());
        vo.setTotalWorkHours(baseline.getTotalWorkHours());
        vo.setWeightedUnitPrice(baseline.getWeightedUnitPrice());

        // 件单比
        BigDecimal itemsPerOrder = baseline.getTotalOrders() > 0
                ? BigDecimal.valueOf(baseline.getTotalItems())
                    .divide(BigDecimal.valueOf(baseline.getTotalOrders()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        vo.setItemsPerOrder(itemsPerOrder);

        // 日均出库单量
        int workDays = getWorkDays(warehouseName, monthStr);
        vo.setDailyAvgOrders(workDays > 0
                ? BigDecimal.valueOf(baseline.getTotalOrders()).divide(BigDecimal.valueOf(workDays), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // 各操作类型工时占比（从工作量统计表）
        vo.setWorkHoursBreakdown(getWorkHoursBreakdown(warehouseCode, monthStr));

        // 劳务人员分布
        vo.setLaborDistribution(getLaborDistribution(warehouseName, monthStr));

        return vo;
    }

    public List<CompareResultVO> compare(String codes, Integer year, Integer month) {
        String monthStr = String.format("%d-%02d", year, month);
        String[] codeArr = codes.split(",");
        List<CompareResultVO> results = new ArrayList<>();

        for (String code : codeArr) {
            String trimmed = code.trim();
            String whName = warehouseService.getWarehouseName(trimmed);
            MonthlyBaselineVO baseline = buildMonthlyBaseline(trimmed, whName, year, month, monthStr);
            if (baseline == null) continue;

            CompareResultVO vo = new CompareResultVO();
            vo.setWarehouseCode(trimmed);
            vo.setWarehouseName(whName);
            vo.setDailyAvgFee(baseline.getDailyAvgFee());
            vo.setCostPerOrder(baseline.getCostPerOrder());
            vo.setCostPerItem(baseline.getCostPerItem());
            vo.setTotalWorkHours(baseline.getTotalWorkHours());

            // 计算人效
            int workDays = getWorkDays(whName, monthStr);
            BigDecimal laborEfficiency = BigDecimal.ZERO;
            if (baseline.getAvgHeadcount() > 0 && workDays > 0) {
                laborEfficiency = BigDecimal.valueOf(baseline.getTotalOrders())
                        .divide(BigDecimal.valueOf((long) baseline.getAvgHeadcount() * workDays), 2, RoundingMode.HALF_UP);
            }
            vo.setLaborEfficiency(laborEfficiency);
            results.add(vo);
        }
        return results;
    }

    // --- 内部方法 ---

    private MonthlyBaselineVO buildMonthlyBaseline(String warehouseCode, String warehouseName,
                                                    Integer year, Integer month, String monthStr) {
        String attWarehouseName = warehouseService.getAttendanceWarehouseName(warehouseName);
        // 出库单聚合
        QueryWrapper<OutboundOrder> orderQw = new QueryWrapper<>();
        orderQw.select("COUNT(*) as totalOrders",
                "SUM(CAST(物料总数量 AS DECIMAL(20,2))) as totalItems");
        orderQw.eq("库房编码", warehouseCode);
        orderQw.apply("DATE_FORMAT(创建时间, '%Y-%m') = {0}", monthStr);
        List<Map<String, Object>> orderRows = outboundOrderMapper.selectMaps(orderQw);

        long totalOrders = 0;
        long totalItems = 0;
        if (!orderRows.isEmpty() && orderRows.get(0) != null) {
            totalOrders = toLong(orderRows.get(0).get("totalOrders"));
            totalItems = toLong(orderRows.get(0).get("totalItems"));
        }

        // 出勤统计聚合
        QueryWrapper<AttendanceStatistics> attQw = new QueryWrapper<>();
        attQw.select("SUM(CAST(工作时长 AS DECIMAL(10,2))) as totalWorkHours",
                "COUNT(DISTINCT 员工编码) as distinctEmployees",
                "COUNT(DISTINCT 考勤日期) as workDays");
        if (attWarehouseName != null) {
            attQw.eq("库房", attWarehouseName);
        }
        attQw.apply("DATE_FORMAT(考勤日期, '%Y-%m') = {0}", monthStr);
        List<Map<String, Object>> attRows = attendanceStatisticsMapper.selectMaps(attQw);

        BigDecimal totalWorkHours = BigDecimal.ZERO;
        int workDays = 1;
        int avgHeadcount = 0;
        if (!attRows.isEmpty() && attRows.get(0) != null) {
            Map<String, Object> att = attRows.get(0);
            totalWorkHours = toBigDecimal(att.get("totalWorkHours"));
            workDays = Math.max(1, toInt(att.get("workDays")));
            avgHeadcount = toInt(att.get("distinctEmployees"));
        }

        // 加权平均单价
        BigDecimal weightedUnitPrice = dashboardService.getWeightedUnitPrice(warehouseName);

        // 费用计算
        BigDecimal totalFee = totalWorkHours
                .multiply(weightedUnitPrice)
                .multiply(BigDecimal.ONE.add(TAX_RATE))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal dailyAvgFee = totalFee.divide(BigDecimal.valueOf(workDays), 2, RoundingMode.HALF_UP);
        BigDecimal costPerOrder = totalOrders > 0
                ? totalFee.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal costPerItem = totalItems > 0
                ? totalFee.divide(BigDecimal.valueOf(totalItems), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        MonthlyBaselineVO vo = new MonthlyBaselineVO();
        vo.setWarehouseCode(warehouseCode);
        vo.setWarehouseName(warehouseName);
        vo.setYear(year);
        vo.setMonth(month);
        vo.setDailyAvgFee(dailyAvgFee);
        vo.setTotalFee(totalFee);
        vo.setTotalOrders(totalOrders);
        vo.setTotalItems(totalItems);
        vo.setCostPerOrder(costPerOrder);
        vo.setCostPerItem(costPerItem);
        vo.setAvgHeadcount(avgHeadcount);
        vo.setTotalWorkHours(totalWorkHours);
        vo.setWeightedUnitPrice(weightedUnitPrice);
        return vo;
    }

    private int getWorkDays(String warehouseName, String monthStr) {
        String attWarehouseName = warehouseService.getAttendanceWarehouseName(warehouseName);
        QueryWrapper<AttendanceStatistics> qw = new QueryWrapper<>();
        qw.select("COUNT(DISTINCT 考勤日期) as workDays");
        if (attWarehouseName != null) {
            qw.eq("库房", attWarehouseName);
        }
        qw.apply("DATE_FORMAT(考勤日期, '%Y-%m') = {0}", monthStr);
        List<Map<String, Object>> rows = attendanceStatisticsMapper.selectMaps(qw);
        if (rows.isEmpty() || rows.get(0) == null) return 1;
        return Math.max(1, toInt(rows.get(0).get("workDays")));
    }

    private Map<String, BigDecimal> getWorkHoursBreakdown(String warehouseCode, String monthStr) {
        // 从工作量统计表按操作大类聚合
        QueryWrapper<WorkloadStatisticsInfo> qw = new QueryWrapper<>();
        qw.select("操作大类 as category", "COUNT(*) as cnt")
                .eq("工厂编码", warehouseCode)
                .groupBy("操作大类");
        List<Map<String, Object>> rows = workloadStatisticsInfoMapper.selectMaps(qw);

        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String category = String.valueOf(row.get("category"));
            BigDecimal cnt = toBigDecimal(row.get("cnt"));
            breakdown.put(category, cnt);
        }
        return breakdown;
    }

    private Map<String, Object> getLaborDistribution(String warehouseName, String monthStr) {
        String attWarehouseName = warehouseService.getAttendanceWarehouseName(warehouseName);
        QueryWrapper<AttendanceStatistics> qw = new QueryWrapper<>();
        qw.select("员工类型 as empType", "COUNT(DISTINCT 员工编码) as cnt");
        if (attWarehouseName != null) {
            qw.eq("库房", attWarehouseName);
        }
        qw.apply("DATE_FORMAT(考勤日期, '%Y-%m') = {0}", monthStr)
                .groupBy("员工类型");
        List<Map<String, Object>> rows = attendanceStatisticsMapper.selectMaps(qw);

        int fixedCount = 0;
        int tempCount = 0;
        for (Map<String, Object> row : rows) {
            String empType = String.valueOf(row.get("empType"));
            int cnt = toInt(row.get("cnt"));
            if (empType.contains("固定") || empType.contains("正式")) {
                fixedCount += cnt;
            } else {
                tempCount += cnt;
            }
        }
        int total = fixedCount + tempCount;
        Map<String, Object> dist = new LinkedHashMap<>();
        dist.put("fixedCount", fixedCount);
        dist.put("tempCount", tempCount);
        dist.put("fixedRatio", total > 0 ? BigDecimal.valueOf(fixedCount).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        return dist;
    }
}
