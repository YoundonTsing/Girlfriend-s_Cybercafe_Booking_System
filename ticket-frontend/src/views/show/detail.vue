<template>
  <div class="show-detail-container" v-loading="loading">
    <div v-if="show" class="show-detail">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="8" :md="8" :lg="6">
          <div class="poster-container">
            <img :src="show.posterUrl" class="poster-image">
            <div class="show-status" :class="getStatusClass(show.status)">
              {{ getStatusText(show.status) }}
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="16" :md="16" :lg="18">
          <div class="show-info">
            <h1 class="show-title">{{ show.name || show.title }}</h1>
            <div class="show-meta">
              <p><i class="el-icon-time"></i> 营业时间：{{ show.showTime || '24小时营业' }}</p>
              <p><i class="el-icon-location-information"></i> 网咖地址：{{ show.venueName || show.venue }}</p>
              <p><i class="el-icon-money"></i> 价格范围：¥{{ show.minPrice }}/小时 - ¥{{ show.maxPrice }}/小时</p>
              <p><i class="el-icon-monitor"></i> 机位类型：{{ getShowTypeText(show.type) }}</p>
            </div>
            <div class="show-actions">
              <el-button type="primary" size="large" @click="handleBookSeat" :disabled="!canBookSeat">立即预约</el-button>
              <el-button type="info" icon="el-icon-star-off" size="large">收藏机位</el-button>
              <el-button type="success" icon="el-icon-share" size="large">分享给好友</el-button>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-tabs v-model="activeTab" class="show-tabs">
        <el-tab-pane label="机位详情" name="detail">
          <div class="seat-description" v-html="show.description || getSeatDescription(show.type)"></div>
        </el-tab-pane>
        <el-tab-pane label="时段信息" name="tickets">
          <div class="time-slot-list">
            <el-table :data="show.priceList || getDefaultTimeSlots()" style="width: 100%">
              <el-table-column prop="priceName" label="时段套餐" width="180"></el-table-column>
              <el-table-column prop="price" label="价格" width="180">
                <template #default="scope">
                  ¥{{ scope.row.price }}
                </template>
              </el-table-column>
              <el-table-column prop="description" label="说明"></el-table-column>
              <el-table-column prop="stock" label="可预约状态" width="120">
                <template #default="scope">
                  <el-tag :type="scope.row.stock > 0 ? 'success' : 'danger'">
                    {{ scope.row.stock > 0 ? '可预约' : '已满' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="scope">
                  <el-button 
                    type="primary" 
                    size="small" 
                    @click="selectTimeSlot(scope.row)"
                    :disabled="scope.row.stock <= 0 || !canBookSeat">
                    选择
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
        <el-tab-pane label="网咖信息" name="venue">
          <div class="cafe-info">
            <h3>{{ show.venue?.name || '自由点网咖' }}</h3>
            <p><i class="el-icon-location-information"></i> 地址：{{ show.venue?.address || show.venue || '北京市朝阳区科技园区' }}</p>
            <p><i class="el-icon-phone"></i> 电话：{{ show.venue?.phone || '400-888-9999' }}</p>
            <p><i class="el-icon-info"></i> 交通指南：{{ show.venue?.trafficInfo || '地铁10号线直达，多路公交可达' }}</p>
            <p><i class="el-icon-service"></i> 设施服务：高端电竞设备、24小时营业、免费WiFi、饮料小食</p>
            <p><i class="el-icon-star-on"></i> 特色：专业电竞环境、舒适座椅、安静氛围</p>
          </div>
        </el-tab-pane>
        <el-tab-pane label="预约须知" name="notice">
          <div class="booking-notice" v-html="show.ticketNotice || getDefaultBookingNotice()"></div>
        </el-tab-pane>
      </el-tabs>
    </div>


  </div>
</template>

<script>
import { useShowStore } from '@/stores/show'
import { useUserStore } from '@/stores/user'
import { useOrderStore } from '@/stores/order'

export default {
  name: 'ShowDetail',
  setup() {
    const showStore = useShowStore()
    const userStore = useUserStore()
    const orderStore = useOrderStore()
    return {
      showStore,
      userStore,
      orderStore
    }
  },
  data() {
    return {
      activeTab: 'detail'
    }
  },
  computed: {
    show() {
      return this.showStore.currentShow
    },
    loading() {
      return this.showStore.loading
    },
    isLoggedIn() {
      return this.userStore.isLoggedIn
    },
    canBookSeat() {
      return this.show && (this.show.status === 'ON_SALE' || this.show.status === 1) && this.isLoggedIn
    }
  },
  created() {
    const showId = this.$route.params.id
    this.showStore.fetchShowDetail(showId)
  },
  methods: {
    getStatusClass(status) {
      switch (status) {
        case 'ON_SALE':
          return 'status-on-sale'
        case 'SOLD_OUT':
          return 'status-sold-out'
        case 'UPCOMING':
          return 'status-upcoming'
        case 'ENDED':
          return 'status-ended'
        default:
          return ''
      }
    },
    getStatusText(status) {
      switch (status) {
        case 'ON_SALE':
        case 1:
          return '可预约'
        case 'SOLD_OUT':
        case 2:
          return '已满'
        case 'UPCOMING':
        case 0:
          return '即将开放'
        case 'ENDED':
        case 3:
          return '已结束'
        default:
          return '可预约'
      }
    },
    getShowTypeText(type) {
      switch (type) {
        case 1:
          return '新客电竞机位'
        case 2:
          return '中级电竞机位'
        case 3:
          return '高级电竞机位'
        case 4:
          return '包厢电竞机位'
        case 5:
          return 'SVIP电竞机位'
        default:
          return '电竞机位'
      }
    },
    getSeatDescription(type) {
      const descriptions = {
        1: '<h3>新客电竞机位</h3><p>适合新手玩家的入门级配置，提供舒适的游戏体验。</p><ul><li>处理器：Intel i5-8400</li><li>显卡：GTX 1060 6G</li><li>内存：16GB DDR4</li><li>显示器：24英寸 1080P</li><li>外设：机械键盘 + 游戏鼠标</li></ul>',
        2: '<h3>中级电竞机位</h3><p>双屏配置，RGB灯效，适合进阶玩家。</p><ul><li>处理器：Intel i7-9700</li><li>显卡：RTX 2070</li><li>内存：32GB DDR4</li><li>显示器：双24英寸 1080P</li><li>外设：RGB机械键盘 + 电竞鼠标</li><li>特色：RGB灯效系统</li></ul>',
        3: '<h3>高级电竞机位</h3><p>超宽屏配置，专业电竞体验。</p><ul><li>处理器：Intel i9-10900K</li><li>显卡：RTX 3070</li><li>内存：32GB DDR4</li><li>显示器：34英寸超宽屏 1440P</li><li>外设：专业电竞外设套装</li><li>特色：曲面超宽屏</li></ul>',
        4: '<h3>包厢电竞机位</h3><p>独立包厢，私密舒适的游戏环境。</p><ul><li>处理器：Intel i9-11900K</li><li>显卡：RTX 3080</li><li>内存：32GB DDR4</li><li>显示器：27英寸 2K 144Hz</li><li>外设：顶级电竞外设</li><li>特色：独立包厢，私密空间</li></ul>',
        5: '<h3>SVIP电竞机位</h3><p>三屏配置，顶级硬件，极致游戏体验。</p><ul><li>处理器：Intel i9-12900K</li><li>显卡：RTX 4080</li><li>内存：64GB DDR5</li><li>显示器：三联27英寸 2K 165Hz</li><li>外设：定制版电竞外设</li><li>特色：三屏环绕，VIP服务</li></ul>'
      }
      return descriptions[type] || '<p>高品质电竞机位，为您提供优质的游戏体验。</p>'
    },
    getDefaultTimeSlots() {
      return [
        { id: 1, priceName: '2小时套餐', price: 20, description: '适合短时间游戏', stock: 5 },
        { id: 2, priceName: '4小时套餐', price: 35, description: '半天游戏时光', stock: 3 },
        { id: 3, priceName: '6小时套餐', price: 50, description: '深度游戏体验', stock: 2 },
        { id: 4, priceName: '包夜套餐', price: 80, description: '通宵达旦（22:00-08:00）', stock: 1 }
      ]
    },
    getDefaultBookingNotice() {
      return `
        <h3>预约须知</h3>
        <h4>预约规则：</h4>
        <ul>
          <li>请提前30分钟到店，凭预约码入座</li>
          <li>超时15分钟未到店，系统将自动释放机位</li>
          <li>支持提前1小时免费取消预约</li>
        </ul>
        <h4>使用规范：</h4>
        <ul>
          <li>禁止在机位上饮食，可在休息区用餐</li>
          <li>保持安静，避免影响其他用户</li>
          <li>爱护设备，损坏照价赔偿</li>
          <li>禁止安装未经授权的软件</li>
        </ul>
        <h4>服务承诺：</h4>
        <ul>
          <li>24小时营业，全年无休</li>
          <li>免费提供饮用水和基础办公用品</li>
          <li>专业技术支持，设备故障快速处理</li>
          <li>会员积分制度，享受更多优惠</li>
        </ul>
      `
    },
    handleBookSeat() {
      if (!this.isLoggedIn) {
        this.$message.warning('请先登录')
        this.$router.push('/login?redirect=' + this.$route.fullPath)
        return
      }
      this.activeTab = 'tickets'
    },
    selectTimeSlot(timeSlot) {
      // 跳转到预约详情页面，传递时段信息
      const query = {
        priceName: timeSlot.priceName,
        price: timeSlot.price,
        description: timeSlot.description,
        stock: timeSlot.stock
      }
      
      this.$router.push({
        name: 'BookingDetail',
        params: {
          showId: this.show.id,
          timeSlotId: timeSlot.id
        },
        query: query
      })
    }
  }
}
</script>

<style scoped>
.show-detail-container {
  padding: 20px;
  background: var(--ai-gradient-bg);
  min-height: 100vh;
  color: var(--ai-text-primary);
}

.poster-container {
  position: relative;
  margin-bottom: 20px;
}

.poster-image {
  width: 100%;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  border: 2px solid var(--ai-border-primary);
  transition: all 0.3s ease;
}

.poster-image:hover {
  transform: scale(1.02);
  box-shadow: 0 12px 40px var(--ai-shadow-green);
}

.show-status {
  position: absolute;
  top: 15px;
  right: 15px;
  padding: 8px 16px;
  border-radius: 20px;
  color: white;
  font-size: 12px;
  font-weight: 600;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.status-on-sale {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
}

.status-sold-out {
  background: linear-gradient(45deg, #ff4757, #ff6b7a);
}

.status-upcoming {
  background: linear-gradient(45deg, var(--ai-tech-blue), #60a5fa);
}

.status-ended {
  background: linear-gradient(45deg, #6c757d, #8e9aaf);
}

.show-info {
  background: var(--ai-gradient-card);
  padding: 24px;
  border-radius: 16px;
  border: 1px solid var(--ai-border-primary);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
}

.show-title {
  margin-top: 0;
  margin-bottom: 24px;
  font-size: 28px;
  font-weight: 700;
  color: var(--ai-text-primary);
  text-shadow: 0 0 10px var(--ai-shadow-green);
  background: linear-gradient(45deg, var(--ai-nvidia-green), var(--ai-tech-blue));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.show-meta {
  margin-bottom: 24px;
}

.show-meta p {
  margin: 12px 0;
  font-size: 16px;
  color: var(--ai-text-secondary);
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
}

.show-meta p:hover {
  color: var(--ai-nvidia-green);
  text-shadow: 0 0 5px var(--ai-shadow-green);
}

.show-meta p i {
  margin-right: 8px;
  color: var(--ai-nvidia-green);
  font-size: 18px;
}

.show-actions {
  margin-bottom: 20px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.show-actions .el-button {
  border-radius: 25px;
  padding: 12px 24px;
  font-weight: 600;
  transition: all 0.3s ease;
  border: none;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.show-actions .el-button--primary {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
  color: white;
}

.show-actions .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px var(--ai-shadow-green);
}

.show-actions .el-button--info {
  background: linear-gradient(45deg, var(--ai-tech-blue), #60a5fa);
  color: white;
}

.show-actions .el-button--info:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px var(--ai-shadow-blue);
}

.show-actions .el-button--success {
  background: linear-gradient(45deg, #10b981, #34d399);
  color: white;
}

.show-actions .el-button--success:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.4);
}

.show-tabs {
  margin-top: 30px;
  background: var(--ai-gradient-card);
  border-radius: 16px;
  border: 1px solid var(--ai-border-primary);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.show-tabs :deep(.el-tabs__header) {
  background: rgba(118, 185, 0, 0.1);
  margin: 0;
  border-bottom: 2px solid var(--ai-border-primary);
}

.show-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.show-tabs :deep(.el-tabs__item) {
  color: var(--ai-text-secondary);
  font-weight: 600;
  padding: 0 24px;
  height: 50px;
  line-height: 50px;
  border-bottom: 3px solid transparent;
  transition: all 0.3s ease;
}

.show-tabs :deep(.el-tabs__item:hover) {
  color: var(--ai-nvidia-green);
}

.show-tabs :deep(.el-tabs__item.is-active) {
  color: var(--ai-nvidia-green);
  border-bottom-color: var(--ai-nvidia-green);
  text-shadow: 0 0 5px var(--ai-shadow-green);
}

.show-tabs :deep(.el-tabs__content) {
  padding: 24px;
}

.seat-description, .booking-notice {
  line-height: 1.8;
  color: var(--ai-text-primary);
}

.seat-description h3, .booking-notice h3 {
  color: var(--ai-nvidia-green);
  text-shadow: 0 0 5px var(--ai-shadow-green);
  margin-bottom: 16px;
}

.seat-description ul, .booking-notice ul {
  color: var(--ai-text-secondary);
}

.seat-description li, .booking-notice li {
  margin: 8px 0;
  padding-left: 8px;
  border-left: 2px solid var(--ai-nvidia-green);
  margin-left: 16px;
}

.cafe-info {
  line-height: 1.8;
  color: var(--ai-text-primary);
}

.cafe-info h3 {
  margin-top: 0;
  color: var(--ai-nvidia-green);
  text-shadow: 0 0 5px var(--ai-shadow-green);
  font-size: 20px;
}

.cafe-info p {
  margin: 12px 0;
  display: flex;
  align-items: center;
  color: var(--ai-text-secondary);
  transition: all 0.3s ease;
}

.cafe-info p:hover {
  color: var(--ai-nvidia-green);
}

.cafe-info p i {
  margin-right: 8px;
  color: var(--ai-nvidia-green);
  font-size: 16px;
}

.time-slot-list :deep(.el-table) {
  background: rgba(22, 35, 68, 0.3);
  color: #e2e8f0;
  border: 1px solid var(--ai-border-primary);
  border-radius: 12px;
  overflow: hidden;
}

.time-slot-list :deep(.el-table th) {
  background: rgba(118, 185, 0, 0.15) !important;
  color: #a7f3d0 !important;
  border-bottom: 2px solid rgba(118, 185, 0, 0.3) !important;
  font-weight: 600;
  text-shadow: 0 0 3px rgba(118, 185, 0, 0.4);
  padding: 16px 12px;
}

.time-slot-list :deep(.el-table td) {
  border-bottom: 1px solid rgba(118, 185, 0, 0.2) !important;
  background: rgba(10, 14, 23, 0.6) !important;
  color: #cbd5e1 !important;
  padding: 14px 12px;
  transition: all 0.3s ease;
}

.time-slot-list :deep(.el-table tr:hover td) {
  background: rgba(118, 185, 0, 0.08) !important;
  color: #e2e8f0 !important;
}

.time-slot-list :deep(.el-table__empty-block) {
  background: rgba(22, 35, 68, 0.3);
}

.time-slot-list :deep(.el-table__empty-text) {
  color: #94a3b8;
}

/* 价格列特殊样式 */
.time-slot-list :deep(.el-table td:nth-child(2)) {
  color: #fbbf24 !important;
  font-weight: 600;
  font-size: 16px;
}

.time-slot-list :deep(.el-table tr:hover td:nth-child(2)) {
  color: #f59e0b !important;
  text-shadow: 0 0 5px rgba(245, 158, 11, 0.3);
}

/* 状态标签样式优化 */
.time-slot-list :deep(.el-tag--success) {
  background: linear-gradient(45deg, rgba(16, 185, 129, 0.2), rgba(52, 211, 153, 0.2)) !important;
  color: #6ee7b7 !important;
  border: 1px solid rgba(16, 185, 129, 0.4) !important;
  font-weight: 500;
}

.time-slot-list :deep(.el-tag--danger) {
  background: linear-gradient(45deg, rgba(239, 68, 68, 0.2), rgba(248, 113, 113, 0.2)) !important;
  color: #fca5a5 !important;
  border: 1px solid rgba(239, 68, 68, 0.4) !important;
  font-weight: 500;
}

/* 操作按钮样式 */
.time-slot-list :deep(.el-button--primary) {
  background: linear-gradient(45deg, rgba(118, 185, 0, 0.8), rgba(157, 211, 58, 0.8)) !important;
  border: 1px solid rgba(118, 185, 0, 0.6) !important;
  color: #ffffff !important;
  font-weight: 600;
  border-radius: 20px;
  padding: 8px 20px;
  transition: all 0.3s ease;
}

.time-slot-list :deep(.el-button--primary:hover) {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(118, 185, 0, 0.4) !important;
}

.time-slot-list :deep(.el-button--primary:disabled) {
  background: rgba(100, 116, 139, 0.3) !important;
  border: 1px solid rgba(100, 116, 139, 0.2) !important;
  color: #64748b !important;
  transform: none;
  box-shadow: none;
}

.booking-confirm p {
  margin: 12px 0;
  font-size: 16px;
  color: var(--ai-text-primary);
}


</style>