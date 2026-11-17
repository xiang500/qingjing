<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchClothingPage, fetchOutfits } from '@/api/clothing'
import { streamChat } from '@/api/ai'
import { createOutfit } from '@/api/outfit'
import {
  CATEGORY_LABELS,
  CATEGORY_ORDER,
  type CategoryCode,
  type Clothing,
  type Outfit,
} from '@/types/clothing'
import StyleBoard from '@/components/styling/StyleBoard.vue'
import { evaluatePalette } from '@/utils/palette'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const theme = useThemeStore()
const auth = useAuthStore()

const items = ref<Clothing[]>([])
const outfits = ref<Outfit[]>([])
const selected = reactive(new Map<CategoryCode, Clothing>())
const loading = ref(true)
const error = ref('')

/** 上传件：直接作为一层贴到画板上 */
const CUSTOM_TYPES = [
  { value: 'TOP', label: '上衣' },
  { value: 'DRESS', label: '连衣裙' },
  { value: 'BOTTOM', label: '裤装 / 裙子' },
] as const

const customGarment = ref<{ url: string; category: string; name: string } | null>(null)
const customType = ref<'TOP' | 'DRESS' | 'BOTTOM'>('TOP')
const pendingImage = ref('')
const pendingName = ref('')
const uploading = ref(false)
const uploadError = ref('')

/** AI 点评 */
const review = ref('')
const reviewing = ref(false)
const reviewError = ref('')
let reviewAbort: AbortController | null = null

const currentOutfit = computed(() => [...selected.values()])
const aiOutfit = computed(() => outfits.value[0] ?? null)
const wornCount = computed(() => currentOutfit.value.length + (customGarment.value ? 1 : 0))

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

/** 选择器分类筛选：只影响展示，不参与搭配/评分/保存逻辑 */
const activeCategory = ref<CategoryCode | ''>('')

const visibleGroups = computed(() => {
  if (!activeCategory.value) {
    return grouped.value
  }
  return grouped.value.filter(([category]) => category === activeCategory.value)
})

function toggleCategory(category: CategoryCode) {
  activeCategory.value = activeCategory.value === category ? '' : category
}

/** 本地点评：页面与评测脚本共用同一份实现（src/utils/palette.ts），保证结论一致 */
const palette = computed(() => evaluatePalette(currentOutfit.value))

/** 保存当前搭配为套装（写入 outfit 表，之后可在社区发布里选择） */
const saving = ref(false)

async function saveOutfit() {
  if (!auth.isLoggedIn) {
    ElMessage.info('请先在右上角登录，登录后再保存套装')
    return
  }
  if (!currentOutfit.value.length) {
    ElMessage.warning('先在画板上放几件单品，再保存套装')
    return
  }
  if (customGarment.value) {
    ElMessage.warning('上传的图片暂不支持保存到套装，请先移除再保存')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt('给这套搭配起个名字', '保存套装', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: `我的搭配 ${new Date().toLocaleDateString('zh-CN')}`,
      inputPattern: /\S+/,
      inputErrorMessage: '名称不能为空',
    })
    saving.value = true
    await createOutfit({
      name: value.trim(),
      itemIds: currentOutfit.value.map((item) => item.id),
      reason: palette.value.reason,
      score: palette.value.harmony,
    })
    ElMessage.success('套装已保存，可去「灵感社区」发布这套搭配')
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err instanceof Error ? err.message : '保存失败，请稍后重试')
    }
  } finally {
    saving.value = false
  }
}


/** 把当前搭配写成一段结构化描述，既可给 AI，也方便解释 */
function describeOutfit(): string {
  const parts = currentOutfit.value.map(
    (item) => `${CATEGORY_LABELS[item.category]}=${item.name}（${item.colorHex ?? '无色值'}）`,
  )
  if (customGarment.value) {
    parts.push(`上传件=${customGarment.value.name}`)
  }
  return parts.length ? parts.join('、') : '（空）'
}

async function askReview() {
  if (!wornCount.value || reviewing.value) {
    return
  }
  reviewing.value = true
  reviewError.value = ''
  review.value = ''
  reviewAbort?.abort()
  reviewAbort = new AbortController()
  const prompt = [
    '【搭配点评】',
    `我在搭配一套衣服：${describeOutfit()}。`,
    `本地测算：色相跨度 ${palette.value.hueSpan}°（${palette.value.tone}），明度跨度 ${palette.value.lightSpan}。`,
    '请按【色彩】【场景】【比例】三项给建议，每项一行、不超过 45 字，',
    '只针对上面列出的单品，不要编造我没提到的衣服，也不要给品牌或购买建议。',
  ].join('')
  try {
    await streamChat(
      { message: prompt, scene: 'styling' },
      {
        onDelta: (text) => {
          review.value += text
        },
        onDone: () => {
          reviewing.value = false
        },
        onError: (message) => {
          reviewError.value = message
          reviewing.value = false
        },
      },
      reviewAbort.signal,
    )
  } catch (err) {
    reviewError.value = err instanceof Error ? err.message : 'AI 点评失败'
  } finally {
    reviewing.value = false
  }
}

// ---------------------------------------------------------------------------
// 搭配操作
// ---------------------------------------------------------------------------

/** 上传件与衣橱单品互斥，避免同部位叠两层 */
function releaseCustom(category: string) {
  const current = customGarment.value
  if (!current) {
    return
  }
  const clash =
    current.category === category ||
    (current.category === 'DRESS' && (category === 'TOP' || category === 'BOTTOM')) ||
    (category === 'DRESS' && (current.category === 'TOP' || current.category === 'BOTTOM'))
  if (clash) {
    customGarment.value = null
  }
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
    releaseCustom(item.category)
    selected.set(item.category, item)
  }
  review.value = ''
}

function isWorn(item: Clothing) {
  return selected.get(item.category)?.id === item.id
}

function removeCategory(category: CategoryCode) {
  selected.delete(category)
  review.value = ''
}

function clearOutfit() {
  selected.clear()
  customGarment.value = null
  review.value = ''
}

function wearAiOutfit() {
  const outfit = aiOutfit.value
  if (!outfit) {
    return
  }
  selected.clear()
  customGarment.value = null
  for (const id of outfit.itemIds) {
    const item = items.value.find((candidate) => candidate.id === id)
    if (item) {
      selected.set(item.category, item)
    }
  }
  review.value = ''
}

function applyIds(raw: unknown) {
  const ids = String(raw ?? '')
    .split(',')
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isFinite(value) && value > 0)
  if (!ids.length) {
    return
  }
  selected.clear()
  for (const id of ids) {
    const item = items.value.find((candidate) => candidate.id === id)
    if (item) {
      if (item.category === 'DRESS') {
        selected.delete('TOP')
        selected.delete('BOTTOM')
      }
      selected.set(item.category, item)
    }
  }
}

// ---------------------------------------------------------------------------
// 上传自己的衣服
// ---------------------------------------------------------------------------

/** 白底商品图 → 透明背景，叠到深色画板上才不会带一块白框 */
function knockOutBackground(canvas: HTMLCanvasElement): HTMLCanvasElement {
  const context = canvas.getContext('2d')
  if (!context) {
    return canvas
  }
  const image = context.getImageData(0, 0, canvas.width, canvas.height)
  const data = image.data
  for (let i = 0; i < data.length; i += 4) {
    const r = data[i]
    const g = data[i + 1]
    const b = data[i + 2]
    const max = Math.max(r, g, b)
    const min = Math.min(r, g, b)
    // 接近纯白且低饱和 → 视为背景
    if (min > 232 && max - min < 18) {
      data[i + 3] = 0
    }
  }
  context.putImageData(image, 0, 0)
  return canvas
}

async function imageToCanvas(source: Blob | string, max = 1024): Promise<HTMLCanvasElement | null> {
  try {
    const blob = typeof source === 'string' ? await (await fetch(source)).blob() : source
    const bitmap = await createImageBitmap(blob)
    const scale = Math.min(1, max / Math.max(bitmap.width, bitmap.height))
    const target = document.createElement('canvas')
    target.width = Math.max(1, Math.round(bitmap.width * scale))
    target.height = Math.max(1, Math.round(bitmap.height * scale))
    const context = target.getContext('2d')
    if (!context) {
      bitmap.close()
      return null
    }
    context.drawImage(bitmap, 0, 0, target.width, target.height)
    bitmap.close()
    return knockOutBackground(target)
  } catch {
    return null
  }
}

async function onFilePicked(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  if (!file.type.startsWith('image/')) {
    uploadError.value = '请选择图片文件（PNG / JPG）'
    return
  }
  uploading.value = true
  uploadError.value = ''
  const canvas = await imageToCanvas(file)
  if (!canvas) {
    uploadError.value = '图片解析失败，换一张试试'
    uploading.value = false
    return
  }
  pendingImage.value = canvas.toDataURL('image/png')
  pendingName.value = file.name.replace(/\.[^.]+$/, '')
  uploading.value = false
}

function wearCustom() {
  if (!pendingImage.value) {
    return
  }
  const category = customType.value
  if (category === 'DRESS') {
    selected.delete('TOP')
    selected.delete('BOTTOM')
  } else {
    selected.delete('DRESS')
    selected.delete(category === 'TOP' ? 'TOP' : 'BOTTOM')
  }
  customGarment.value = {
    url: pendingImage.value,
    category,
    name: pendingName.value || '我的衣服',
  }
  review.value = ''
}

function clearCustom() {
  customGarment.value = null
  review.value = ''
}

// ---------------------------------------------------------------------------

async function loadData() {
  // 未登录不拉数据：后端会用缺省演示账号兜底，退出登录后会看到上一个账号的衣橱
  if (!auth.isLoggedIn) {
    clearBoard()
    loading.value = false
    return
  }
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
    }
    if (route.query.ids) {
      applyIds(route.query.ids)
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '衣橱数据加载失败'
  } finally {
    loading.value = false
  }
}

/** 清空画板与本地加载的数据（退出登录 / 切换账号时调用） */
function clearBoard() {
  selected.clear()
  items.value = []
  outfits.value = []
  customGarment.value = null
  activeCategory.value = ''
  review.value = ''
  reviewError.value = ''
  error.value = ''
}

/**
 * 登录态变化时同步工作台：
 * - 退出：清空画板，回到空板引导
 * - 登录：重新拉当前账号的衣橱与套装
 */
watch(
  () => auth.isLoggedIn,
  async (loggedIn) => {
    if (!loggedIn) {
      clearBoard()
      return
    }
    await loadData()
  },
)

onMounted(async () => {
  await loadData()
  const tryon = route.query.tryon
  if (typeof tryon === 'string' && tryon) {
    const canvas = await imageToCanvas(tryon)
    if (canvas) {
      pendingImage.value = canvas.toDataURL('image/png')
      pendingName.value = '示例单品'
      wearCustom()
    }
  }
})

onBeforeUnmount(() => {
  reviewAbort?.abort()
})
</script>

<template>
  <div class="space-y-6 sm:space-y-8">
    <!-- ============ 页头：工作台宣言 + 主操作 ============ -->
    <section v-reveal class="flex flex-col gap-6 pt-1 lg:flex-row lg:items-end lg:justify-between">
      <div class="max-w-2xl">
        <p class="eyebrow">Studio · Look Board</p>
        <h1 class="display mt-4 text-[clamp(2.1rem,4vw,3.4rem)] leading-[1.15] text-mist-100">
          把整套搭配，<br />
          <span class="accent-text">先在光里看一遍</span>
        </h1>
        <p class="mt-4 max-w-[38rem] text-sm leading-[1.95] text-mist-400">
          点选单品，每件单品会作为一张平铺卡落到版面上，按上装 / 下装 / 外套 / 鞋包分区排布；配色协调先由本地规则算出可解释的分数，
          再交给 AI 补上场景与比例建议——AI 不可用时，结论依然成立。
        </p>
        <div class="mt-5 flex flex-wrap items-center gap-2">
          <span class="chip cursor-default">
            <span class="size-1.5 rounded-full bg-gold-300" />
            {{ theme.meta.label }} · {{ theme.meta.light }}
          </span>
          <span class="chip cursor-default">在身 {{ wornCount }} 件</span>
          <span class="chip cursor-default">衣橱 {{ items.length }} 件</span>
        </div>
      </div>

      <div class="flex flex-wrap items-center gap-2 lg:justify-end">
        <button type="button" class="btn-primary" :disabled="saving" @click="saveOutfit">
          <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M5 4h11l3 3v13H5V4Z" stroke-linejoin="round" />
            <path d="M8 4v6h8V5M8 20v-6h8v6" stroke-linejoin="round" />
          </svg>
          {{ saving ? '保存中…' : '保存这套' }}
        </button>
        <button type="button" class="btn-ghost" @click="clearOutfit">清空画板</button>
        <button type="button" class="btn-ghost" :disabled="!aiOutfit" @click="wearAiOutfit">
          一键穿上 AI 套装
        </button>
      </div>
    </section>

    <div class="grid gap-5 lg:grid-cols-[22rem_minmax(0,1fr)] lg:items-start">
      <div class="order-1 space-y-4 lg:order-2">
        <!-- ============ 造型板：一块 4:5 的编辑纸面 ============ -->
        <section v-reveal class="relative">
          <div class="mx-auto flex w-full max-w-[42rem] flex-wrap items-end justify-between gap-2 px-1 pb-3.5">
            <div class="flex items-center gap-3">
              <p class="eyebrow">Editorial Board</p>
              <span class="hidden text-[11px] tracking-[0.18em] text-mist-500 sm:inline">本季造型板</span>
            </div>
            <span class="season-chip">{{ theme.meta.label }} · {{ theme.meta.light }}</span>
          </div>

          <div class="relative mx-auto w-full max-w-[42rem]">
            <StyleBoard :items="currentOutfit" :custom="customGarment" :focus="activeCategory || undefined" />
            <div
              v-if="loading"
              class="absolute inset-0 z-30 grid place-items-center rounded-[1.7rem] bg-ink-950/55 text-xs text-mist-300 backdrop-blur-sm"
            >
              正在加载衣橱…
            </div>
          </div>

          <p class="mx-auto mt-3 w-full max-w-[42rem] px-1 text-[10px] leading-[1.85] text-mist-500">
            不是假试衣：每件单品摊成一张平铺卡，按网格排进这张 4:5 的纸面上——像一本杂志的造型内页。
          </p>
        </section>

        <!-- ============ 在身单品 ============ -->
        <div v-if="wornCount" class="flex flex-wrap items-center gap-2">
          <span class="pl-1 text-[10px] uppercase tracking-[0.28em] text-mist-500">在身</span>
          <button
            v-if="customGarment"
            type="button"
            class="worn-chip worn-chip-accent group"
            @click="clearCustom"
          >
            <span class="photo-tile size-5 overflow-hidden rounded-full">
              <img :src="customGarment.url" :alt="customGarment.name" class="size-full object-cover" />
            </span>
            {{ customGarment.name }}
            <span class="chip-remove">×</span>
          </button>
          <button
            v-for="item in currentOutfit"
            :key="item.id"
            type="button"
            class="worn-chip group"
            @click="removeCategory(item.category)"
          >
            <span
              class="size-2.5 rounded-full ring-1 ring-inset ring-black/20"
              :style="{ background: item.colorHex ?? '#9AA5B1' }"
            />
            {{ item.name }}
            <span class="chip-remove">×</span>
          </button>
        </div>

        <!-- ============ 配色评分 + AI 点评 ============ -->
        <section class="glass p-4 sm:p-5">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p class="eyebrow">Palette Reading</p>
              <h2 class="display mt-2 text-lg text-mist-100">配色与完整度</h2>
            </div>
            <span
              class="rounded-full border border-ink-700/60 bg-ink-850/30 px-2.5 py-1 text-[10px] tracking-[0.12em] text-mist-400"
            >
              {{ palette.tone }}
            </span>
          </div>

          <template v-if="wornCount">
            <div class="mt-4 flex flex-wrap items-end gap-x-3 gap-y-1">
              <p class="display text-[2.7rem] leading-none tabular-nums text-gold-300">
                {{ palette.harmony }}
              </p>
              <span class="pb-1 text-[11px] tracking-[0.16em] text-mist-500">/ 100 · 色彩协调</span>
            </div>
            <div class="mt-3 h-1.5 overflow-hidden rounded-full bg-ink-700/40">
              <span
                class="block h-full origin-left rounded-full bg-gold-300 transition-transform duration-300 ease-out"
                :style="{ transform: `scaleX(${palette.harmony / 100})` }"
              />
            </div>
            <p class="mt-3 text-[12px] leading-[1.9] text-mist-300">{{ palette.reason }}</p>
            <div class="mt-3 flex flex-wrap gap-1.5">
              <span class="metric">
                色相跨度 <span class="tabular-nums text-mist-100">{{ palette.hueSpan }}°</span>
              </span>
              <span class="metric">
                明度跨度 <span class="tabular-nums text-mist-100">{{ palette.lightSpan }}</span>
              </span>
              <span class="metric" :class="{ 'metric-accent': palette.complete }">
                {{ palette.complete ? '整套齐了 · 上装 + 下装 + 鞋履' : `还缺 ${palette.missing.join(' / ')}` }}
              </span>
              <span v-for="tag in palette.tags" :key="tag" class="palette-tag">{{ tag }}</span>
            </div>
          </template>

          <p
            v-else
            class="mt-4 rounded-xl border border-dashed border-ink-700/60 px-3.5 py-3 text-[11px] leading-[1.8] text-mist-500"
          >
            先放一件单品到画板上，配色评分与完整度会随选择实时更新。
          </p>

          <div class="mt-4 flex flex-wrap items-center justify-between gap-2 border-t border-ink-700/40 pt-4">
            <div class="flex flex-wrap items-baseline gap-2">
              <p class="text-[10px] uppercase tracking-[0.28em] text-mist-500">AI 点评</p>
              <span class="text-[10px] text-mist-500">色彩 · 场景 · 比例</span>
            </div>
            <button
              type="button"
              class="btn-ghost !px-3 !py-1.5 !text-[11px]"
              :disabled="reviewing || !wornCount"
              @click="askReview"
            >
              {{ reviewing ? '分析中…' : 'AI 深度点评' }}
            </button>
          </div>

          <p
            v-if="reviewError"
            class="mt-2.5 rounded-xl border border-rose-400/40 bg-rose-400/10 px-3 py-2 text-[11px] text-mist-100"
          >
            {{ reviewError }}
          </p>
          <div
            v-if="review"
            class="glass-soft mt-2.5 whitespace-pre-wrap px-4 py-3 text-[12px] leading-[1.9] text-mist-200"
          >
            {{ review }}
          </div>
          <p v-else-if="reviewing" class="mt-2.5 text-[11px] text-mist-500">
            正在让大模型补场景与比例建议…
          </p>
          <p v-else-if="wornCount" class="mt-2.5 text-[11px] leading-[1.8] text-mist-500">
            点「AI 深度点评」，会按【色彩】【场景】【比例】三项补上可执行的建议。
          </p>
        </section>
      </div>

      <!-- ============ 右栏：AI 套装 / 上传 / 单品选择器 ============ -->
      <aside class="order-2 space-y-4 lg:order-1">
        <!-- AI 套装 -->
        <section v-if="aiOutfit" class="glass-soft p-5">
          <div class="flex flex-wrap items-center justify-between gap-2 text-[10px] uppercase tracking-[0.28em] text-mist-500">
            <span class="inline-flex items-center gap-2">
              <span class="size-1.5 rounded-full bg-gold-300" />
              AI Look · 推荐套装
            </span>
            <span
              v-if="aiOutfit.score"
              class="rounded-full border border-gold-300/30 bg-gold-300/10 px-2 py-0.5 text-[10px] tracking-normal text-gold-200"
            >
              契合度 {{ aiOutfit.score }}
            </span>
          </div>
          <h2 class="display mt-2 text-xl text-mist-100">{{ aiOutfit.name }}</h2>
          <p class="mt-1.5 line-clamp-2 text-xs leading-relaxed text-mist-400">{{ aiOutfit.aiReason }}</p>
          <div class="mt-3 flex flex-wrap gap-1.5">
            <span
              v-for="name in aiOutfit.itemNames"
              :key="name"
              class="rounded-md border border-ink-700/60 bg-ink-850/30 px-2 py-0.5 text-[10px] text-mist-300"
            >
              {{ name }}
            </span>
          </div>
          <div class="mt-4 flex gap-2">
            <button type="button" class="btn-primary flex-1 !py-2 !text-xs" @click="wearAiOutfit">
              穿上这套
            </button>
            <button type="button" class="btn-ghost !py-2 !text-xs" @click="clearOutfit">清空</button>
          </div>
        </section>

        <!-- 上传我的衣服 -->
        <section v-reveal class="glass-soft p-5">
          <div class="flex items-start justify-between gap-2">
            <div>
              <p class="eyebrow">Upload</p>
              <h2 class="display mt-2 text-base text-mist-100">上传我的衣服</h2>
            </div>
            <span class="rounded-full border border-ink-700/60 px-2 py-0.5 text-[10px] text-mist-400">
              自动去白底
            </span>
          </div>
          <p class="mt-2 text-[11px] leading-[1.8] text-mist-500">
            拍一张平铺照（正面、纯色背景最好），去掉背景后直接作为一张平铺卡落到画板上。
          </p>

          <label
            class="mt-3 flex cursor-pointer items-center gap-3 rounded-2xl border border-dashed border-ink-700/70 bg-ink-850/30 p-2.5 transition-colors hover:border-gold-300/55"
          >
            <input type="file" accept="image/*" class="hidden" @change="onFilePicked" />
            <span class="photo-tile grid size-14 shrink-0 place-items-center overflow-hidden rounded-xl">
              <img
                v-if="pendingImage"
                :src="pendingImage"
                alt="待上传的衣服"
                class="size-full object-contain p-1"
              />
              <svg
                v-else
                class="size-5 text-white/40"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.6"
              >
                <path d="M12 5v14M5 12h14" stroke-linecap="round" />
              </svg>
            </span>
            <span class="min-w-0">
              <span class="block truncate text-[11px] text-mist-200">
                {{ uploading ? '读取中…' : pendingName || '选择图片 / 拖入文件' }}
              </span>
              <span class="mt-0.5 block text-[10px] text-mist-500">PNG · JPG，只在本机处理</span>
            </span>
          </label>

          <p
            v-if="uploadError"
            class="mt-2 rounded-lg border border-rose-400/40 bg-rose-400/10 px-2.5 py-1.5 text-[10px] text-mist-100"
          >
            {{ uploadError }}
          </p>

          <div class="mt-3 flex gap-1.5">
            <button
              v-for="type in CUSTOM_TYPES"
              :key="type.value"
              type="button"
              class="chip flex-1 !justify-center !px-1 !py-1.5 !text-[10px]"
              :class="{ 'chip-active': customType === type.value }"
              @click="customType = type.value"
            >
              {{ type.label }}
            </button>
          </div>

          <div class="mt-2 flex gap-2">
            <button
              type="button"
              class="btn-primary flex-1 !py-2 !text-xs"
              :disabled="!pendingImage"
              @click="wearCustom"
            >
              放到画板
            </button>
            <button
              type="button"
              class="btn-ghost !py-2 !text-xs"
              :disabled="!customGarment"
              @click="clearCustom"
            >
              移除
            </button>
          </div>
        </section>

        <!-- 单品选择器 -->
        <section class="glass-soft p-5">
          <div class="flex items-center justify-between gap-2">
            <div>
              <p class="eyebrow">Wardrobe Picks</p>
              <h2 class="display mt-2 text-base text-mist-100">衣橱单品</h2>
            </div>
            <span class="text-[10px] tabular-nums text-mist-500">{{ items.length }} 件</span>
          </div>

          <p
            v-if="error"
            class="mt-3 rounded-xl border border-rose-400/40 bg-rose-400/10 px-3 py-2 text-[11px] text-mist-100"
          >
            {{ error }}
          </p>

          <template v-if="grouped.length">
            <div class="mt-3 flex flex-wrap gap-1.5">
              <button
                type="button"
                class="chip !py-1 !text-[11px]"
                :class="{ 'chip-active': activeCategory === '' }"
                @click="activeCategory = ''"
              >
                全部
              </button>
              <button
                v-for="[category, list] in grouped"
                :key="category"
                type="button"
                class="chip !py-1 !text-[11px]"
                :class="{ 'chip-active': activeCategory === category }"
                @click="toggleCategory(category)"
              >
                {{ CATEGORY_LABELS[category] }}
                <span class="tabular-nums opacity-60">{{ list.length }}</span>
              </button>
            </div>

            <div class="mt-3 max-h-[30rem] space-y-4 overflow-y-auto pr-0.5">
              <div v-for="[category, list] in visibleGroups" :key="category">
                <p class="mb-2 flex items-center gap-2 text-[10px] uppercase tracking-[0.24em] text-mist-500">
                  {{ CATEGORY_LABELS[category] }}
                  <span class="h-px flex-1 bg-ink-700/50" />
                  <span class="tabular-nums">{{ list.length }}</span>
                </p>
                <div class="grid grid-cols-3 gap-1.5">
                  <button
                    v-for="item in list"
                    :key="item.id"
                    type="button"
                    class="picker-tile group relative text-left"
                    :class="{ 'picker-tile-on': isWorn(item) }"
                    :aria-pressed="isWorn(item)"
                    @click="wear(item)"
                  >
                    <span class="photo-tile block aspect-[4/5] overflow-hidden rounded-[0.8rem]">
                      <img
                        v-if="item.imageUrl"
                        :src="item.imageUrl"
                        :alt="item.name"
                        class="size-full object-contain p-1.5 transition-transform duration-300 group-hover:scale-[1.06]"
                        loading="lazy"
                      />
                      <span v-else class="display grid size-full place-items-center text-2xl text-white/25">
                        {{ item.categoryLabel.slice(0, 1) }}
                      </span>
                    </span>
                    <span
                      class="mt-1 block truncate text-[10px] text-mist-300 transition-colors group-hover:text-mist-100"
                    >
                      {{ item.name }}
                    </span>
                    <span
                      v-if="isWorn(item)"
                      class="absolute right-1 top-1 grid size-4 place-items-center rounded-full bg-gold-300 text-[9px] font-bold text-ink-950 shadow"
                    >
                      ✓
                    </span>
                  </button>
                </div>
              </div>
            </div>
          </template>

          <p v-else-if="loading" class="mt-4 text-[11px] text-mist-500">正在读取衣橱…</p>

          <div
            v-else-if="!error"
            class="mt-4 rounded-2xl border border-dashed border-ink-700/70 px-4 py-8 text-center"
          >
            <p class="display text-sm text-mist-200">衣橱里还没有单品</p>
            <p class="mt-1 text-[11px] leading-relaxed text-mist-500">
              先去「数字衣橱」录入几件，这里就能直接挑着搭。
            </p>
          </div>
        </section>

        <p class="px-1 text-[10px] leading-[1.8] text-mist-500">
          技术说明：单品图按各自的图形包围盒裁剪成透明图层，再按品类落位，叠成一张平铺造型卡，
          不需要 3D 建模也能看到整套搭配的层次关系；色彩评分由本地规则计算
          （色相最短弧 + 明度跨度 + 品类完整度），结论可复现。
        </p>
      </aside>
    </div>
  </div>
</template>

<style scoped>
/* 季节胶囊：用 accent 令牌，换季自动换色 */
.season-chip {
  flex: none;
  border-radius: 999px;
  border: 1px solid color-mix(in oklab, var(--c-accent) 32%, transparent);
  background: color-mix(in oklab, var(--c-accent) 12%, transparent);
  padding: 0.3rem 0.7rem;
  font-size: 0.66rem;
  letter-spacing: 0.06em;
  color: var(--c-accent-strong);
  white-space: nowrap;
}

/* 在身单品：小玻璃胶囊，悬停上浮一点 */
.worn-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  border-radius: 999px;
  border: 1px solid color-mix(in oklab, var(--c-line) 100%, transparent);
  background: color-mix(in oklab, var(--c-glass-a) 58%, transparent);
  padding: 0.26rem 0.62rem 0.26rem 0.34rem;
  font-size: 0.68rem;
  color: var(--c-ink-3);
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease;
}

.worn-chip:hover {
  transform: translateY(-1px);
  border-color: color-mix(in oklab, var(--c-accent) 48%, transparent);
  color: var(--c-ink);
}

.worn-chip-accent {
  border-color: color-mix(in oklab, var(--c-accent) 42%, transparent);
  background: color-mix(in oklab, var(--c-accent) 12%, transparent);
  color: var(--c-accent-strong);
}

.worn-chip-accent:hover {
  border-color: color-mix(in oklab, var(--c-accent) 62%, transparent);
  background: color-mix(in oklab, var(--c-accent) 18%, transparent);
}

.chip-remove {
  color: var(--c-muted-2);
  transition: color 0.18s ease;
}

.worn-chip:hover .chip-remove {
  color: var(--c-accent-strong);
}

/* 评分指标小标签 */
.metric {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  border-radius: 0.5rem;
  border: 1px solid color-mix(in oklab, var(--c-line) 100%, transparent);
  background: color-mix(in oklab, var(--c-glass-a) 50%, transparent);
  padding: 0.22rem 0.5rem;
  font-size: 0.66rem;
  color: var(--c-ink-3);
}

.metric-accent {
  border-color: color-mix(in oklab, var(--c-accent) 40%, transparent);
  background: color-mix(in oklab, var(--c-accent) 12%, transparent);
  color: var(--c-accent-strong);
}

.palette-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 0.5rem;
  border: 1px solid color-mix(in oklab, var(--c-accent) 26%, transparent);
  background: color-mix(in oklab, var(--c-accent) 9%, transparent);
  padding: 0.22rem 0.5rem;
  font-size: 0.66rem;
  color: var(--c-accent-strong);
}

/* 选择器缩略图：悬停上浮 + accent 描边，选中是 accent 外环 */
.picker-tile {
  border-radius: 1rem;
  padding: 0.2rem;
  border: 1px solid transparent;
  background: transparent;
  transition:
    transform 0.22s ease,
    border-color 0.22s ease,
    background 0.22s ease,
    box-shadow 0.22s ease;
}

.picker-tile:hover {
  transform: translateY(-2px);
  border-color: color-mix(in oklab, var(--c-accent) 45%, transparent);
  background: color-mix(in oklab, var(--c-glass-a) 62%, transparent);
}

.picker-tile-on {
  border-color: color-mix(in oklab, var(--c-accent) 68%, transparent);
  background: color-mix(in oklab, var(--c-accent) 12%, transparent);
  box-shadow: 0 0 0 1px color-mix(in oklab, var(--c-accent) 38%, transparent);
}

.picker-tile-on:hover {
  border-color: color-mix(in oklab, var(--c-accent) 78%, transparent);
}
</style>