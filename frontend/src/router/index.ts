import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * 路由全部懒加载：首屏只加载衣橱主视图，
 * 看板（ECharts）、工作台、社区等重页面按需下载，弱网下也能先看到内容。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'wardrobe',
      component: () => import('@/views/WardrobeView.vue'),
      meta: { title: '数字衣橱' },
    },
    {
      path: '/assistant',
      name: 'assistant',
      component: () => import('@/views/AssistantView.vue'),
      meta: { title: 'AI 穿搭顾问' },
    },
    {
      path: '/mirror',
      name: 'mirror',
      component: () => import('@/views/MirrorView.vue'),
      meta: { title: '搭配工作台' },
    },
    {
      path: '/community',
      name: 'community',
      component: () => import('@/views/CommunityView.vue'),
      meta: { title: '灵感社区' },
    },
    {
      path: '/stats',
      name: 'stats',
      component: () => import('@/views/StatsView.vue'),
      meta: { title: '数据看板' },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { title: '个人中心' },
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('@/views/AdminView.vue'),
      meta: { title: '管理后台', requiresAdmin: true },
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  if (!to.meta.requiresAdmin) {
    return true
  }
  const auth = useAuthStore()
  // 刷新页面后 user 还没拉回来，先 restore 一次再判断
  if (auth.token && !auth.user) {
    await auth.restore()
  }
  if (auth.isAdmin) {
    return true
  }
  ElMessage.warning('管理后台仅管理员可访问')
  return { path: '/' }
})

router.afterEach((to) => {
  const title = (to.meta.title as string | undefined) ?? '数字衣橱'
  document.title = `${title} · 轻境`
})

export default router