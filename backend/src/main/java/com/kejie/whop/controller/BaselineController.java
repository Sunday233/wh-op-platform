package com.kejie.whop.controller;

import com.kejie.whop.model.vo.CompareResultVO;
import com.kejie.whop.model.vo.MonthlyBaselineVO;
import com.kejie.whop.model.vo.PageResult;
import com.kejie.whop.model.vo.Result;
import com.kejie.whop.model.vo.WarehouseDetailVO;
import com.kejie.whop.service.BaselineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/baseline")
@RequiredArgsConstructor
public class BaselineController {

    private final BaselineService baselineService;

    @GetMapping("/monthly")
    public Result<?> monthly(
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (year == null) year = java.time.LocalDate.now().getYear();
        if (month == null) month = java.time.LocalDate.now().getMonthValue();
        List<MonthlyBaselineVO> all = baselineService.getMonthlyBaseline(warehouseCode, year, month);
        if (page != null && size != null) {
            int fromIndex = Math.min((page - 1) * size, all.size());
            int toIndex = Math.min(fromIndex + size, all.size());
            return Result.ok(PageResult.of(all.subList(fromIndex, toIndex), all.size(), page, size));
        }
        return Result.ok(all);
    }

    @GetMapping("/warehouse/{warehouseCode}")
    public Result<WarehouseDetailVO> warehouseDetail(
            @PathVariable String warehouseCode,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (year == null) year = java.time.LocalDate.now().getYear();
        if (month == null) month = java.time.LocalDate.now().getMonthValue();
        return Result.ok(baselineService.getWarehouseDetail(warehouseCode, year, month));
    }

    @GetMapping("/compare")
    public Result<List<CompareResultVO>> compare(
            @RequestParam String codes,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (year == null) year = java.time.LocalDate.now().getYear();
        if (month == null) month = java.time.LocalDate.now().getMonthValue();
        return Result.ok(baselineService.compare(codes, year, month));
    }
}
