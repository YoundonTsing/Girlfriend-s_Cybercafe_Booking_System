<template>
  <div class="show-list-container">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-input
          placeholder="搜索机位类型"
          v-model="searchQuery"
          class="search-input"
          @keyup.enter.native="handleSearch"
        >
          <el-button slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
        </el-input>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="filter-row">
      <el-col :span="24">
        <el-radio-group v-model="showType" @change="handleTypeChange">
          <el-radio-button label="">全部机位</el-radio-button>
          <el-radio-button label="NEWBIE">新客电竞机位</el-radio-button>
          <el-radio-button label="INTERMEDIATE">中级电竞机位</el-radio-button>
          <el-radio-button label="ADVANCED">高级电竞机位</el-radio-button>
          <el-radio-button label="VIP_ROOM">包厢电竞机位</el-radio-button>
          <el-radio-button label="SVIP">SVIP电竞机位</el-radio-button>
        </el-radio-group>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="show-list">
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="show in showList" :key="show.id" class="show-item">
        <el-card :body-style="{ padding: '0px' }" shadow="hover" @click.native="goToDetail(show.id)">
          <div class="show-image">
            <img :src="show.posterUrl" class="image">
            <div class="show-status" :class="getStatusClass(show.status)">
              {{ getStatusText(show.status) }}
            </div>
          </div>
          <div class="show-info">
            <h3 class="show-title">{{ show.title }}</h3>
            <div class="show-time">营业时间：10:00-24:00</div>
            <div class="show-venue">{{ show.venueName }}</div>
            <div class="show-price">¥{{ show.minPrice }}/小时 起</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="currentPage"
      :page-sizes="[12, 24, 36, 48]"
      :page-size="pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      class="pagination"
    >
    </el-pagination>
  </div>
</template>

<script>
import { useShowStore } from '@/stores/show'

export default {
  name: 'ShowList',
  setup() {
    const showStore = useShowStore()
    return {
      showStore
    }
  },
  data() {
    return {
      searchQuery: '',
      showType: '',
      currentPage: 1,
      pageSize: 12
    }
  },
  computed: {
    showList() {
      return this.showStore.showList
    },
    total() {
      return this.showStore.total
    },
    loading() {
      return this.showStore.loading
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
        keyword: this.searchQuery,
        type: this.showType
      }
      this.showStore.fetchShowList(params)
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchData()
    },
    handleTypeChange() {
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
    goToDetail(id) {
      // 根据showId计算对应的sessionId (show_id + 9 = session_id)
      const sessionId = parseInt(id) + 9
      this.$router.push(`/seat/selection/${id}/${sessionId}`)
    },
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
          return '可预约'
        case 'SOLD_OUT':
          return '已满'
        case 'UPCOMING':
          return '即将开放'
        case 'ENDED':
          return '维护中'
        default:
          return ''
      }
    }
  }
}
</script>

<style scoped>
.show-list-container {
  padding: 20px;
}

.search-input {
  margin-bottom: 20px;
}

/* 科技主题搜索框样式 */
.search-input :deep(.el-input__inner) {
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  color: var(--ai-text-primary);
  border-radius: 25px;
  padding-left: 20px;
}

.search-input :deep(.el-input__inner:focus) {
  border-color: var(--ai-nvidia-green);
  box-shadow: 0 0 0 2px var(--ai-shadow-green);
}

.search-input :deep(.el-input-group__append) {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
  border: 1px solid var(--ai-nvidia-green);
  border-radius: 0 25px 25px 0;
}

.search-input :deep(.el-input-group__append .el-button) {
  background: transparent;
  border: none;
  color: #ffffff;
}

.search-input :deep(.el-input-group__append .el-button:hover) {
  background: rgba(255, 255, 255, 0.2);
}

.filter-row {
  margin-bottom: 20px;
}

.show-list {
  margin-bottom: 20px;
}

.show-item {
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.show-item:hover {
  transform: translateY(-8px);
}

.show-item :deep(.el-card) {
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.show-item:hover :deep(.el-card) {
  border-color: var(--ai-nvidia-green);
  box-shadow: 
    0 12px 40px rgba(0, 0, 0, 0.4),
    0 0 20px var(--ai-shadow-green);
}

.show-image {
  position: relative;
  height: 200px;
  overflow: hidden;
  border-radius: 8px 8px 0 0;
}

.image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

/* SVG图片响应式优化 */
.image[src$=".svg"] {
  object-fit: contain;
  object-position: center;
  background: linear-gradient(135deg, rgba(16, 20, 33, 0.95), rgba(28, 35, 56, 0.9));
  padding: 5px;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.show-item:hover .image {
  transform: scale(1.05);
}

.show-status {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 5px 10px;
  border-radius: 4px;
  color: white;
  font-size: 12px;
}

.status-on-sale {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
  box-shadow: 0 0 8px var(--ai-shadow-green);
}

.status-sold-out {
  background: linear-gradient(45deg, #ff4757, #ff6b7a);
  box-shadow: 0 0 8px rgba(255, 71, 87, 0.4);
}

.status-upcoming {
  background: linear-gradient(45deg, var(--ai-tech-blue), #66d9ff);
  box-shadow: 0 0 8px var(--ai-shadow-blue);
}

.status-ended {
  background: linear-gradient(45deg, #57606f, #747d8c);
  box-shadow: 0 0 8px rgba(87, 96, 111, 0.4);
}

.show-info {
  padding: 16px;
  background: var(--ai-gradient-card);
  border-radius: 0 0 8px 8px;
}

.show-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ai-text-primary);
  text-shadow: 0 0 5px rgba(255, 255, 255, 0.3);
}

.show-time, .show-venue {
  font-size: 14px;
  color: var(--ai-text-secondary);
  margin-top: 5px;
}

.show-price {
  margin-top: 12px;
  font-size: 18px;
  color: var(--ai-nvidia-green);
  font-weight: bold;
  text-shadow: 0 0 8px var(--ai-shadow-green);
}

.pagination {
  text-align: center;
  margin-top: 20px;
}

/* 将蓝色选择按钮改为英伟达绿色 */
.filter-row :deep(.el-radio-button__inner) {
  border-color: var(--ai-border-primary);
  background: var(--ai-gradient-card);
  color: var(--ai-text-primary);
  transition: all 0.3s ease;
}

.filter-row :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
  border-color: var(--ai-nvidia-green);
  color: #ffffff;
  box-shadow: 0 0 10px var(--ai-shadow-green);
  text-shadow: 0 0 3px rgba(0, 0, 0, 0.3);
}

.filter-row :deep(.el-radio-button__inner:hover) {
  border-color: var(--ai-nvidia-green);
  color: var(--ai-nvidia-green);
  background: rgba(118, 185, 0, 0.1);
  box-shadow: 0 0 5px var(--ai-shadow-green);
}

.filter-row :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-top-left-radius: 20px;
  border-bottom-left-radius: 20px;
}

.filter-row :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-top-right-radius: 20px;
  border-bottom-right-radius: 20px;
}
</style>