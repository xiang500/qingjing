<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import LoginDialog from '@/components/LoginDialog.vue'
import SeasonBackdrop from '@/components/SeasonBackdrop.vue'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useWardrobeStore } from '@/stores/wardrobe'
import { useCommunityStore } from '@/stores/community'
import { useAssistantStore } from '@/stores/assistant'
import { SEASON_AUTOPLAY_MS } from '@/theme/seasons'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()
const wardrobe = useWardrobeStore()
const community = useCommunityStore()
const assistant = useAssistantStore()
const loginOpen = ref(false)

const tabs = computed(() => {
  const base = [
    { name: 'wardrobe', to: '/', label: '数字衣橱', en: 'Wardrobe' },
    { name: 'mirror', to: '/mirror', label: '搭配工作台', en: 'Studio' },
    { name: 'assistant', to: '/assistant', label: 'AI 顾问', en: 'Advisor' },
    { name: 'community', to: '/community', label: '灵感社区', en: 'Community' },
    { name: 'stats', to: '/stats', label: '数据看板', en: 'Insights' },
    { name: 'profile', to: '/profile', label: '个人中心', en: 'Profile' },
  ]
  // 管理后台只对管理员可见，普通用户看不到入口
  return auth.isAdmin
    ? [...base, { name: 'admin', to: '/admin', label: '管理后台', en: 'Admin' }]
    : base
})

const activeName = computed(() => route.name)
const autoplaySeconds = Math.round(SEASON_AUTOPLAY_MS / 1000)

/**
 * 登录态变化（登录 / 退出 / 切换账号）时统一处理：
 * - 退出：清空所有用户数据，并回到默认首页（衣橱空态，画廊不显示照片）
 * - 登录：重新拉取当前账号的衣橱、社区与会话，不需要手动刷新页面
 */
watch(
  () => auth.token,
  async (token, previous) => {
    if (token === previous) {
      return
    }
    if (token) {
      await Promise.all([
        wardrobe.init(),
        community.load(1),
        assistant.loadStatus(),
        assistant.loadSessions(),
      ])
      return
    }
    wardrobe.reset()
    community.reset()
    assistant.reset()
    if (route.path !== '/') {
      void router.push('/')
    }
  },
)

onMounted(() => {
  void auth.restore()
  // 四季自动轮播：每 30 秒换一季，不需要用户点按钮
  theme.startAutoPlay()
})

onBeforeUnmount(() => {
  theme.stopAutoPlay()
})
</script>

<template>
  <el-config-provider :locale="zhCn">
    <div class="relative flex min-h-screen flex-col">
      <SeasonBackdrop />

      <header class="sticky top-0 z-40 pt-3 sm:pt-4">
        <div class="mx-auto w-full max-w-[86rem] px-4 sm:px-6">
          <div
            class="glass flex flex-wrap items-center gap-x-2 gap-y-1.5 rounded-[1.35rem] px-3 py-2 sm:flex-nowrap sm:gap-3 sm:rounded-full sm:pl-4 sm:pr-2"
          >
            <RouterLink to="/" class="group flex shrink-0 items-center gap-2.5" aria-label="轻境首页">
              <span class="seal">
                <span class="font-display text-sm leading-none">轻</span>
              </span>
              <span class="hidden flex-col leading-none whitespace-nowrap md:flex">
                <span class="font-display text-[15px] tracking-[0.24em] text-mist-100">轻境</span>
                <span class="mt-1 text-[8px] uppercase tracking-[0.34em] text-mist-500">
                  Lightness AI
                </span>
              </span>
            </RouterLink>

            <nav
              class="order-3 -mx-1 flex w-full min-w-0 items-center gap-0.5 overflow-x-auto px-1 py-0.5 sm:order-none sm:mx-0 sm:w-auto sm:px-0"
            >
              <RouterLink
                v-for="tab in tabs"
                :key="tab.name"
                :to="tab.to"
                class="nav-link"
                :class="{ 'nav-link-on': activeName === tab.name }"
              >
                <span>{{ tab.label }}</span>
                <span class="hidden text-[9px] uppercase tracking-[0.18em] opacity-60 2xl:inline">
                  {{ tab.en }}
                </span>
              </RouterLink>
            </nav>

            <div class="ml-auto flex shrink-0 items-center gap-2">
              <!-- 季节自动轮播指示（纯展示，不可点击）：进度条走完就换下一季 -->
              <span
                class="season-badge"
                :title="`四季自动轮播：每 ${autoplaySeconds} 秒换一季 · 当前 ${theme.meta.label} · ${theme.meta.name}（${theme.meta.light}）`"
              >
                <span class="season-dot" aria-hidden="true" />
                <span class="season-badge-label">
                  {{ theme.meta.label }} · {{ theme.meta.name }}
                </span>
                <span class="season-bar" aria-hidden="true">
                  <span
                    :key="theme.season"
                    class="season-bar-fill"
                    :style="{ animationDuration: `${SEASON_AUTOPLAY_MS}ms` }"
                  />
                </span>
              </span>

              <div class="flex items-center gap-1.5 rounded-full p-0.5">
                <span class="avatar-badge">
                  {{ auth.isLoggedIn ? auth.displayName.slice(0, 1) : '轻' }}
                </span>
                <span class="hidden max-w-[5.5rem] truncate text-[11px] text-mist-300 sm:inline">
                  {{ auth.isLoggedIn ? auth.displayName : '演示账号' }}
                </span>
                <button
                  v-if="!auth.isLoggedIn"
                  type="button"
                  class="login-btn"
                  @click="loginOpen = true"
                >
                  登录
                </button>
                <span
                  v-if="auth.isLoggedIn && auth.isAdmin"
                  class="rounded-full border border-gold-300/40 bg-gold-300/10 px-2 py-0.5 text-[10px] text-gold-200"
                >
                  管理员
                </span>
                <button
                  v-if="auth.isLoggedIn"
                  type="button"
                  class="px-1.5 py-1 text-[11px] text-mist-500 transition-colors hover:text-mist-200"
                  @click="auth.logout()"
                >
                  退出
                </button>
              </div>
            </div>
          </div>
        </div>
      </header>

      <main class="mx-auto w-full max-w-[86rem] flex-1 px-4 pb-24 pt-6 sm:px-6 sm:pt-8">
        <RouterView v-slot="{ Component }">
          <Transition name="fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>

      <footer class="relative border-t border-ink-700/40 py-6">
        <div
          class="mx-auto flex w-full max-w-[86rem] flex-col items-center justify-between gap-2 px-5 text-[11px] text-mist-500 sm:flex-row sm:px-6"
        >
          <span class="flex items-center gap-2">
            <span class="font-display tracking-[0.2em] text-mist-400">轻境</span>
            <span class="opacity-60">·</span>
            AI 实时穿搭与数字衣橱系统
          </span>
          <span class="flex items-center gap-3">
            <span>{{ theme.meta.label }} · {{ theme.meta.light }}（{{ theme.meta.name }}）</span>
            <span class="hidden opacity-60 sm:inline">端侧视觉推理 · 云端 Agent 编排 · 浏览器实时渲染</span>
          </span>
        </div>
      </footer>

      <LoginDialog :open="loginOpen" @close="loginOpen = false" />
    </div>
  </el-config-provider>
</template>

<style scoped>
.seal {
  position: relative;
  display: grid;
  place-items: center;
  width: 2.15rem;
  height: 2.15rem;
  flex: none;
  border-radius: 50%;
  color: #fff;
  background: radial-gradient(
    120% 120% at 30% 20%,
    color-mix(in oklab, var(--c-accent) 72%, white),
    var(--c-accent)
  );
  box-shadow:
    inset 0 1px 0 color-mix(in oklab, white 55%, transparent),
    0 6px 16px -8px color-mix(in oklab, var(--c-accent) 80%, transparent);
  transition: transform 0.4s ease;
}

.seal::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 1px solid color-mix(in oklab, var(--c-accent) 32%, transparent);
  transition: transform 0.5s ease;
}

.group:hover .seal {
  transform: scale(1.04);
}

.group:hover .seal::after {
  transform: scale(1.14);
  opacity: 0.5;
}

.nav-link {
  position: relative;
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
  padding: 0.48rem 0.7rem;
  border-radius: 999px;
  font-size: 0.82rem;
  color: var(--c-ink-3);
  white-space: nowrap;
  transition:
    color 0.2s ease,
    background 0.2s ease,
    box-shadow 0.2s ease;
}

.nav-link:hover {
  color: var(--c-ink);
  background: color-mix(in oklab, var(--c-glass-a) 55%, transparent);
}

.nav-link-on {
  color: var(--c-ink);
  background: color-mix(in oklab, var(--c-glass-a) 92%, transparent);
  box-shadow:
    inset 0 1px 0 color-mix(in oklab, white 65%, transparent),
    inset 0 0 0 1px color-mix(in oklab, var(--c-line-strong) 40%, transparent),
    0 8px 18px -16px var(--c-glass-shadow);
}

.avatar-badge {
  display: grid;
  place-items: center;
  width: 1.9rem;
  height: 1.9rem;
  border-radius: 50%;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--c-accent-ink);
  background: linear-gradient(
    140deg,
    color-mix(in oklab, var(--c-accent) 78%, white),
    var(--c-accent)
  );
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.5),
    0 4px 12px -8px var(--c-glass-shadow);
}

.login-btn {
  border: 0;
  background: transparent;
  border-radius: 999px;
  padding: 0.32rem 0.6rem;
  font-family: inherit;
  font-size: 0.68rem;
  color: var(--c-ink-2);
  cursor: pointer;
  white-space: nowrap;
  transition:
    color 0.2s ease,
    background 0.2s ease;
}

.login-btn:hover {
  color: var(--c-accent-strong);
  background: color-mix(in oklab, var(--c-accent) 12%, transparent);
}

@media (max-width: 640px) {
  .nav-link {
    padding: 0.45rem 0.6rem;
    font-size: 0.78rem;
  }
}

/* ---------- 季节自动轮播指示（不可点击） ---------- */
.season-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.42rem;
  padding: 0.28rem 0.62rem;
  border-radius: 999px;
  border: 1px solid color-mix(in oklab, var(--c-line) 100%, transparent);
  background: color-mix(in oklab, var(--c-glass-a) 55%, transparent);
  color: var(--c-ink-3);
  font-size: 0.66rem;
  letter-spacing: 0.04em;
  white-space: nowrap;
  user-select: none;
}

.season-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex: none;
  background: var(--c-accent);
  box-shadow: 0 0 0 0 color-mix(in oklab, var(--c-accent) 55%, transparent);
  animation: season-pulse 2.4s ease-out infinite;
}

.season-badge-label {
  font-family: var(--font-display);
  font-size: 0.72rem;
  color: var(--c-accent-strong);
}

.season-bar {
  position: relative;
  width: 2.5rem;
  height: 3px;
  border-radius: 999px;
  overflow: hidden;
  background: color-mix(in oklab, var(--c-ink) 14%, transparent);
}

.season-bar-fill {
  display: block;
  height: 100%;
  width: 100%;
  transform-origin: left center;
  background: linear-gradient(
    90deg,
    color-mix(in oklab, var(--c-accent) 55%, transparent),
    var(--c-accent)
  );
  animation-name: season-progress;
  animation-timing-function: linear;
  animation-fill-mode: forwards;
}

@keyframes season-progress {
  from {
    transform: scaleX(0);
  }
  to {
    transform: scaleX(1);
  }
}

@keyframes season-pulse {
  0% {
    box-shadow: 0 0 0 0 color-mix(in oklab, var(--c-accent) 45%, transparent);
  }
  70% {
    box-shadow: 0 0 0 5px transparent;
  }
  100% {
    box-shadow: 0 0 0 0 transparent;
  }
}

@media (max-width: 640px) {
  .season-badge {
    padding: 0.26rem 0.5rem;
    gap: 0.34rem;
  }

  .season-badge-label {
    display: none;
  }

  .season-bar {
    width: 1.8rem;
  }
}
</style>
