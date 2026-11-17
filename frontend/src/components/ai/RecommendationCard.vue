<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { Recommendation } from '@/types/ai'
import { downloadShareCard } from '@/utils/shareCard'
import { createOutfit } from '@/api/outfit'
import { adoptRecommendation } from '@/api/ai'

const props = defineProps<{ recommendation: Recommendation }>()

const emit = defineEmits<{
  (event: 'retry'): void
}>()

const router = useRouter()

function tryOn() {
  const ids = props.recommendation.items.map((item) => item.id).join(',')
  const recommendationId = props.recommendation.recommendationId
  if (recommendationId) {
    void adoptRecommendation(recommendationId, 'TRY_ON').catch(() => {})
  }
  void router.push({ path: '/mirror', query: { ids } })
}

function share() {
  const images = props.recommendation.items
    .map((item) => item.imageUrl)
    .filter((url): url is string => Boolean(url))
    .join('|')
  void router.push({
    path: '/community',
    query: {
      publish: '1',
      title: props.recommendation.name,
      content: props.recommendation.reason ?? '',
      images,
    },
  })
}

function saveCard() {
  void downloadShareCard({
    title: props.recommendation.name,
    reason: props.recommendation.reason,
    score: props.recommendation.score ?? undefined,
    occasion: props.recommendation.occasion,
    items: props.recommendation.items,
  })
}

/** 把 AI 推荐的这套直接存进「我的套装」，和手动保存走同一个接口 */
async function saveOutfit() {
  try {
    await createOutfit({
      name: props.recommendation.name,
      itemIds: props.recommendation.items.map((item) => item.id),
      occasion: props.recommendation.occasion,
      reason: props.recommendation.reason,
      score: props.recommendation.score ?? undefined,
    })
    const recommendationId = props.recommendation.recommendationId
    if (recommendationId) {
      await adoptRecommendation(recommendationId, 'SAVE').catch(() => {})
    }
    ElMessage.success('已保存到「我的套装」')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败，请稍后重试')
  }
}

const totalWears = () => props.recommendation.items.reduce((sum, item) => sum + (item.wearCount ?? 0), 0)
</script>

<template>
  <article class="glass-soft mt-1 overflow-hidden">
    <!-- 卡片头：AI 标签 · 套装名 · 匹配度 -->
    <header class="flex flex-wrap items-center justify-between gap-2 border-b border-ink-700/40 px-3.5 py-2.5">
      <div class="flex min-w-0 items-center gap-2">
        <span
          class="inline-flex shrink-0 items-center gap-1.5 rounded-full border border-gold-300/35 bg-gold-300/10 px-2 py-0.5 text-[10px] tracking-[0.08em] text-gold-200"
        >
          <span class="size-1 rounded-full bg-gold-300" />
          AI 搭配
        </span>
        <span class="truncate text-xs text-mist-200">{{ recommendation.name }}</span>
        <span v-if="recommendation.occasion" class="shrink-0 text-[10px] text-mist-500">
          · {{ recommendation.occasion }}
        </span>
      </div>
      <span
        v-if="recommendation.score != null"
        class="shrink-0 rounded-full border border-gold-300/35 bg-gold-300/10 px-2 py-0.5 text-[10px] text-gold-200 tabular-nums"
      >
        匹配度 {{ recommendation.score }}
      </span>
    </header>

    <!-- 单品缩略图：一律放在深色影棚底上，换季也不影响看清衣服 -->
    <div class="grid grid-cols-4 gap-2 px-3.5 py-3 sm:grid-cols-5">
      <figure v-for="item in recommendation.items" :key="item.id" class="group min-w-0">
        <div class="photo-tile aspect-square rounded-xl ring-1 ring-inset ring-white/10">
          <span
            v-if="item.colorHex"
            class="pointer-events-none absolute inset-0 opacity-25"
            :style="{ background: `radial-gradient(120% 120% at 30% 10%, ${item.colorHex}, transparent 70%)` }"
          />
          <img
            v-if="item.imageUrl"
            :src="item.imageUrl"
            :alt="item.name"
            class="relative size-full object-contain p-2 transition-transform duration-300 group-hover:scale-[1.04]"
            loading="lazy"
          />
          <span v-else class="on-photo relative grid size-full place-items-center text-[11px]">
            {{ item.categoryLabel }}
          </span>
        </div>
        <figcaption class="mt-1 line-clamp-1 text-[10px] text-mist-400">{{ item.name }}</figcaption>
      </figure>
    </div>

    <!-- 推荐理由 · 穿着数据 · 采纳操作 -->
    <div class="space-y-2.5 px-3.5 pb-3.5">
      <p v-if="recommendation.reason" class="text-xs leading-[1.85] text-mist-400">
        {{ recommendation.reason }}
      </p>

      <div class="flex flex-wrap items-center gap-x-3 gap-y-1 text-[10px] text-mist-500">
        <span>{{ recommendation.items.length }} 件单品</span>
        <span class="size-1 rounded-full bg-mist-500/40" />
        <span>累计穿着 {{ totalWears() }} 次</span>
      </div>

      <div class="flex flex-wrap items-center gap-1.5 border-t border-ink-700/40 pt-2.5">
        <button type="button" class="btn-primary px-3.5 py-1.5 text-[11px]" @click="tryOn">
          在工作台试穿
        </button>
        <button type="button" class="btn-ghost px-3 py-1.5 text-[11px]" @click="saveOutfit">
          保存套装
        </button>
        <button type="button" class="btn-ghost px-3 py-1.5 text-[11px]" @click="emit('retry')">
          换一套
        </button>
        <button type="button" class="btn-ghost px-3 py-1.5 text-[11px]" @click="saveCard">
          存分享卡
        </button>
        <button type="button" class="btn-ghost px-3 py-1.5 text-[11px] text-gold-300" @click="share">
          分享到社区
        </button>
      </div>
    </div>
  </article>
</template>
