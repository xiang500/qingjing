<script setup lang="ts">
import type { ThinkingStep } from '@/types/ai'

defineProps<{
  steps: ThinkingStep[]
  active: boolean
}>()
</script>

<template>
  <div class="flex flex-col gap-1.5 rounded-xl border border-white/8 bg-white/[0.02] px-3 py-2.5">
    <div
      v-for="(step, index) in steps"
      :key="step.phase"
      class="flex items-center gap-2 text-[11px]"
      :class="
        active && index === steps.length - 1 ? 'text-gold-200' : 'text-mist-500'
      "
    >
      <span
        v-if="active && index === steps.length - 1"
        class="size-1.5 animate-pulse rounded-full bg-gold-300"
      />
      <svg
        v-else
        class="size-3 text-emerald-300/80"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2.4"
      >
        <path d="m5 13 4 4L19 7" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
      <span>{{ step.text }}</span>
    </div>
  </div>
</template>