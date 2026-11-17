<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useThemeStore } from '@/stores/theme'
import type { CategoryCode, Clothing } from '@/types/clothing'

/**
 * 造型板 · 杂志内页
 * ---------------------------------------------------------------------------
 * 版式原则（与旧的「绝对定位 + 百分比 + 旋转」版本彻底切开）：
 *   1) 卡片全部由 CSS Grid 落位：2 列、行高 1fr，按品类阅读顺序自然填充；
 *      没有 position:absolute、没有负 margin、没有连线，任何两张卡片都不会叠压。
 *   2) 卡片内部是「影棚底 + 容纳图 + 底部图注条」的纵向结构，图注永远水平、
 *      完整可见（单品名单独 truncate，其余信息不裁）。
 *   3) 卡片只做 ±0.8° 的微角度，且与 grid gap（--space-3 = 12px）留足余量，
 *      旋转后的包围盒也不会碰到邻卡。
 *   4) 纸面固定深底（.photo-tile + .glass 外框），季节只通过 accent 光与文案参与。
 */

type SlotKey = 'OUTER' | 'TOP' | 'DRESS' | 'BOTTOM' | 'SHOES' | 'BAG' | 'ACCESSORY'

/** 阅读顺序，也就是网格的填充顺序：外套 → 裙装 → 上装 → 下装 → 鞋 → 包 → 配饰 */
const READING_ORDER: SlotKey[] = ['OUTER', 'DRESS', 'TOP', 'BOTTOM', 'SHOES', 'BAG', 'ACCESSORY']

/**
 * 卡片微倾角：相邻卡片反向，最大 0.8°。
 * 再大就会让旋转后的包围盒吃掉 grid gap，几何自查会出现「相邻卡片接触」。
 */
const TILTS = [-0.8, 0.6, 0.5, -0.6, 0.7, -0.5, 0.4]

/** 空状态占位格：四张虚线卡，让「2 列 × 网格」的结构一眼可见 */
const EMPTY_SLOTS: Array<{ key: SlotKey; label: string }> = [
  { key: 'OUTER', label: '外套' },
  { key: 'TOP', label: '上装' },
  { key: 'BOTTOM', label: '下装' },
  { key: 'SHOES', label: '鞋履' },
]

const CATEGORY_CODES: CategoryCode[] = ['TOP', 'BOTTOM', 'OUTER', 'DRESS', 'SHOES', 'BAG', 'ACCESSORY']

/** 场合：从单品的 styleTags 里读，按优先级取第一个命中的 */
const OCCASION_RULES: Array<{ tag: string; label: string }> = [
  { tag: '通勤', label: '通勤' },
  { tag: '约会', label: '约会' },
  { tag: '度假', label: '度假' },
  { tag: '运动', label: '运动' },
  { tag: '居家', label: '居家' },
  { tag: '法式', label: '法式' },
  { tag: '复古', label: '复古' },
  { tag: '英伦', label: '英伦' },
  { tag: '优雅', label: '优雅' },
  { tag: '气场', label: '气场' },
  { tag: '精致', label: '精致' },
  { tag: '休闲', label: '休闲' },
  { tag: '温柔', label: '温柔' },
]

interface CardDraft {
  id: string
  slot: SlotKey
  name: string
  categoryLabel: string
  src: string
  /** 价格 · 穿着次数，压成一行，没有就是空串 */
  meta: string
  colorHex: string
  colorName: string
}

interface BoardCard extends CardDraft {
  /** 版面编号：01 / 02 / … */
  index: string
}

interface Swatch {
  hex: string
  label: string
}

const props = defineProps<{
  items: Clothing[]
  custom?: { url: string; category: string; name: string } | null
  /** 高亮正在挑选的品类 */
  focus?: string
}>()

const theme = useThemeStore()
const season = computed(() => theme.meta)

/** 纸面宽度决定排的是「宽松版」还是「紧凑版」：影响字号、图注是否折行 */
const boardEl = ref<HTMLElement | null>(null)
const compact = ref(typeof window !== 'undefined' && window.innerWidth < 500)
let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  const element = boardEl.value
  if (!element || typeof ResizeObserver === 'undefined') {
    return
  }
  resizeObserver = new ResizeObserver((entries) => {
    const width = entries[0]?.contentRect.width ?? 0
    compact.value = width > 0 && width < 480
  })
  resizeObserver.observe(element)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
})

function slotOfCategory(category: CategoryCode): SlotKey {
  switch (category) {
    case 'OUTER':
      return 'OUTER'
    case 'DRESS':
      return 'DRESS'
    case 'BOTTOM':
      return 'BOTTOM'
    case 'SHOES':
      return 'SHOES'
    case 'BAG':
      return 'BAG'
    case 'ACCESSORY':
      return 'ACCESSORY'
    case 'TOP':
      return 'TOP'
  }
}

function slotOfCustom(category: string): SlotKey {
  if (category === 'DRESS') {
    return 'DRESS'
  }
  if (category === 'BOTTOM') {
    return 'BOTTOM'
  }
  if (category === 'OUTER') {
    return 'OUTER'
  }
  return 'TOP'
}

function customCategoryText(category: string): string {
  if (category === 'DRESS') {
    return '连衣裙'
  }
  if (category === 'BOTTOM') {
    return '下装'
  }
  if (category === 'OUTER') {
    return '外套'
  }
  return '上装'
}

/** 单品图：种子数据是 /clothing/x.svg，实际素材在 /catalog 下 */
function catalogUrl(imageUrl?: string): string {
  if (!imageUrl) {
    return ''
  }
  const name = imageUrl.split('/').pop() ?? ''
  return name.endsWith('.svg') ? `/catalog/${name}` : imageUrl
}

function priceText(price?: number): string {
  if (typeof price !== 'number' || !Number.isFinite(price) || price <= 0) {
    return ''
  }
  return `¥${Number.isInteger(price) ? price : price.toFixed(2)}`
}

function metaText(price?: number, wearCount?: number): string {
  const parts = [priceText(price)]
  if (typeof wearCount === 'number' && wearCount > 0) {
    parts.push(`穿 ${wearCount} 次`)
  }
  return parts.filter(Boolean).join(' · ')
}

const cards = computed<BoardCard[]>(() => {
  const drafts: CardDraft[] = []
  for (const item of props.items) {
    drafts.push({
      id: `item-${item.id}`,
      slot: slotOfCategory(item.category),
      name: item.name,
      categoryLabel: item.categoryLabel,
      src: catalogUrl(item.imageUrl),
      meta: metaText(item.price, item.wearCount),
      colorHex: item.colorHex ?? '',
      colorName: item.color ?? '',
    })
  }
  const custom = props.custom
  if (custom?.url) {
    drafts.push({
      id: 'custom',
      slot: slotOfCustom(custom.category),
      name: custom.name,
      categoryLabel: customCategoryText(custom.category),
      src: custom.url,
      meta: '',
      colorHex: '',
      colorName: '',
    })
  }
  drafts.sort((left, right) => READING_ORDER.indexOf(left.slot) - READING_ORDER.indexOf(right.slot))
  return drafts.map((draft, index) => ({ ...draft, index: String(index + 1).padStart(2, '0') }))
})

const focusSlot = computed<SlotKey | null>(() => {
  const focus = props.focus
  if (!focus) {
    return null
  }
  const category = CATEGORY_CODES.find((code) => code === focus)
  return category ? slotOfCategory(category) : null
})

const occasion = computed(() => {
  const tags = new Set<string>()
  for (const item of props.items) {
    for (const tag of item.styleTags) {
      tags.add(tag)
    }
  }
  const hit = OCCASION_RULES.find((rule) => tags.has(rule.tag))
  return hit?.label ?? '日常'
})

/** 底部衬线 caption 的名单部分：单品名用 × 串起来，超过四件截断 */
const captionNames = computed(() => {
  const names = cards.value.map((card) => card.name)
  if (!names.length) {
    return ''
  }
  return names.length > 4 ? `${names.slice(0, 4).join(' × ')} 等` : names.join(' × ')
})

/** caption 后缀：季节 + 场合，始终可见，不被长名单截掉 */
const captionSuffix = computed(() => `${season.value.label}日${occasion.value}`)

/** 配色条：从选中单品的 colorHex 去重取 ≤5 块 */
const palette = computed<Swatch[]>(() => {
  const seen = new Set<string>()
  const swatches: Swatch[] = []
  for (const card of cards.value) {
    const hex = card.colorHex.trim()
    if (!hex) {
      continue
    }
    const key = hex.toLowerCase()
    if (seen.has(key)) {
      continue
    }
    seen.add(key)
    swatches.push({
      hex,
      label: card.colorName.trim() || hex.replace('#', '').toUpperCase(),
    })
    if (swatches.length === 5) {
      break
    }
  }
  return swatches
})

/** 只有一件时：单卡居中、保留 4:5 卡形，不把整张纸撑成一列 */
const loneCard = computed(() => cards.value.length === 1)
/** 两件以内：行高改成自适应，卡片按 4:5 卡形在版面里居中 */
const sparse = computed(() => cards.value.length > 0 && cards.value.length <= 2)

/**
 * 行高节奏：第一行（外套 / 上装或连衣裙）最高，最后一行（包袋 / 配饰）最矮，
 * 用 3 档明确的 fr 比例把「主次」放进网格轨道里，卡片之间依旧是等距网格、不叠压。
 */
const stageRows = computed(() => {
  const count = cards.value.length
  if (count < 3) {
    return ''
  }
  const rows = Math.ceil(count / 2)
  if (rows >= 4) {
    return 'rows-4'
  }
  return rows === 3 ? 'rows-3' : 'rows-2'
})

/**
 * 奇数件时让最后一张跨 2 列：网格永远填满，最后一行不留单个空洞
 * （5 件时是包袋，7 件时是配饰；下装跨列也符合「跨 2 列或左列」的排法）。
 */
function isWide(index: number): boolean {
  return cards.value.length > 1 && cards.value.length % 2 === 1 && index === cards.value.length - 1
}

/** 卡片微倾角；选中品类时整体抬起 2px（只动 transform） */
function cardTilt(index: number): string {
  const tilt = TILTS[index % TILTS.length]
  return focusSlot.value === cards.value[index]?.slot
    ? `translateY(-2px) rotate(${tilt}deg)`
    : `rotate(${tilt}deg)`
}
</script>

<template>
  <div
    ref="boardEl"
    class="board photo-tile glass relative aspect-[4/5] w-full overflow-hidden"
    :class="{ 'is-compact': compact }"
  >
    <!-- ================= 刊头：LOOK 01 + 季节 ／ OCCASION + 件数 + 配色条 ================= -->
    <header class="board-head">
      <div class="flex min-w-0 items-center gap-[var(--space-2)] pt-0.5">
        <span class="board-eyebrow shrink-0 text-white/45">Look 01</span>
        <span class="h-px w-5 shrink-0 bg-gold-300/70" aria-hidden="true" />
        <span class="display board-season truncate leading-none on-photo">
          {{ season.label }} · {{ season.light }}
        </span>
      </div>

      <div class="flex shrink-0 flex-col items-end gap-[var(--space-1)]">
        <span class="board-eyebrow text-white/35">Occasion</span>
        <span class="board-occasion leading-none text-gold-300">
          {{ occasion }}<span v-if="cards.length" class="ml-1 text-white/40">/ {{ cards.length }} 件</span>
        </span>
        <ul v-if="palette.length" class="palette-bar" aria-label="配色条">
          <li
            v-for="swatch in palette"
            :key="swatch.hex"
            class="palette-chip"
            :style="{ background: swatch.hex }"
            :title="swatch.label"
          />
        </ul>
      </div>
    </header>

    <!-- ================= 网格层：卡片位置只由 Grid 决定 ================= -->
    <div class="board-stage" :class="[stageRows, { 'is-sparse': sparse, 'is-lone': loneCard }]">
      <template v-if="cards.length">
        <article
          v-for="(card, index) in cards"
          :key="card.id"
          class="look-card"
          :class="{ 'is-focus': focusSlot === card.slot, 'look-card-wide': isWide(index) }"
          :style="{ transform: cardTilt(index) }"
        >
          <div class="look-media">
            <img
              v-if="card.src"
              :src="card.src"
              :alt="card.name"
              class="look-img"
              loading="lazy"
            />
            <span v-else class="display look-fallback">{{ card.categoryLabel.slice(0, 1) }}</span>
          </div>

          <!-- 图注条：水平、完整显示；只有单品名允许 truncate -->
          <div class="look-caption">
            <span class="look-index tabular-nums">{{ card.index }}</span>
            <span class="look-kind">{{ card.categoryLabel }}</span>
            <span class="look-name on-photo">{{ card.name }}</span>
            <span v-if="card.meta" class="look-meta">{{ card.meta }}</span>
          </div>
        </article>
      </template>

      <!-- 空状态：虚线占位卡包住中央玻璃引导卡，版面结构可见 -->
      <template v-else>
        <template v-for="(slot, index) in EMPTY_SLOTS" :key="slot.key">
          <div class="empty-slot">
            <span class="empty-plus" aria-hidden="true">+</span>
            <span class="empty-slot-label">{{ slot.label }}</span>
          </div>
          <div v-if="index === 1" class="empty-guide">
            <span class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4">
                <path
                  d="M12 4.5a2 2 0 0 1 2 2c0 1.2-1.4 1.5-2 2.4-.6-.9-2-1.2-2-2.4a2 2 0 0 1 2-2Z"
                  stroke-linejoin="round"
                />
                <path d="M9 12h6l-1 7.5H10L9 12Z" stroke-linejoin="round" />
              </svg>
            </span>
            <p class="display empty-title on-photo">造型板还空着</p>
            <p class="empty-text">
              从右侧挑一件单品，它会作为一张平铺卡落到版面上，按上装 / 下装 / 外套 / 鞋履分区排布。
            </p>
          </div>
        </template>
      </template>
    </div>

    <!-- ================= 底部：衬线 caption ================= -->
    <footer class="board-foot">
      <p v-if="captionNames" class="display board-caption flex min-w-0 items-baseline gap-[var(--space-2)]">
        <span class="min-w-0 truncate text-white/70">{{ captionNames }}</span>
        <span class="shrink-0 text-white/45">· {{ captionSuffix }}</span>
      </p>
      <p v-else class="board-caption text-white/30">尚未挑选单品</p>
      <p class="board-footnote ml-auto shrink-0 uppercase text-white/30">Editorial Look</p>
    </footer>
  </div>
</template>

<style scoped>
/*
 * 纸面：固定深底 + 极轻的经纬线 + 顶部季节光。
 * 全部画在背景层上，不需要再铺绝对定位的装饰元素。
 */
.board {
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-lg);
  background:
    radial-gradient(
      58% 38% at 50% 0%,
      color-mix(in oklab, var(--c-accent) 20%, transparent),
      transparent 72%
    ),
    linear-gradient(to right, rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    radial-gradient(120% 120% at 32% 8%, rgba(255, 255, 255, 0.08), transparent 62%),
    #101216;
  background-size:
    auto,
    2rem 2rem,
    2rem 2rem,
    auto,
    auto;
}

/* ---------- 刊头 ---------- */
.board-head {
  position: relative;
  z-index: 2;
  display: flex;
  flex: none;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4) 0;
}

.board-eyebrow {
  font-size: 9px;
  letter-spacing: 0.38em;
  text-transform: uppercase;
}

.board-season {
  font-size: 15px;
}

.board-occasion {
  font-size: 11px;
  letter-spacing: 0.04em;
}

/* 配色条：Pantone 式连续小色块，最多 5 块，横向一条 */
.palette-bar {
  display: flex;
  height: 12px;
  overflow: hidden;
  border-radius: var(--radius-xs);
  border: 1px solid rgba(255, 255, 255, 0.25);
}

.palette-chip {
  display: block;
  width: 18px;
  height: 100%;
}

/* ---------- 网格层：卡片只在这里落位 ---------- */
.board-stage {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-auto-flow: row dense;
  grid-auto-rows: minmax(0, 1fr);
  gap: var(--space-3);
  flex: 1 1 auto;
  min-height: 0;
  padding: var(--space-2) var(--space-4) var(--space-3);
}

/* 行高节奏：第一行（外套 / 上装）更高，最后一行（包袋 / 配饰）更矮 */
.board-stage.rows-2 {
  grid-template-rows: 1.12fr 1fr;
}

.board-stage.rows-3 {
  grid-template-rows: 1.1fr 1fr 0.9fr;
}

.board-stage.rows-4 {
  grid-template-rows: 1.08fr 1fr 1fr 0.9fr;
}

/* 1~2 件：行高交给卡片卡形，整组在版面里垂直居中 */
.board-stage.is-sparse {
  grid-auto-rows: minmax(0, auto);
  align-content: center;
}

.board-stage.is-sparse .look-card {
  width: 100%;
  max-height: 100%;
  aspect-ratio: 4 / 5;
}

/* 单件：跨两列居中，保留 4:5 卡形，不把纸面拉成一条 */
.board-stage.is-lone .look-card {
  grid-column: span 2;
  justify-self: center;
  width: min(100%, 24rem);
}

/* ---------- 卡片 ---------- */
.look-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 255, 255, 0.16);
  background:
    radial-gradient(125% 125% at 30% 6%, rgba(255, 255, 255, 0.085), transparent 62%),
    #171b21;
  box-shadow:
    0 18px 30px -22px rgba(0, 0, 0, 0.9),
    0 2px 6px rgba(0, 0, 0, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
  transform-origin: 50% 50%;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

/* 奇数件时最后一张跨 2 列，收掉网格末尾的空洞 */
.look-card-wide {
  grid-column: span 2;
}

/* 选中某品类：accent 描边 + 抬起（位移写在 inline transform 里） */
.look-card.is-focus {
  border-color: color-mix(in oklab, var(--c-accent) 58%, transparent);
  box-shadow:
    0 0 0 1px color-mix(in oklab, var(--c-accent) 32%, transparent),
    0 22px 34px -22px rgba(0, 0, 0, 0.95);
}

/* 单品图区：图铺满格、object-contain 保证完整不裁；留白用 padding */
.look-media {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  grid-template-rows: minmax(0, 1fr);
  flex: 1 1 0;
  min-height: 0;
  padding: var(--space-3);
}

.look-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.look-fallback {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  font-size: 18px;
  color: rgba(255, 255, 255, 0.25);
}

/* 图注条：横向一行，编号 / 品类 / 单品名 / 价格·次数；只有名字 truncate */
.look-caption {
  display: flex;
  flex: none;
  align-items: baseline;
  gap: var(--space-2);
  min-width: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.09);
  background: linear-gradient(to top, rgba(6, 8, 11, 0.94), rgba(8, 11, 15, 0.72));
  padding: var(--space-2) var(--space-3);
  white-space: nowrap;
}

.look-index {
  flex: none;
  font-size: 8px;
  letter-spacing: 0.14em;
  color: rgba(255, 255, 255, 0.4);
}

.look-kind {
  flex: none;
  font-size: 8px;
  letter-spacing: 0.12em;
  color: rgba(255, 255, 255, 0.32);
}

.look-name {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 10px;
  font-weight: 500;
  line-height: 1.3;
}

.look-meta {
  flex: none;
  font-size: 8.5px;
  letter-spacing: 0.02em;
  color: rgba(255, 255, 255, 0.45);
}

/* ---------- 空状态：虚线占位卡 + 中央玻璃卡 ---------- */
.empty-slot {
  display: grid;
  place-content: center;
  justify-items: center;
  gap: var(--space-1);
  min-width: 0;
  min-height: 0;
  border-radius: var(--radius-md);
  border: 1px dashed rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.025);
}

.empty-plus {
  font-size: 12px;
  font-weight: 300;
  color: rgba(255, 255, 255, 0.25);
}

.empty-slot-label {
  font-size: 8px;
  letter-spacing: 0.3em;
  color: rgba(255, 255, 255, 0.25);
}

.empty-guide {
  grid-column: span 2;
  align-self: center;
  justify-self: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  width: min(100%, 20rem);
  padding: var(--space-5);
  text-align: center;
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: linear-gradient(150deg, rgba(30, 34, 41, 0.82), rgba(10, 12, 16, 0.78));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.14),
    inset 0 0 0 1px rgba(255, 255, 255, 0.06),
    0 22px 40px -26px rgba(0, 0, 0, 0.9);
}

.empty-icon {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.7);
}

.empty-icon svg {
  width: 15px;
  height: 15px;
}

.empty-title {
  font-size: 13px;
}

.empty-text {
  max-width: 17rem;
  font-size: 10.5px;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.55);
}

/* ---------- 底部衬线 caption ---------- */
.board-foot {
  position: relative;
  z-index: 2;
  display: flex;
  flex: none;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-3);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: var(--space-2) var(--space-4);
}

.board-caption {
  font-size: 11px;
}

.board-footnote {
  font-size: 8px;
  letter-spacing: 0.28em;
}

/* ---------- 窄纸面（手机 / 窄栏）：字号收紧，图注折成两行但信息不丢 ---------- */
.is-compact .board-head {
  padding: var(--space-2) var(--space-3) 0;
}

.is-compact .board-stage {
  padding: var(--space-2) var(--space-3) var(--space-2);
}

.is-compact .board-foot {
  padding: var(--space-1) var(--space-3);
}

.is-compact .board-eyebrow {
  font-size: 8px;
  letter-spacing: 0.3em;
}

.is-compact .board-season {
  font-size: 12.5px;
}

.is-compact .board-occasion {
  font-size: 10px;
}

.is-compact .palette-bar {
  height: 9px;
}

.is-compact .palette-chip {
  width: 14px;
}

.is-compact .board-caption {
  font-size: 9.5px;
}

.is-compact .board-footnote {
  display: none;
}

.is-compact .look-media {
  padding: var(--space-2);
}

.is-compact .look-caption {
  flex-wrap: wrap;
  gap: var(--space-1) var(--space-2);
  padding: var(--space-1) var(--space-2);
  white-space: normal;
}

.is-compact .look-name {
  flex: 1 1 60%;
  font-size: 8px;
}

.is-compact .look-index,
.is-compact .look-kind {
  font-size: 6.5px;
}

/* 手机上价格 / 穿着次数单独占一行，避免把名字挤成一个字 */
.is-compact .look-meta {
  flex: 1 0 100%;
  font-size: 7px;
}

.is-compact .empty-guide {
  padding: var(--space-3);
}

.is-compact .empty-title {
  font-size: 12px;
}

.is-compact .empty-text {
  font-size: 9.5px;
  line-height: 1.7;
}

.is-compact .empty-slot-label {
  font-size: 7px;
  letter-spacing: 0.2em;
}
</style>