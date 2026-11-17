import { request } from './http'
import type { CommunityPost, PostComment, PublishPayload } from '@/types/community'
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

export function fetchComments(postId: number) {
  return request<PostComment[]>({ url: `/community/posts/${postId}/comments`, method: 'GET' })
}

export function addComment(postId: number, content: string) {
  return request<PostComment>({
    url: `/community/posts/${postId}/comments`,
    method: 'POST',
    data: { content },
  })
}

export function deleteComment(commentId: number) {
  return request<void>({ url: `/community/comments/${commentId}`, method: 'DELETE' })
}