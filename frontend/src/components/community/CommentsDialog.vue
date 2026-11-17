<script setup lang="ts">
import { ref, watch } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import { addComment, deleteComment, fetchComments } from '@/api/community'
import type { CommunityPost, PostComment } from '@/types/community'

const props = defineProps<{
  open: boolean
  post: CommunityPost | null
}>()

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'changed', count: number): void
}>()

const comments = ref<PostComment[]>([])
const content = ref('')
const loading = ref(false)
const submitting = ref(false)
const error = ref('')

watch(
  () => props.open,
  async (open) => {
    if (!open) {
      return
    }
    content.value = ''
    error.value = ''
    await load()
  },
)

watch(
  () => props.post?.id,
  async () => {
    if (props.open) {
      await load()
    }
  },
)

async function load() {
  if (!props.post) {
    return
  }
  loading.value = true
  error.value = ''
  try {
    comments.value = await fetchComments(props.post.id)
    emit('changed', comments.value.length)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '评论加载失败'
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!props.post) {
    return
  }
  const text = content.value.trim()
  if (!text) {
    return
  }
  submitting.value = true
  error.value = ''
  try {
    const created = await addComment(props.post.id, text)
    comments.value = [...comments.value, created]
    content.value = ''
    emit('changed', comments.value.length)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '评论失败'
  } finally {
    submitting.value = false
  }
}

async function remove(comment: PostComment) {
  try {
    await deleteComment(comment.id)
    comments.value = comments.value.filter((item) => item.id !== comment.id)
    emit('changed', comments.value.length)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除失败'
  }
}

function timeText(value: string) {
  if (!value) {
    return ''
  }
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getMonth() + 1}/${date.getDate()} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
</script>

<template>
  <BaseModal
    :open="open"
    :title="post ? `评论 · ${post.title}` : '评论'"
    description="聊聊这套搭配，或者给作者一点建议"
    width="md"
    @close="emit('close')"
  >
    <div class="space-y-4">
      <div class="flex items-center justify-between text-[10px] uppercase tracking-[0.24em] text-mist-500">
        <span>Comments</span>
        <span class="tracking-normal tabular-nums">{{ comments.length }} 条</span>
      </div>

      <!-- 评论列表：hairline 分隔 -->
      <div class="max-h-72 overflow-y-auto rounded-[1.15rem] border border-ink-700/40 bg-ink-900/20">
        <div v-if="loading" class="space-y-3 px-4 py-4">
          <div v-for="index in 3" :key="index" class="flex gap-3">
            <div class="skeleton size-7 shrink-0 rounded-full" />
            <div class="flex-1 space-y-1.5">
              <div class="skeleton h-2.5 w-24 rounded" />
              <div class="skeleton h-3 w-3/4 rounded" />
            </div>
          </div>
        </div>

        <ul v-else-if="comments.length" class="divide-y divide-ink-700/40">
          <li v-for="comment in comments" :key="comment.id" class="flex gap-3 px-4 py-3">
            <span class="author-seal size-7 shrink-0">{{ comment.authorName.slice(0, 1) }}</span>
            <div class="min-w-0 flex-1">
              <div class="flex items-baseline gap-2">
                <span class="truncate text-[11px] font-medium text-mist-200">{{ comment.authorName }}</span>
                <span class="shrink-0 text-[10px] text-mist-500">{{ timeText(comment.createdAt) }}</span>
                <button
                  v-if="comment.mine"
                  type="button"
                  class="ml-auto shrink-0 text-[10px] text-mist-500 transition-colors hover:text-gold-200"
                  @click="remove(comment)"
                >
                  删除
                </button>
              </div>
              <p class="mt-1 whitespace-pre-wrap break-words text-xs leading-relaxed text-mist-300">
                {{ comment.content }}
              </p>
            </div>
          </li>
        </ul>

        <div v-else class="flex flex-col items-center gap-2 px-4 py-8 text-center">
          <span class="grid size-10 place-items-center rounded-full border border-ink-700/60 text-mist-500">
            <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path
                d="M20 12a8 8 0 0 1-8 8H5l-1 2v-4.5A8 8 0 0 1 12 4a8 8 0 0 1 8 8Z"
                stroke-linejoin="round"
              />
            </svg>
          </span>
          <p class="text-xs text-mist-500">还没有评论，来抢第一位。</p>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="border-t border-ink-700/40 pt-3">
        <label class="field-label" for="comment-content">说点什么</label>
        <textarea
          id="comment-content"
          v-model="content"
          rows="2"
          maxlength="200"
          class="field-input resize-none"
          placeholder="给作者一点建议，或者聊聊这套搭配…"
          @keydown.enter.exact.prevent="submit"
        />
        <div class="mt-2 flex items-center justify-between gap-3">
          <span class="text-[10px] text-mist-500 tabular-nums">{{ content.length }} / 200</span>
          <button
            type="button"
            class="btn-primary px-4 py-2 text-xs"
            :disabled="submitting || !content.trim()"
            @click="submit"
          >
            {{ submitting ? '发送中…' : '发表评论' }}
          </button>
        </div>
      </div>

      <p
        v-if="error"
        class="rounded-xl border border-gold-300/35 bg-gold-300/10 px-3 py-2 text-xs leading-relaxed text-mist-200"
      >
        {{ error }}
      </p>
    </div>
  </BaseModal>
</template>

<style scoped>
/* 与 PostCard 一致的作者印章：季节 accent 渐变 + 外圈细环 */
.author-seal {
  position: relative;
  display: grid;
  place-items: center;
  border-radius: 999px;
  font-size: 0.62rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--c-accent-ink);
  background: linear-gradient(
    140deg,
    color-mix(in oklab, var(--c-accent) 78%, white),
    var(--c-accent)
  );
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.5),
    0 4px 12px -8px var(--c-glass-shadow);
}

.author-seal::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: inherit;
  border: 1px solid color-mix(in oklab, var(--c-accent) 42%, transparent);
  pointer-events: none;
}
</style>
