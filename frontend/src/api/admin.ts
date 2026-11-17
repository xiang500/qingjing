import { request } from './http'

export interface AdminOverview {
  users: number
  clothing: number
  outfits: number
  posts: number
  chatSessions: number
  recommendations: number
  knowledgeChunks: number
}

export interface AdminUser {
  id: number
  username: string
  nickname: string
  gender?: number | null
  heightCm?: number | null
  weightKg?: number | null
  bodyType?: string | null
  stylePreference: string[]
  createdAt?: string | null
}

export interface KnowledgeChunk {
  id: number
  title: string
  category?: string | null
  tags: string[]
  content: string
  enabled: boolean
  createdAt?: string | null
}

export interface KnowledgePayload {
  title: string
  category?: string
  tags?: string[]
  content: string
  enabled?: boolean
}

export function fetchAdminOverview() {
  return request<AdminOverview>({ url: '/admin/overview', method: 'GET' })
}

export function fetchAdminUsers() {
  return request<AdminUser[]>({ url: '/admin/users', method: 'GET' })
}

export function fetchKnowledge() {
  return request<KnowledgeChunk[]>({ url: '/admin/knowledge', method: 'GET' })
}

export function createKnowledge(payload: KnowledgePayload) {
  return request<KnowledgeChunk>({ url: '/admin/knowledge', method: 'POST', data: payload })
}

export function updateKnowledge(id: number, payload: KnowledgePayload) {
  return request<KnowledgeChunk>({ url: `/admin/knowledge/${id}`, method: 'PUT', data: payload })
}

export function deleteKnowledge(id: number) {
  return request<void>({ url: `/admin/knowledge/${id}`, method: 'DELETE' })
}
