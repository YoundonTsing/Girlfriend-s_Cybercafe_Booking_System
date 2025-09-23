import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'

// 公共路由
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { hidden: true }
  },
  {
    path: '/register',
    component: () => import('@/views/register/index.vue'),
    meta: { hidden: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/home',
    children: [
      {
        path: 'home',
        component: () => import('@/views/home/index.vue'),
        name: 'Home',
        meta: { title: '首页', icon: 'dashboard' }
      }
    ]
  },
  {
    path: '/show',
    component: Layout,
    children: [
      {
        path: 'list',
        component: () => import('@/views/show/list.vue'),
        name: 'ShowList',
        meta: { title: '演出列表', icon: 'list' }
      },
      {
        path: 'detail/:id',
        component: () => import('@/views/show/detail.vue'),
        name: 'ShowDetail',
        meta: { title: '演出详情', icon: 'form', hidden: true }
      }
    ]
  },
  {
    path: '/seat',
    component: Layout,
    meta: { hidden: true },
    children: [
      {
        path: 'selection/:showId/:sessionId',
        component: () => import('@/views/seat/selection.vue'),
        name: 'SeatSelection',
        meta: { title: '选择座位', hidden: true }
      }
    ]
  },
  {
    path: '/booking',
    component: Layout,
    meta: { hidden: true },
    children: [
      {
        path: 'detail/:showId/:timeSlotId',
        component: () => import('@/views/booking/detail.vue'),
        name: 'BookingDetail',
        meta: { title: '预约确认', icon: 'form', hidden: true }
      }
    ]
  },
  {
    path: '/order',
    component: Layout,
    children: [
      {
        path: 'list',
        component: () => import('@/views/order/list.vue'),
        name: 'OrderList',
        meta: { title: '我的订单', icon: 'order' }
      },
      {
        path: 'detail/:orderNo',
        component: () => import('@/views/order/detail.vue'),
        name: 'OrderDetail',
        meta: { title: '订单详情', icon: 'form', hidden: true }
      },
      {
        path: 'create',
        component: () => import('@/views/order/create.vue'),
        name: 'OrderCreate',
        meta: { title: '创建订单', icon: 'form', hidden: true }
      }
    ]
  },
  {
    path: '/stock',
    component: Layout,
    redirect: '/stock/dashboard',
    meta: { title: '余座监控', icon: 'monitor' },
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/StockDashboard.vue'),
        name: 'StockDashboard',
        meta: { title: '余座监控', icon: 'monitor' }
      },
      {
        path: 'monitoring',
        component: () => import('@/views/MonitoringDashboard.vue'),
        name: 'MonitoringDashboard',
        meta: { title: '系统监控', icon: 'data-analysis' }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    meta: { hidden: true },
    children: [
      {
        path: 'profile',
        component: () => import('@/views/user/profile.vue'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes
})

export function resetRouter(): void {
  // Vue 3的路由重置方式
  router.replace('/login')
}

export default router