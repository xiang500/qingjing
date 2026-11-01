import { request } from './http'
import type { CommunityPost, PublishPayload } from '@/types/community'
import type { PageResult } from '@/types/clothing'

export function fetchPosts(page = 1, size = 12) {
  return request<PageResult<CommunityPost>>({
    url: '/community/posts',
    method: 'GET',
    params: { page, size },
  })
}

export function publishPost(payload: PublishPayload) {
  return request<CommunityPost>({ url: '/community/posts', method: 'POST', data: payload })
}

export function togglePostLike(id: number) {
  return request<{ liked: boolean; likeCount: number }>({
    url: `/community/posts/${id}/like`,
    method: 'POST',
  })
}

export function deletePost(id: number) {
  return request<void>({ url: `/community/posts/${id}`, method: 'DELETE' })
}