<script setup lang="ts">
import { ref, reactive, watch, onMounted, computed } from 'vue'
import VChart from 'vue-echarts'
import { useAppStore } from '@/stores/app'
import { calculate, getEstimateDefaults, getWarehouseDetail } from '@/api'
import type { EstimateRequest, EstimateResultVO, WarehouseDetailVO } from '@/types/api'
import type { Rule } from 'ant-design-vue/es/form'

const appStore = useAppStore()
const formRef = ref()
const loading = ref(false)
const result = ref<EstimateResultVO | null>(null)
const historyData = ref<WarehouseDetailVO | null>(null)

const form = reactive<EstimateRequest>({
  dailyOrders: 0,
  itemsPerOrder: 0,
  workDays: 22,
  laborEfficiency: 0,
  fixedLaborPrice: 0,
  tempLaborPrice: 0,
  fixedLaborRatio: 0,
  taxRate: 0.06,
})

const rules: Record<string, Rule[]> = {
  dailyOrders: [{ required: true, message: '请输入日均单量', type: 'number', min: 1 }],
  itemsPerOrder: [{ required: true, message: '请输入件单比', type: 'number', min: 0.01 }],
  workDays: [{ required: true, message: '请输入工作天数', type: 'number', min: 1, max: 31 }],
  laborEfficiency: [{ required: true, message: '请输入人效', type: 'number', min: 0.01 }],
  fixedLaborPrice: [{ required: true, message: '请输入固定劳务单价', type: 'number', min: 0.01 }],
  tempLaborPrice: [{ required: true, message: '请输入临时劳务单价', type: 'number', min: 0.01 }],
  fixedLaborRatio: [{ required: true, message: '请输入固临比', type: 'number', min: 0, max: 1 }],
  taxRate: [{ required: true, message: '请输入税率', type: 'number', min: 0, max: 1 }],
}

async function loadDefaults() {
  const wh = appStore.currentWarehouse
  if (!wh) return
  try {
    const defaults = await getEstimateDefaults(wh)
    Object.assign(form, defaults)
  } catch {
    // use current values
  }
  try {
    historyData.value = await getWarehouseDetail(wh)
  } catch {
    historyData.value = null
  }
}

async function handleCalculate() {
  try {
    await formRef.value.validateFields()
  } catch {
    return
  }
  loading.value = true
  try {
    result.value = await calculate({ ...form })
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

// 历史对比图
const compareOption = computed(() => {
  if (!result.value) return null
  const dims = ['月度费用', '单均成本', '件均成本']
  const estValues = [result.value.monthlyFee, result.value.costPerOrder, result.value.costPerItem]
  const histValues = historyData.value
    ? [historyData.value.totalFee, historyData.value.costPerOrder, historyData.value.costPerItem]
    : [0, 0, 0]
  return {
    tooltip: { trigger: 'axis' as const },
    legend: { bottom: 0, data: ['估算值', '历史值'] },
    grid: { left: 80, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category' as const, data: dims },
    yAxis: { type: 'value' as const },
    series: [
      { name: '估算值', type: 'bar' as const, data: estValues, itemStyle: { color: '#1890ff' } },
      { name: '历史值', type: 'bar' as const, data: histValues, itemStyle: { color: '#52c41a' } },
    ],
  }
})

onMounted(loadDefaults)
watch(() => appStore.currentWarehouse, () => {
  result.value = null
  loadDefaults()
})
</script>

<template>
  <div class="p-4">
    <a-row :gutter="16">
      <!-- 左侧：参数表单 -->
      <a-col :span="12">
        <a-card title="参数输入">
          <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="日均单量" name="dailyOrders">
                  <a-input-number v-model:value="form.dailyOrders" :min="0" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="件单比" name="itemsPerOrder">
                  <a-input-number v-model:value="form.itemsPerOrder" :min="0" :step="0.1" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="工作天数" name="workDays">
                  <a-input-number v-model:value="form.workDays" :min="1" :max="31" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="人效 (件/人时)" name="laborEfficiency">
                  <a-input-number v-model:value="form.laborEfficiency" :min="0" :step="0.1" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="固定劳务单价 (元/h)" name="fixedLaborPrice">
                  <a-input-number v-model:value="form.fixedLaborPrice" :min="0" :step="0.5" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="临时劳务单价 (元/h)" name="tempLaborPrice">
                  <a-input-number v-model:value="form.tempLaborPrice" :min="0" :step="0.5" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="固临比 (0-1)" name="fixedLaborRatio">
                  <a-input-number v-model:value="form.fixedLaborRatio" :min="0" :max="1" :step="0.05" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="税率" name="taxRate">
                  <a-input-number v-model:value="form.taxRate" :min="0" :max="1" :step="0.01" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item>
              <a-button type="primary" :loading="loading" @click="handleCalculate">计算</a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <!-- 右侧：计算结果 -->
      <a-col :span="12">
        <a-card title="计算结果">
          <div v-if="!result" class="text-center py-8 text-gray-400">请填写参数后点击计算</div>
          <a-descriptions v-else bordered :column="1" size="small">
            <a-descriptions-item label="预估人数">{{ result.estimatedHeadcount.toFixed(1) }} 人</a-descriptions-item>
            <a-descriptions-item label="预估总工时">{{ result.estimatedTotalHours.toFixed(1) }} h</a-descriptions-item>
            <a-descriptions-item label="加权平均单价">¥{{ result.weightedUnitPrice.toFixed(2) }}/h</a-descriptions-item>
            <a-descriptions-item label="月度费用">¥{{ result.monthlyFee.toFixed(2) }}</a-descriptions-item>
            <a-descriptions-item label="单均成本">¥{{ result.costPerOrder.toFixed(2) }}/单</a-descriptions-item>
            <a-descriptions-item label="件均成本">¥{{ result.costPerItem.toFixed(2) }}/件</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
    </a-row>

    <!-- 历史对比图 -->
    <a-card title="与历史数据对比" class="mt-4">
      <div v-if="!compareOption" class="text-center py-8 text-gray-400">
        {{ !result ? '请先完成计算' : (!historyData ? '暂无历史数据' : '') }}
      </div>
      <v-chart v-else :option="compareOption" style="height: 320px" autoresize />
    </a-card>
  </div>
</template>
