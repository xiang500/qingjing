<script setup lang="ts">
defineProps<{
  title: string
  description: string
  actionLabel?: string
}>()

const emit = defineEmits<{ (event: 'action'): void }>()
</script>

<template>
  <div class="glass-card flex flex-col items-center justify-center gap-3 px-6 py-16 text-center">
    <span class="grid size-14 place-items-center rounded-full border border-gold-400/25 bg-gold-400/8">
      <svg class="size-6 text-gold-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4">
        <path d="M4 19h16M7 19V9l5-4 5 4v10" stroke-linecap="round" stroke-linejoin="round" />
        <path d="M10 19v-4h4v4" stroke-linecap="round" />
      </svg>
    </span>
    <h3 class="font-display text-lg text-mist-100">{{ title }}</h3>
    <p class="max-w-md text-sm text-mist-500">{{ description }}</p>
    <button v-if="actionLabel" type="button" class="btn-primary mt-2" @click="emit('action')">
      {{ actionLabel }}
    </button>
  </div>
</template>