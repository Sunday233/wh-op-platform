import axios from 'axios'
import { message } from 'ant-design-vue'
import type {
  Result,
  DashboardOverviewVO,
  TrendDataVO,
  MonthlyBaselineVO,
  WarehouseDetailVO,
  CompareResultVO,
  FactorRankVO,
  CorrelationMatrixVO,
  EstimateRequest,
  EstimateResultVO,
  ReportGenerateRequest,
  ReportVO,
  WarehouseVO,
} from '@/types/api'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 响应拦截器：统一处理 Result<T> 解包
http.interceptors.response.use(
  (response) => {
    const result = response.data as Result<unknown>
    if (result.code === 200) {
      return result.data as never
    }
    message.error(result.message || '请求失败')
    return Promise.reject(new Error(result.message))
  },
  (error) => {
    message.error('网络错误，请稍后重试')
    return Promise.reject(error)
  },
)

// ─── Dashboard ───

export function getOverview(warehouseCode: string, month: string) {
  return http.get<never, DashboardOverviewVO>('/dashboard/overview', {
    params: { warehouseCode, month },
  })
}

export function getTrend(warehouseCode: string, startMonth: string, endMonth: string, type: string) {
  return http.get<never, TrendDataVO[]>('/dashboard/trend', {
    params: { warehouseCode, startMonth, endMonth, type },
  })
}

// ─── Baseline ───

export function getMonthlyBaseline(warehouseCode?: string, year?: number, month?: number) {
  return http.get<never, MonthlyBaselineVO[]>('/baseline/monthly', {
    params: { warehouseCode, year, month },
  })
}

export function getWarehouseDetail(warehouseCode: string, year?: number, month?: number) {
  return http.get<never, WarehouseDetailVO>(`/baseline/warehouse/${warehouseCode}`, {
    params: { year, month },
  })
}

export function compareWarehouses(codes: string[], year?: number, month?: number) {
  return http.get<never, CompareResultVO[]>('/baseline/compare', {
    params: { codes: codes.join(','), year, month },
  })
}

// ─── Impact ───

export function getFactors(warehouseCode: string) {
  return http.get<never, FactorRankVO[]>('/impact/factors', {
    params: { warehouseCode },
  })
}

export function getCorrelation(warehouseCode: string) {
  return http.get<never, CorrelationMatrixVO>('/impact/correlation', {
    params: { warehouseCode },
  })
}

// ─── Estimate ───

export function calculate(request: EstimateRequest) {
  return http.post<never, EstimateResultVO>('/estimate/calculate', request)
}

export function getEstimateDefaults(warehouseCode: string) {
  return http.get<never, EstimateRequest>(`/estimate/defaults/${warehouseCode}`)
}

// ─── Report ───

export function generateReport(request: ReportGenerateRequest) {
  return http.post<never, ReportVO>('/report/generate', request)
}

export function getReportList() {
  return http.get<never, ReportVO[]>('/report/list')
}

export function getReportDetail(id: string) {
  return http.get<never, ReportVO>(`/report/${id}`)
}

// ─── Warehouse ───

export function getWarehouses() {
  return http.get<never, WarehouseVO[]>('/warehouses')
}
