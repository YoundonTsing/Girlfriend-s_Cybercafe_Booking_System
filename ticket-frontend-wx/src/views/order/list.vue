<template>
  <div class="order-list-container">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="预约状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable>
            <el-option label="待确认" value="CREATED"></el-option>
            <el-option label="已确认" value="PAID"></el-option>
            <el-option label="已取消" value="CANCELED"></el-option>
            <el-option label="已超时" value="TIMEOUT"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table
      v-loading="loading"
      :data="orderList"
      style="width: 100%"
      class="order-table">
      <el-table-column
        prop="orderNo"
        label="预约编号"
        width="180">
      </el-table-column>
      <el-table-column
        label="机位信息"
        min-width="300">
        <template #default="scope">
          <div class="show-info">
            <div class="show-image">
              <img :src="getShowPoster(scope.row.showName)" alt="机位图片">
            </div>
            <div class="show-detail">
              <h4>{{ scope.row.showName }}</h4>
              <p>预约时段：{{ formatDate(scope.row.showTime) }}</p>
              <p>{{ scope.row.venue }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        prop="priceName"
        label="时段"
        width="120">
      </el-table-column>
      <el-table-column
        prop="quantity"
        label="数量"
        width="80">
      </el-table-column>
      <el-table-column
        prop="totalAmount"
        label="金额"
        width="100">
        <template #default="scope">
          <span class="amount">¥{{ scope.row.totalAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="createTime"
        label="创建时间"
        width="180">
      </el-table-column>
      <el-table-column
        prop="status"
        label="状态"
        width="100">
        <template #default="scope">
          <el-tag :type="getOrderStatusType(scope.row.status)">
            {{ getOrderStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="200">
        <template #default="scope">
          <el-button
            size="small"
            @click="handleDetail(scope.row.orderNo)">
            详情
          </el-button>
          <el-button
            size="small"
            type="primary"
            @click="handlePay(scope.row.orderNo)"
            v-if="scope.row.status === 0">
            确认预约
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleCancel(scope.row.orderNo)"
            v-if="scope.row.status === 0">
            取消预约
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="currentPage"
      :page-sizes="[10, 20, 30, 50]"
      :page-size="pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      class="pagination">
    </el-pagination>
  </div>
</template>

<script>
import { useOrderStore } from '@/stores/order'

export default {
  name: 'OrderList',
  setup() {
    const orderStore = useOrderStore()
    return {
      orderStore
    }
  },
  data() {
    return {
      filterForm: {
        status: ''
      },
      currentPage: 1,
      pageSize: 10
    }
  },
  computed: {
    orderList() {
      return this.orderStore.orderList
    },
    total() {
      return this.orderStore.total
    },
    loading() {
      return this.orderStore.loading
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      const params = {
        page: this.currentPage,
        size: this.pageSize,
        status: this.filterForm.status
      }
      this.orderStore.fetchOrderList(params)
    },
    handleFilter() {
      this.currentPage = 1
      this.fetchData()
    },
    resetFilter() {
      this.filterForm.status = ''
      this.currentPage = 1
      this.fetchData()
    },
    handleSizeChange(val) {
      this.pageSize = val
      this.fetchData()
    },
    handleCurrentChange(val) {
      this.currentPage = val
      this.fetchData()
    },
    handleDetail(orderNo) {
      this.$router.push(`/order/detail/${orderNo}`)
    },
    handlePay(orderNo) {
      this.orderStore.payOrder(orderNo)
        .then(() => {
          this.$message.success('支付成功')
          this.fetchData()
        })
        .catch(error => {
          this.$message.error(error.message || '支付失败')
        })
    },
    handleCancel(orderNo) {
      this.$confirm('确定要取消订单吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.orderStore.cancelOrder(orderNo)
          .then(() => {
            this.$message.success('订单已取消')
            this.fetchData()
          })
          .catch(error => {
            this.$message.error(error.message || '取消订单失败')
          })
      }).catch(() => {})
    },
    getOrderStatusType(status) {
      switch (status) {
        case 0:
        case 'CREATED':
          return 'warning'
        case 1:
        case 'PAID':
          return 'success'
        case 2:
        case 'CANCELED':
          return 'info'
        case 'TIMEOUT':
          return 'danger'
        default:
          return 'info'
      }
    },
    getOrderStatusText(status) {
      switch (status) {
        case 0:
        case 'CREATED':
          return '待确认'
        case 1:
        case 'PAID':
          return '已确认'
        case 2:
        case 'CANCELED':
          return '已取消'
        case 'TIMEOUT':
          return '已超时'
        default:
          return '未知状态'
      }
    },
    getShowPoster(showName) {
      // 根据机位名称返回对应的本地SVG图片
      if (!showName) {
        return '/images/seat_intermediate.svg'
      }
      
      const name = showName.toLowerCase()
      
      // 网咖机位类型匹配
      if (name.includes('svip') || name.includes('超级vip') || name.includes('至尊')) {
        return '/images/seat_svip.svg'
      } else if (name.includes('vip') || name.includes('包间') || name.includes('豪华')) {
        return '/images/seat_vip_room.svg'
      } else if (name.includes('高级') || name.includes('advanced') || name.includes('高端')) {
        return '/images/seat_advanced.svg'
      } else if (name.includes('新手') || name.includes('新客') || name.includes('newbie') || name.includes('入门')) {
        return '/images/seat_newbie.svg'
      } else if (name.includes('中级') || name.includes('intermediate') || name.includes('标准')) {
        return '/images/seat_intermediate.svg'
      }
      
      // 默认返回中级座位
      return '/images/seat_intermediate.svg'
    },
    formatDate(dateTime) {
      if (!dateTime) return '-'
      const date = new Date(dateTime)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
  }
}
</script>

<style scoped>
.order-list-container {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-card :deep(.el-card) {
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.filter-card :deep(.el-card__body) {
  background: transparent;
}

.filter-form {
  display: flex;
  justify-content: space-between;
}

/* 过滤表单样式优化 */
.filter-form :deep(.el-form-item__label) {
  color: var(--ai-text-primary);
  font-weight: 500;
}

.filter-form :deep(.el-select .el-input__inner) {
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  color: var(--ai-text-primary);
  border-radius: 20px;
}

.filter-form :deep(.el-select .el-input__inner:focus) {
  border-color: var(--ai-nvidia-green);
  box-shadow: 0 0 0 2px var(--ai-shadow-green);
}

.filter-form :deep(.el-button) {
  border-radius: 20px;
  padding: 8px 20px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.filter-form :deep(.el-button--primary) {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
  border: none;
  box-shadow: 0 2px 8px var(--ai-shadow-green);
}

.filter-form :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px var(--ai-shadow-green);
}

.filter-form :deep(.el-button:not(.el-button--primary)) {
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  color: var(--ai-text-primary);
}

.filter-form :deep(.el-button:not(.el-button--primary):hover) {
  border-color: var(--ai-nvidia-green);
  color: var(--ai-nvidia-green);
  transform: translateY(-2px);
}

.order-table {
  margin-bottom: 20px;
}

/* 科技主题表格样式优化 */
.order-table :deep(.el-table) {
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.order-table :deep(.el-table__header-wrapper) {
  background: rgba(192, 100, 135, 0.1);
  border-bottom: 2px solid var(--ai-border-primary);
}

.order-table :deep(.el-table__header th) {
  background: rgba(118, 185, 0, 0.1) !important;
  color: #addf4a !important;
  font-weight: 600;
  text-shadow: 0 0 5px rgba(118, 185, 0, 0.3);
  border-bottom: 1px solid var(--ai-border-primary) !important;
  padding: 16px 12px;
  transition: all 0.3s ease;
}

.order-table :deep(.el-table__header th:hover) {
  background: #a79c3c !important;
  color: #000000 !important;
  text-shadow: none;
  cursor: pointer;
}

.order-table :deep(.el-table__body tr) {
  background: transparent;
  transition: all 0.3s ease;
}

.order-table :deep(.el-table__body tr:hover) {
  background: rgba(118, 185, 0, 0.05) !important;
}

.order-table :deep(.el-table__body td) {
  background: transparent !important;
  border-bottom: 1px solid var(--ai-border-primary) !important;
  color: var(--ai-text-primary);
  padding: 16px 12px;
}

.order-table :deep(.el-table__body-wrapper) {
  background: transparent;
}

.order-table :deep(.el-table__empty-block) {
  background: transparent;
}

.order-table :deep(.el-table__empty-text) {
  color: var(--ai-text-secondary);
}

.show-info {
  display: flex;
  align-items: center;
  padding: 8px;
  background: rgba(118, 185, 0, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(118, 185, 0, 0.2);
  transition: all 0.3s ease;
}

.show-info:hover {
  background: rgba(118, 185, 0, 0.1);
  border-color: var(--ai-nvidia-green);
  box-shadow: 0 0 10px var(--ai-shadow-green);
}

.show-image {
  width: 60px;
  height: 80px;
  margin-right: 12px;
  overflow: hidden;
  border-radius: 8px;
  border: 1px solid var(--ai-border-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  background: var(--ai-gradient-card);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
}

.show-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.3s ease;
  background: transparent;
}

.show-info:hover .show-image {
  border-color: var(--ai-nvidia-green);
  box-shadow: 0 2px 12px var(--ai-shadow-green);
}

.show-info:hover .show-image img {
  transform: scale(1.1);
}

.show-detail {
  flex: 1;
}

.show-detail h4 {
  margin: 0 0 6px 0;
  font-size: 14px;
  color: var(--ai-text-primary);
  font-weight: 600;
  text-shadow: 0 0 3px rgba(255, 255, 255, 0.3);
}

.show-detail p {
  margin: 2px 0;
  font-size: 12px;
  color: var(--ai-text-secondary);
}

/* 金额显示样式 */
.order-table :deep(.el-table__body td) .amount {
  color: var(--ai-nvidia-green);
  font-weight: 600;
  font-size: 14px;
  text-shadow: 0 0 5px var(--ai-shadow-green);
}

/* 状态标签样式优化 */
.order-table :deep(.el-tag) {
  border-radius: 20px;
  padding: 4px 12px;
  font-weight: 500;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.order-table :deep(.el-tag--warning) {
  background: linear-gradient(45deg, #ffd700, #ffed4e);
  color: #333333;
}

.order-table :deep(.el-tag--success) {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
  color: #ffffff;
}

.order-table :deep(.el-tag--info) {
  background: linear-gradient(45deg, #57606f, #747d8c);
  color: #ffffff;
}

.order-table :deep(.el-tag--danger) {
  background: linear-gradient(45deg, #ff4757, #ff6b7a);
  color: #ffffff;
}

/* 操作按钮样式优化 */
.order-table :deep(.el-button--small) {
  border-radius: 15px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 500;
  margin-right: 6px;
  transition: all 0.3s ease;
}

.order-table :deep(.el-button--primary.el-button--small) {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
  border: none;
  box-shadow: 0 2px 8px var(--ai-shadow-green);
}

.order-table :deep(.el-button--primary.el-button--small:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px var(--ai-shadow-green);
}

.order-table :deep(.el-button--danger.el-button--small) {
  background: linear-gradient(45deg, #ff4757, #ff6b7a);
  border: none;
  box-shadow: 0 2px 8px rgba(255, 71, 87, 0.4);
}

.order-table :deep(.el-button--danger.el-button--small:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 71, 87, 0.6);
}

.order-table :deep(.el-button.el-button--small) {
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  color: var(--ai-text-primary);
}

.order-table :deep(.el-button.el-button--small:hover) {
  border-color: var(--ai-nvidia-green);
  color: var(--ai-nvidia-green);
  transform: translateY(-2px);
}

.pagination {
  text-align: center;
  margin-top: 20px;
}
</style>