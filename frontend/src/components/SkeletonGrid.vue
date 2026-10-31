<script setup lang="ts">
withDefaults(defineProps<{ count?: number }>(), { count: 8 })
</script>

<template>
  <div class="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-4">
    <div
      v-for="index in count"
      :key="index"
      class="overflow-hidden rounded-2xl border border-white/8 bg-white/[0.02]"
    >
      <div class="skeleton aspect-[4/5] w-full" />
      <div class="space-y-2 p-4">
        <div class="skeleton h-3.5 w-3/4 rounded" />
        <div class="skeleton h-3 w-1/2 rounded" />
        <div class="skeleton h-3 w-2/5 rounded" />
      </div>
    </div>
  </div>
</template>