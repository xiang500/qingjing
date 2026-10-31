<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef } from 'vue'
import { fetchClothingPage, fetchOutfits } from '@/api/clothing'
import { CATEGORY_LABELS, CATEGORY_ORDER, type CategoryCode, type Clothing, type Outfit } from '@/types/clothing'
import { MirrorScene, type GarmentInput } from '@/mirror/MirrorScene'
import { usePoseTracker } from '@/mirror/usePoseTracker'

const canvas = ref<HTMLCanvasElement | null>(null)
const scene = shallowRef<MirrorScene | null>(null)

const items = ref<Clothing[]>([])
const outfits = ref<Outfit[]>([])
const selected = reactive(new Map<CategoryCode, Clothing>())
const loading = ref(true)
const error = ref('')
const autoRotate = ref(true)

const cameraOpen = ref(false)
const video = ref<HTMLVideoElement | null>(null)
const overlay = ref<HTMLCanvasElement | null>(null)
const { status, message, fps, start, stop } = usePoseTracker(video, overlay)

const currentOutfit = computed(() => [...selected.values()])
const aiOutfit = computed(() => outfits.value[0] ?? null)

const grouped = computed(() => {
  const map = new Map<CategoryCode, Clothing[]>()
  for (const category of CATEGORY_ORDER) {
    const list = items.value.filter((item) => item.category === category)
    if (list.length) {
      map.set(category, list)
    }
  }
  return [...map.entries()]
})

function toGarment(item: Clothing): GarmentInput {
  return {
    id: item.id,
    name: item.name,
    category: item.category,
    subCategory: item.subCategory,
    colorHex: item.colorHex,
  }
}

function applyToScene() {
  scene.value?.setOutfit(currentOutfit.value.map(toGarment))
}

function wear(item: Clothing) {
  if (item.category === 'DRESS') {
    selected.delete('TOP')
    selected.delete('BOTTOM')
  }
  if (item.category === 'TOP' || item.category === 'BOTTOM') {
    selected.delete('DRESS')
  }
  if (selected.get(item.category)?.id === item.id) {
    selected.delete(item.category)
  } else {
    selected.set(item.category, item)
  }
  applyToScene()
}

function isWorn(item: Clothing) {
  return selected.get(item.category)?.id === item.id
}

function removeCategory(category: CategoryCode) {
  selected.delete(category)
  applyToScene()
}

function wearAiOutfit() {
  const outfit = aiOutfit.value
  if (!outfit) {
    return
  }
  selected.clear()
  for (const id of outfit.itemIds) {
    const item = items.value.find((candidate) => candidate.id === id)
    if (item) {
      selected.set(item.category, item)
    }
  }
  applyToScene()
}

function clearOutfit() {
  selected.clear()
  applyToScene()
}

async function toggleCamera() {
  cameraOpen.value = !cameraOpen.value
  if (cameraOpen.value) {
    await nextTick()
    await start()
  } else {
    stop()
  }
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [page, outfitList] = await Promise.all([
      fetchClothingPage({ page: 1, size: 100 }),
      fetchOutfits(),
    ])
    items.value = page.records
    outfits.value = outfitList
    if (outfitList.length) {
      for (const id of outfitList[0].itemIds) {
        const item = page.records.find((candidate) => candidate.id === id)
        if (item) {
          selected.set(item.category, item)
        }
      }
      applyToScene()
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '衣橱数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (canvas.value) {
    scene.value = new MirrorScene(canvas.value)
  }
  await loadData()
})

onBeforeUnmount(() => {
  stop()
  scene.value?.dispose()
})

function toggleRotate() {
  autoRotate.value = !autoRotate.value
  scene.value?.setAutoRotate(autoRotate.value)
}
</script>

<template>
  <div class="space-y-5">
    <section class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
      <div class="max-w-2xl">
        <p class="text-[11px] uppercase tracking-[0.4em] text-gold-400/80">Real-time Mirror</p>
        <h1 class="mt-2 font-display text-3xl leading-tight text-mist-100 sm:text-4xl">
          实时试衣镜，
          <span class="text-gold-200">让 AI 的推荐直接穿在身上</span>
        </h1>
        <p class="mt-3 text-sm leading-relaxed text-mist-500">
          左侧是浏览器实时渲染的 3D 数字人（Three.js），右侧是真实衣橱。
          点击单品即时换装，GSAP 负责过渡；打开摄像头后由 MediaPipe 在本机完成姿态识别。
        </p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <button type="button" class="btn-ghost" @click="toggleRotate">
          {{ autoRotate ? '暂停旋转' : '自动旋转' }}
        </button>
        <button type="button" class="btn-ghost" @click="scene?.resetView()">重置视角</button>
        <button type="button" class="btn-primary" :disabled="!aiOutfit" @click="wearAiOutfit">
          一键穿上 AI 套装
        </button>
      </div>
    </section>

    <div class="grid gap-5 lg:grid-cols-[minmax(0,1fr)_21rem]">
      <section class="space-y-4">
        <div
          class="relative aspect-[4/5] w-full overflow-hidden rounded-3xl border border-white/10 bg-gradient-to-b from-ink-800/70 via-ink-900/60 to-ink-950 sm:aspect-[5/4] lg:aspect-[4/5]"
        >
          <canvas ref="canvas" class="size-full cursor-grab touch-none active:cursor-grabbing" />
          <div class="pointer-events-none absolute left-4 top-4 flex items-center gap-2 text-[10px] uppercase tracking-[0.28em] text-mist-400">
            <span class="size-1.5 animate-pulse rounded-full bg-emerald-300" />
            WebGL · 实时渲染
          </div>
          <div class="pointer-events-none absolute right-4 top-4 rounded-full border border-white/15 bg-ink-950/70 px-3 py-1 text-[10px] text-mist-300 backdrop-blur">
            {{ currentOutfit.length }} 件在身 · 拖动旋转
          </div>

          <div v-if="currentOutfit.length" class="absolute inset-x-3 bottom-3 flex flex-wrap gap-2">
            <button
              v-for="item in currentOutfit"
              :key="item.id"
              type="button"
              class="group flex items-center gap-1.5 rounded-full border border-white/15 bg-ink-950/75 py-1 pl-1 pr-2.5 text-[11px] text-mist-200 backdrop-blur transition-colors hover:border-rose-300/40"
              @click="removeCategory(item.category)"
            >
              <span class="size-4 rounded-full border border-white/20" :style="{ background: item.colorHex ?? '#9AA5B1' }" />
              {{ item.name }}
              <span class="text-mist-500 group-hover:text-rose-300">×</span>
            </button>
          </div>

          <div
            v-if="loading"
            class="absolute inset-0 grid place-items-center bg-ink-950/40 text-xs text-mist-400 backdrop-blur-sm"
          >
            正在加载衣橱与 3D 场景…
          </div>
        </div>

        <div class="glass-card overflow-hidden">
          <div class="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
            <div class="min-w-0">
              <p class="text-[10px] uppercase tracking-[0.28em] text-mist-500">Camera · MediaPipe</p>
              <p class="mt-0.5 truncate text-xs text-mist-300">
                {{ message || '开启摄像头，人体关键点在本机推理，视频不上传' }}
              </p>
            </div>
            <div class="flex items-center gap-2">
              <span v-if="status === 'ready'" class="rounded-full border border-emerald-300/25 bg-emerald-400/10 px-2 py-0.5 text-[10px] text-emerald-200">
                {{ fps }} FPS
              </span>
              <button type="button" class="btn-ghost !px-3 !py-1.5 !text-[11px]" @click="toggleCamera">
                {{ cameraOpen ? '关闭摄像头' : status === 'loading' ? '加载中…' : '开启摄像头' }}
              </button>
            </div>
          </div>

          <div v-if="cameraOpen" class="relative border-t border-white/8 bg-black">
            <video ref="video" class="max-h-[26rem] w-full scale-x-[-1] object-cover" playsinline muted />
            <canvas ref="overlay" class="pointer-events-none absolute inset-0 size-full" />
            <p class="absolute inset-x-0 bottom-0 bg-ink-950/70 px-3 py-1.5 text-[10px] text-mist-400">
              {{ status === 'ready' ? '金色骨架 = MediaPipe 33 点姿态，端侧推理' : message }}
            </p>
          </div>
        </div>
      </section>

      <aside class="space-y-4">
        <section v-if="aiOutfit" class="glass-card p-4">
          <div class="flex items-center justify-between text-[10px] uppercase tracking-[0.28em] text-mist-500">
            <span>AI 套装</span>
            <span v-if="aiOutfit.score" class="rounded-full border border-gold-400/30 px-2 py-0.5 text-[10px] text-gold-200">
              契合度 {{ aiOutfit.score }}
            </span>
          </div>
          <h2 class="mt-2 font-display text-lg text-mist-100">{{ aiOutfit.name }}</h2>
          <p class="mt-1 line-clamp-2 text-xs leading-relaxed text-mist-500">{{ aiOutfit.aiReason }}</p>
          <div class="mt-3 flex flex-wrap gap-1.5">
            <span
              v-for="name in aiOutfit.itemNames"
              :key="name"
              class="rounded-md border border-white/10 bg-white/[0.04] px-2 py-0.5 text-[10px] text-mist-300"
            >
              {{ name }}
            </span>
          </div>
          <div class="mt-3 flex gap-2">
            <button type="button" class="btn-primary flex-1 !py-1.5 !text-[11px]" @click="wearAiOutfit">穿上这套</button>
            <button type="button" class="btn-ghost !py-1.5 !text-[11px]" @click="clearOutfit">清空</button>
          </div>
        </section>

        <section class="glass-card p-4">
          <div class="flex items-center justify-between">
            <h2 class="text-xs uppercase tracking-[0.28em] text-mist-500">衣橱单品</h2>
            <span class="text-[10px] text-mist-500">{{ items.length }} 件</span>
          </div>

          <p v-if="error" class="mt-3 rounded-lg border border-rose-400/25 bg-rose-400/8 px-3 py-2 text-[11px] text-rose-100">
            {{ error }}
          </p>

          <div class="mt-3 max-h-[30rem] space-y-4 overflow-y-auto pr-1">
            <div v-for="[category, list] in grouped" :key="category">
              <p class="mb-2 text-[10px] uppercase tracking-[0.2em] text-mist-500">
                {{ CATEGORY_LABELS[category] }}
              </p>
              <div class="grid grid-cols-2 gap-2">
                <button
                  v-for="item in list"
                  :key="item.id"
                  type="button"
                  class="group relative overflow-hidden rounded-xl border text-left transition-all"
                  :class="
                    isWorn(item)
                      ? 'border-gold-400/60 bg-gold-400/10'
                      : 'border-white/10 bg-white/[0.02] hover:border-white/25'
                  "
                  @click="wear(item)"
                >
                  <span class="block aspect-[4/3] overflow-hidden bg-ink-900/60">
                    <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" class="size-full object-contain p-1" loading="lazy" />
                  </span>
                  <span class="block truncate px-2 py-1.5 text-[10px] text-mist-300">{{ item.name }}</span>
                  <span
                    v-if="isWorn(item)"
                    class="absolute right-1.5 top-1.5 grid size-4 place-items-center rounded-full bg-gold-300 text-[9px] font-bold text-ink-950"
                  >✓</span>
                </button>
              </div>
            </div>
          </div>
        </section>

        <p class="px-1 text-[10px] leading-relaxed text-mist-500">
          说明：数字人是程序化几何体，服装以「壳层」方式贴合，不做布料物理模拟——
          这样能保证换装即时、不穿模，也符合「材质切换 + 简易变形」的技术路线。
        </p>
      </aside>
    </div>
  </div>
</template>