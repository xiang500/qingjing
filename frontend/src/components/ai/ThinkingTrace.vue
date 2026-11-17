<script setup lang="ts">
import type { ThinkingStep } from '@/types/ai'

defineProps<{
  steps: ThinkingStep[]
  active: boolean
}>()
</script>

<template>
  <details class="trace glass-soft overflow-hidden" :open="active">
    <summary
      class="flex cursor-pointer list-none items-center gap-2 px-3.5 py-2 text-[11px] text-mist-400 transition-colors duration-200 hover:text-mist-200"
    >
      <span
        class="size-1.5 shrink-0 rounded-full"
        :class="active ? 'animate-pulse bg-gold-300' : 'bg-mist-500/60'"
      />
      <span class="tracking-[0.14em]">思考轨迹</span>
      <span class="text-mist-500">{{ steps.length }} 步</span>
      <span v-if="active" class="text-gold-300">· 进行中</span>
      <svg
        class="chevron ml-auto size-3.5 shrink-0 text-mist-500"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
      >
        <path d="m6 9 6 6 6-6" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
    </summary>

    <ul class="divide-y divide-ink-700/40 border-t border-ink-700/40 px-3.5">
      <li
        v-for="(step, index) in steps"
        :key="step.phase"
        class="flex items-center gap-2 py-1.5 text-[11px] leading-relaxed"
        :class="active && index === steps.length - 1 ? 'text-gold-300' : 'text-mist-400'"
      >
        <span
          v-if="active && index === steps.length - 1"
          class="size-1.5 shrink-0 animate-pulse rounded-full bg-gold-300"
        />
        <svg
          v-else
          class="size-3 shrink-0 text-mist-500"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.4"
        >
          <path d="m5 13 4 4L19 7" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <span class="min-w-0">{{ step.text }}</span>
      </li>
    </ul>
  </details>
</template>

<style scoped>
/* 原生 details 的折叠箭头收起，换成右侧那只，只动 transform */
.trace > summary {
  list-style: none;
}

.trace > summary::-webkit-details-marker {
  display: none;
}

.trace .chevron {
  transition: transform 0.22s ease;
}

.trace[open] .chevron {
  transform: rotate(180deg);
}
</style>
