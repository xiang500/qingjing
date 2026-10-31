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
        <div class="absolute inset-0 bg-ink-950/80 backdrop-blur-sm" @click="emit('close')" />
        <div
          class="relative w-full overflow-hidden rounded-t-3xl border border-white/10 bg-ink-900/95 shadow-2xl sm:rounded-3xl"
          :class="widthClass[width]"
          role="dialog"
          aria-modal="true"
        >
          <header class="flex items-start justify-between gap-4 border-b border-white/8 px-6 py-5">
            <div>
              <h2 class="font-display text-lg tracking-wide text-mist-100">{{ title }}</h2>
              <p v-if="description" class="mt-1 text-xs text-mist-500">{{ description }}</p>
            </div>
            <button
              type="button"
              class="grid size-8 place-items-center rounded-full border border-white/10 text-mist-300 transition-colors hover:border-gold-400/40 hover:text-gold-200"
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

          <footer v-if="$slots.footer" class="border-t border-white/8 px-6 py-4">
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>