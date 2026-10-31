import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchAiStatus, fetchChatMessages, fetchChatSessions, streamChat } from '@/api/ai'
import type { ChatMessage, ChatSession, ThinkingStep } from '@/types/ai'

/**
 * AI 顾问状态机：消息流、思考步骤、会话切换与中断。
 * 流式增量直接写进最后一条 assistant 消息，避免整表替换造成的重渲染。
 */
export const useAssistantStore = defineStore('assistant', () => {
  const messages = ref<ChatMessage[]>([])
  const sessions = ref<ChatSession[]>([])
  const sessionId = ref<number | undefined>(undefined)
  const steps = ref<ThinkingStep[]>([])
  const provider = ref('')
  const sending = ref(false)
  const error = ref('')

  let controller: AbortController | null = null
  let sequence = 0

  const hasMessages = computed(() => messages.value.length > 0)

  const lastUserMessage = computed(() => {
    for (let i = messages.value.length - 1; i >= 0; i--) {
      if (messages.value[i].role === 'user') {
        return messages.value[i].content
      }
    }
    return ''
  })

  async function loadStatus() {
    try {
      const status = await fetchAiStatus()
      provider.value = status.provider
    } catch {
      // 状态接口失败时保持空白，meta 事件仍会在对话时补齐
    }
  }

  async function loadSessions() {
    try {
      sessions.value = await fetchChatSessions()
    } catch {
      // 会话列表失败不影响主对话
    }
  }

  async function openSession(id: number) {
    stop()
    sessionId.value = id
    steps.value = []
    error.value = ''
    try {
      messages.value = await fetchChatMessages(id)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '历史消息加载失败'
    }
  }

  function newSession() {
    stop()
    sessionId.value = undefined
    messages.value = []
    steps.value = []
    error.value = ''
  }

  async function send(text: string) {
    const question = text.trim()
    if (!question || sending.value) {
      return
    }

    error.value = ''
    steps.value = []
    provider.value = ''

    const userMessage: ChatMessage = { id: `u-${++sequence}`, role: 'user', content: question }
    const assistantMessage: ChatMessage = {
      id: `a-${++sequence}`,
      role: 'assistant',
      content: '',
      streaming: true,
    }
    messages.value.push(userMessage, assistantMessage)

    sending.value = true
    controller = new AbortController()

    try {
      await streamChat(
        { message: question, sessionId: sessionId.value, scene: 'WARDROBE' },
        {
          onMeta: (meta) => {
            sessionId.value = meta.sessionId
            provider.value = meta.provider
          },
          onStatus: (step) => {
            if (!steps.value.some((item) => item.phase === step.phase)) {
              steps.value.push(step)
            }
          },
          onDelta: (delta) => {
            assistantMessage.content += delta
          },
          onRecommendation: (recommendation) => {
            assistantMessage.recommendation = recommendation
          },
          onError: (message) => {
            error.value = message
            assistantMessage.streaming = false
            if (!assistantMessage.content) {
              assistantMessage.content = '这次没能生成建议，换个问法再试试？'
            }
          },
          onDone: () => {
            assistantMessage.streaming = false
            void loadSessions()
          },
        },
        controller.signal,
      )
    } catch (err) {
      if (err instanceof DOMException && err.name === 'AbortError') {
        assistantMessage.content += '\n\n（已停止生成）'
      } else {
        error.value = err instanceof Error ? err.message : '生成失败，请稍后重试'
        if (!assistantMessage.content) {
          assistantMessage.content = '连接中断了，请确认后端服务在 8080 端口运行。'
        }
      }
    } finally {
      assistantMessage.streaming = false
      sending.value = false
      controller = null
    }
  }

  function stop() {
    controller?.abort()
    controller = null
    sending.value = false
  }

  return {
    messages,
    sessions,
    sessionId,
    steps,
    provider,
    sending,
    error,
    hasMessages,
    lastUserMessage,
    loadStatus,
    loadSessions,
    openSession,
    newSession,
    send,
    stop,
  }
})