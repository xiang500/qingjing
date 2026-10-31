import type { ChatMessage, ChatSession, StreamHandlers } from '@/types/ai'

/** 与 http.ts 保持一致的演示用户身份头 */
function userIdHeader(): Record<string, string> {
  const token = localStorage.getItem('qj.token')
  if (token) {
    return { Authorization: `Bearer ${token}` }
  }
  return { 'X-User-Id': localStorage.getItem('qj.userId') ?? '1' }
}

export async function fetchChatSessions(): Promise<ChatSession[]> {
  const response = await fetch('/api/ai/sessions', { headers: userIdHeader() })
  const body = await response.json()
  if (body.code !== 0) {
    throw new Error(body.message ?? '会话列表加载失败')
  }
  return body.data as ChatSession[]
}

export async function fetchChatMessages(sessionId: number): Promise<ChatMessage[]> {
  const response = await fetch(`/api/ai/sessions/${sessionId}/messages`, { headers: userIdHeader() })
  const body = await response.json()
  if (body.code !== 0) {
    throw new Error(body.message ?? '历史消息加载失败')
  }
  return (body.data as ChatMessage[]).map((message) => ({
    ...message,
    id: message.id ?? `${sessionId}-${Math.random()}`,
  }))
}

export async function removeChatSession(sessionId: number): Promise<void> {
  await fetch(`/api/ai/sessions/${sessionId}`, { method: 'DELETE', headers: userIdHeader() })
}

export interface AiStatus {
  provider: string
  remote: boolean
  tools: string[]
  knowledgeChunks: number
}

export async function fetchAiStatus(): Promise<AiStatus> {
  const response = await fetch('/api/ai/status', { headers: userIdHeader() })
  const body = await response.json()
  if (body.code !== 0) {
    throw new Error(body.message ?? 'AI 状态获取失败')
  }
  return body.data as AiStatus
}

export interface StreamChatPayload {
  message: string
  sessionId?: number
  scene?: string
}

/**
 * 用 fetch + ReadableStream 消费 SSE。
 * EventSource 只支持 GET，而对话需要携带较长的中文消息体，因此走 POST 流式读取。
 */
export async function streamChat(
  payload: StreamChatPayload,
  handlers: StreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const response = await fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...userIdHeader() },
    body: JSON.stringify(payload),
    signal,
  })

  if (!response.ok || !response.body) {
    throw new Error(`AI 服务返回 ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const dispatch = (block: string) => {
    let event = 'message'
    const dataLines: string[] = []
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) {
        event = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trim())
      }
    }
    if (dataLines.length === 0) {
      return
    }
    let data: unknown
    try {
      data = JSON.parse(dataLines.join('\n'))
    } catch {
      return
    }
    switch (event) {
      case 'meta':
        handlers.onMeta?.(data as never)
        break
      case 'status':
        handlers.onStatus?.(data as never)
        break
      case 'delta':
        handlers.onDelta?.((data as { text: string }).text)
        break
      case 'recommendation':
        handlers.onRecommendation?.((data as { recommendation: never }).recommendation)
        break
      case 'done':
        handlers.onDone?.(data as never)
        break
      case 'error':
        handlers.onError?.((data as { message: string }).message)
        break
      default:
        break
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    let boundary = buffer.indexOf('\n\n')
    while (boundary >= 0) {
      const block = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 2)
      if (block.trim()) {
        dispatch(block)
      }
      boundary = buffer.indexOf('\n\n')
    }
  }
  if (buffer.trim()) {
    dispatch(buffer)
  }
}