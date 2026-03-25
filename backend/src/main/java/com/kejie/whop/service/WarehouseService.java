package com.kejie.whop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kejie.whop.mapper.OutboundOrderMapper;
import com.kejie.whop.model.entity.OutboundOrder;
import com.kejie.whop.model.vo.WarehouseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final OutboundOrderMapper outboundOrderMapper;

    public List<WarehouseVO> list() {
        QueryWrapper<OutboundOrder> qw = new QueryWrapper<>();
        qw.select("DISTINCT 库房编码, 库房名称")
                .isNotNull("库房编码")
                .ne("库房编码", "");
        List<Map<String, Object>> rows = outboundOrderMapper.selectMaps(qw);
        return rows.stream().map(row -> {
            WarehouseVO vo = new WarehouseVO();
            vo.setWarehouseCode(String.valueOf(row.get("库房编码")));
            vo.setWarehouseName(String.valueOf(row.get("库房名称")));
            return vo;
        }).toList();
    }

    /**
     * 根据仓库编码获取仓库名称
     */
    public String getWarehouseName(String warehouseCode) {
        QueryWrapper<OutboundOrder> qw = new QueryWrapper<>();
        qw.select("DISTINCT 库房名称")
                .eq("库房编码", warehouseCode)
                .last("LIMIT 1");
        List<Map<String, Object>> rows = outboundOrderMapper.selectMaps(qw);
        if (rows.isEmpty()) {
            return null;
        }
        return String.valueOf(rows.get(0).get("库房名称"));
    }
}
