/** AI 对话相关的类型定义 */

export interface RecommendationItem {
  id: number
  name: string
  category: string
  categoryLabel: string
  color?: string
  colorHex?: string
  imageUrl?: string
  wearCount?: number
  favorite?: boolean
}

export interface Recommendation {
  name: string
  occasion?: string
  score?: number
  reason?: string
  items: RecommendationItem[]
}

export type ChatRole = 'user' | 'assistant'

export interface ChatMessage {
  id: number | string
  role: ChatRole
  content: string
  recommendation?: Recommendation
  streaming?: boolean
}

export interface ChatSession {
  id: number
  title: string
  scene?: string
  updatedAt?: string
}

export interface ThinkingStep {
  phase: string
  text: string
}

export interface ChatMeta {
  sessionId: number
  provider: string
  tools: string[]
  knowledgeChunks?: number
}

export interface StreamHandlers {
  onMeta?: (meta: ChatMeta) => void
  onStatus?: (step: ThinkingStep) => void
  onDelta?: (text: string) => void
  onRecommendation?: (recommendation: Recommendation) => void
  onDone?: (payload: { sessionId: number; messageId: number; tools: string[] }) => void
  onError?: (message: string) => void
}