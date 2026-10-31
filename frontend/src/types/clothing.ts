/** 与后端 ClothingVO 一一对应的类型定义 */

export type CategoryCode =
  | 'TOP'
  | 'BOTTOM'
  | 'OUTER'
  | 'DRESS'
  | 'SHOES'
  | 'BAG'
  | 'ACCESSORY'

export const CATEGORY_ORDER: CategoryCode[] = [
  'TOP',
  'BOTTOM',
  'OUTER',
  'DRESS',
  'SHOES',
  'BAG',
  'ACCESSORY',
]

export const CATEGORY_LABELS: Record<CategoryCode, string> = {
  TOP: '上装',
  BOTTOM: '下装',
  OUTER: '外套',
  DRESS: '连衣裙',
  SHOES: '鞋履',
  BAG: '包袋',
  ACCESSORY: '配饰',
}

export const SEASON_LABELS: Record<string, string> = {
  SPRING: '春',
  SUMMER: '夏',
  AUTUMN: '秋',
  WINTER: '冬',
}

export interface Clothing {
  id: number
  name: string
  category: CategoryCode
  categoryLabel: string
  accentColor: string
  subCategory?: string
  color?: string
  colorHex?: string
  season: string[]
  styleTags: string[]
  material?: string
  brand?: string
  imageUrl?: string
  modelUrl?: string
  price?: number
  purchaseDate?: string
  wearCount: number
  lastWornAt?: string
  favorite: boolean
  note?: string
  createdAt: string
}

export interface ClothingPayload {
  name: string
  category: CategoryCode
  subCategory?: string
  color?: string
  colorHex?: string
  season: string[]
  styleTags: string[]
  material?: string
  brand?: string
  imageUrl?: string
  modelUrl?: string
  price?: number
  purchaseDate?: string
  favorite: boolean
  note?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

export interface WardrobeStats {
  totalCount: number
  favoriteCount: number
  unwornCount: number
  categoryCount: number
  totalValue: number
  avgWearCount: number
  categoryCounts: Record<string, number>
  topWornNames: string[]
}

export interface CategoryMeta {
  code: CategoryCode
  label: string
  accentColor: string
}

export interface Outfit {
  id: number
  name: string
  coverUrl?: string
  occasion?: string
  season?: string
  itemIds: number[]
  itemNames: string[]
  score?: number
  aiGenerated: boolean
  aiReason?: string
  createdAt: string
}

export interface WardrobeQuery {
  category?: CategoryCode | ''
  keyword?: string
  favorite?: boolean
  season?: string
  sort?: 'latest' | 'wearCount' | 'priceAsc' | 'priceDesc'
  page?: number
  size?: number
}