<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'
import { useAssistantStore } from '@/stores/assistant'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import ThinkingTrace from '@/components/ai/ThinkingTrace.vue'
import RecommendationCard from '@/components/ai/RecommendationCard.vue'
import type { ChatSession } from '@/types/ai'

const store = useAssistantStore()
const auth = useAuthStore()
const { messages, sessions, steps, provider, sending, error, hasMessages } = storeToRefs(store)
const theme = useThemeStore()
const route = useRoute()

const draft = ref('')
const scroller = ref<HTMLElement | null>(null)

const suggestions = [
  '今天通勤 12℃，帮我配一套能直接穿的',
  '周末约会想穿得温柔一点',
  '帮我看看衣橱利用率，哪些单品该重新上场',
  '我 165cm、A 型身材，怎么穿更显高？',
]

/** 输入框占位：跟着当前季节的光线换一句问法 */
const SEASON_PLACEHOLDERS = {
  spring: '问问春日通勤怎么叠穿…',
  summer: '问问正午出门怎么穿得凉快…',
  autumn: '问问秋日通勤怎么叠穿…',
  winter: '问问雪天通勤怎么穿得暖…',
} as const

const placeholder = computed(() => SEASON_PLACEHOLDERS[theme.meta.key])

const isOffline = () => provider.value.includes('本地')

async function scrollToBottom() {
  await nextTick()
  const element = scroller.value
  if (element) {
    element.scrollTo({ top: element.scrollHeight, behavior: 'smooth' })
  }
}

watch(
  () => [messages.value.length, messages.value.at(-1)?.content.length ?? 0],
  () => {
    void scrollToBottom()
  },
)

async function submit(text?: string) {
  const content = (text ?? draft.value).trim()
  if (!content || sending.value) {
    return
  }
  if (!auth.isLoggedIn) {
    ElMessage.info('请先在右上角登录，登录后 AI 才能读取你的衣橱')
    return
  }
  draft.value = ''
  await store.send(content)
}

function useSuggestion(text: string) {
  void submit(text)
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void submit()
  }
}

function startNewChat() {
  store.newSession()
  draft.value = ''
}

/** 重命名历史会话 */
async function renameSession(session: ChatSession) {
  try {
    const { value } = await ElMessageBox.prompt('新的会话名称', '重命名会话', {
      inputValue: session.title,
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '名称不能为空',
    })
    await store.renameSession(session.id, value.trim())
    ElMessage.success('会话已重命名')
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err instanceof Error ? err.message : '重命名失败')
    }
  }
}

function retry() {
  void store.retry()
}

onMounted(async () => {
  void store.loadStatus()
  await store.loadSessions()
  const presetSession = route.query.session
  if (typeof presetSession === 'string' && Number(presetSession) > 0) {
    await store.openSession(Number(presetSession))
    return
  }
  const preset = route.query.q
  if (typeof preset === 'string' && preset.trim()) {
    await submit(preset)
  }
})
</script>

<template>
  <div class="page-stack">
    <!-- ============ 区块一：顾问页首 · 开始对话 ============ -->
    <section class="section-block">
      <header class="section-head">
        <div class="min-w-0">
          <span class="eyebrow">AI Stylist · Consultant</span>
          <div class="mt-2 flex flex-wrap items-baseline gap-x-3 gap-y-1">
            <h1 class="display text-[clamp(1.5rem,2.4vw,2rem)] leading-tight text-mist-100">
              轻境穿搭顾问
            </h1>
            <p class="text-[12px] leading-relaxed text-mist-400">
              先看天气，再翻你的{{ theme.meta.name }}衣橱，给出一套今天就能穿的搭配。
            </p>
          </div>
        </div>

        <!-- 首屏大动作：亮底主按钮；模型状态属于次要信息，压暗 -->
        <div class="action-row">
          <span
            class="inline-flex max-w-[15rem] items-center gap-2 rounded-[var(--radius-pill)] border px-3 py-1 text-[10px]"
            :class="
              isOffline()
                ? 'border-gold-300/35 bg-gold-300/10 text-gold-200'
                : 'border-ink-700/60 text-mist-400'
            "
            :title="isOffline() ? '未配置大模型 Key，正在使用本地策略引擎' : provider"
          >
            <span
              class="size-1.5 shrink-0 rounded-full"
              :class="provider ? 'bg-gold-300' : 'bg-mist-500/50'"
            />
            <span class="truncate">{{ provider || '正在接入模型…' }}</span>
          </span>
          <button type="button" class="btn-primary" @click="startNewChat">
            <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14" stroke-linecap="round" />
            </svg>
            新建会话
          </button>
        </div>
      </header>
    </section>

    <!-- ============ 区块二：会话档案（左） · 对话与输入（右） ============ -->
    <div
      class="grid gap-4 lg:h-[calc(100dvh_-_17.5rem)] lg:min-h-[26rem] lg:grid-cols-[17rem_minmax(0,1fr)] lg:items-stretch lg:gap-5"
    >
      <!-- 左：会话档案区块 -->
      <aside class="glass hidden min-h-0 min-w-0 flex-col overflow-hidden lg:flex">
        <header class="shrink-0 border-b border-ink-700/40 px-4 py-4">
          <div class="section-head">
            <div class="min-w-0">
              <span class="eyebrow">Session Archive</span>
              <h2>会话档案</h2>
              <p>共 {{ sessions.length }} 个存档，换季后也能翻回来。</p>
            </div>
          </div>
        </header>

        <div class="min-h-0 flex-1 overflow-y-auto p-2">
          <ul class="space-y-1">
            <li v-for="session in sessions" :key="session.id" class="group relative">
              <button
                type="button"
                class="flex w-full items-center gap-2 rounded-[var(--radius-sm)] border py-2 pl-3 pr-8 text-left transition duration-200"
                :class="
                  session.id === store.sessionId
                    ? 'border-gold-300/35 bg-gold-300/12 text-mist-100'
                    : 'border-transparent text-mist-300 hover:border-ink-700/50 hover:bg-gold-300/8 hover:text-mist-100'
                "
                :aria-current="session.id === store.sessionId ? 'true' : undefined"
                @click="store.openSession(session.id)"
              >
                <span
                  class="size-1.5 shrink-0 rounded-full"
                  :class="session.id === store.sessionId ? 'bg-gold-300' : 'bg-mist-500/50'"
                />
                <span class="min-w-0 flex-1 truncate text-xs">{{ session.title }}</span>
              </button>
              <button
                type="button"
                class="absolute right-2 top-1/2 grid size-6 -translate-y-1/2 place-items-center rounded-full text-mist-500 opacity-50 transition duration-200 hover:text-gold-200 focus-visible:opacity-100 group-hover:opacity-100"
                title="重命名会话"
                aria-label="重命名会话"
                @click.stop="renameSession(session)"
              >
                <svg class="size-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
                  <path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3Z" stroke-linecap="round" stroke-linejoin="round" />
                  <path d="m13.5 6.5 3 3" stroke-linecap="round" />
                </svg>
              </button>
            </li>
          </ul>

          <p v-if="sessions.length === 0" class="px-2 py-4 text-[11px] leading-relaxed text-mist-500">
            还没有历史会话。问出第一句，这里会自动留下记录。
          </p>
        </div>

        <div class="shrink-0 border-t border-ink-700/40 px-4 py-3">
          <p class="flex items-center gap-2 text-[10px] tracking-[0.16em] text-mist-500">
            <span class="size-1 rounded-full bg-gold-300/80" />
            {{ theme.meta.label }} · {{ theme.meta.light }}
          </p>
        </div>
      </aside>

      <!-- 右：对话区块 + 输入区块 -->
      <div class="flex min-h-0 min-w-0 flex-col gap-4">
        <!-- 对话区块 -->
        <section
          class="glass relative flex h-[76dvh] min-h-[30rem] min-w-0 flex-1 flex-col overflow-hidden lg:h-auto lg:min-h-0"
        >
          <span
            class="pointer-events-none absolute inset-x-0 top-0 z-10 h-px bg-gradient-to-r from-transparent via-gold-300/45 to-transparent"
          />

          <header class="shrink-0 border-b border-ink-700/40 px-4 py-4 sm:px-5">
            <div class="section-head">
              <div class="min-w-0">
                <span class="eyebrow">Current Conversation</span>
                <h2>正在进行的这一轮</h2>
                <p>{{ theme.meta.label }} · {{ theme.meta.light }}｜回答会随季节归入「最近会话」</p>
              </div>
              <div class="action-row">
                <span class="chip cursor-default tabular-nums">{{ messages.length }} 条消息</span>
              </div>
            </div>
          </header>

          <div ref="scroller" class="min-h-0 flex-1 overflow-y-auto px-4 py-4 sm:px-5 sm:py-5">
            <!-- 空状态：季节宣言 + 快捷提问 -->
            <div v-if="!hasMessages" class="flex min-h-full flex-col items-center justify-center gap-6 py-4">
              <div class="max-w-xl text-center">
                <div class="flex justify-center">
                  <span class="eyebrow">AI Stylist</span>
                </div>
                <h3 class="display mt-4 text-[1.7rem] leading-[1.35] text-mist-100 sm:text-[2rem]">
                  今天穿什么，<span class="accent-text">我帮你算</span>
                </h3>
                <p class="mx-auto mt-3 max-w-md text-[13px] leading-[1.9] text-mist-400">
                  先看天气、再翻你的{{ theme.meta.name }}衣橱，最后给出一套今天能直接穿的搭配。
                  没有配置大模型 Key 时，本地策略引擎会跑同一套工具流程。
                </p>
              </div>

              <div class="grid w-full max-w-xl gap-3 sm:grid-cols-2">
                <button
                  v-for="(item, index) in suggestions"
                  :key="item"
                  type="button"
                  class="glass-soft group flex items-start gap-3 border border-transparent px-4 py-3 text-left transition duration-200 hover:-translate-y-0.5 hover:border-gold-300/35"
                  @click="useSuggestion(item)"
                >
                  <span
                    class="mt-1 grid size-6 shrink-0 place-items-center rounded-full border border-gold-300/30 bg-gold-300/10 text-[10px] font-medium text-gold-200"
                  >
                    {{ String(index + 1).padStart(2, '0') }}
                  </span>
                  <span
                    class="text-xs leading-relaxed text-mist-300 transition-colors duration-200 group-hover:text-mist-100"
                  >
                    {{ item }}
                  </span>
                </button>
              </div>
            </div>

            <!-- 消息流 -->
            <div v-else class="space-y-6">
              <template v-for="message in messages" :key="message.id">
                <div v-if="message.role === 'user'" class="flex justify-end">
                  <div
                    class="max-w-[85%] rounded-[var(--radius-md)] rounded-br-[var(--radius-xs)] border border-gold-300/30 bg-gold-300/15 px-4 py-3 text-[13px] leading-relaxed text-mist-100 sm:text-sm"
                  >
                    {{ message.content }}
                  </div>
                </div>

                <div v-else class="flex gap-3">
                  <span
                    class="display mt-1 grid size-7 shrink-0 place-items-center rounded-full border border-gold-300/30 bg-gold-300/10 text-[11px] text-gold-200"
                  >
                    轻
                  </span>
                  <div class="min-w-0 flex-1 space-y-2">
                    <ThinkingTrace
                      v-if="steps.length"
                      :steps="steps"
                      :active="message.streaming === true"
                    />

                    <div
                      v-if="message.content"
                      class="glass-soft whitespace-pre-line rounded-tl-[var(--radius-xs)] px-4 py-3 text-[13px] leading-[1.9] text-mist-200 sm:text-sm"
                    >{{ message.content }}<span
                      v-if="message.streaming"
                      class="ml-1 inline-block h-3.5 w-[2px] translate-y-0.5 animate-pulse rounded-full bg-gold-300 align-middle"
                    /></div>

                    <div
                      v-else-if="message.streaming"
                      class="glass-soft flex items-center gap-2 rounded-tl-[var(--radius-xs)] px-4 py-3 text-xs text-mist-500"
                    >
                      <span class="size-1.5 animate-pulse rounded-full bg-gold-300" />
                      正在组织搭配…
                    </div>

                    <RecommendationCard
                      v-if="message.recommendation"
                      :recommendation="message.recommendation"
                      @retry="submit('换一套思路，给我另一个搭配')"
                    />
                  </div>
                </div>
              </template>

              <div
                v-if="error"
                class="flex items-start justify-between gap-3 rounded-[var(--radius-md)] border border-rose-400/30 bg-rose-400/10 px-4 py-3 text-xs leading-relaxed text-mist-200"
              >
                <span class="flex flex-1 items-start gap-2">
                  <svg
                    class="mt-1 size-4 shrink-0 text-rose-400"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.6"
                  >
                    <circle cx="12" cy="12" r="9" />
                    <path d="M12 8v5M12 16h.01" stroke-linecap="round" />
                  </svg>
                  <span>{{ error }}</span>
                </span>
                <button
                  type="button"
                  class="shrink-0 text-gold-300 transition-colors duration-200 hover:text-gold-200"
                  @click="retry"
                >
                  重试上一条
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- 输入区块：单独成块，不和消息流贴在一起 -->
        <section class="section-block surface-sunken gap-3 p-4">
          <header class="section-head">
            <div class="min-w-0">
              <span class="eyebrow">Composer</span>
              <h2>问轻境一句</h2>
              <p>Enter 发送 · Shift + Enter 换行；没配模型 Key 时走本地策略引擎。</p>
            </div>
            <div class="action-row">
              <button
                v-if="sending"
                type="button"
                class="btn-ghost"
                @click="store.stop()"
              >
                停止
              </button>
              <button
                type="button"
                class="btn-primary"
                :disabled="sending || !draft.trim()"
                @click="submit()"
              >
                <span>{{ sending ? '生成中' : '发送' }}</span>
                <svg
                  v-if="!sending"
                  class="size-3.5"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M12 19V5M6 11l6-6 6 6" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                <span v-else class="size-1.5 animate-pulse rounded-full bg-current" />
              </button>
            </div>
          </header>

          <textarea
            v-model="draft"
            rows="1"
            class="field-input max-h-32 min-h-[2.5rem] resize-none text-[13px]"
            :placeholder="placeholder"
            aria-label="向 AI 顾问提问"
            :disabled="sending"
            @keydown="handleKeydown"
          />
        </section>
      </div>
    </div>
  </div>
</template>