<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { GalleryItem } from '@/types/gallery'
import { catalogImage, isPhotoImage } from '@/utils/image'

/**
 * 旋转衣橱画廊（参考视频 00:32 的 3D coverflow 效果）。
 *
 * 用 CSS 3D（perspective + rotateY + translateZ）把卡片摆成一条弧线，
 * 中间的卡片正对镜头，两侧卡片向内旋转、后退并压暗；
 * 自动轮播 + 悬停暂停 + 左右箭头 / 圆点手动切换。
 */

const props = withDefaults(
  defineProps<{
    items: GalleryItem[]
    /** 自动轮播间隔（毫秒） */
    interval?: number
    /** 画廊标题 */
    title?: string
    /** 标题上方的英文小标 */
    label?: string
  }>(),
  {
    interval: 4200,
    title: '旋转衣橱',
    label: 'Wardrobe Gallery',
  },
)

const index = ref(0)
/** 悬停 / 按住圆点时暂停，避免用户想细看却被切走 */
const paused = ref(false)
/** 当前屏幕宽度能同时露出一侧几张卡片 */
const radius = ref(2)
const reduceMotion = ref(false)
let timer: number | undefined

const source = computed<GalleryItem[]>(() => props.items)
const count = computed(() => source.value.length)

const counter = computed(
  () => `${String(index.value + 1).padStart(2, '0')} / ${String(count.value).padStart(2, '0')}`,
)

/** 真实数据加载完成后卡片数量可能变化，避免索引越界停在空白上 */
watch(count, (next) => {
  if (index.value >= next) {
    index.value = 0
  }
})

/** 环形最短距离：首尾相连，最后一张和第一张之间只差一格 */
function offsetOf(i: number): number {
  const n = count.value
  if (!n) {
    return 0
  }
  let delta = i - index.value
  if (delta > n / 2) {
    delta -= n
  }
  if (delta < -n / 2) {
    delta += n
  }
  return delta
}

/** 每个偏移量对应的 3D 摆位：中间正对，两侧向内旋转并后退、压暗 */
function styleOf(i: number): Record<string, string> {
  const delta = offsetOf(i)
  const distance = Math.abs(delta)
  const hidden = distance > radius.value
  return {
    transform: [
      'translate(-50%, -50%)',
      `translateX(${delta * 82}%)`,
      `translateZ(${-distance * 120}px)`,
      `rotateY(${-delta * 40}deg)`,
      `scale(${1 - distance * 0.07})`,
    ].join(' '),
    opacity: hidden ? '0' : String(1 - distance * 0.12),
    zIndex: String(20 - distance),
    visibility: hidden ? 'hidden' : 'visible',
    filter: `brightness(${1 - distance * 0.1})`,
  }
}

function go(step: number) {
  const n = count.value
  if (!n) {
    return
  }
  index.value = (index.value + step + n) % n
}

function select(i: number) {
  index.value = i
}

function tick() {
  if (!paused.value && !reduceMotion.value && count.value > 1) {
    go(1)
  }
}

function onResize() {
  radius.value = window.innerWidth >= 1024 ? 2 : 1
}

onMounted(() => {
  reduceMotion.value = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  onResize()
  window.addEventListener('resize', onResize)
  timer = window.setInterval(tick, props.interval)
})

onBeforeUnmount(() => {
  window.clearInterval(timer)
  window.removeEventListener('resize', onResize)
})
</script>

<template>
  <section
    class="glass relative overflow-hidden"
    aria-roledescription="carousel"
    aria-label="衣橱画廊"
  >
    <div class="relative px-4 pb-4 pt-4 sm:px-6 sm:pt-5">
      <header class="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p class="eyebrow">{{ label }}</p>
          <h2 class="display mt-2 text-lg text-mist-100 sm:text-xl">
            {{ title }}
            <span v-if="count" class="ml-2 text-[11px] tracking-normal text-mist-500">悬停暂停 · 点两侧卡片切换</span>
          </h2>
        </div>
        <div v-if="count" class="flex items-center gap-2">
          <span class="text-[11px] tabular-nums tracking-[0.18em] text-mist-500">{{ counter }}</span>
          <button
            type="button"
            class="grid size-8 place-items-center rounded-full border border-ink-700/60 text-mist-300 transition-colors hover:border-gold-300/50 hover:text-gold-200"
            aria-label="上一件"
            @click="go(-1)"
          >
            <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="m15 6-6 6 6 6" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </button>
          <button
            type="button"
            class="grid size-8 place-items-center rounded-full border border-ink-700/60 text-mist-300 transition-colors hover:border-gold-300/50 hover:text-gold-200"
            aria-label="下一件"
            @click="go(1)"
          >
            <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="m9 6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </button>
        </div>
      </header>

      <div
        v-if="count"
        class="relative mt-1 h-[clamp(19rem,33vw,26rem)] [perspective:1400px]"
        @mouseenter="paused = true"
        @mouseleave="paused = false"
      >
        <button
          v-for="(item, i) in source"
          :key="item.id"
          type="button"
          class="absolute left-1/2 top-1/2 w-[clamp(11rem,23vw,19rem)] overflow-hidden rounded-2xl border bg-[#101216] text-left shadow-[0_34px_90px_-38px_rgba(0,0,0,0.85)] transition-all duration-700 ease-out will-change-transform"
          :class="
            i === index
              ? 'border-gold-300/50 ring-1 ring-gold-300/20'
              : 'border-white/10 hover:border-white/25'
          "
          :style="styleOf(i)"
          :aria-hidden="i !== index"
          :aria-label="`查看「${item.name}」`"
          @click="select(i)"
        >
          <span class="relative block aspect-[4/5] overflow-hidden">
            <span
              class="absolute inset-0"
              :style="{
                background: `radial-gradient(120% 120% at 32% 8%, ${item.colorHex ?? '#8d887c'}2e, transparent 64%), #101216`,
              }"
            />
            <img
              v-if="catalogImage(item.imageUrl)"
              :src="catalogImage(item.imageUrl)"
              :alt="item.name"
              class="relative size-full transition-transform duration-500 hover:scale-[1.04]"
              :class="
                isPhotoImage(catalogImage(item.imageUrl))
                  ? 'object-cover'
                  : 'object-contain p-3'
              "
              draggable="false"
              loading="lazy"
            />
            <span v-else class="relative grid size-full place-items-center display text-4xl text-white/20">
              {{ item.categoryLabel.slice(0, 1) }}
            </span>
            <span
              class="absolute left-2.5 top-2.5 rounded-full border border-white/20 bg-black/35 px-2 py-0.5 text-[10px] tracking-[0.1em] text-white/85 backdrop-blur"
            >
              {{ item.categoryLabel }}
            </span>
          </span>

          <span class="flex items-center justify-between gap-2 border-t border-white/10 bg-[#101216] px-3 py-2">
            <span class="min-w-0">
              <span class="block truncate text-[12px] text-white/90">{{ item.name }}</span>
              <span class="block truncate text-[10px] text-white/45">{{ item.meta || item.categoryLabel }}</span>
            </span>
            <span class="flex shrink-0 items-center gap-1 text-[10px] text-white/55">
              <span
                class="size-2 rounded-full ring-1 ring-inset ring-white/25"
                :style="{ background: item.colorHex ?? '#6b7280' }"
              />
              {{ item.wearCount != null ? `穿 ${item.wearCount} 次` : item.categoryLabel }}
            </span>
          </span>
        </button>
      </div>

      <!-- 空态：衣橱还没有单品时，不显示任何照片，只给引导 -->
      <div
        v-else
        class="mt-3 flex h-[clamp(13rem,22vw,18rem)] flex-col items-center justify-center gap-3 rounded-[var(--radius-md)] border border-dashed border-ink-700/60 px-6 text-center"
      >
        <span class="grid size-11 place-items-center rounded-full border border-ink-700/60 text-mist-500">
          <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M4 19h16M7 19V9l5-4 5 4v10" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M10 19v-4h4v4" stroke-linecap="round" />
          </svg>
        </span>
        <p class="text-sm font-medium text-mist-200">画廊还空着</p>
        <p class="max-w-xs text-[11px] leading-relaxed text-mist-500">
          录入第一件单品后，它会出现在这条旋转画廊里。
        </p>
      </div>

      <!-- 圆点：选择任意一件，当前那张会转到中间 -->
      <div v-if="count" class="mt-2 flex items-center justify-center gap-1.5">
        <button
          v-for="(item, i) in source"
          :key="item.id"
          type="button"
          class="h-1.5 rounded-full transition-all duration-300"
          :class="i === index ? 'w-6 bg-gold-300' : 'w-1.5 bg-mist-500/30 hover:bg-mist-400/60'"
          :aria-label="`切换到「${item.name}」`"
          :aria-current="i === index"
          @click="select(i)"
        />
      </div>
    </div>
  </section>
</template>
