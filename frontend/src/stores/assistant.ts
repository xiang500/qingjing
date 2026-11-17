import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchAiStatus, fetchChatMessages, fetchChatSessions, renameChatSession, streamChat } from '@/api/ai'
import type { ChatMessage, ChatSession, ThinkingStep } from '@/types/ai'

/** 本地是否持有登录态 */
function hasSession(): boolean {
  try {
    return Boolean(window.localStorage.getItem('qj.token'))
  } catch {
    return false
  }
}

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
    if (!hasSession()) {
      return
    }
    try {
      const status = await fetchAiStatus()
      provider.value = status.provider
    } catch {
      // 状态接口失败时保持空白，meta 事件仍会在对话时补齐
    }
  }

  async function loadSessions() {
    if (!hasSession()) {
      sessions.value = []
      return
    }
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

  /** 重试上一条用户消息（出错时使用） */
  async function retry() {
    const question = lastUserMessage.value
    if (!question || sending.value) {
      return
    }
    await send(question)
  }

  /** 重命名会话，成功后同步侧栏标题 */
  async function renameSession(id: number, title: string) {
    await renameChatSession(id, title)
    const target = sessions.value.find((session) => session.id === id)
    if (target) {
      target.title = title
    }
  }

  /** 清空会话与消息（退出登录 / 切换账号时调用） */
  function reset() {
    controller?.abort()
    controller = null
    messages.value = []
    sessions.value = []
    sessionId.value = undefined
    steps.value = []
    provider.value = ''
    sending.value = false
    error.value = ''
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
    retry,
    renameSession,
    reset,
  }
})