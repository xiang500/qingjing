<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import LoginDialog from '@/components/LoginDialog.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const loginOpen = ref(false)

const tabs = [
  { name: 'wardrobe', to: '/', label: '数字衣橱', en: 'Wardrobe' },
  { name: 'assistant', to: '/assistant', label: 'AI 顾问', en: 'Assistant' },
  { name: 'mirror', to: '/mirror', label: '实时试衣镜', en: 'Mirror' },
  { name: 'community', to: '/community', label: '灵感社区', en: 'Community' },
]

const activeName = computed(() => route.name)

onMounted(() => {
  void auth.restore()
})
</script>

<template>
  <div class="relative flex min-h-screen flex-col">
    <header
      class="sticky top-0 z-30 border-b border-white/10 bg-ink-950/70 backdrop-blur-xl"
    >
      <div class="mx-auto flex h-16 w-full max-w-7xl items-center gap-6 px-5 sm:px-8">
        <RouterLink to="/" class="group flex items-center gap-3">
          <span
            class="relative grid size-9 place-items-center rounded-full border border-gold-400/50 text-gold-300"
          >
            <span class="font-display text-sm leading-none">轻</span>
            <span
              class="absolute inset-0 rounded-full border border-gold-400/20 transition-transform duration-500 group-hover:scale-110"
            />
          </span>
          <span class="hidden flex-col leading-tight sm:flex">
            <span class="font-display text-base tracking-[0.22em] text-mist-100">轻境</span>
            <span class="text-[10px] uppercase tracking-[0.32em] text-mist-500">Lightness AI</span>
          </span>
        </RouterLink>

        <nav class="ml-2 flex items-center gap-1 overflow-x-auto">
          <RouterLink
            v-for="tab in tabs"
            :key="tab.name"
            :to="tab.to"
            class="group relative flex items-center gap-2 rounded-full px-3.5 py-2 text-sm transition-colors"
            :class="
              activeName === tab.name
                ? 'bg-white/10 text-gold-200'
                : 'text-mist-300 hover:bg-white/5 hover:text-mist-100'
            "
          >
            <span>{{ tab.label }}</span>
            <span class="hidden text-[10px] uppercase tracking-[0.2em] text-mist-500 lg:inline">
              {{ tab.en }}
            </span>
          </RouterLink>
        </nav>

        <div class="ml-auto flex items-center gap-3">
          <span
            class="hidden items-center gap-2 rounded-full border border-emerald-400/25 bg-emerald-400/10 px-3 py-1.5 text-[11px] text-emerald-200 md:inline-flex"
          >
            <span class="size-1.5 animate-pulse rounded-full bg-emerald-300" />
            阶段三 · 3D 试衣
          </span>
          <div class="flex items-center gap-2 rounded-full border border-white/10 py-1 pl-1 pr-1.5">
            <span
              class="grid size-7 place-items-center rounded-full bg-gradient-to-br from-gold-300 to-gold-500 text-xs font-semibold text-ink-950"
            >
              {{ auth.isLoggedIn ? auth.displayName.slice(0, 1) : '轻' }}
            </span>
            <span class="max-w-[6rem] truncate text-xs text-mist-300">
              {{ auth.isLoggedIn ? auth.displayName : '演示账号' }}
            </span>
            <button
              v-if="!auth.isLoggedIn"
              type="button"
              class="rounded-full border border-gold-400/40 px-2.5 py-1 text-[11px] text-gold-200 transition-colors hover:bg-gold-400/10"
              @click="loginOpen = true"
            >
              登录
            </button>
            <button
              v-else
              type="button"
              class="rounded-full px-2 py-1 text-[11px] text-mist-500 transition-colors hover:text-mist-200"
              @click="auth.logout()"
            >
              退出
            </button>
          </div>
        </div>
      </div>
    </header>

    <main class="mx-auto w-full max-w-7xl flex-1 px-5 pb-24 pt-8 sm:px-8">
      <RouterView v-slot="{ Component }">
        <Transition name="fade" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>

    <footer class="border-t border-white/5 py-6">
      <div
        class="mx-auto flex w-full max-w-7xl flex-col items-center justify-between gap-2 px-5 text-[11px] text-mist-500 sm:flex-row sm:px-8"
      >
        <span>轻境 · AI 实时穿搭与 3D 数字衣橱系统</span>
        <span>端侧视觉推理 · 云端 Agent 编排 · 浏览器实时渲染</span>
      </div>
    </footer>

    <LoginDialog :open="loginOpen" @close="loginOpen = false" />
  </div>
</template>