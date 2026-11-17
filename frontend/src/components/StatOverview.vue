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
    },
    {
      key: 'favorite',
      label: '收藏单品',
      en: 'Favorites',
      value: stats ? String(stats.favoriteCount) : '—',
      unit: '件',
      hint:
        stats && stats.totalCount > 0
          ? `占比 ${Math.round((stats.favoriteCount / stats.totalCount) * 100)}%`
          : '标记你的心动物件',
    },
    {
      key: 'unworn',
      label: '待唤醒单品',
      en: 'Unworn',
      value: stats ? String(stats.unwornCount) : '—',
      unit: '件',
      hint: stats ? `平均穿着 ${stats.avgWearCount} 次` : '从未穿过的单品',
    },
    {
      key: 'value',
      label: '衣橱总值',
      en: 'Value',
      value: stats ? `¥${money(stats.totalValue)}` : '—',
      unit: '',
      hint: stats?.topWornNames.length ? `最常穿：${stats.topWornNames[0]}` : '记录购入价即可统计',
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
  <section class="glass overflow-hidden">
    <div class="grid divide-y divide-ink-700/40 sm:grid-cols-2 sm:divide-y-0 xl:grid-cols-4">
      <article
        v-for="card in cards"
        :key="card.key"
        class="relative px-5 py-4 sm:border-l sm:border-ink-700/40 sm:first:border-l-0"
      >
        <div class="flex items-center justify-between gap-2 text-[10px] uppercase tracking-[0.26em] text-mist-500">
          <span class="truncate">{{ card.en }}</span>
          <span class="shrink-0 tracking-normal text-mist-400">{{ card.label }}</span>
        </div>
        <div class="mt-2 flex items-baseline gap-1.5">
          <span
            class="display text-[1.75rem] leading-none text-mist-100 tabular-nums transition-opacity"
            :class="{ 'opacity-40': loading }"
          >
            {{ card.value }}
          </span>
          <span v-if="card.unit" class="text-[11px] text-mist-500">{{ card.unit }}</span>
        </div>
        <p class="mt-1.5 truncate text-[11px] text-mist-400">{{ card.hint }}</p>
      </article>
    </div>

    <div v-if="distribution.length" class="border-t border-ink-700/40 px-5 py-3">
      <div class="flex items-center gap-4">
        <span class="shrink-0 text-[10px] uppercase tracking-[0.24em] text-mist-500">品类分布</span>
        <div class="flex h-1.5 flex-1 overflow-hidden rounded-full bg-ink-700/40">
          <span
            v-for="entry in distribution"
            :key="entry.label"
            class="h-full transition-all duration-500"
            :style="{ width: `${entry.percent}%`, background: entry.color }"
          />
        </div>
        <span class="shrink-0 text-[11px] text-mist-500">{{ stats?.totalCount }} 件</span>
      </div>
      <ul class="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px] text-mist-300">
        <li v-for="entry in distribution" :key="entry.label" class="flex items-center gap-1.5">
          <span class="size-1.5 rounded-full" :style="{ background: entry.color }" />
          <span>{{ entry.label }}</span>
          <span class="text-mist-500">{{ entry.count }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>
