<template>
  <div class="home-container">
    <h1>欢迎来到自由点网咖预约系统</h1>
    <p>为您提供便捷的网咖机位预约服务</p>
    
    <!-- 轮播图 -->
    <el-carousel 
      :interval="4000" 
      :type="isMobile ? '' : 'card'" 
      :height="isMobile ? '250px' : '400px'" 
      class="banner mobile-container" 
      v-if="bannerList.length > 0"
    >
      <el-carousel-item v-for="item in bannerList" :key="item.id">
        <div class="banner-item mobile-card" @click="goToDetail(item.id)">
          <img :src="item.posterUrl" class="banner-image">
          <div class="banner-info">
            <h2 class="mobile-title">{{ item.title }}</h2>
            <p class="mobile-text">{{ item.showTime }} | {{ item.venueName }}</p>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>

    <!-- 热门机位 -->
    <div class="section mobile-container">
      <div class="section-header">
        <h2 class="mobile-title">热门机位</h2>
        <router-link to="/show/list" class="more touch-button">查看更多</router-link>
      </div>
      <div class="mobile-grid grid-4 show-list">
        <div v-for="show in hotShows" :key="show.id" class="show-item">
          <el-card :body-style="{ padding: '0px' }" shadow="hover" @click="goToDetail(show.id)" class="mobile-card">
            <div class="show-image">
              <img :src="show.posterUrl" class="image">
              <div class="show-status" :class="getStatusClass(show.status)">
                {{ getStatusText(show.status) }}
              </div>
            </div>
            <div class="show-info">
              <h3 class="show-title mobile-subtitle">{{ show.title }}</h3>
              <div class="show-time mobile-text">{{ show.showTime }}</div>
              <div class="show-venue mobile-text">{{ show.venueName }}</div>
              <div class="show-price">¥{{ show.minPrice }} 起</div>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 即将开放 -->
    <div class="section mobile-container">
      <div class="section-header">
        <h2 class="mobile-title">即将开放</h2>
        <router-link to="/show/list?type=UPCOMING" class="more touch-button">查看更多</router-link>
      </div>
      <div class="mobile-grid grid-4 show-list">
        <div v-for="show in upcomingShows" :key="show.id" class="show-item">
          <el-card :body-style="{ padding: '0px' }" shadow="hover" @click="goToDetail(show.id)" class="mobile-card">
            <div class="show-image">
              <img :src="show.posterUrl" class="image">
              <div class="show-status status-upcoming">
                即将开售
              </div>
            </div>
            <div class="show-info">
              <h3 class="show-title mobile-subtitle">{{ show.title }}</h3>
              <div class="show-time mobile-text">{{ show.showTime }}</div>
              <div class="show-venue mobile-text">{{ show.venueName }}</div>
              <div class="show-price">¥{{ show.minPrice }} 起</div>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 机位分类 -->
    <div class="section mobile-container">
      <div class="section-header">
        <h2 class="mobile-title">机位分类</h2>
      </div>
      <div class="mobile-grid grid-3 category-list">
        <div v-for="(category, index) in categories" :key="index" class="category-item">
          <el-card shadow="hover" class="category-card mobile-card touch-button" @click="goToCategory(category.type)">
            <div class="category-icon">
              <i :class="category.icon"></i>
            </div>
            <div class="category-name mobile-text">{{ category.name }}</div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getShowList } from '@/api/show'
import { ElMessage } from 'element-plus'

defineOptions({
  name: 'Home'
})

const router = useRouter()

// 响应式数据
const bannerList = ref([])
const hotShows = ref([])
const upcomingShows = ref([])

// 机位分类常量
const categories = [
  { name: '新客电竞机位', icon: 'el-icon-monitor', type: 'NEWBIE' },
  { name: '初级电竞机位', icon: 'el-icon-cpu', type: 'INTERMEDIATE' },
  { name: '高级电竞机位', icon: 'el-icon-trophy', type: 'ADVANCED' },
  { name: '包厢电竞机位', icon: 'el-icon-house', type: 'VIP_ROOM' },
  { name: 'SVIP电竞机位', icon: 'el-icon-star-on', type: 'SVIP' }
]

// 获取轮播图演出
const fetchBannerShows = async () => {
  try {
    const response = await getShowList({ page: 1, size: 5, sort: 'hot' })
    bannerList.value = response.data.records
  } catch (error) {
    console.error('获取轮播图演出失败', error)
    ElMessage.error('获取轮播图演出失败')
  }
}

// 获取热门演出
const fetchHotShows = async () => {
  try {
    const response = await getShowList({ page: 1, size: 8, sort: 'hot', status: 'ON_SALE' })
    hotShows.value = response.data.records
  } catch (error) {
    console.error('获取热门演出失败', error)
    ElMessage.error('获取热门演出失败')
  }
}

// 获取即将开售演出
const fetchUpcomingShows = async () => {
  try {
    const response = await getShowList({ page: 1, size: 8, status: 'UPCOMING' })
    upcomingShows.value = response.data.records
  } catch (error) {
    console.error('获取即将开售演出失败', error)
    ElMessage.error('获取即将开售演出失败')
  }
}

// 跳转到演出详情
const goToDetail = (id) => {
  router.push(`/show/detail/${id}`)
}

// 跳转到分类列表
const goToCategory = (type) => {
  router.push(`/show/list?type=${type}`)
}

// 获取状态样式类
const getStatusClass = (status) => {
  const statusMap = {
    'ON_SALE': 'status-on-sale',
    'SOLD_OUT': 'status-sold-out',
    'UPCOMING': 'status-upcoming',
    'ENDED': 'status-ended'
  }
  return statusMap[status] || ''
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'ON_SALE': '售票中',
    'SOLD_OUT': '售罄',
    'UPCOMING': '即将开售',
    'ENDED': '已结束'
  }
  return statusMap[status] || ''
}

// 移动端检测
const isMobile = ref(false)

const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

// 组件挂载时执行
onMounted(() => {
  fetchBannerShows()
  fetchHotShows()
  fetchUpcomingShows()
  
  // 检测移动端
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

// 组件卸载时清理
onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
.home-container {
  padding: 20px;
  position: relative;
  z-index: 2;
}

.home-container h1 {
  text-align: center;
  color: var(--ai-text-primary);
  text-shadow: 0 0 15px var(--ai-nvidia-green), 0 0 30px rgba(118, 185, 0, 0.5);
  font-size: 2.5rem;
  margin-bottom: 30px;
  background: linear-gradient(45deg, #ffffff, var(--ai-nvidia-green));
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: titleGlow 3s ease-in-out infinite alternate;
}

.home-container p {
  text-align: center;
  color: var(--ai-text-secondary);
  margin-bottom: 40px;
}

@keyframes titleGlow {
  0% {
    text-shadow: 0 0 15px var(--ai-nvidia-green), 0 0 30px rgba(118, 185, 0, 0.5);
  }
  100% {
    text-shadow: 0 0 25px var(--ai-nvidia-green), 0 0 40px rgba(118, 185, 0, 0.8);
  }
}

.banner {
  margin-bottom: 40px;
}

.banner-item {
  position: relative;
  height: 100%;
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--ai-border-primary);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  transition: all 0.3s ease;
}

.banner-item:hover {
  border-color: var(--ai-nvidia-green);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5), 0 0 20px var(--ai-shadow-green);
  transform: translateY(-5px);
}

.banner-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 12px;
}

/* 轮播图SVG响应式优化 */
.banner-image[src$=".svg"] {
  object-fit: contain;
  object-position: center;
  background: linear-gradient(135deg, rgba(16, 20, 33, 0.95), rgba(28, 35, 56, 0.9));
  padding: 10px;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px;
  background: linear-gradient(to top, rgba(10, 14, 23, 0.9), rgba(10, 14, 23, 0.4), transparent);
  color: var(--ai-text-primary);
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
  backdrop-filter: blur(5px);
}

.banner-info h2 {
  margin: 0 0 10px 0;
  color: var(--ai-text-primary);
  text-shadow: 0 0 8px var(--ai-nvidia-green);
}

.banner-info p {
  margin: 0;
  color: var(--ai-text-accent);
}

.section {
  margin-bottom: 40px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px 0;
  border-bottom: 1px solid var(--ai-border-primary);
  position: relative;
}

.section-header::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 60px;
  height: 2px;
  background: linear-gradient(90deg, var(--ai-nvidia-green), var(--ai-tech-blue));
  animation: sectionGlow 2s ease-in-out infinite alternate;
}

@keyframes sectionGlow {
  0% {
    box-shadow: 0 0 5px var(--ai-nvidia-green);
  }
  100% {
    box-shadow: 0 0 15px var(--ai-nvidia-green);
  }
}

.section-header h2 {
  margin: 0;
  color: var(--ai-text-primary);
  text-shadow: 0 0 8px var(--ai-tech-blue);
  font-size: 1.5rem;
}

.more {
  color: var(--ai-tech-blue);
  text-decoration: none;
  text-shadow: 0 0 5px var(--ai-shadow-blue);
  transition: all 0.3s ease;
}

.more:hover {
  color: #66d9ff;
  text-shadow: 0 0 10px var(--ai-shadow-blue);
  transform: translateX(5px);
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
  padding: 6px 12px;
  border-radius: 20px;
  color: white;
  font-size: 12px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(5px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.status-on-sale {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
  box-shadow: 0 0 10px var(--ai-shadow-green);
}

.status-sold-out {
  background: linear-gradient(45deg, #ff4757, #ff6b7a);
  box-shadow: 0 0 10px rgba(255, 71, 87, 0.4);
}

.status-upcoming {
  background: linear-gradient(45deg, var(--ai-tech-blue), #66d9ff);
  box-shadow: 0 0 10px var(--ai-shadow-blue);
}

.status-ended {
  background: linear-gradient(45deg, #57606f, #747d8c);
  box-shadow: 0 0 10px rgba(87, 96, 111, 0.4);
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

.category-list {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.category-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--ai-gradient-card) !important;
  border: 1px solid var(--ai-border-primary) !important;
  border-radius: 16px !important;
  padding: 30px 20px !important;
  position: relative;
  overflow: hidden;
}

.category-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    linear-gradient(45deg, transparent 48%, rgba(118, 185, 0, 0.05) 49%, rgba(118, 185, 0, 0.05) 51%, transparent 52%),
    linear-gradient(-45deg, transparent 48%, rgba(0, 179, 255, 0.05) 49%, rgba(0, 179, 255, 0.05) 51%, transparent 52%);
  background-size: 20px 20px;
  opacity: 0.3;
  pointer-events: none;
}

.category-card:hover {
  transform: translateY(-8px);
  border-color: var(--ai-nvidia-green) !important;
  box-shadow: 
    0 12px 40px rgba(0, 0, 0, 0.4),
    0 0 20px var(--ai-shadow-green) !important;
}

.category-card:hover::before {
  opacity: 0.6;
  animation: categoryPulse 1s ease-in-out;
}

@keyframes categoryPulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.category-icon {
  font-size: 48px;
  margin-bottom: 15px;
  color: var(--ai-tech-blue);
  text-shadow: 0 0 15px var(--ai-shadow-blue);
  transition: all 0.3s ease;
  position: relative;
  z-index: 2;
}

.category-card:hover .category-icon {
  color: var(--ai-nvidia-green);
  text-shadow: 0 0 20px var(--ai-shadow-green);
  transform: scale(1.2);
}

.category-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--ai-text-primary);
  text-shadow: 0 0 5px rgba(255, 255, 255, 0.3);
  position: relative;
  z-index: 2;
}

.category-card:hover .category-name {
  color: var(--ai-nvidia-green);
  text-shadow: 0 0 8px var(--ai-shadow-green);
}

/* 移动端样式适配 */
@media (max-width: 768px) {
  .home-container {
    padding: var(--mobile-spacing-base);
  }
  
  .home-container h1 {
    font-size: var(--mobile-font-2xl);
    margin-bottom: var(--mobile-spacing-lg);
  }
  
  .home-container p {
    font-size: var(--mobile-font-base);
    margin-bottom: var(--mobile-spacing-xl);
  }
  
  .section {
    margin-bottom: var(--mobile-spacing-xl);
  }
  
  .section-header h2 {
    font-size: var(--mobile-font-xl);
  }
  
  .more {
    font-size: var(--mobile-font-sm);
    padding: var(--mobile-spacing-sm) var(--mobile-spacing-base);
  }
  
  .show-item {
    margin-bottom: var(--mobile-spacing-base);
  }
  
  .show-image {
    height: 150px;
  }
  
  .show-info {
    padding: var(--mobile-spacing-base);
  }
  
  .show-title {
    font-size: var(--mobile-font-base);
    margin-bottom: var(--mobile-spacing-sm);
  }
  
  .show-time,
  .show-venue {
    font-size: var(--mobile-font-sm);
    margin-top: var(--mobile-spacing-xs);
  }
  
  .show-price {
    font-size: var(--mobile-font-lg);
    margin-top: var(--mobile-spacing-sm);
  }
  
  .category-card {
    padding: var(--mobile-spacing-base) !important;
  }
  
  .category-icon {
    font-size: 32px;
    margin-bottom: var(--mobile-spacing-sm);
  }
  
  .category-name {
    font-size: var(--mobile-font-sm);
  }
  
  .banner-info h2 {
    font-size: var(--mobile-font-lg);
    margin-bottom: var(--mobile-spacing-sm);
  }
  
  .banner-info p {
    font-size: var(--mobile-font-sm);
  }
}

@media (max-width: 480px) {
  .home-container {
    padding: var(--mobile-spacing-sm);
  }
  
  .home-container h1 {
    font-size: var(--mobile-font-xl);
    margin-bottom: var(--mobile-spacing-base);
  }
  
  .section {
    margin-bottom: var(--mobile-spacing-lg);
  }
  
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--mobile-spacing-sm);
  }
  
  .section-header h2 {
    font-size: var(--mobile-font-lg);
  }
  
  .more {
    align-self: flex-end;
  }
  
  .show-image {
    height: 120px;
  }
  
  .category-icon {
    font-size: 24px;
  }
  
  .banner-info {
    padding: var(--mobile-spacing-sm);
  }
  
  .banner-info h2 {
    font-size: var(--mobile-font-base);
  }
  
  .banner-info p {
    font-size: var(--mobile-font-xs);
  }
}
</style>