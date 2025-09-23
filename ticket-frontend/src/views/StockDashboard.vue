<template>
  <div class="stock-dashboard">
    <div class="dashboard-header">
      <h1>座位库存监控看板</h1>
      <div class="refresh-controls">
        <el-switch
          v-model="autoRefresh"
          active-text="自动刷新"
          inactive-text="手动刷新"
          @change="(val: string | number | boolean) => toggleAutoRefresh(val as boolean)"
        />
        <el-button @click="refreshData" :loading="loading" type="primary">
          刷新数据
        </el-button>
      </div>
    </div>

    <div class="dashboard-stats">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ totalTickets }}</div>
              <div class="stat-label">座位档数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ totalStock }}</div>
              <div class="stat-label">总库存</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ availableStock }}</div>
              <div class="stat-label">可用库存</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ lockedStock }}</div>
              <div class="stat-label">锁定库存</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div class="dashboard-content">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>座位库存详情</span>
            <span class="last-update">最后更新: {{ lastUpdateTime }}</span>
          </div>
        </template>
        
        <el-table 
          :data="stockData" 
          v-loading="loading"
          stripe
          style="width: 100%"
          :default-sort="{prop: 'ticket.price', order: 'ascending'}"
          :row-class-name="getRowClassName"
        >
          <el-table-column prop="ticket.id" label="票档ID" width="80">
            <template #default="scope">
              <span :class="getTicketIdClass(scope.row.ticket)">
                {{ scope.row.ticket.id }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="ticket.name" label="票档名称" width="150">
            <template #default="scope">
              <span :class="getTicketNameClass(scope.row.ticket)">
                {{ scope.row.ticket.name }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="ticket.price" label="价格" width="100" sortable>
            <template #default="scope">
              <span :class="getPriceClass(scope.row.ticket.price)">
                ¥{{ scope.row.ticket.price }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="stock.totalStock" label="总库存" width="100" sortable>
            <template #default="scope">
              <span :class="getTotalStockClass(scope.row.stock?.totalStock || 0)">
                {{ scope.row.stock?.totalStock || 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="stock.availableStock" label="可用库存" width="100" sortable>
            <template #default="scope">
              <span :class="getStockClass(scope.row.stock?.availableStock || 0)">
                {{ scope.row.stock?.availableStock || 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="stock.lockedStock" label="锁定库存" width="100" sortable>
            <template #default="scope">
              <span :class="getLockedStockClass(scope.row.stock?.lockedStock || 0)">
                {{ scope.row.stock?.lockedStock || 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="stock.soldStock" label="已售库存" width="100" sortable>
            <template #default="scope">
              <span :class="getSoldStockClass(scope.row.stock?.soldStock || 0)">
                {{ scope.row.stock?.soldStock || 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="库存状态" width="120">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.stock)">
                {{ getStatusText(scope.row.stock) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="库存占用率" width="150">
            <template #default="scope">
              <el-progress 
                :percentage="getUsagePercentage(scope.row.stock)"
                :color="getProgressColor(scope.row.stock)"
                :stroke-width="8"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button size="small" @click="viewDetails(scope.row)">
                详情
              </el-button>
              <el-button size="small" type="warning" @click="initStock(scope.row)">
                初始化库存
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 监控图表区域 -->
    <div class="charts-section" v-if="showCharts">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>库存监控图表</span>
            <el-switch
              v-model="showCharts"
              active-text="显示图表"
              inactive-text="隐藏图表"
            />
          </div>
        </template>
        
        <el-row :gutter="20">
          <!-- 库存分布饼图 -->
          <el-col :span="12">
            <div class="chart-container">
              <v-chart :option="stockDistributionOption" style="height: 300px" />
            </div>
          </el-col>
          
          <!-- 价格区间库存柱状图 -->
          <el-col :span="12">
            <div class="chart-container">
              <v-chart :option="priceRangeOption" style="height: 300px" />
            </div>
          </el-col>
        </el-row>
        
        <el-row :gutter="20" style="margin-top: 20px">
          <!-- 库存利用率仪表盘 -->
          <el-col :span="12">
            <div class="chart-container">
              <v-chart :option="utilizationGaugeOption" style="height: 300px" />
            </div>
          </el-col>
          
          <!-- 库存趋势线图 -->
          <el-col :span="12">
            <div class="chart-container">
              <v-chart :option="stockTrendOption" style="height: 300px" />
            </div>
          </el-col>
        </el-row>
      </el-card>
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="座位库存详情" width="600px">
      <div v-if="selectedTicket">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="票档ID">{{ selectedTicket.ticket.id }}</el-descriptions-item>
          <el-descriptions-item label="票档名称">{{ selectedTicket.ticket.name }}</el-descriptions-item>
          <el-descriptions-item label="价格">¥{{ selectedTicket.ticket.price }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ selectedTicket.ticket.status === 1 ? '售票中' : '停售' }}</el-descriptions-item>
          <el-descriptions-item label="总库存">{{ selectedTicket.stock?.totalStock || 0 }}</el-descriptions-item>
          <el-descriptions-item label="可用库存">{{ selectedTicket.stock?.availableStock || 0 }}</el-descriptions-item>
          <el-descriptions-item label="锁定库存">{{ selectedTicket.stock?.lockedStock || 0 }}</el-descriptions-item>
          <el-descriptions-item label="已售库存">{{ selectedTicket.stock?.soldStock || 0 }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 初始化库存对话框 -->
    <el-dialog v-model="initDialogVisible" title="初始化库存" width="400px">
      <el-form :model="initForm" label-width="100px">
        <el-form-item label="座位名称">
          <el-input v-model="initForm.ticketName" disabled />
        </el-form-item>
        <el-form-item label="初始库存" required>
          <el-input-number 
            v-model="initForm.totalStock" 
            :min="1" 
            :max="10000"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="initDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmInitStock" :loading="initLoading">
          确认初始化
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ticketApi } from '@/api/ticket'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart, GaugeChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

// 注册ECharts组件
use([
  CanvasRenderer,
  PieChart,
  BarChart,
  GaugeChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

// 响应式数据
const loading = ref(false)
const autoRefresh = ref(true)
const stockData = ref<any[]>([])
const showCharts = ref(true)
const stockHistory = ref<{ time: string, total: number, available: number, locked: number }[]>([])
const lastUpdateTime = ref('')
const detailDialogVisible = ref(false)
const initDialogVisible = ref(false)
const selectedTicket = ref<any>(null)
const initLoading = ref(false)
const refreshTimer = ref<number | null>(null)

// 初始化表单
const initForm = ref({
  ticketId: null as number | null,
  ticketName: '',
  totalStock: 100
})

// 计算属性
const totalTickets = computed(() => stockData.value.length)
const totalStock = computed(() => 
  stockData.value.reduce((sum, item) => sum + (item.stock?.totalStock || 0), 0)
)
const availableStock = computed(() => 
  stockData.value.reduce((sum, item) => sum + (item.stock?.availableStock || 0), 0)
)
const lockedStock = computed(() => 
  stockData.value.reduce((sum, item) => sum + (item.stock?.lockedStock || 0), 0)
)

// 图表配置
const stockDistributionOption = computed(() => ({
  title: {
    text: '库存分布',
    left: 'center',
    textStyle: {
      fontSize: 16,
      fontWeight: 'bold',
      color: '#333'
    }
  },
  tooltip: {
    trigger: 'item',
    formatter: '{a} <br/>{b}: {c} ({d}%)'
  },
  legend: {
    orient: 'vertical',
    left: 'left',
    textStyle: {
      color: '#666'
    }
  },
  series: [
    {
      name: '库存分布',
      type: 'pie',
      radius: '60%',
      data: [
        { value: availableStock.value, name: '可用库存', itemStyle: { color: '#67C23A' } },
        { value: lockedStock.value, name: '锁定库存', itemStyle: { color: '#E6A23C' } },
        { value: stockData.value.reduce((sum, item) => sum + (item.stock?.soldStock || 0), 0), name: '已售库存', itemStyle: { color: '#F56C6C' } }
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

const priceRangeOption = computed(() => {
  const priceRanges = {
    '0-30': { count: 0, stock: 0 },
    '31-70': { count: 0, stock: 0 },
    '71-100': { count: 0, stock: 0 },
    '100+': { count: 0, stock: 0 }
  }
  
  stockData.value.forEach(item => {
    const price = item.ticket?.price || 0
    const stock = item.stock?.totalStock || 0
    
    if (price <= 30) {
      priceRanges['0-30'].count++
      priceRanges['0-30'].stock += stock
    } else if (price <= 70) {
      priceRanges['31-70'].count++
      priceRanges['31-70'].stock += stock
    } else if (price <= 100) {
      priceRanges['71-100'].count++
      priceRanges['71-100'].stock += stock
    } else {
      priceRanges['100+'].count++
      priceRanges['100+'].stock += stock
    }
  })
  
  return {
    title: {
      text: '价格区间库存分析',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#333'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: {
      type: 'category',
      data: Object.keys(priceRanges),
      axisLabel: {
        color: '#666'
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#666'
      }
    },
    series: [
      {
        name: '库存数量',
        type: 'bar',
        data: Object.values(priceRanges).map(range => range.stock),
        itemStyle: {
          color: '#409EFF'
        }
      }
    ]
  }
})

const utilizationGaugeOption = computed(() => {
  const total = totalStock.value
  const used = total - availableStock.value
  const utilization = total > 0 ? (used / total * 100) : 0
  
  return {
    title: {
      text: '库存利用率',
      left: 'center',
      top: '10px',
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#333'
      }
    },
    series: [
      {
        name: '利用率',
        type: 'gauge',
        progress: {
          show: true
        },
        axisLine: {
          lineStyle: {
            color: [[0.3, '#67e0e3'], [0.7, '#37a2da'], [1, '#fd666d']],
            width: 8
          }
        },
        axisTick: {
          distance: -30,
          length: 8,
          lineStyle: {
            color: '#666',
            width: 2
          }
        },
        splitLine: {
          distance: -30,
          length: 30,
          lineStyle: {
            color: '#666',
            width: 4
          }
        },
        axisLabel: {
          color: '#666',
          distance: 40,
          fontSize: 12
        },
        detail: {
          valueAnimation: true,
          formatter: '{value}%',
          fontSize: 20,
          color: '#333'
        },
        data: [
          {
            value: utilization.toFixed(1),
            name: '利用率'
          }
        ]
      }
    ]
  }
})

const stockTrendOption = computed(() => ({
  title: {
    text: '库存变化趋势',
    left: 'center',
    top: '10px',
    textStyle: {
      fontSize: 16,
      fontWeight: 'bold',
      color: '#333'
    }
  },
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    data: ['总库存', '可用库存', '锁定库存'],
    top: '40px',
    textStyle: {
      color: '#666'
    }
  },
  grid: {
    top: '80px',
    left: '50px',
    right: '30px',
    bottom: '50px'
  },
  xAxis: {
    type: 'category',
    data: stockHistory.value.map(item => item.time),
    axisLabel: {
      color: '#666'
    }
  },
  yAxis: {
    type: 'value',
    axisLabel: {
      color: '#666'
    }
  },
  series: [
    {
      name: '总库存',
      type: 'line',
      data: stockHistory.value.map(item => item.total),
      smooth: true,
      lineStyle: {
        color: '#409EFF'
      }
    },
    {
      name: '可用库存',
      type: 'line',
      data: stockHistory.value.map(item => item.available),
      smooth: true,
      lineStyle: {
        color: '#67C23A'
      }
    },
    {
      name: '锁定库存',
      type: 'line',
      data: stockHistory.value.map(item => item.locked),
      smooth: true,
      lineStyle: {
        color: '#E6A23C'
      }
    }
  ]
}))

// 获取库存数据
const fetchStockData = async (silent = false) => {
  try {
    if (!silent) {
      loading.value = true
    }
    const response = await ticketApi.getAllTicketStock()
    if (response.code === 200) {
      stockData.value = response.data || []
      lastUpdateTime.value = new Date().toLocaleString()
      
      // 记录历史数据用于趋势图
      const now = new Date()
      const timeLabel = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
      
      const historyData = {
        time: timeLabel,
        total: totalStock.value,
        available: availableStock.value,
        locked: lockedStock.value
      }
      
      stockHistory.value.push(historyData)
      // 保持最近20个数据点
      if (stockHistory.value.length > 20) {
        stockHistory.value.shift()
      }
    } else {
      if (!silent) {
        ElMessage.error(response.message || '获取座位数据失败')
      }
    }
  } catch (error) {
    console.error('获取座位数据失败:', error)
    if (!silent) {
      ElMessage.error('获取座位数据失败')
    }
  } finally {
    if (!silent) {
      loading.value = false
    }
  }
}

// 刷新数据
const refreshData = () => {
  fetchStockData()
}

// 切换自动刷新
const toggleAutoRefresh = (value: boolean) => {
  if (value) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

// 开始自动刷新
const startAutoRefresh = () => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
  }
  refreshTimer.value = setInterval(() => {
    fetchStockData(true) // 自动刷新时不显示loading
  }, 10000) // 每10秒刷新一次
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
}

// 获取表格行样式类名
const getRowClassName = ({ row }: { row: any }) => {
  const ticketName = row.ticket?.name || ''
  if (ticketName.includes('2小时')) return 'row-2hour'
  if (ticketName.includes('4小时')) return 'row-4hour'
  if (ticketName.includes('6小时')) return 'row-6hour'
  if (ticketName.includes('通宵')) return 'row-overnight'
  return 'row-default'
}

// 获取票档ID样式
const getTicketIdClass = (ticket: any) => {
  const price = ticket?.price || 0
  if (price <= 30) return 'ticket-id-low'
  if (price <= 70) return 'ticket-id-medium'
  return 'ticket-id-high'
}

// 获取票档名称样式
const getTicketNameClass = (ticket: any) => {
  const name = ticket?.name || ''
  if (name.includes('2小时')) return 'ticket-name-2hour'
  if (name.includes('4小时')) return 'ticket-name-4hour'
  if (name.includes('6小时')) return 'ticket-name-6hour'
  if (name.includes('通宵')) return 'ticket-name-overnight'
  return 'ticket-name-default'
}

// 获取价格样式
const getPriceClass = (price: number) => {
  if (price <= 30) return 'price-low'
  if (price <= 50) return 'price-medium'
  if (price <= 80) return 'price-high'
  return 'price-premium'
}

// 获取总库存样式
const getTotalStockClass = (stock: number) => {
  if (stock >= 800) return 'total-stock-high'
  if (stock >= 400) return 'total-stock-medium'
  if (stock >= 200) return 'total-stock-low'
  return 'total-stock-very-low'
}

// 获取锁定库存样式
const getLockedStockClass = (stock: number) => {
  if (stock > 0) return 'locked-stock-active'
  return 'locked-stock-normal'
}

// 获取已售库存样式
const getSoldStockClass = (stock: number) => {
  if (stock > 0) return 'sold-stock-active'
  return 'sold-stock-normal'
}

// 获取库存状态样式
const getStockClass = (stock: number) => {
  if (stock === 0) return 'stock-empty'
  if (stock < 10) return 'stock-low'
  return 'stock-normal'
}

// 获取状态类型
const getStatusType = (stock: any) => {
  if (!stock) return 'info'
  if (stock.availableStock === 0) return 'danger'
  if (stock.availableStock < 10) return 'warning'
  return 'success'
}

// 获取状态文本
const getStatusText = (stock: any) => {
  if (!stock) return '无库存'
  if (stock.availableStock === 0) return '售罄'
  if (stock.availableStock < 10) return '座位紧张'
  return '座位充足'
}

// 获取使用率百分比
const getUsagePercentage = (stock: any) => {
  if (!stock || stock.totalStock === 0) return 0
  return Math.round(((stock.totalStock - stock.availableStock) / stock.totalStock) * 100)
}

// 获取进度条颜色
const getProgressColor = (stock: any) => {
  const percentage = getUsagePercentage(stock)
  if (percentage >= 90) return '#f56c6c'
  if (percentage >= 70) return '#e6a23c'
  return '#67c23a'
}

// 查看详情
const viewDetails = (row: any) => {
  selectedTicket.value = row
  detailDialogVisible.value = true
}

// 初始化库存
const initStock = (row: any) => {
  initForm.value.ticketId = row.ticket.id
  initForm.value.ticketName = row.ticket.name
  initForm.value.totalStock = 100
  initDialogVisible.value = true
}

// 确认初始化库存
const confirmInitStock = async () => {
  try {
    initLoading.value = true
    
    // 添加调试日志
    console.log('=== 初始化库存调试信息 ===')
    console.log('票档ID:', initForm.value.ticketId)
    console.log('票档名称:', initForm.value.ticketName)
    console.log('初始库存:', initForm.value.totalStock)
    console.log('参数类型:', typeof initForm.value.totalStock)
    console.log('=========================')
    
    const response = await ticketApi.initializeStock(initForm.value.ticketId!, initForm.value.totalStock)
    if (response.code === 200) {
      ElMessage.success('库存初始化成功')
      initDialogVisible.value = false
      // 添加短暂延迟确保数据库事务完全提交
      setTimeout(async () => {
        await fetchStockData() // 刷新数据
      }, 500)
    } else {
      ElMessage.error(response.message || '库存初始化失败')
    }
  } catch (error) {
    console.error('库存初始化失败:', error)
    ElMessage.error('库存初始化失败')
  } finally {
    initLoading.value = false
  }
}

// 生命周期
onMounted(() => {
  fetchStockData()
  if (autoRefresh.value) {
    startAutoRefresh()
  }
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.stock-dashboard {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
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

.dashboard-stats {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-content {
  padding: 10px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.dashboard-content {
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.last-update {
  font-size: 12px;
  color: #909399;
}

/* 表格行背景样式 */
:deep(.row-2hour) {
  background-color: #e8f4fd !important;
}

:deep(.row-4hour) {
  background-color: #fff2e8 !important;
}

:deep(.row-6hour) {
  background-color: #f0f9ff !important;
}

:deep(.row-overnight) {
  background-color: #fdf2f8 !important;
}

:deep(.row-default) {
  background-color: #fafafa !important;
}

/* 票档ID样式 */
.ticket-id-low {
  color: #67c23a;
  font-weight: bold;
  background-color: #f0f9ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.ticket-id-medium {
  color: #e6a23c;
  font-weight: bold;
  background-color: #fdf6ec;
  padding: 2px 6px;
  border-radius: 4px;
}

.ticket-id-high {
  color: #f56c6c;
  font-weight: bold;
  background-color: #fef0f0;
  padding: 2px 6px;
  border-radius: 4px;
}

/* 票档名称样式 */
.ticket-name-2hour {
  color: #409eff;
  font-weight: 600;
  background: linear-gradient(135deg, #e8f4fd, #d4edda);
  padding: 4px 8px;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.ticket-name-4hour {
  color: #e6a23c;
  font-weight: 600;
  background: linear-gradient(135deg, #fff2e8, #ffeaa7);
  padding: 4px 8px;
  border-radius: 6px;
  border-left: 3px solid #e6a23c;
}

.ticket-name-6hour {
  color: #909399;
  font-weight: 600;
  background: linear-gradient(135deg, #f0f9ff, #ddd6fe);
  padding: 4px 8px;
  border-radius: 6px;
  border-left: 3px solid #909399;
}

.ticket-name-overnight {
  color: #722ed1;
  font-weight: 600;
  background: linear-gradient(135deg, #fdf2f8, #e879f9);
  padding: 4px 8px;
  border-radius: 6px;
  border-left: 3px solid #722ed1;
}

.ticket-name-default {
  color: #303133;
  font-weight: 500;
}

/* 价格样式 */
.price-low {
  color: #67c23a;
  font-weight: bold;
  font-size: 16px;
  background-color: #f0f9ff;
  padding: 3px 8px;
  border-radius: 8px;
  border: 1px solid #67c23a;
}

.price-medium {
  color: #e6a23c;
  font-weight: bold;
  font-size: 16px;
  background-color: #fdf6ec;
  padding: 3px 8px;
  border-radius: 8px;
  border: 1px solid #e6a23c;
}

.price-high {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
  background-color: #fef0f0;
  padding: 3px 8px;
  border-radius: 8px;
  border: 1px solid #f56c6c;
}

.price-premium {
  color: #722ed1;
  font-weight: bold;
  font-size: 16px;
  background: linear-gradient(135deg, #fdf2f8, #e879f9);
  padding: 3px 8px;
  border-radius: 8px;
  border: 1px solid #722ed1;
}

/* 总库存样式 */
.total-stock-high {
  color: #67c23a;
  font-weight: bold;
  background-color: #f0f9ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.total-stock-medium {
  color: #e6a23c;
  font-weight: bold;
  background-color: #fdf6ec;
  padding: 2px 6px;
  border-radius: 4px;
}

.total-stock-low {
  color: #f56c6c;
  font-weight: bold;
  background-color: #fef0f0;
  padding: 2px 6px;
  border-radius: 4px;
}

.total-stock-very-low {
  color: #909399;
  font-weight: bold;
  background-color: #f4f4f5;
  padding: 2px 6px;
  border-radius: 4px;
}

/* 锁定库存样式 */
.locked-stock-active {
  color: #e6a23c;
  font-weight: bold;
  background-color: #fdf6ec;
  padding: 2px 6px;
  border-radius: 4px;
  animation: pulse 2s infinite;
}

.locked-stock-normal {
  color: #909399;
  font-weight: normal;
}

/* 已售库存样式 */
.sold-stock-active {
  color: #67c23a;
  font-weight: bold;
  background-color: #f0f9ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.sold-stock-normal {
  color: #909399;
  font-weight: normal;
}

/* 可用库存样式 */
.stock-empty {
  color: #f56c6c;
  font-weight: bold;
  background-color: #fef0f0;
  padding: 2px 6px;
  border-radius: 4px;
  animation: blink 1s infinite;
}

.stock-low {
  color: #e6a23c;
  font-weight: bold;
  background-color: #fdf6ec;
  padding: 2px 6px;
  border-radius: 4px;
}

.stock-normal {
  color: #67c23a;
  font-weight: bold;
  background-color: #f0f9ff;
  padding: 2px 6px;
  border-radius: 4px;
}

/* 动画效果 */
@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.7; }
  100% { opacity: 1; }
}

@keyframes blink {
  0% { background-color: #fef0f0; }
  50% { background-color: #f56c6c; color: white; }
  100% { background-color: #fef0f0; }
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #2c3e50 !important;
  color: #ffffff !important;
  font-weight: 600;
}

:deep(.el-table th:hover) {
  background-color: #2c3e50 !important;
}

:deep(.el-table .el-table__header-wrapper .el-table__header th) {
  background-color: #2c3e50 !important;
  color: #ffffff !important;
}

:deep(.el-table .el-table__header-wrapper .el-table__header th:hover) {
  background-color: #2c3e50 !important;
}

:deep(.el-progress-bar__outer) {
  border-radius: 4px;
}

:deep(.el-progress-bar__inner) {
  border-radius: 4px;
}

/* 数据更新时的平滑过渡效果 */
.el-table {
  transition: opacity 0.3s ease-in-out;
}

.stat-value {
  transition: all 0.3s ease-in-out;
}

/* 移除可能的闪烁效果 */
.el-table__body-wrapper {
  transition: none;
}

.el-table__body {
  transition: none;
}

/* 图表相关样式 */
.charts-section {
  margin-top: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.chart-container {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1rem;
  border: 1px solid rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  text-align: center;
}

.chart-container h4 {
  margin: 0 0 15px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 图表动画效果 */
.charts-section .el-card {
  transition: all 0.3s ease;
}

.chart-container:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  background: rgba(255, 255, 255, 0.95);
}

.charts-section .el-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .charts-section .el-col {
    margin-bottom: 20px;
  }
  
  .chart-container {
    padding: 5px;
  }
  
  .chart-container h4 {
    font-size: 14px;
  }
}
</style>