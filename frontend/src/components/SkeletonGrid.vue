<script setup lang="ts">
withDefaults(defineProps<{ count?: number }>(), { count: 8 })
</script>

<template>
  <div class="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-4">
    <div
      v-for="index in count"
      :key="index"
      class="glass-soft overflow-hidden rounded-[1.35rem] p-2"
    >
      <div class="skeleton aspect-[4/5] w-full rounded-[1.05rem]" />
      <div class="space-y-2 px-2 pb-2 pt-3">
        <div class="skeleton h-3.5 w-3/4 rounded" />
        <div class="skeleton h-3 w-1/2 rounded" />
        <div class="skeleton h-3 w-2/5 rounded" />
      </div>
    </div>
  </div>
</template>
