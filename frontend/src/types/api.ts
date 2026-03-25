/** 后端统一响应包装 */
export interface Result<T> {
  code: number
  message: string
  data: T
}

/** 分页响应 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// ─── Dashboard ───

export interface DashboardOverviewVO {
  totalOrders: number
  totalWorkHours: number
  monthlyFee: number
  laborEfficiency: number
  avgCostPerOrder: number
  avgCostPerItem: number
}

export interface TrendDataVO {
  date: string
  warehouseCode: string
  warehouseName: string
  value: number
  type: string
}

// ─── Baseline ───

export interface MonthlyBaselineVO {
  warehouseCode: string
  warehouseName: string
  year: number
  month: number
  dailyAvgFee: number
  totalFee: number
  totalOrders: number
  totalItems: number
  costPerOrder: number
  costPerItem: number
  avgHeadcount: number
  totalWorkHours: number
  weightedUnitPrice: number
}

export interface WarehouseDetailVO {
  warehouseCode: string
  warehouseName: string
  year: number
  month: number
  dailyAvgFee: number
  totalFee: number
  totalOrders: number
  totalItems: number
  costPerOrder: number
  costPerItem: number
  avgHeadcount: number
  totalWorkHours: number
  weightedUnitPrice: number
}

export interface CompareResultVO {
  warehouseCode: string
  warehouseName: string
  totalFee: number
  totalOrders: number
  costPerOrder: number
  costPerItem: number
  avgHeadcount: number
}

// ─── Impact ───

export interface FactorRankVO {
  rank: number
  factorName: string
  correlation: number
  description: string
}

export interface CorrelationMatrixVO {
  factors: string[]
  matrix: number[][]
}

// ─── Estimate ───

export interface EstimateRequest {
  dailyOrders: number
  itemsPerOrder: number
  workDays: number
  laborEfficiency: number
  fixedLaborPrice: number
  tempLaborPrice: number
  fixedLaborRatio: number
  taxRate: number
}

export interface EstimateResultVO {
  estimatedHeadcount: number
  estimatedTotalHours: number
  weightedUnitPrice: number
  monthlyFee: number
  costPerOrder: number
  costPerItem: number
}

// ─── Report ───

export interface ReportGenerateRequest {
  warehouseCode: string
  startMonth: string
  endMonth: string
}

export interface ReportVO {
  id: string
  title: string
  warehouseCode: string
  warehouseName: string
  startMonth: string
  endMonth: string
  createdAt: string
  content?: string
}

// ─── Warehouse ───

export interface WarehouseVO {
  warehouseCode: string
  warehouseName: string
}
