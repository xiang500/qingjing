<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'
import { useAssistantStore } from '@/stores/assistant'
import ThinkingTrace from '@/components/ai/ThinkingTrace.vue'
import RecommendationCard from '@/components/ai/RecommendationCard.vue'

const store = useAssistantStore()
const { messages, sessions, steps, provider, sending, error, hasMessages } = storeToRefs(store)
const route = useRoute()

const draft = ref('')
const scroller = ref<HTMLElement | null>(null)

const suggestions = [
  '今天上海通勤穿什么？',
  '周末约会想穿得温柔一点',
  '帮我看看衣橱利用率，哪些衣服该重新上场',
  '我 165cm、A 型身材，怎么穿显高？',
]

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
  <div class="grid gap-5 lg:grid-cols-[15.5rem_minmax(0,1fr)]">
    <aside class="hidden flex-col gap-3 lg:flex">
      <button type="button" class="btn-primary w-full" @click="startNewChat">
        <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 5v14M5 12h14" stroke-linecap="round" />
        </svg>
        新的对话
      </button>

      <div class="glass-card flex-1 overflow-hidden p-2">
        <p class="px-2 py-2 text-[10px] uppercase tracking-[0.28em] text-mist-500">最近会话</p>
        <ul class="space-y-1">
          <li v-for="session in sessions" :key="session.id">
            <button
              type="button"
              class="w-full truncate rounded-lg px-2.5 py-2 text-left text-xs transition-colors"
              :class="
                session.id === store.sessionId
                  ? 'bg-gold-400/10 text-gold-200'
                  : 'text-mist-400 hover:bg-white/5 hover:text-mist-100'
              "
              @click="store.openSession(session.id)"
            >
              {{ session.title }}
            </button>
          </li>
          <li v-if="sessions.length === 0" class="px-2.5 py-3 text-[11px] leading-relaxed text-mist-500">
            还没有历史对话，从右边问第一句吧。
          </li>
        </ul>
      </div>
    </aside>

    <section class="glass-card flex max-h-[78vh] min-h-[68vh] flex-col overflow-hidden">
      <header class="flex items-center justify-between gap-3 border-b border-white/8 px-5 py-3.5">
        <div class="flex min-w-0 items-center gap-2.5">
          <span class="grid size-8 place-items-center rounded-full border border-gold-400/30 bg-gold-400/5 text-[11px] text-gold-200">
            AI
          </span>
          <div class="min-w-0">
            <p class="truncate font-display text-sm text-mist-100">轻境穿搭顾问</p>
            <p class="truncate text-[10px] text-mist-500">
              {{ provider || '正在接入模型…' }}
            </p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <span
            v-if="isOffline()"
            class="hidden rounded-full border border-amber-300/25 bg-amber-300/10 px-2 py-0.5 text-[10px] text-amber-200 sm:inline"
          >
            未配置 Key · 本地策略引擎
          </span>
          <button
            v-if="sending"
            type="button"
            class="btn-ghost !px-3 !py-1 !text-[11px]"
            @click="store.stop()"
          >
            停止
          </button>
          <button type="button" class="btn-ghost !px-3 !py-1 !text-[11px] lg:hidden" @click="startNewChat">
            新对话
          </button>
        </div>
      </header>

      <div ref="scroller" class="flex-1 space-y-4 overflow-y-auto px-4 py-5 sm:px-5">
        <div v-if="!hasMessages" class="flex h-full flex-col items-start justify-center gap-4">
          <div>
            <h1 class="font-display text-2xl text-mist-100">今天穿什么，我帮你算</h1>
            <p class="mt-2 max-w-md text-sm leading-relaxed text-mist-500">
              我会先查天气、再翻你的真实衣橱，最后给出一套能直接穿的搭配。
              没有配置大模型 Key 时，会用本地策略引擎跑同一套工具流程。
            </p>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="item in suggestions"
              :key="item"
              type="button"
              class="chip !text-[11px]"
              @click="useSuggestion(item)"
            >
              {{ item }}
            </button>
          </div>
        </div>

        <template v-for="message in messages" :key="message.id">
          <div v-if="message.role === 'user'" class="flex justify-end">
            <div class="max-w-[85%] rounded-2xl rounded-br-md border border-gold-400/25 bg-gold-400/10 px-4 py-2.5 text-sm text-mist-100">
              {{ message.content }}
            </div>
          </div>

          <div v-else class="flex gap-2.5">
            <span class="mt-0.5 grid size-7 shrink-0 place-items-center rounded-full border border-white/12 bg-white/5 text-[10px] text-mist-300">
              AI
            </span>
            <div class="min-w-0 flex-1">
              <ThinkingTrace v-if="steps.length" :steps="steps" :active="message.streaming === true" class="mb-2" />
              <div
                v-if="message.content"
                class="whitespace-pre-line text-sm leading-relaxed text-mist-200"
              >{{ message.content }}<span
                v-if="message.streaming"
                class="ml-0.5 inline-block h-4 w-[2px] translate-y-0.5 animate-pulse bg-gold-300 align-middle"
              /></div>
              <div v-else-if="message.streaming" class="flex items-center gap-1.5 text-xs text-mist-500">
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
          class="rounded-xl border border-rose-400/25 bg-rose-400/8 px-3 py-2 text-xs text-rose-100"
        >
          {{ error }}
        </div>
      </div>

      <footer class="border-t border-white/8 px-4 py-3 sm:px-5">
        <div class="flex items-end gap-2">
          <textarea
            v-model="draft"
            rows="1"
            class="field-input max-h-32 min-h-[2.75rem] resize-none py-3"
            placeholder="例如：明天杭州 18℃，我要去见客户，穿哪套？"
            :disabled="sending"
            @keydown="handleKeydown"
          />
          <button
            type="button"
            class="btn-primary h-11 shrink-0"
            :disabled="sending || !draft.trim()"
            @click="submit()"
          >
            {{ sending ? '生成中' : '发送' }}
          </button>
        </div>
        <p class="mt-2 text-[10px] text-mist-500">
          Enter 发送 · Shift + Enter 换行 · 推荐结果会随对话保存到「最近会话」
        </p>
      </footer>
    </section>
  </div>
</template>