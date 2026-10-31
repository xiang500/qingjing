import { request } from './http'
import type {
  CategoryMeta,
  Clothing,
  ClothingPayload,
  Outfit,
  PageResult,
  WardrobeQuery,
  WardrobeStats,
} from '@/types/clothing'

export function fetchClothingPage(query: WardrobeQuery) {
  return request<PageResult<Clothing>>({
    url: '/clothing',
    method: 'GET',
    params: {
      ...query,
      category: query.category || undefined,
      keyword: query.keyword?.trim() || undefined,
      favorite: query.favorite || undefined,
    },
  })
}

export function fetchClothingDetail(id: number) {
  return request<Clothing>({ url: `/clothing/${id}`, method: 'GET' })
}

export function createClothing(payload: ClothingPayload) {
  return request<Clothing>({ url: '/clothing', method: 'POST', data: payload })
}

export function updateClothing(id: number, payload: ClothingPayload) {
  return request<Clothing>({ url: `/clothing/${id}`, method: 'PUT', data: payload })
}

export function deleteClothing(id: number) {
  return request<void>({ url: `/clothing/${id}`, method: 'DELETE' })
}

export function toggleClothingFavorite(id: number) {
  return request<Clothing>({ url: `/clothing/${id}/favorite`, method: 'PATCH' })
}

export function markClothingWorn(id: number) {
  return request<Clothing>({ url: `/clothing/${id}/wear`, method: 'POST' })
}

export function fetchWardrobeStats() {
  return request<WardrobeStats>({ url: '/clothing/stats', method: 'GET' })
}

export function fetchCategories() {
  return request<CategoryMeta[]>({ url: '/meta/categories', method: 'GET' })
}

export function fetchOutfits() {
  return request<Outfit[]>({ url: '/outfit', method: 'GET' })
}