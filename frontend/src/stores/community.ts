import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { deletePost, fetchPosts, publishPost, togglePostLike } from '@/api/community'
import type { CommunityPost, PublishPayload } from '@/types/community'

/**
 * 社区信息流状态：分页、乐观点赞、发布后回到第一页。
 */
export const useCommunityStore = defineStore('community', () => {
  const posts = ref<CommunityPost[]>([])
  const total = ref(0)
  const page = ref(1)
  const size = 9
  const pages = ref(0)
  const loading = ref(false)
  const publishing = ref(false)
  const error = ref('')

  const isEmpty = computed(() => !loading.value && posts.value.length === 0)

  async function load(target = page.value) {
    loading.value = true
    error.value = ''
    try {
      const result = await fetchPosts(target, size)
      posts.value = result.records
      total.value = result.total
      pages.value = result.pages
      page.value = result.page
    } catch (err) {
      error.value = err instanceof Error ? err.message : '社区加载失败'
    } finally {
      loading.value = false
    }
  }

  async function like(post: CommunityPost) {
    const previous = { liked: post.liked, count: post.likeCount }
    post.liked = !post.liked
    post.likeCount += post.liked ? 1 : -1
    try {
      const result = await togglePostLike(post.id)
      post.liked = result.liked
      post.likeCount = result.likeCount
    } catch (err) {
      post.liked = previous.liked
      post.likeCount = previous.count
      error.value = err instanceof Error ? err.message : '点赞失败'
    }
  }

  async function publish(payload: PublishPayload) {
    publishing.value = true
    error.value = ''
    try {
      await publishPost(payload)
      await load(1)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : '发布失败'
      return false
    } finally {
      publishing.value = false
    }
  }

  async function remove(post: CommunityPost) {
    try {
      await deletePost(post.id)
      await load(post.mine ? 1 : page.value)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '删除失败'
    }
  }

  return { posts, total, page, pages, loading, publishing, error, isEmpty, load, like, publish, remove }
})