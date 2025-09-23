<template>
  <div class="monitoring-dashboard">
    <div class="dashboard-header">
      <h1>系统监控看板</h1>
      <div class="refresh-controls">
        <el-switch
          v-model="autoRefresh"
          active-text="自动刷新"
          inactive-text="手动刷新"
          @change="toggleAutoRefresh"
        />
        <el-button @click="refreshAllData" :loading="loading" type="primary">
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 系统健康状态 -->
    <div class="health-status">
      <el-card>
        <template #header>
          <span>系统健康状态</span>
          <span class="last-update">最后更新: {{ lastUpdateTime }}</span>
        </template>
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="health-item">
              <el-tag :type="getHealthTagType(healthData.status)" size="large">
                {{ healthData.status || 'UNKNOWN' }}
              </el-tag>
              <div class="health-label">系统状态</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="health-item">
              <el-tag :type="getHealthTagType(healthData.database)" size="large">
                {{ healthData.database || 'UNKNOWN' }}
              </el-tag>
              <div class="health-label">数据库</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="health-item">
              <el-tag :type="getHealthTagType(healthData.redis)" size="large">
                {{ healthData.redis || 'UNKNOWN' }}
              </el-tag>
              <div class="health-label">Redis</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="health-item">
              <el-tag :type="getHealthTagType(healthData.nacos)" size="large">
                {{ healthData.nacos || 'UNKNOWN' }}
              </el-tag>
              <div class="health-label">Nacos</div>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </div>

    <!-- 监控指标卡片 -->
    <div class="metrics-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="metric-card">
            <div class="metric-content">
              <div class="metric-value">{{ stockStats.totalStock || 0 }}</div>
              <div class="metric-label">总库存</div>
              <div class="metric-trend" :class="getTrendClass('stock')">
                <i class="el-icon-arrow-up"></i> +5.2%
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="metric-card">
            <div class="metric-content">
              <div class="metric-value">{{ (successRateStats.successRate || 0).toFixed(1) }}%</div>
              <div class="metric-label">成功率</div>
              <div class="metric-trend" :class="getTrendClass('success')">
                <i class="el-icon-arrow-up"></i> +2.1%
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="metric-card">
            <div class="metric-content">
              <div class="metric-value">{{ (performanceStats.averageResponseTime || 0).toFixed(0) }}ms</div>
              <div class="metric-label">平均响应时间</div>
              <div class="metric-trend" :class="getTrendClass('performance')">
                <i class="el-icon-arrow-down"></i> -8.3%
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="metric-card">
            <div class="metric-content">
              <div class="metric-value">{{ exceptionStats.totalExceptions || 0 }}</div>
              <div class="metric-label">异常总数</div>
              <div class="metric-trend" :class="getTrendClass('exception')">
                <i class="el-icon-arrow-down"></i> -12.5%
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <el-row :gutter="20">
        <!-- 库存水位图 -->
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>库存水位监控</span>
            </template>
            <div class="chart-container">
              <v-chart :option="stockChartOption" style="height: 300px" />
            </div>
          </el-card>
        </el-col>
        
        <!-- 成功率趋势图 -->
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>操作成功率趋势</span>
            </template>
            <div class="chart-container">
              <v-chart :option="successRateChartOption" style="height: 300px" />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px">
        <!-- 性能监控图 -->
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>性能监控</span>
            </template>
            <div class="chart-container">
              <v-chart :option="performanceChartOption" style="height: 300px" />
            </div>
          </el-card>
        </el-col>
        
        <!-- 异常统计图 -->
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>异常统计</span>
            </template>
            <div class="chart-container">
              <v-chart :option="exceptionChartOption" style="height: 300px" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart, GaugeChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { monitorApi } from '@/api/monitor'
import type {
  MonitorStats,
  SuccessRateStats,
  PerformanceStats,
  ExceptionStats,
  HealthStatus
} from '@/api/monitor'

// 注册ECharts组件
use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  GaugeChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

// 响应式数据
const loading = ref(false)
const autoRefresh = ref(true)
const lastUpdateTime = ref('')
const refreshTimer = ref<number | null>(null)

// 监控数据
const healthData = ref<Partial<HealthStatus>>({})
const stockStats = ref<Partial<MonitorStats>>({})
const successRateStats = ref<Partial<SuccessRateStats>>({})
const performanceStats = ref<Partial<PerformanceStats>>({})
const exceptionStats = ref<Partial<ExceptionStats>>({})

// 历史数据用于图表
const stockHistory = ref<number[]>([])
const successRateHistory = ref<number[]>([])
const responseTimeHistory = ref<number[]>([])
const timeLabels = ref<string[]>([])

// 图表配置
const stockChartOption = computed(() => ({
  title: {
    text: '库存水位',
    left: 'center'
  },
  tooltip: {
    trigger: 'item',
    formatter: '{a} <br/>{b}: {c} ({d}%)'
  },
  legend: {
    orient: 'vertical',
    left: 'left'
  },
  series: [
    {
      name: '库存分布',
      type: 'pie',
      radius: '50%',
      data: [
        { value: stockStats.value.availableStock || 0, name: '可用库存' },
        { value: stockStats.value.lockedStock || 0, name: '锁定库存' },
        { value: stockStats.value.soldStock || 0, name: '已售库存' }
      ],
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }
  ]
}))

const successRateChartOption = computed(() => ({
  title: {
    text: '成功率趋势',
    left: 'center'
  },
  tooltip: {
    trigger: 'axis'
  },
  xAxis: {
    type: 'category',
    data: timeLabels.value
  },
  yAxis: {
    type: 'value',
    min: 0,
    max: 100,
    axisLabel: {
      formatter: '{value}%'
    }
  },
  series: [
    {
      name: '成功率',
      type: 'line',
      data: successRateHistory.value,
      smooth: true,
      lineStyle: {
        color: '#67C23A'
      },
      areaStyle: {
        color: 'rgba(103, 194, 58, 0.2)'
      }
    }
  ]
}))

const performanceChartOption = computed(() => ({
  title: {
    text: '响应时间监控',
    left: 'center'
  },
  tooltip: {
    trigger: 'axis',
    formatter: '{b}<br/>{a}: {c}ms'
  },
  xAxis: {
    type: 'category',
    data: timeLabels.value
  },
  yAxis: {
    type: 'value',
    axisLabel: {
      formatter: '{value}ms'
    }
  },
  series: [
    {
      name: '平均响应时间',
      type: 'line',
      data: responseTimeHistory.value,
      smooth: true,
      lineStyle: {
        color: '#409EFF'
      }
    }
  ]
}))

const exceptionChartOption = computed(() => ({
  title: {
    text: '异常分布',
    left: 'center'
  },
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow'
    }
  },
  xAxis: {
    type: 'category',
    data: ['库存不足', '并发冲突', '系统错误', '网络错误']
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      name: '异常数量',
      type: 'bar',
      data: [
        exceptionStats.value.stockInsufficientCount || 0,
        exceptionStats.value.concurrencyConflictCount || 0,
        exceptionStats.value.systemErrorCount || 0,
        exceptionStats.value.networkErrorCount || 0
      ],
      itemStyle: {
        color: '#F56C6C'
      }
    }
  ]
}))

// 方法
const fetchHealthData = async () => {
  try {
    const response = await monitorApi.getHealthStatus()
    if (response.code === 200) {
      healthData.value = response.data
    }
  } catch (error) {
    console.error('获取健康状态失败:', error)
  }
}

const fetchStockStats = async () => {
  try {
    const response = await monitorApi.getStockStats()
    if (response.code === 200) {
      stockStats.value = response.data
    }
  } catch (error) {
    console.error('获取库存统计失败:', error)
  }
}

const fetchSuccessRateStats = async () => {
  try {
    const response = await monitorApi.getSuccessRateStats()
    if (response.code === 200) {
      successRateStats.value = response.data
      // 更新历史数据
      successRateHistory.value.push(response.data.successRate || 0)
      if (successRateHistory.value.length > 20) {
        successRateHistory.value.shift()
      }
    }
  } catch (error) {
    console.error('获取成功率统计失败:', error)
  }
}

const fetchPerformanceStats = async () => {
  try {
    const response = await monitorApi.getPerformanceStats()
    if (response.code === 200) {
      performanceStats.value = response.data
      // 更新历史数据
      responseTimeHistory.value.push(response.data.averageResponseTime || 0)
      if (responseTimeHistory.value.length > 20) {
        responseTimeHistory.value.shift()
      }
    }
  } catch (error) {
    console.error('获取性能统计失败:', error)
  }
}

const fetchExceptionStats = async () => {
  try {
    const response = await monitorApi.getExceptionStats()
    if (response.code === 200) {
      exceptionStats.value = response.data
    }
  } catch (error) {
    console.error('获取异常统计失败:', error)
  }
}

const refreshAllData = async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchHealthData(),
      fetchStockStats(),
      fetchSuccessRateStats(),
      fetchPerformanceStats(),
      fetchExceptionStats()
    ])
    
    // 更新时间标签
    const now = new Date()
    const timeLabel = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
    timeLabels.value.push(timeLabel)
    if (timeLabels.value.length > 20) {
      timeLabels.value.shift()
    }
    
    lastUpdateTime.value = now.toLocaleString()
    ElMessage.success('数据刷新成功')
  } catch (error) {
    ElMessage.error('数据刷新失败')
  } finally {
    loading.value = false
  }
}

const toggleAutoRefresh = (value: string | number | boolean) => {
  const boolValue = Boolean(value)
  if (boolValue) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

const startAutoRefresh = () => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
  }
  refreshTimer.value = setInterval(() => {
    refreshAllData()
  }, 30000) // 每30秒刷新一次
}

const stopAutoRefresh = () => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
}

const getHealthTagType = (status: string | undefined) => {
  switch (status) {
    case 'UP':
      return 'success'
    case 'DOWN':
      return 'danger'
    case 'DEGRADED':
      return 'warning'
    default:
      return 'info'
  }
}

const getTrendClass = (type: string) => {
  // 这里可以根据实际趋势数据来判断
  const trends = {
    stock: 'positive',
    success: 'positive',
    performance: 'positive', // 响应时间降低是好事
    exception: 'positive' // 异常减少是好事
  }
  return trends[type as keyof typeof trends] || 'neutral'
}

// 生命周期
onMounted(() => {
  refreshAllData()
  if (autoRefresh.value) {
    startAutoRefresh()
  }
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.monitoring-dashboard {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.dashboard-header h1 {
  margin: 0;
  color: #303133;
  font-size: 24px;
}

.refresh-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.last-update {
  color: #909399;
  font-size: 12px;
  float: right;
}

.health-status {
  margin-bottom: 20px;
}

.health-item {
  text-align: center;
}

.health-label {
  margin-top: 8px;
  color: #606266;
  font-size: 14px;
}

.metrics-cards {
  margin-bottom: 20px;
}

.metric-card {
  text-align: center;
}

.metric-content {
  padding: 20px;
}

.metric-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.metric-label {
  color: #606266;
  font-size: 14px;
  margin-bottom: 8px;
}

.metric-trend {
  font-size: 12px;
}

.metric-trend.positive {
  color: #67C23A;
}

.metric-trend.negative {
  color: #F56C6C;
}

.metric-trend.neutral {
  color: #909399;
}

.charts-section {
  margin-top: 20px;
}

.chart-container {
  width: 100%;
  height: 300px;
}

:deep(.el-card__header) {
  background-color: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-card__body) {
  padding: 20px;
}
</style>