import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由全部懒加载：首屏只加载衣橱主视图，
 * Three.js / MediaPipe 等重依赖会随「试衣镜」路由按需加载。
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
      meta: { title: '实时试衣镜' },
    },
    {
      path: '/community',
      name: 'community',
      component: () => import('@/views/CommunityView.vue'),
      meta: { title: '灵感社区' },
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.afterEach((to) => {
  const title = (to.meta.title as string | undefined) ?? '数字衣橱'
  document.title = `${title} · 轻境`
})

export default router