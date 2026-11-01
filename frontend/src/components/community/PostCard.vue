<script setup lang="ts">
import { computed } from 'vue'
import type { CommunityPost } from '@/types/community'

const props = defineProps<{ post: CommunityPost }>()

const emit = defineEmits<{
  (event: 'like'): void
  (event: 'remove'): void
  (event: 'tryOn', ids: number[]): void
}>()

const cover = computed(() => props.post.images[0])
const initial = computed(() => props.post.authorName.slice(0, 1))
const canTryOn = computed(() => props.post.itemIds.length > 0)

const createdText = computed(() => {
  if (!props.post.createdAt) {
    return ''
  }
  const date = new Date(props.post.createdAt.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) {
    return props.post.createdAt
  }
  return `${date.getMonth() + 1} 月 ${date.getDate()} 日`
})
</script>

<template>
  <article class="group glass-card flex flex-col overflow-hidden">
    <div class="relative aspect-[4/3] overflow-hidden bg-ink-900/60">
      <img
        v-if="cover"
        :src="cover"
        :alt="post.title"
        loading="lazy"
        class="size-full object-contain p-3 transition-transform duration-500 group-hover:scale-[1.03]"
      />
      <div v-else class="grid size-full place-items-center text-xs text-mist-500">暂无配图</div>

      <span
        v-if="post.outfitName"
        class="absolute left-3 top-3 rounded-full border border-white/15 bg-ink-950/70 px-2.5 py-1 text-[10px] text-mist-200 backdrop-blur"
      >
        {{ post.outfitName }}
      </span>
      <button
        v-if="post.mine"
        type="button"
        class="absolute right-3 top-3 grid size-7 place-items-center rounded-full border border-white/15 bg-ink-950/70 text-[11px] text-mist-300 backdrop-blur transition-colors hover:border-rose-300/50 hover:text-rose-200"
        title="删除这条分享"
        @click="emit('remove')"
      >
        ×
      </button>
    </div>

    <div class="flex flex-1 flex-col gap-2 p-4">
      <div class="flex items-center gap-2">
        <span class="grid size-6 place-items-center rounded-full bg-gradient-to-br from-gold-300 to-gold-500 text-[10px] font-semibold text-ink-950">
          {{ initial }}
        </span>
        <span class="truncate text-[11px] text-mist-400">{{ post.authorName }}</span>
        <span class="ml-auto shrink-0 text-[10px] text-mist-500">{{ createdText }}</span>
      </div>

      <h3 class="line-clamp-1 font-display text-base text-mist-100">{{ post.title }}</h3>
      <p v-if="post.content" class="line-clamp-2 text-xs leading-relaxed text-mist-500">{{ post.content }}</p>

      <div v-if="post.itemNames.length" class="flex flex-wrap gap-1.5">
        <span
          v-for="name in post.itemNames.slice(0, 4)"
          :key="name"
          class="rounded-md border border-white/10 bg-white/[0.04] px-1.5 py-0.5 text-[10px] text-mist-300"
        >
          {{ name }}
        </span>
      </div>

      <div class="mt-auto flex items-center justify-between gap-2 pt-2">
        <button
          type="button"
          class="flex items-center gap-1.5 rounded-full px-2 py-1 text-[11px] transition-colors"
          :class="post.liked ? 'text-rose-300' : 'text-mist-400 hover:text-rose-300'"
          :aria-pressed="post.liked"
          @click="emit('like')"
        >
          <svg
            class="size-4"
            viewBox="0 0 24 24"
            :fill="post.liked ? 'currentColor' : 'none'"
            stroke="currentColor"
            stroke-width="1.6"
          >
            <path d="M12 20s-7-4.6-7-9.4A4.1 4.1 0 0 1 12 7a4.1 4.1 0 0 1 7 3.6C19 15.4 12 20 12 20Z" stroke-linejoin="round" />
          </svg>
          {{ post.likeCount }}
        </button>

        <button
          type="button"
          class="rounded-full border border-white/12 px-2.5 py-1 text-[10px] text-mist-300 transition-colors disabled:opacity-40"
          :class="{ 'hover:border-gold-400/50 hover:text-gold-200': canTryOn }"
          :disabled="!canTryOn"
          @click="emit('tryOn', post.itemIds)"
        >
          试穿此搭
        </button>
      </div>
    </div>
  </article>
</template>