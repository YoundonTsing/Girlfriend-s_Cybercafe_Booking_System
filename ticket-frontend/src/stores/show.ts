import { defineStore } from 'pinia'
import { getShowList, getShowDetail } from '@/api/show'
import type { Show, PageResponse } from '@/types'

interface ShowState {
  showList: Show[]
  currentShow: Show | null
  total: number
  loading: boolean
}

export const useShowStore = defineStore('show', {
  state: (): ShowState => ({
    showList: [],
    currentShow: null,
    total: 0,
    loading: false
  }),

  getters: {
    hotShows: (state): Show[] => state.showList.filter(show => show.isHot),
    upcomingShows: (state): Show[] => state.showList.filter(show => show.status === 'UPCOMING')
  },

  actions: {
    // 获取演出列表
    async fetchShowList(params: Record<string, any> = {}): Promise<PageResponse<Show>> {
      this.loading = true
      try {
        const response = await getShowList(params)
        const { data } = response
        this.showList = data.records || []
        this.total = data.total || 0
        return data
      } catch (error) {
        throw error
      } finally {
        this.loading = false
      }
    },

    // 获取演出详情
    async fetchShowDetail(id: string): Promise<Show> {
      this.loading = true
      try {
        const response = await getShowDetail(id)
        const { data } = response
        this.currentShow = data
        return data
      } catch (error) {
        throw error
      } finally {
        this.loading = false
      }
    },

    // 清空当前演出
    clearCurrentShow(): void {
      this.currentShow = null
    }
  }
})