<script setup lang="ts">
import { computed } from 'vue'
import type { CategoryMeta, WardrobeStats } from '@/types/clothing'

const props = defineProps<{
  stats: WardrobeStats | null
  categories: CategoryMeta[]
  loading: boolean
}>()

const money = (value: number) =>
  value.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 0 })

const cards = computed(() => {
  const stats = props.stats
  return [
    {
      key: 'total',
      label: '衣橱总件数',
      en: 'Pieces',
      value: stats ? String(stats.totalCount) : '—',
      unit: '件',
      hint: stats ? `覆盖 ${stats.categoryCount} 个品类` : '正在读取衣橱…',
      glow: 'rgba(214, 189, 128, 0.35)',
    },
    {
      key: 'favorite',
      label: '收藏单品',
      en: 'Favorites',
      value: stats ? String(stats.favoriteCount) : '—',
      unit: '件',
      hint: stats && stats.totalCount > 0
        ? `占比 ${Math.round((stats.favoriteCount / stats.totalCount) * 100)}%`
        : '标记你的心动物件',
      glow: 'rgba(201, 138, 155, 0.32)',
    },
    {
      key: 'unworn',
      label: '待唤醒单品',
      en: 'Unworn',
      value: stats ? String(stats.unwornCount) : '—',
      unit: '件',
      hint: stats ? `平均穿着 ${stats.avgWearCount} 次` : '从未穿过的单品',
      glow: 'rgba(143, 163, 126, 0.32)',
    },
    {
      key: 'value',
      label: '衣橱总值',
      en: 'Value',
      value: stats ? `¥${money(stats.totalValue)}` : '—',
      unit: '',
      hint: stats?.topWornNames.length
        ? `最常穿：${stats.topWornNames[0]}`
        : '记录购入价即可统计',
      glow: 'rgba(124, 158, 195, 0.32)',
    },
  ]
})

const distribution = computed(() => {
  const counts = props.stats?.categoryCounts ?? {}
  const total = Object.values(counts).reduce((sum, value) => sum + value, 0)
  if (total === 0) {
    return []
  }
  return props.categories
    .map((category) => ({
      label: category.label,
      color: category.accentColor,
      count: counts[category.label] ?? 0,
    }))
    .filter((entry) => entry.count > 0)
    .map((entry) => ({ ...entry, percent: (entry.count / total) * 100 }))
})
</script>

<template>
  <section class="space-y-4">
    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <article
        v-for="card in cards"
        :key="card.key"
        class="glass-card relative overflow-hidden p-5"
      >
        <span
          class="pointer-events-none absolute -right-8 -top-10 size-24 rounded-full blur-2xl"
          :style="{ background: card.glow }"
        />
        <div class="flex items-center justify-between text-[10px] uppercase tracking-[0.3em] text-mist-500">
          <span>{{ card.en }}</span>
          <span>{{ card.label }}</span>
        </div>
        <div class="mt-4 flex items-baseline gap-1.5">
          <span
            class="font-display text-3xl leading-none text-mist-100 transition-opacity"
            :class="{ 'opacity-40': loading }"
          >
            {{ card.value }}
          </span>
          <span v-if="card.unit" class="text-xs text-mist-500">{{ card.unit }}</span>
        </div>
        <p class="mt-3 truncate text-xs text-mist-500">{{ card.hint }}</p>
      </article>
    </div>

    <div v-if="distribution.length" class="glass-card p-5">
      <div class="flex items-center justify-between text-xs text-mist-500">
        <span class="uppercase tracking-[0.28em]">品类分布</span>
        <span>{{ stats?.totalCount }} 件单品</span>
      </div>
      <div class="mt-4 flex h-2.5 w-full overflow-hidden rounded-full bg-white/5">
        <span
          v-for="entry in distribution"
          :key="entry.label"
          class="h-full transition-all duration-500"
          :style="{ width: `${entry.percent}%`, background: entry.color }"
        />
      </div>
      <ul class="mt-4 flex flex-wrap gap-x-5 gap-y-2 text-xs text-mist-300">
        <li v-for="entry in distribution" :key="entry.label" class="flex items-center gap-2">
          <span class="size-2 rounded-full" :style="{ background: entry.color }" />
          <span>{{ entry.label }}</span>
          <span class="text-mist-500">{{ entry.count }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>