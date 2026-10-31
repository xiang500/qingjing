<script setup lang="ts">
import type { Recommendation } from '@/types/ai'

const props = defineProps<{ recommendation: Recommendation }>()

const emit = defineEmits<{
  (event: 'retry'): void
}>()

const scoreTone = (score: number) => {
  if (score >= 90) {
    return 'text-emerald-200 border-emerald-300/30 bg-emerald-400/10'
  }
  if (score >= 80) {
    return 'text-gold-200 border-gold-300/30 bg-gold-400/10'
  }
  return 'text-mist-300 border-white/15 bg-white/5'
}

const totalWears = () => props.recommendation.items.reduce((sum, item) => sum + (item.wearCount ?? 0), 0)
</script>

<template>
  <div class="mt-3 overflow-hidden rounded-2xl border border-gold-400/20 bg-gradient-to-br from-gold-400/[0.07] to-transparent">
    <div class="flex items-center justify-between gap-3 border-b border-white/5 px-4 py-2.5">
      <div class="flex min-w-0 items-center gap-2">
        <span class="rounded-full border border-gold-300/30 bg-gold-400/10 px-2 py-0.5 text-[10px] text-gold-200">
          AI 搭配
        </span>
        <span class="truncate text-xs text-mist-200">{{ recommendation.name }}</span>
        <span v-if="recommendation.occasion" class="text-[10px] text-mist-500">· {{ recommendation.occasion }}</span>
      </div>
      <span
        v-if="recommendation.score != null"
        class="shrink-0 rounded-full border px-2 py-0.5 text-[10px]"
        :class="scoreTone(recommendation.score)"
      >
        契合度 {{ recommendation.score }}
      </span>
    </div>

    <div class="grid grid-cols-4 gap-2 px-4 py-3 sm:grid-cols-5">
      <figure v-for="item in recommendation.items" :key="item.id" class="group min-w-0">
        <div
          class="relative aspect-square overflow-hidden rounded-xl border border-white/10 bg-ink-900/60"
          :style="{ background: `radial-gradient(120% 120% at 30% 10%, ${item.colorHex ?? '#8CA3B8'}33, rgba(7,10,16,0.4) 70%)` }"
        >
          <img
            v-if="item.imageUrl"
            :src="item.imageUrl"
            :alt="item.name"
            class="size-full object-contain p-1.5"
            loading="lazy"
          />
          <span v-else class="grid size-full place-items-center text-xs text-mist-500">
            {{ item.categoryLabel }}
          </span>
        </div>
        <figcaption class="mt-1 line-clamp-1 text-[10px] text-mist-400">{{ item.name }}</figcaption>
      </figure>
    </div>

    <div class="space-y-2 px-4 pb-3">
      <p v-if="recommendation.reason" class="text-xs leading-relaxed text-mist-400">
        {{ recommendation.reason }}
      </p>
      <div class="flex flex-wrap items-center gap-3 text-[10px] text-mist-500">
        <span>{{ recommendation.items.length }} 件单品</span>
        <span class="size-1 rounded-full bg-mist-500/40" />
        <span>累计穿着 {{ totalWears() }} 次</span>
        <span class="ml-auto flex items-center gap-2">
          <button type="button" class="btn-ghost !px-2.5 !py-1 !text-[10px]" @click="emit('retry')">
            换一套
          </button>
          <button
            type="button"
            class="btn-ghost !border-gold-400/30 !px-2.5 !py-1 !text-[10px] !text-gold-200 opacity-60"
            disabled
            title="阶段三接入 3D 换装后启用"
          >
            在试衣镜试穿
          </button>
        </span>
      </div>
    </div>
  </div>
</template>