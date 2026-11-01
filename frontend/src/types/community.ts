/** 社区相关类型 */

export interface CommunityPost {
  id: number
  title: string
  content: string
  images: string[]
  tags: string[]
  likeCount: number
  commentCount: number
  liked: boolean
  mine: boolean
  authorName: string
  authorAvatar?: string | null
  outfitName?: string | null
  itemIds: number[]
  itemNames: string[]
  itemImages: string[]
  createdAt: string
}

export interface PublishPayload {
  title: string
  content?: string
  outfitId?: number
  tags?: string[]
  images?: string[]
}