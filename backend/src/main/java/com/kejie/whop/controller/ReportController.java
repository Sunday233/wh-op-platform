package com.kejie.whop.controller;

import com.kejie.whop.model.dto.ReportGenerateRequest;
import com.kejie.whop.model.vo.ReportVO;
import com.kejie.whop.model.vo.Result;
import com.kejie.whop.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/generate")
    public Result<ReportVO> generate(@Valid @RequestBody ReportGenerateRequest request) {
        return Result.ok(reportService.generate(request));
    }

    @GetMapping("/list")
    public Result<List<ReportVO>> list() {
        return Result.ok(reportService.list());
    }

    @GetMapping("/{id}")
    public Result<ReportVO> detail(@PathVariable String id) {
        return Result.ok(reportService.getById(id));
    }
}
