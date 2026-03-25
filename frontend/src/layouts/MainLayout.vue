<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DashboardOutlined,
  BarChartOutlined,
  ExperimentOutlined,
  CalculatorOutlined,
  FileTextOutlined,
} from '@ant-design/icons-vue'
import { useAppStore } from '@/stores/app'
import { getWarehouses } from '@/api'
import type { WarehouseVO } from '@/types/api'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()

const collapsed = ref(false)
const warehouses = ref<WarehouseVO[]>([])

const selectedKeys = computed(() => {
  const path = route.path
  return [path === '/' ? '/' : path]
})

function onMenuClick({ key }: { key: string }) {
  router.push(key)
}

watch(
  () => appStore.currentWarehouse,
  () => {},
)

onMounted(async () => {
  try {
    warehouses.value = await getWarehouses()
    if (warehouses.value.length > 0 && !appStore.currentWarehouse) {
      appStore.currentWarehouse = warehouses.value[0].warehouseCode
    }
  } catch {
    // 仓库列表加载失败时静默处理
  }
})
</script>

<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible breakpoint="md">
      <div
        style="
          height: 32px;
          margin: 16px;
          color: #fff;
          font-size: 14px;
          font-weight: bold;
          text-align: center;
          line-height: 32px;
          white-space: nowrap;
          overflow: hidden;
        "
      >
        {{ collapsed ? '费用' : '仓内操作费用分析' }}
      </div>
      <a-menu
        theme="dark"
        mode="inline"
        :selected-keys="selectedKeys"
        @click="onMenuClick"
      >
        <a-menu-item key="/">
          <DashboardOutlined />
          <span>数据看板</span>
        </a-menu-item>
        <a-menu-item key="/baseline">
          <BarChartOutlined />
          <span>费用基线</span>
        </a-menu-item>
        <a-menu-item key="/impact">
          <ExperimentOutlined />
          <span>影响因素</span>
        </a-menu-item>
        <a-menu-item key="/estimate">
          <CalculatorOutlined />
          <span>成本估算</span>
        </a-menu-item>
        <a-menu-item key="/report">
          <FileTextOutlined />
          <span>报告</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header
        style="
          background: #fff;
          padding: 0 24px;
          display: flex;
          align-items: center;
          justify-content: flex-end;
        "
      >
        <a-select
          v-model:value="appStore.currentWarehouse"
          placeholder="选择仓库"
          class="warehouse-select"
          :options="
            warehouses.map((w) => ({
              value: w.warehouseCode,
              label: w.warehouseName,
            }))
          "
        />
      </a-layout-header>
      <a-layout-content style="margin: 16px">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.warehouse-select {
  width: 240px;
}
@media (max-width: 767px) {
  .warehouse-select {
    width: 160px;
  }
}
</style>
