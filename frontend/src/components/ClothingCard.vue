<script setup lang="ts">
import { computed, ref } from 'vue'
import { SEASON_LABELS, type Clothing } from '@/types/clothing'

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

const fallbackStyle = computed(() => ({
  background: `radial-gradient(120% 120% at 30% 10%, ${props.item.accentColor}33, rgba(7,10,16,0.2) 70%)`,
}))
</script>

<template>
  <article
    class="group relative flex flex-col overflow-hidden rounded-2xl border border-white/10 bg-white/[0.03] transition duration-300 hover:-translate-y-1 hover:border-gold-400/35 hover:shadow-[0_24px_60px_-30px_rgba(214,189,128,0.55)]"
  >
    <div class="relative aspect-[4/5] overflow-hidden" :style="fallbackStyle">
      <img
        v-if="item.imageUrl && !imageFailed"
        :src="item.imageUrl"
        :alt="item.name"
        loading="lazy"
        class="size-full object-contain p-5 transition-transform duration-500 group-hover:scale-[1.05]"
        @error="imageFailed = true"
      />
      <div v-else class="grid size-full place-items-center">
        <span class="font-display text-4xl text-mist-100/30">{{ item.categoryLabel.slice(0, 1) }}</span>
      </div>

      <span
        class="absolute left-3 top-3 rounded-full border border-white/15 bg-ink-950/70 px-2.5 py-1 text-[11px] text-mist-200 backdrop-blur"
      >
        {{ item.categoryLabel }}
      </span>

      <button
        type="button"
        class="absolute right-3 top-3 grid size-8 place-items-center rounded-full border border-white/15 bg-ink-950/70 backdrop-blur transition-colors"
        :class="item.favorite ? 'text-rose-300' : 'text-mist-300 hover:text-rose-300'"
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
        class="absolute inset-x-3 bottom-3 flex translate-y-3 gap-2 opacity-0 transition duration-300 group-hover:translate-y-0 group-hover:opacity-100 group-focus-within:translate-y-0 group-focus-within:opacity-100"
      >
        <button
          type="button"
          class="flex-1 rounded-lg bg-gold-300/95 px-2 py-1.5 text-[11px] font-medium text-ink-950 transition-colors hover:bg-gold-200"
          @click.stop="emit('wear')"
        >
          今天穿了
        </button>
        <button
          type="button"
          class="rounded-lg border border-white/20 bg-ink-950/80 px-2.5 py-1.5 text-[11px] text-mist-100 backdrop-blur transition-colors hover:border-gold-400/50"
          @click.stop="emit('edit')"
        >
          编辑
        </button>
        <button
          type="button"
          class="rounded-lg border border-rose-400/30 bg-ink-950/80 px-2.5 py-1.5 text-[11px] text-rose-200 backdrop-blur transition-colors hover:border-rose-300/60"
          @click.stop="emit('remove')"
        >
          删除
        </button>
      </div>
    </div>

    <div class="flex flex-1 flex-col gap-2 p-4">
      <div class="flex items-start justify-between gap-2">
        <h3 class="line-clamp-1 text-sm font-medium text-mist-100">{{ item.name }}</h3>
        <span v-if="item.colorHex" class="mt-1 size-3 shrink-0 rounded-full border border-white/20" :style="{ background: item.colorHex }" />
      </div>
      <p class="line-clamp-1 text-xs text-mist-500">
        {{ [item.subCategory, item.brand, item.material].filter(Boolean).join(' · ') || '未补充材质信息' }}
      </p>
      <div class="mt-auto flex items-center justify-between pt-2 text-[11px] text-mist-500">
        <span>{{ priceText }}</span>
        <span class="flex items-center gap-1">
          <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
            <path d="M12 7v5l3 2" stroke-linecap="round" />
            <circle cx="12" cy="12" r="8" />
          </svg>
          {{ item.wearCount }} 次
        </span>
      </div>
      <div class="flex flex-wrap items-center gap-1.5">
        <span v-if="seasonText" class="rounded-md bg-white/5 px-1.5 py-0.5 text-[10px] text-mist-300">
          {{ seasonText }}
        </span>
        <span
          v-for="tag in item.styleTags.slice(0, 2)"
          :key="tag"
          class="rounded-md bg-white/5 px-1.5 py-0.5 text-[10px] text-mist-300"
        >
          {{ tag }}
        </span>
      </div>
    </div>
  </article>
</template>