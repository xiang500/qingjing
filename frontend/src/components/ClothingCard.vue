<script setup lang="ts">
import { computed, ref } from 'vue'
import { SEASON_LABELS, type Clothing } from '@/types/clothing'
import { catalogImage, isPhotoImage } from '@/utils/image'

const props = defineProps<{ item: Clothing }>()

const emit = defineEmits<{
  (event: 'favorite'): void
  (event: 'wear'): void
  (event: 'edit'): void
  (event: 'remove'): void
}>()

const imageFailed = ref(false)

const priceText = computed(() =>
  props.item.price == null ? '未记录价格' : `¥${Number(props.item.price).toLocaleString('zh-CN')}`,
)

const seasonText = computed(() =>
  props.item.season.map((season) => SEASON_LABELS[season] ?? season).join(' · '),
)

/** 单品图统一用透明底版本，衬在固定的深色影棚底上 */
const imageUrl = computed(() => catalogImage(props.item.imageUrl))

/** 实拍照片铺满裁剪；透明底插画 / SVG 等比留白，避免四周出现大片空白 */
const isPhoto = computed(() => isPhotoImage(imageUrl.value))

const tileStyle = computed(() => ({
  background: `radial-gradient(120% 120% at 32% 8%, ${props.item.accentColor}2e, transparent 62%), #101216`,
}))
</script>

<template>
  <article
    class="glass-soft group relative flex flex-col overflow-hidden rounded-[1.35rem] p-2 transition duration-300 hover:-translate-y-1"
  >
    <div class="product-tile relative aspect-[4/5] overflow-hidden rounded-[1.05rem]" :style="tileStyle">
      <img
        v-if="imageUrl && !imageFailed"
        :src="imageUrl"
        :alt="item.name"
        loading="lazy"
        decoding="async"
        class="size-full transition-transform duration-500 group-hover:scale-[1.05]"
        :class="isPhoto ? 'object-cover' : 'object-contain p-4'"
        @error="imageFailed = true"
      />
      <div v-else class="grid size-full place-items-center">
        <span class="display text-4xl text-white/20">{{ item.categoryLabel.slice(0, 1) }}</span>
      </div>

      <span
        class="absolute left-2.5 top-2.5 rounded-full border border-white/20 bg-black/35 px-2.5 py-1 text-[10px] tracking-[0.1em] text-white/85 backdrop-blur"
      >
        {{ item.categoryLabel }}
      </span>

      <button
        type="button"
        class="absolute right-2.5 top-2.5 grid size-8 place-items-center rounded-full border border-white/20 bg-black/35 text-white/75 backdrop-blur transition-colors"
        :class="item.favorite ? 'text-rose-300' : 'hover:text-rose-300'"
        :aria-label="item.favorite ? '取消收藏' : '收藏'"
        :aria-pressed="item.favorite"
        @click.stop="emit('favorite')"
      >
        <svg
          class="size-4"
          viewBox="0 0 24 24"
          :fill="item.favorite ? 'currentColor' : 'none'"
          stroke="currentColor"
          stroke-width="1.6"
        >
          <path
            d="M12 20s-7-4.6-7-9.4A4.1 4.1 0 0 1 12 7a4.1 4.1 0 0 1 7 3.6C19 15.4 12 20 12 20Z"
            stroke-linejoin="round"
          />
        </svg>
      </button>

      <div
        class="absolute inset-x-2.5 bottom-2.5 flex translate-y-3 gap-1.5 opacity-0 transition duration-300 group-hover:translate-y-0 group-hover:opacity-100 group-focus-within:translate-y-0 group-focus-within:opacity-100"
      >
        <button
          type="button"
          class="flex-1 rounded-lg bg-gold-300 px-2 py-1.5 text-[11px] font-medium text-ink-950 transition-opacity hover:opacity-90"
          @click.stop="emit('wear')"
        >
          今天穿了
        </button>
        <button
          type="button"
          class="rounded-lg border border-white/25 bg-black/50 px-2.5 py-1.5 text-[11px] text-white/90 backdrop-blur transition-colors hover:border-white/50"
          @click.stop="emit('edit')"
        >
          编辑
        </button>
        <button
          type="button"
          class="rounded-lg border border-rose-300/40 bg-black/50 px-2.5 py-1.5 text-[11px] text-rose-200 backdrop-blur transition-colors hover:border-rose-300/70"
          @click.stop="emit('remove')"
        >
          删除
        </button>
      </div>
    </div>

    <div class="flex flex-1 flex-col gap-1.5 px-2 pb-1.5 pt-3">
      <div class="flex items-start justify-between gap-2">
        <h3 class="line-clamp-1 text-[13px] font-medium text-mist-100">{{ item.name }}</h3>
        <span
          v-if="item.colorHex"
          class="mt-1 size-3 shrink-0 rounded-full ring-1 ring-inset ring-black/15"
          :style="{ background: item.colorHex }"
        />
      </div>
      <p class="line-clamp-1 text-[11px] text-mist-500">
        {{ [item.subCategory, item.brand, item.material].filter(Boolean).join(' · ') || '未补充材质信息' }}
      </p>
      <div class="mt-auto flex items-center justify-between pt-1.5 text-[11px] text-mist-500">
        <span class="tabular-nums">{{ priceText }}</span>
        <span class="flex items-center gap-1 tabular-nums">
          <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
            <path d="M12 7v5l3 2" stroke-linecap="round" />
            <circle cx="12" cy="12" r="8" />
          </svg>
          {{ item.wearCount }} 次
        </span>
      </div>
      <div class="flex flex-wrap items-center gap-1.5">
        <span v-if="seasonText" class="rounded-md border border-ink-700/50 px-1.5 py-0.5 text-[10px] text-mist-400">
          {{ seasonText }}
        </span>
        <span
          v-for="tag in item.styleTags.slice(0, 2)"
          :key="tag"
          class="rounded-md border border-ink-700/50 px-1.5 py-0.5 text-[10px] text-mist-400"
        >
          {{ tag }}
        </span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.product-tile {
  isolation: isolate;
}
</style>
