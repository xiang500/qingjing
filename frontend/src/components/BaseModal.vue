<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    open: boolean
    title?: string
    description?: string
    width?: 'sm' | 'md' | 'lg'
  }>(),
  { title: '', description: '', width: 'md' },
)

const emit = defineEmits<{ (event: 'close'): void }>()

const widthClass: Record<'sm' | 'md' | 'lg', string> = {
  sm: 'max-w-sm',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
}

const onKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && props.open) {
    emit('close')
  }
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      document.body.style.overflow = 'hidden'
      window.addEventListener('keydown', onKeydown)
    } else {
      document.body.style.overflow = ''
      window.removeEventListener('keydown', onKeydown)
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="open" class="fixed inset-0 z-50 flex items-end justify-center p-0 sm:items-center sm:p-6">
        <div class="modal-veil absolute inset-0" @click="emit('close')" />
        <div
          class="glass relative w-full overflow-hidden rounded-t-[1.6rem] sm:rounded-[1.6rem]"
          :class="widthClass[width]"
          role="dialog"
          aria-modal="true"
        >
          <header class="flex items-start justify-between gap-4 border-b border-ink-700/50 px-6 py-5">
            <div>
              <h2 class="display text-lg tracking-wide text-mist-100">{{ title }}</h2>
              <p v-if="description" class="mt-1 text-xs text-mist-500">{{ description }}</p>
            </div>
            <button
              type="button"
              class="grid size-8 place-items-center rounded-full border border-ink-700/60 text-mist-300 transition-colors hover:border-gold-300/50 hover:text-gold-200"
              aria-label="关闭"
              @click="emit('close')"
            >
              <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="m6 6 12 12M18 6 6 18" stroke-linecap="round" />
              </svg>
            </button>
          </header>

          <div class="max-h-[70vh] overflow-y-auto px-6 py-5">
            <slot />
          </div>

          <footer v-if="$slots.footer" class="border-t border-ink-700/50 px-6 py-4">
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* 遮罩：不是死黑，而是把季节背景压暗 + 轻微模糊，玻璃才浮得起来 */
.modal-veil {
  background: color-mix(in oklab, var(--c-bg) 62%, transparent);
  -webkit-backdrop-filter: blur(10px) saturate(120%);
  backdrop-filter: blur(10px) saturate(120%);
}
</style>