<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import VChart from 'vue-echarts'
import { useAppStore } from '@/stores/app'
import { getOverview, getTrend } from '@/api'
import type { DashboardOverviewVO, TrendDataVO } from '@/types/api'

const appStore = useAppStore()
const loading = ref(false)
const overview = ref<DashboardOverviewVO | null>(null)

// 趋势数据
const trendData = ref<TrendDataVO[]>([])
const feeBreakdownData = ref<TrendDataVO[]>([])
const workloadData = ref<TrendDataVO[]>([])

const currentMonth = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
})

async function loadData() {
  const wh = appStore.currentWarehouse
  if (!wh) return
  loading.value = true
  try {
    const [ov, trend, fee, workload] = await Promise.all([
      getOverview(wh, currentMonth.value),
      getTrend(wh, currentMonth.value, currentMonth.value, 'outbound_orders'),
      getTrend(wh, currentMonth.value, currentMonth.value, 'monthly_fee_breakdown'),
      getTrend(wh, currentMonth.value, currentMonth.value, 'workload_distribution'),
    ])
    overview.value = ov
    trendData.value = trend
    feeBreakdownData.value = fee
    workloadData.value = workload
  } catch {
    // API 错误由拦截器处理
  } finally {
    loading.value = false
  }
}

// ─── 折线图 option ───
const trendOption = computed(() => {
  const grouped = new Map<string, { dates: string[]; values: number[] }>()
  for (const item of trendData.value) {
    const key = item.warehouseName || item.warehouseCode
    if (!grouped.has(key)) grouped.set(key, { dates: [], values: [] })
    const g = grouped.get(key)!
    g.dates.push(item.date)
    g.values.push(item.value)
  }
  const allDates = [...new Set(trendData.value.map((d) => d.date))].sort()
  return {
    tooltip: { trigger: 'axis' as const },
    legend: { bottom: 0 },
    grid: { left: 60, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category' as const, data: allDates },
    yAxis: { type: 'value' as const },
    series: [...grouped.entries()].map(([name, g]) => ({
      name,
      type: 'line' as const,
      data: g.values,
      smooth: true,
    })),
  }
})

// ─── 费用构成柱状图 option ───
const feeOption = computed(() => {
  const types = [...new Set(feeBreakdownData.value.map((d) => d.type))]
  const months = [...new Set(feeBreakdownData.value.map((d) => d.date))].sort()
  return {
    tooltip: { trigger: 'axis' as const },
    legend: { bottom: 0, data: types },
    grid: { left: 80, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category' as const, data: months },
    yAxis: { type: 'value' as const, axisLabel: { formatter: '¥{value}' } },
    series: types.map((t) => ({
      name: t,
      type: 'bar' as const,
      stack: 'fee',
      data: months.map((m) => {
        const item = feeBreakdownData.value.find((d) => d.type === t && d.date === m)
        return item ? item.value : 0
      }),
    })),
  }
})

// ─── 饼图 option ───
const workloadOption = computed(() => ({
  tooltip: {
    trigger: 'item' as const,
    formatter: '{b}: {c} ({d}%)',
  },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie' as const,
      radius: ['40%', '70%'],
      data: workloadData.value.map((d) => ({
        name: d.type,
        value: d.value,
      })),
    },
  ],
}))

onMounted(loadData)
watch(() => appStore.currentWarehouse, loadData)
</script>

<template>
  <div class="p-4">
    <a-spin :spinning="loading">
      <!-- KPI 卡片 -->
      <a-row :gutter="16" class="mb-4">
        <a-col :span="6">
          <a-card>
            <a-statistic title="总单量" :value="overview?.totalOrders ?? '-'" suffix="单" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="总工时" :value="overview?.totalWorkHours ?? '-'" suffix="h" :precision="1" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="月度费用" :value="overview?.monthlyFee ?? '-'" prefix="¥" :precision="2" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="人效" :value="overview?.laborEfficiency ?? '-'" suffix="件/人时" :precision="2" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 图表区域 -->
      <a-row :gutter="16" class="mb-4">
        <a-col :span="24">
          <a-card title="日出库单量趋势">
            <div v-if="trendData.length === 0 && !loading" class="text-center py-8 text-gray-400">暂无数据</div>
            <v-chart v-else :option="trendOption" style="height: 320px" autoresize />
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-card title="月度费用构成">
            <div v-if="feeBreakdownData.length === 0 && !loading" class="text-center py-8 text-gray-400">暂无数据</div>
            <v-chart v-else :option="feeOption" style="height: 320px" autoresize />
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card title="操作类型工作量分布">
            <div v-if="workloadData.length === 0 && !loading" class="text-center py-8 text-gray-400">暂无数据</div>
            <v-chart v-else :option="workloadOption" style="height: 320px" autoresize />
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>
