<script setup lang="ts">
import type { CategoryCode, CategoryMeta } from '@/types/clothing'

const props = defineProps<{
  categories: CategoryMeta[]
  category: CategoryCode | ''
  keyword: string
  favorite: boolean
  sort: 'latest' | 'wearCount' | 'priceAsc' | 'priceDesc'
  total: number
  hasActiveFilter: boolean
}>()

const emit = defineEmits<{
  (event: 'update:category', value: CategoryCode | ''): void
  (event: 'update:keyword', value: string): void
  (event: 'update:favorite', value: boolean): void
  (event: 'update:sort', value: 'latest' | 'wearCount' | 'priceAsc' | 'priceDesc'): void
  (event: 'reset'): void
}>()

const sortOptions = [
  { value: 'latest', label: '最近入库' },
  { value: 'wearCount', label: '穿着高频' },
  { value: 'priceDesc', label: '价格从高到低' },
  { value: 'priceAsc', label: '价格从低到高' },
] as const

const search = (event: Event) => {
  emit('update:keyword', (event.target as HTMLInputElement).value)
}
</script>

<template>
  <div class="glass-card space-y-4 p-4 sm:p-5">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-center">
      <div class="relative flex-1">
        <svg
          class="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-mist-500"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.6"
        >
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.2-3.2" stroke-linecap="round" />
        </svg>
        <input
          :value="keyword"
          class="field-input pl-9"
          type="search"
          placeholder="搜索单品名称、品牌、材质或颜色…"
          aria-label="搜索衣橱"
          @input="search"
        />
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <button
          type="button"
          class="chip"
          :class="{ 'chip-active': favorite }"
          @click="emit('update:favorite', !favorite)"
        >
          <svg
            class="size-3.5"
            viewBox="0 0 24 24"
            :fill="favorite ? 'currentColor' : 'none'"
            stroke="currentColor"
            stroke-width="1.6"
          >
            <path
              d="M12 20s-7-4.6-7-9.4A4.1 4.1 0 0 1 12 7a4.1 4.1 0 0 1 7 3.6C19 15.4 12 20 12 20Z"
              stroke-linejoin="round"
            />
          </svg>
          仅看收藏
        </button>

        <label class="relative">
          <select
            :value="sort"
            class="appearance-none rounded-full border border-white/10 bg-ink-900/70 py-2 pl-4 pr-9 text-xs text-mist-200 outline-none transition-colors hover:border-gold-400/40 focus:border-gold-400/60"
            aria-label="排序方式"
            @change="emit('update:sort', ($event.target as HTMLSelectElement).value as typeof sort)"
          >
            <option v-for="option in sortOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
          <svg
            class="pointer-events-none absolute right-3 top-1/2 size-3.5 -translate-y-1/2 text-mist-500"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="m6 9 6 6 6-6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </label>
      </div>
    </div>

    <div class="flex flex-wrap items-center gap-2">
      <button
        type="button"
        class="chip"
        :class="{ 'chip-active': category === '' }"
        @click="emit('update:category', '')"
      >
        全部
      </button>
      <button
        v-for="item in props.categories"
        :key="item.code"
        type="button"
        class="chip"
        :class="{ 'chip-active': category === item.code }"
        @click="emit('update:category', item.code)"
      >
        <span class="size-1.5 rounded-full" :style="{ background: item.accentColor }" />
        {{ item.label }}
      </button>

      <span class="ml-auto flex items-center gap-3 text-xs text-mist-500">
        <span>共 {{ total }} 件</span>
        <button
          v-if="hasActiveFilter"
          type="button"
          class="text-gold-300 transition-colors hover:text-gold-200"
          @click="emit('reset')"
        >
          清空筛选
        </button>
      </span>
    </div>
  </div>
</template>