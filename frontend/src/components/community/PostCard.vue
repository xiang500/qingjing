<script setup lang="ts">
import { computed } from 'vue'
import type { CommunityPost } from '@/types/community'
import { isPhotoImage } from '@/utils/image'

const props = defineProps<{ post: CommunityPost }>()

const emit = defineEmits<{
  (event: 'like'): void
  (event: 'remove'): void
  (event: 'comments'): void
  (event: 'tryOn', ids: number[]): void
}>()

const cover = computed(() => props.post.images[0])
const covers = computed(() => props.post.images.filter(Boolean))
const collage = computed(() => covers.value.length >= 3)
const initial = computed(() => props.post.authorName.slice(0, 1))
const canTryOn = computed(() => props.post.itemIds.length > 0)

/** 实拍照片铺满裁剪；透明底插画 / SVG 等比留白并保留原有内边距 */
function imageClass(url: string | undefined, padClass: string): string {
  return isPhotoImage(url) ? 'object-cover' : `object-contain ${padClass}`
}

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
  <article
    class="glass-soft group flex flex-col overflow-hidden rounded-[1.35rem] p-2 transition duration-300 hover:-translate-y-1"
  >
    <!-- 封面：固定 4:5，统一衬在深色影棚底上，任何季节都清晰 -->
    <div class="photo-tile relative aspect-[4/5] overflow-hidden rounded-[1.05rem]">
      <!-- 多图拼贴：一大两小 -->
      <div v-if="collage" class="absolute inset-2.5 grid grid-cols-[1.55fr_1fr] gap-2">
        <div class="min-h-0 overflow-hidden rounded-xl bg-black/25 ring-1 ring-white/10">
          <img
            :src="covers[0]"
            :alt="post.title"
            loading="lazy"
            class="size-full transition-transform duration-300 group-hover:scale-[1.03]"
            :class="imageClass(covers[0], 'p-2')"
          />
        </div>
        <div class="grid min-h-0 grid-rows-2 gap-2">
          <div
            v-for="(src, index) in covers.slice(1, 3)"
            :key="`${src}-${index}`"
            class="min-h-0 overflow-hidden rounded-xl bg-black/25 ring-1 ring-white/10"
          >
            <img
              :src="src"
              :alt="post.title"
              loading="lazy"
              class="size-full transition-transform duration-300 group-hover:scale-[1.03]"
              :class="imageClass(src, 'p-1.5')"
            />
          </div>
        </div>
        <span
          v-if="covers.length > 3"
          class="on-photo absolute bottom-4 left-4 rounded-full border border-white/15 bg-black/40 px-2 py-0.5 text-[10px] backdrop-blur"
        >
          +{{ covers.length - 3 }} 张
        </span>
      </div>

      <img
        v-else-if="cover"
        :src="cover"
        :alt="post.title"
        loading="lazy"
        class="size-full transition-transform duration-300 group-hover:scale-[1.03]"
        :class="imageClass(cover, 'p-4')"
      />
      <div v-else class="grid size-full place-items-center">
        <span class="display text-4xl text-white/25">{{ post.title.slice(0, 1) }}</span>
        <span class="mt-1 text-[10px] tracking-[0.24em] text-white/35">NO IMAGE</span>
      </div>

      <!-- 悬浮在图片上的信息：永远用浅色文字 -->
      <span
        v-if="post.outfitName"
        class="on-photo absolute left-2.5 top-2.5 max-w-[72%] truncate rounded-full border border-white/20 bg-black/35 px-2.5 py-1 text-[10px] tracking-[0.06em] backdrop-blur"
      >
        {{ post.outfitName }}
      </span>
      <button
        v-if="post.mine"
        type="button"
        class="absolute right-2.5 top-2.5 grid size-7 place-items-center rounded-full border border-white/20 bg-black/35 text-white/70 backdrop-blur transition-colors hover:border-gold-300/60 hover:text-gold-200"
        title="删除这条分享"
        aria-label="删除这条分享"
        @click="emit('remove')"
      >
        <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="m6 6 12 12M18 6 6 18" stroke-linecap="round" />
        </svg>
      </button>
    </div>

    <div class="flex flex-1 flex-col gap-2 px-2 pb-1.5 pt-3">
      <!-- 作者：头像是强调色的「印章」，外圈一道细环 -->
      <div class="flex items-center gap-2.5">
        <span class="author-seal size-7 shrink-0">{{ initial }}</span>
        <div class="min-w-0 flex-1">
          <p class="truncate text-[12px] font-medium text-mist-200">{{ post.authorName }}</p>
          <p class="text-[10px] tracking-[0.06em] text-mist-500">{{ createdText || '刚刚分享' }}</p>
        </div>
        <span
          v-if="post.mine"
          class="chip shrink-0 cursor-default border-gold-300/30 px-2 py-0.5 text-[9px] text-gold-200"
        >
          我的
        </span>
      </div>

      <h3 class="display line-clamp-1 text-[15px] text-mist-100">{{ post.title }}</h3>
      <p v-if="post.content" class="line-clamp-2 text-xs leading-relaxed text-mist-400">
        {{ post.content }}
      </p>

      <!-- 套装标签：chip 小号 -->
      <div v-if="post.itemNames.length" class="flex flex-wrap items-center gap-1">
        <span
          v-for="name in post.itemNames.slice(0, 3)"
          :key="name"
          class="chip cursor-default px-2 py-0.5 text-[10px] text-mist-300"
        >
          {{ name }}
        </span>
        <span
          v-if="post.itemNames.length > 3"
          class="chip cursor-default px-2 py-0.5 text-[10px] text-mist-500"
        >
          +{{ post.itemNames.length - 3 }}
        </span>
      </div>

      <!-- 操作区：点赞激活用当前季节的 accent -->
      <div class="mt-auto flex items-center gap-1.5 border-t border-ink-700/40 pt-2.5">
        <button
          type="button"
          class="chip px-2.5 py-1 text-[11px]"
          :class="{ 'chip-active': post.liked }"
          :aria-pressed="post.liked"
          @click="emit('like')"
        >
          <svg
            class="size-3.5"
            viewBox="0 0 24 24"
            :fill="post.liked ? 'currentColor' : 'none'"
            stroke="currentColor"
            stroke-width="1.6"
          >
            <path
              d="M12 20s-7-4.6-7-9.4A4.1 4.1 0 0 1 12 7a4.1 4.1 0 0 1 7 3.6C19 15.4 12 20 12 20Z"
              stroke-linejoin="round"
            />
          </svg>
          <span class="tabular-nums">{{ post.likeCount }}</span>
        </button>

        <button type="button" class="chip px-2.5 py-1 text-[11px]" @click="emit('comments')">
          <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
            <path
              d="M20 12a8 8 0 0 1-8 8H5l-1 2v-4.5A8 8 0 0 1 12 4a8 8 0 0 1 8 8Z"
              stroke-linejoin="round"
            />
          </svg>
          <span class="tabular-nums">{{ post.commentCount }}</span>
        </button>

        <span class="flex-1" />

        <button
          type="button"
          class="chip px-2.5 py-1 text-[10px] tracking-[0.04em]"
          :class="canTryOn ? 'hover:border-gold-300/50' : 'opacity-70'"
          @click="emit('tryOn', post.itemIds)"
        >
          试穿此搭
          <svg class="size-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="m9 6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
/* 作者印章：用当前季节的 accent 做渐变，字色跟随 accent-ink 自动适配深浅季 */
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

/* 头像外圈细环，和 App 顶部头像同一种语言 */
.author-seal::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: inherit;
  border: 1px solid color-mix(in oklab, var(--c-accent) 42%, transparent);
  pointer-events: none;
}
</style>
