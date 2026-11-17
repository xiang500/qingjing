import { request } from './http'
import type { Outfit } from '@/types/clothing'

export interface SaveOutfitPayload {
  name: string
  itemIds: number[]
  occasion?: string
  season?: string
  reason?: string
  score?: number
}

/** 保存一套搭配（工作台 / AI 推荐卡） */
export function createOutfit(payload: SaveOutfitPayload) {
  return request<Outfit>({ url: '/outfit', method: 'POST', data: payload })
}

export function deleteOutfit(id: number) {
  return request<void>({ url: `/outfit/${id}`, method: 'DELETE' })
}
