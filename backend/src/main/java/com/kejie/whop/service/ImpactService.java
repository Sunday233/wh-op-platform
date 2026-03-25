package com.kejie.whop.service;

import com.kejie.whop.client.AnalyticsClient;
import com.kejie.whop.model.vo.CorrelationMatrixVO;
import com.kejie.whop.model.vo.FactorRankVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImpactService {

    private final AnalyticsClient analyticsClient;

    public List<FactorRankVO> getFactors(String warehouseCode) {
        return analyticsClient.getFactors(warehouseCode);
    }

    public CorrelationMatrixVO getCorrelation(String warehouseCode) {
        return analyticsClient.getCorrelation(warehouseCode);
    }
}
