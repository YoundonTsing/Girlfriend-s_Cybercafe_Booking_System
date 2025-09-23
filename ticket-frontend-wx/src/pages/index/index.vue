<template>
  <view class="home-container">
    <!-- 轮播图区域 -->
    <view class="banner-section">
      <swiper 
        class="banner-swiper" 
        :indicator-dots="true" 
        :autoplay="true" 
        :interval="3000" 
        :duration="500"
        indicator-color="rgba(255, 255, 255, 0.5)"
        indicator-active-color="#1890ff"
      >
        <swiper-item v-for="(banner, index) in banners" :key="index">
          <image 
            :src="banner.image" 
            class="banner-image" 
            mode="aspectFill"
            @click="handleBannerClick(banner)"
          />
          <view class="banner-overlay">
            <view class="banner-title">{{ banner.title }}</view>
            <view class="banner-subtitle">{{ banner.subtitle }}</view>
          </view>
        </swiper-item>
      </swiper>
    </view>

    <!-- 快捷入口 -->
    <view class="quick-entry-section">
      <view class="section-title">快捷服务</view>
      <view class="quick-entry-grid">
        <view 
          class="quick-entry-item" 
          v-for="(item, index) in quickEntries" 
          :key="index"
          @click="handleQuickEntry(item)"
        >
          <view class="entry-icon">
            <Icon :name="item.iconName" :size="24" color="#1890ff" />
          </view>
          <view class="entry-text">{{ item.text }}</view>
        </view>
      </view>
    </view>

    <!-- 热门演出 -->
    <view class="hot-shows-section">
      <view class="section-header">
        <view class="section-title">热门演出</view>
        <view class="more-link" @click="goToShowList">更多 ></view>
      </view>
      <scroll-view class="hot-shows-scroll" scroll-x="true" show-scrollbar="false">
        <view class="hot-shows-list">
          <view 
            class="show-card" 
            v-for="(show, index) in hotShows" 
            :key="index"
            @click="goToShowDetail(show.id)"
          >
            <image :src="show.poster" class="show-poster" mode="aspectFill" />
            <view class="show-info">
              <view class="show-title">{{ show.title }}</view>
              <view class="show-venue">{{ show.venue }}</view>
              <view class="show-date">{{ formatDate(show.date) }}</view>
              <view class="show-price">¥{{ show.minPrice }}起</view>
            </view>
            <view class="show-status" :class="getStatusClass(show.status)">
              {{ getStatusText(show.status) }}
            </view>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 推荐演出 -->
    <view class="recommend-section">
      <view class="section-header">
        <view class="section-title">为你推荐</view>
        <view class="more-link" @click="goToShowList">更多 ></view>
      </view>
      <view class="recommend-list">
        <view 
          class="recommend-item" 
          v-for="(show, index) in recommendShows" 
          :key="index"
          @click="goToShowDetail(show.id)"
        >
          <image :src="show.poster" class="recommend-poster" mode="aspectFill" />
          <view class="recommend-info">
            <view class="recommend-title">{{ show.title }}</view>
            <view class="recommend-desc">{{ show.description }}</view>
            <view class="recommend-meta">
              <view class="meta-item">
                <text class="meta-label">时间:</text>
                <text class="meta-value">{{ formatDate(show.date) }}</text>
              </view>
              <view class="meta-item">
                <text class="meta-label">场馆:</text>
                <text class="meta-value">{{ show.venue }}</text>
              </view>
              <view class="meta-item">
                <text class="meta-label">价格:</text>
                <text class="meta-price">¥{{ show.minPrice }}起</text>
              </view>
            </view>
          </view>
          <view class="recommend-status" :class="getStatusClass(show.status)">
            {{ getStatusText(show.status) }}
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getShowPoster } from '@/utils/imageMapping'
import Icon from '@/components/Icon.vue'

export default {
  name: 'HomePage',
  components: {
    Icon
  },
  setup() {
    const userStore = useUserStore()
    
    // 轮播图数据
    const banners = ref([
      {
        id: 1,
        image: getShowPoster('热门音乐会'),
        title: '热门音乐会',
        subtitle: '享受音乐的魅力',
        link: '/pages/show/detail?id=1'
      },
      {
        id: 2,
        image: getShowPoster('经典话剧'),
        title: '经典话剧',
        subtitle: '感受戏剧的力量',
        link: '/pages/show/detail?id=2'
      },
      {
        id: 3,
        image: getShowPoster('舞蹈演出'),
        title: '舞蹈演出',
        subtitle: '优雅的艺术表演',
        link: '/pages/show/detail?id=3'
      },
      {
        id: 4,
        image: getShowPoster('体育赛事'),
        title: '精彩体育赛事',
        subtitle: '激情运动时刻',
        link: '/pages/show/detail?id=4'
      },
      {
        id: 5,
        image: getShowPoster('电影放映'),
        title: '热门电影',
        subtitle: '视觉盛宴体验',
        link: '/pages/show/detail?id=5'
      }
    ])
    
    // 快捷入口数据
    const quickEntries = ref([
      {
        iconName: 'music',
        text: '音乐会',
        type: 'category',
        value: 'music'
      },
      {
        iconName: 'theater',
        text: '话剧',
        type: 'category',
        value: 'drama'
      },
      {
        iconName: 'sport',
        text: '舞蹈',
        type: 'category',
        value: 'dance'
      },
      {
        iconName: 'game',
        text: '体育',
        type: 'category',
        value: 'sports'
      },
      {
        iconName: 'file',
        text: '我的订单',
        type: 'page',
        value: '/pages/order/list'
      },
      {
        iconName: 'ticket',
        text: '电子票',
        type: 'page',
        value: '/pages/ticket/list'
      },
      {
        iconName: 'heart',
        text: '我的收藏',
        type: 'page',
        value: '/pages/user/favorite'
      },
      {
        iconName: 'phone',
        text: '客服',
        type: 'action',
        value: 'contact'
      }
    ])
    
    // 热门演出数据
    const hotShows = ref([
      {
        id: 1,
        title: '周杰伦演唱会',
        poster: getShowPoster('周杰伦演唱会'),
        venue: '鸟巢体育场',
        date: '2024-06-15',
        minPrice: 380,
        status: 'on_sale'
      },
      {
        id: 2,
        title: '《雷雨》话剧',
        poster: getShowPoster('雷雨话剧'),
        venue: '人民艺术剧院',
        date: '2024-06-20',
        minPrice: 180,
        status: 'on_sale'
      },
      {
        id: 3,
        title: 'NBA中国赛',
        poster: getShowPoster('NBA体育'),
        venue: '五棵松体育馆',
        date: '2024-07-01',
        minPrice: 680,
        status: 'pre_sale'
      }
    ])
    
    // 推荐演出数据
    const recommendShows = ref([
      {
        id: 4,
        title: '《天鹅湖》芭蕾舞',
        poster: getShowPoster('天鹅湖舞蹈'),
        venue: '国家大剧院',
        date: '2024-06-25',
        minPrice: 280,
        status: 'on_sale'
      },
      {
        id: 5,
        title: '德云社相声专场',
        poster: getShowPoster('德云社相声'),
        venue: '德云社剧场',
        date: '2024-06-30',
        minPrice: 120,
        status: 'on_sale'
      },
      {
        id: 6,
        title: '《阿凡达》IMAX',
        poster: getShowPoster('阿凡达电影'),
        venue: 'IMAX影城',
        date: '2024-07-05',
        minPrice: 80,
        status: 'coming_soon'
      }
    ])

    // 加载数据
    const loadData = async () => {
      try {
        // 模拟API调用
        await loadHotShows()
        await loadRecommendShows()
      } catch (error) {
        console.error('加载数据失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      }
    }
    
    // 加载热门演出
    const loadHotShows = async () => {
      // 模拟API数据
      hotShows.value = [
        {
          id: 1,
          title: '经典音乐会',
          poster: '/static/images/1.png',
          venue: '大剧院',
          date: '2024-02-15 19:30',
          minPrice: 180,
          status: 'on_sale'
        },
        {
          id: 2,
          title: '话剧《雷雨》',
          poster: '/static/images/2.png',
          venue: '话剧院',
          date: '2024-02-20 19:30',
          minPrice: 120,
          status: 'on_sale'
        },
        {
          id: 3,
          title: '芭蕾舞演出',
          poster: '/static/images/3.png',
          venue: '艺术中心',
          date: '2024-02-25 19:30',
          minPrice: 200,
          status: 'sold_out'
        }
      ]
    }
    
    // 加载推荐演出
    const loadRecommendShows = async () => {
      // 模拟API数据
      recommendShows.value = [
        {
          id: 4,
          title: '新年音乐会',
          poster: '/static/images/4.png',
          description: '庆祝新年的精彩音乐演出，汇聚多位知名音乐家',
          venue: '音乐厅',
          date: '2024-03-01 19:30',
          minPrice: 150,
          status: 'on_sale'
        },
        {
          id: 5,
          title: '儿童剧《小王子》',
          poster: '/static/images/5.png',
          description: '适合全家观看的温馨儿童剧，带来欢乐与感动',
          venue: '儿童剧院',
          date: '2024-03-05 15:00',
          minPrice: 80,
          status: 'on_sale'
        }
      ]
    }

    // 轮播图点击
    const handleBannerClick = (banner) => {
      if (banner.link) {
        uni.navigateTo({
          url: banner.link
        })
      }
    }
    
    // 快捷入口点击
    const handleQuickEntry = (item) => {
      switch (item.type) {
        case 'category':
          uni.navigateTo({
            url: `/pages/show/list?category=${item.value}`
          })
          break
        case 'page':
          if (item.value.includes('/pages/order/list') || item.value.includes('/pages/ticket/list')) {
            // 需要登录的页面
            if (!userStore.isLoggedIn) {
              uni.navigateTo({
                url: '/pages/login/index'
              })
              return
            }
          }
          uni.navigateTo({
            url: item.value
          })
          break
        case 'action':
          if (item.value === 'contact') {
            uni.showModal({
              title: '客服联系方式',
              content: '客服电话：400-123-4567\n工作时间：9:00-18:00',
              showCancel: false
            })
          }
          break
      }
    }
    
    // 跳转到演出列表
    const goToShowList = () => {
      uni.switchTab({
        url: '/pages/show/list'
      })
    }
    
    // 跳转到演出详情
    const goToShowDetail = (showId) => {
      uni.navigateTo({
        url: `/pages/show/detail?id=${showId}`
      })
    }

    // 获取状态样式类
    const getStatusClass = (status) => {
      const statusMap = {
        'on_sale': 'status-on-sale',
        'sold_out': 'status-sold-out',
        'upcoming': 'status-upcoming',
        'ended': 'status-ended'
      }
      return statusMap[status] || 'status-on-sale'
    }
    
    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        'on_sale': '在售',
        'sold_out': '售罄',
        'upcoming': '即将开售',
        'ended': '已结束'
      }
      return statusMap[status] || '在售'
    }
    
    // 格式化日期
    const formatDate = (dateStr) => {
      // 将 "2024-02-15 19:30" 格式转换为 iOS 兼容的格式
      const isoDateStr = dateStr.replace(' ', 'T')
      const date = new Date(isoDateStr)
      
      // 检查日期是否有效
      if (isNaN(date.getTime())) {
        console.warn('Invalid date format:', dateStr)
        return dateStr // 返回原始字符串作为备用
      }
      
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${month}-${day} ${hours}:${minutes}`
    }
    
    // 组件挂载时执行
    onMounted(() => {
      loadData()
    })
    
    return {
       banners,
       quickEntries,
       hotShows,
       recommendShows,
       handleBannerClick,
       handleQuickEntry,
       goToShowList,
       goToShowDetail,
       getStatusClass,
       getStatusText,
       formatDate
     }
  }
}
</script>

<style scoped>
.home-container {
  padding: 20rpx;
  background-color: #f8f8f8;
}

.header {
  text-align: center;
  margin-bottom: 40rpx;
}

.title {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.subtitle {
  display: block;
  font-size: 28rpx;
  color: #666;
}

/* 轮播图样式 */
.banner {
  height: 400rpx;
  margin-bottom: 40rpx;
  border-radius: 20rpx;
  overflow: hidden;
}

.banner-item {
  position: relative;
  width: 100%;
  height: 100%;
}

.banner-image {
  width: 100%;
  height: 100%;
}

.banner-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
  padding: 40rpx 30rpx 30rpx;
  color: white;
}

.banner-title {
  display: block;
  font-size: 32rpx;
  font-weight: bold;
  margin-bottom: 10rpx;
}

.banner-desc {
  display: block;
  font-size: 24rpx;
  opacity: 0.9;
}

/* 区块样式 */
.section {
  margin-bottom: 40rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
  padding: 0 20rpx;
}

.section-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.more {
  font-size: 28rpx;
  color: #007aff;
}

/* 演出网格样式 */
.show-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  padding: 0 20rpx;
}

.show-item {
  background: white;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);
}

.show-card {
  width: 100%;
}

.show-image {
  position: relative;
  width: 100%;
  height: 200rpx;
}

.image {
  width: 100%;
  height: 100%;
}

.show-status {
  position: absolute;
  top: 20rpx;
  right: 20rpx;
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
  font-size: 20rpx;
  color: white;
}

.status-on-sale {
  background-color: #52c41a;
}

.status-sold-out {
  background-color: #ff4d4f;
}

.status-upcoming {
  background-color: #1890ff;
}

.status-ended {
  background-color: #d9d9d9;
  color: #666;
}

.show-info {
  padding: 20rpx;
}

.show-title {
  display: block;
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 10rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.show-time,
.show-venue {
  display: block;
  font-size: 24rpx;
  color: #666;
  margin-bottom: 8rpx;
}

.show-price {
  display: block;
  font-size: 28rpx;
  color: #ff4d4f;
  font-weight: bold;
}

/* 分类网格样式 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20rpx;
  padding: 0 20rpx;
}

.category-item {
  background: white;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);
}

.category-card {
  padding: 40rpx 20rpx;
  text-align: center;
}

.category-icon {
  margin-bottom: 20rpx;
}

.category-icon .iconfont {
  font-size: 60rpx;
  color: #007aff;
}

.category-name {
  display: block;
  font-size: 24rpx;
  color: #333;
}
</style>