<script setup lang="ts">
import { ref, watch, onMounted, computed, onUnmounted } from 'vue'
import axios from 'axios'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'
import { useAppStore } from '@/stores/app'
import { getMonthlyBaseline, compareWarehouses, getWarehouses, useAbortController } from '@/api'
import type { MonthlyBaselineVO, CompareResultVO, WarehouseVO, PageResult } from '@/types/api'

const appStore = useAppStore()
const loading = ref(false)
const error = ref<string | null>(null)
const baselineData = ref<MonthlyBaselineVO[]>([])
const warehouses = ref<WarehouseVO[]>([])
const pagination = ref({ current: 1, pageSize: 20, total: 0 })
const { getSignal, abort: abortRequests } = useAbortController()

// 筛选条件
const filterWarehouse = ref<string | undefined>(undefined)
const filterMonth = ref<dayjs.Dayjs | null>(null)

// 双仓对比
const compareA = ref<string | undefined>(undefined)
const compareB = ref<string | undefined>(undefined)
const compareLoading = ref(false)
const compareData = ref<CompareResultVO[]>([])

const columns = [
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 160 },
  { title: '年份', dataIndex: 'year', key: 'year', width: 70 },
  { title: '月份', dataIndex: 'month', key: 'month', width: 70 },
  { title: '日均费用', dataIndex: 'dailyAvgFee', key: 'dailyAvgFee', width: 120, customRender: ({ text }: { text: number }) => `¥${text?.toFixed(2) ?? '-'}` },
  { title: '总费用', dataIndex: 'totalFee', key: 'totalFee', width: 120, customRender: ({ text }: { text: number }) => `¥${text?.toFixed(2) ?? '-'}` },
  { title: '总单量', dataIndex: 'totalOrders', key: 'totalOrders', width: 100, customRender: ({ text }: { text: number }) => text?.toLocaleString() ?? '-' },
  { title: '总件数', dataIndex: 'totalItems', key: 'totalItems', width: 100, customRender: ({ text }: { text: number }) => text?.toLocaleString() ?? '-' },
  { title: '单均成本', dataIndex: 'costPerOrder', key: 'costPerOrder', width: 110, customRender: ({ text }: { text: number }) => `¥${text?.toFixed(2) ?? '-'}` },
  { title: '件均成本', dataIndex: 'costPerItem', key: 'costPerItem', width: 110, customRender: ({ text }: { text: number }) => `¥${text?.toFixed(2) ?? '-'}` },
  { title: '平均人数', dataIndex: 'avgHeadcount', key: 'avgHeadcount', width: 100, customRender: ({ text }: { text: number }) => text?.toFixed(1) ?? '-' },
  { title: '总工时', dataIndex: 'totalWorkHours', key: 'totalWorkHours', width: 100, customRender: ({ text }: { text: number }) => `${text?.toFixed(1) ?? '-'} h` },
  { title: '加权单价', dataIndex: 'weightedUnitPrice', key: 'weightedUnitPrice', width: 120, customRender: ({ text }: { text: number }) => `¥${text?.toFixed(2) ?? '-'}/h` },
]

async function loadData() {
  loading.value = true
  error.value = null
  const signal = getSignal()
  try {
    const wh = filterWarehouse.value || undefined
    const year = filterMonth.value ? filterMonth.value.year() : undefined
    const month = filterMonth.value ? filterMonth.value.month() + 1 : undefined
    const res = await getMonthlyBaseline(wh, year, month, pagination.value.current, pagination.value.pageSize, signal)
    if ('records' in res) {
      baselineData.value = (res as PageResult<MonthlyBaselineVO>).records
      pagination.value.total = (res as PageResult<MonthlyBaselineVO>).total
    } else {
      baselineData.value = res as MonthlyBaselineVO[]
      pagination.value.total = baselineData.value.length
    }
  } catch (e) {
    if (!axios.isCancel(e)) {
      error.value = '基线数据加载失败，请重试'
    }
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: { current?: number; pageSize?: number }) {
  pagination.value.current = pag.current ?? 1
  pagination.value.pageSize = pag.pageSize ?? 20
  loadData()
}

async function handleCompare() {
  if (!compareA.value || !compareB.value) return
  compareLoading.value = true
  try {
    compareData.value = await compareWarehouses([compareA.value, compareB.value])
  } catch {
    // handled by interceptor
  } finally {
    compareLoading.value = false
  }
}

// 单价对比图
const priceOption = computed(() => {
  const names = baselineData.value.map((d) => `${d.warehouseName} ${d.year}-${String(d.month).padStart(2, '0')}`)
  const prices = baselineData.value.map((d) => d.weightedUnitPrice)
  return {
    tooltip: { trigger: 'axis' as const },
    grid: { left: 80, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category' as const, data: names, axisLabel: { rotate: 20 } },
    yAxis: { type: 'value' as const, axisLabel: { formatter: '¥{value}/h' } },
    series: [{ type: 'bar' as const, data: prices, itemStyle: { color: '#1890ff' } }],
  }
})

// 双仓对比图
const compareOption = computed(() => {
  if (compareData.value.length < 2) return null
  const dims = ['总费用', '总单量', '单均成本', '件均成本', '平均人数']
  const a = compareData.value[0]
  const b = compareData.value[1]
  return {
    tooltip: { trigger: 'axis' as const },
    legend: { bottom: 0, data: [a.warehouseName, b.warehouseName] },
    grid: { left: 80, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category' as const, data: dims },
    yAxis: { type: 'value' as const },
    series: [
      { name: a.warehouseName, type: 'bar' as const, data: [a.totalFee, a.totalOrders, a.costPerOrder, a.costPerItem, a.avgHeadcount] },
      { name: b.warehouseName, type: 'bar' as const, data: [b.totalFee, b.totalOrders, b.costPerOrder, b.costPerItem, b.avgHeadcount] },
    ],
  }
})

onMounted(async () => {
  try {
    warehouses.value = await getWarehouses()
  } catch { /* ignore */ }
  filterWarehouse.value = appStore.currentWarehouse ?? undefined
  loadData()
})

watch(() => appStore.currentWarehouse, (val) => {
  filterWarehouse.value = val ?? undefined
  loadData()
})

onUnmounted(abortRequests)
</script>

<template>
  <div class="p-4">
    <a-result v-if="error" status="error" :title="error">
      <template #extra>
        <a-button type="primary" @click="loadData">重试</a-button>
      </template>
    </a-result>
    <template v-else>
    <!-- 筛选面板 -->
    <a-card class="mb-4">
      <a-space>
        <a-select
          v-model:value="filterWarehouse"
          placeholder="选择仓库"
          allow-clear
          style="width: 220px"
          :options="warehouses.map((w) => ({ value: w.warehouseCode, label: w.warehouseName }))"
        />
        <a-date-picker v-model:value="filterMonth" picker="month" format="YYYY-MM" placeholder="选择月份" />
        <a-button type="primary" :loading="loading" @click="loadData">查询</a-button>
      </a-space>
    </a-card>

    <!-- 月度基线表格 -->
    <a-card title="月度基线数据" class="mb-4">
      <a-table
        :columns="columns"
        :data-source="baselineData"
        :loading="loading"
        :row-key="(r: MonthlyBaselineVO) => `${r.warehouseCode}_${r.year}_${r.month}`"
        :pagination="{ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total, showSizeChanger: true }"
        :scroll="{ x: 1300 }"
        size="small"
        @change="handleTableChange"
      />
    </a-card>

    <!-- 劳务单价对比图 -->
    <a-card title="加权平均劳务单价对比" class="mb-4">
      <a-empty v-if="baselineData.length === 0 && !loading" description="暂无基线数据" />
      <v-chart v-else :option="priceOption" style="height: 320px" autoresize />
    </a-card>

    <!-- 双仓对比 -->
    <a-card title="双仓月度对比">
      <a-space class="mb-4">
        <a-select v-model:value="compareA" placeholder="仓库 A" style="width: 200px" :options="warehouses.map((w) => ({ value: w.warehouseCode, label: w.warehouseName }))" />
        <a-select v-model:value="compareB" placeholder="仓库 B" style="width: 200px" :options="warehouses.map((w) => ({ value: w.warehouseCode, label: w.warehouseName }))" />
        <a-button type="primary" :loading="compareLoading" :disabled="!compareA || !compareB" @click="handleCompare">对比</a-button>
      </a-space>
      <div v-if="!compareOption && !compareLoading" class="text-center py-8 text-gray-400">请选择两个仓库后点击对比</div>
      <v-chart v-else-if="compareOption" :option="compareOption" style="height: 320px" autoresize />
    </a-card>
    </template>
  </div>
</template>
