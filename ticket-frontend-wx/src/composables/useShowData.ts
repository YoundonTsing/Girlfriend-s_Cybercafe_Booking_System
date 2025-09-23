/**
 * 演出/机位数据相关的业务逻辑组合函数
 * 平台无关，可在Web和小程序中复用
 */
import { ref, computed } from 'vue'
import { useShowStore } from '@/stores'
import type { ShowListParams } from '@/types'

export function useShowData() {
  const showStore = useShowStore()
  
  // 响应式状态
  const loading = ref(false)
  const error = ref<string | null>(null)
  const currentPage = ref(1)
  const pageSize = ref(10)
  
  // 计算属性
  const showList = computed(() => showStore.showList)
  const currentShow = computed(() => showStore.currentShow)
  const hotShows = computed(() => showStore.hotShows)
  const upcomingShows = computed(() => showStore.upcomingShows)
  const total = computed(() => showStore.total)
  const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
  
  // 获取演出列表
  const fetchShowList = async (params?: ShowListParams) => {
    loading.value = true
    error.value = null
    
    try {
      const requestParams = {
        page: currentPage.value,
        limit: pageSize.value,
        ...params
      }
      
      await showStore.fetchShowList(requestParams)
      return true
    } catch (err: any) {
      error.value = err.message || '获取演出列表失败'
      return false
    } finally {
      loading.value = false
    }
  }
  
  // 获取演出详情
  const fetchShowDetail = async (id: string | number) => {
    loading.value = true
    error.value = null
    
    try {
      await showStore.fetchShowDetail(String(id))
      return true
    } catch (err: any) {
      error.value = err.message || '获取演出详情失败'
      return false
    } finally {
      loading.value = false
    }
  }
  
  // 搜索演出
  const searchShows = async (keyword: string, filters?: { category?: string; city?: string }) => {
    return fetchShowList({
      keyword,
      ...filters
    })
  }
  
  // 切换页面
  const changePage = async (page: number, params?: ShowListParams) => {
    currentPage.value = page
    return fetchShowList(params)
  }
  
  // 重置分页
  const resetPagination = () => {
    currentPage.value = 1
  }
  
  // 清除错误
  const clearError = () => {
    error.value = null
  }
  
  // 清除当前演出
  const clearCurrentShow = () => {
    showStore.clearCurrentShow()
  }
  
  return {
    // 状态
    loading,
    error,
    currentPage,
    pageSize,
    
    // 计算属性
    showList,
    currentShow,
    hotShows,
    upcomingShows,
    total,
    totalPages,
    
    // 方法
    fetchShowList,
    fetchShowDetail,
    searchShows,
    changePage,
    resetPagination,
    clearError,
    clearCurrentShow
  }
}