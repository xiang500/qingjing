<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { CSSProperties } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { fetchDashboard } from '@/api/dashboard'
import { useThemeStore } from '@/stores/theme'
import type { CpwItem, DashboardData } from '@/types/dashboard'

echarts.use([BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const theme = useThemeStore()

const data = ref<DashboardData | null>(null)
const loading = ref(true)
const error = ref('')

const categoryRef = ref<HTMLDivElement | null>(null)
const topWornRef = ref<HTMLDivElement | null>(null)
const tagsRef = ref<HTMLDivElement | null>(null)
const valueRef = ref<HTMLDivElement | null>(null)
const trendRef = ref<HTMLDivElement | null>(null)
const heatScrollRef = ref<HTMLDivElement | null>(null)
let charts: ReturnType<typeof echarts.init>[] = []

/* ============================================================
 * 图表配色：渲染时从季节令牌（CSS 变量）读取
 * 换季 → 变量变化 → 销毁旧实例后重建，图表与界面同光
 * ============================================================ */
interface Rgb {
  r: number
  g: number
  b: number
}

interface ChartPalette {
  dark: boolean
  accent: string
  accentStrong: string
  ink: string
  ink2: string
  ink3: string
  muted: string
  line: string
  bg: string
  bg2: string
  label: string
  pie: string[]
  trend: string[]
}

function parseColor(value: string): Rgb | null {
  const hex = value.trim().replace(/^#/, '')
  if (!/^[0-9a-fA-F]{6}$/.test(hex)) {
    return null
  }
  return {
    r: Number.parseInt(hex.slice(0, 2), 16),
    g: Number.parseInt(hex.slice(2, 4), 16),
    b: Number.parseInt(hex.slice(4, 6), 16),
  }
}

/** 给 token 加透明度（token 本身是 hex 时拼出 rgba，画布上才稳） */
function rgba(color: string, alpha: number): string {
  const rgb = parseColor(color)
  if (!rgb) {
    return color
  }
  return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${alpha})`
}

/** 两色线性混合，用于把浅色季的洗涤色往墨色方向压深一档 */
function mix(from: string, to: string, ratio: number): string {
  const a = parseColor(from)
  const b = parseColor(to)
  if (!a || !b) {
    return from
  }
  const channel = (x: number, y: number) =>
    Math.round(x + (y - x) * ratio)
      .toString(16)
      .padStart(2, '0')
  return `#${channel(a.r, b.r)}${channel(a.g, b.g)}${channel(a.b, b.b)}`
}

function readChartPalette(): ChartPalette {
  const css = getComputedStyle(document.documentElement)
  const read = (name: string, fallback: string) => css.getPropertyValue(name).trim() || fallback

  const accent = read('--c-accent', '#c8a06d')
  const accentStrong = read('--c-accent-strong', accent)
  const ink = read('--c-ink', '#f6eee2')
  const ink2 = read('--c-ink-2', ink)
  const ink3 = read('--c-ink-3', ink)
  const muted = read('--c-muted', ink3)
  const line = read('--c-line', 'rgba(255,255,255,0.12)')
  const bg = read('--c-bg', '#14100b')
  const bg2 = read('--c-bg-2', bg)
  const wash1 = read('--c-wash-1', accent)
  const wash2 = read('--c-wash-2', accent)
  const wash3 = read('--c-wash-3', accent)
  const dark = document.documentElement.dataset.scheme === 'dark'
  // 浅色季的洗涤色偏粉彩，直接上画布会发灰，往墨色压 45% 保留色相又够清楚
  const tone = (value: string) => (dark ? value : mix(value, ink, 0.45))

  const piePool = [
    accent,
    tone(wash3),
    tone(wash2),
    tone(wash1),
    mix(accentStrong, ink, dark ? 0.18 : 0.12),
    tone(ink3),
    mix(accent, tone(wash3), 0.5),
  ]

  return {
    dark,
    accent,
    accentStrong,
    ink,
    ink2,
    ink3,
    muted,
    line,
    bg,
    bg2,
    // 浅色季的 muted 对比度不够，坐标轴/图例改用更深的 ink-3
    label: dark ? muted : ink3,
    pie: [...new Set(piePool)],
    trend: [accent, tone(wash2), tone(ink3)],
  }
}

function chartStyles(palette: ChartPalette) {
  const axisLabel = { color: palette.label, fontSize: 10 }
  const splitLine = { lineStyle: { color: palette.line } }
  const axisLine = { lineStyle: { color: palette.line } }
  const legendText = { color: palette.label, fontSize: 10 }
  const tooltip = {
    backgroundColor: palette.bg2,
    borderColor: palette.line,
    borderWidth: 1,
    padding: [8, 12],
    textStyle: { color: palette.ink, fontSize: 11 },
    extraCssText: 'border-radius:10px;box-shadow:0 18px 40px -28px rgba(0,0,0,0.55);',
  }
  return { axisLabel, splitLine, axisLine, legendText, tooltip }
}

const overviewCards = computed(() => {
  const o = data.value?.overview
  if (!o) {
    return []
  }
  return [
    { label: '衣橱单品', value: o.clothingCount, unit: '件', hint: `${o.favoriteCount} 件收藏` },
    { label: '穿搭套装', value: o.outfitCount, unit: '套', hint: `${o.unwornCount} 件还没穿过` },
    { label: '累计穿着', value: o.wearTotal, unit: '次', hint: '来自单品穿着记录' },
    { label: '衣橱总值', value: `¥${Math.round(o.totalValue).toLocaleString('zh-CN')}`, unit: '', hint: '已记录购入价合计' },
    { label: 'AI 会话', value: o.aiSessions, unit: '个', hint: '与穿搭顾问的对话' },
    { label: '社区分享', value: o.postCount, unit: '条', hint: '自己发布的搭配' },
    { label: '互动次数', value: o.interactions, unit: '次', hint: '帖子获赞 + 评论' },
    { label: '推荐曝光', value: o.recommendShown, unit: '次', hint: 'AI 给出的搭配建议' },
    { label: '推荐采纳', value: o.recommendAdopted, unit: '次', hint: '试穿或保存了推荐' },
    { label: '推荐采纳率', value: `${o.adoptRate}%`, unit: '', hint: '采纳 / 曝光' },
  ]
})

/* ============================================================
 * CPW（单次穿着成本）概览
 * 后端可能还没部署：cpw / cpwItems 一律按可选 + 本地兜底，
 * 字段缺失时渲染空态，不阻塞整页。
 * ============================================================ */
function toNumber(value: unknown, fallback = 0): number {
  const parsed = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

/** digits=0 取整并千分位；digits=1 保留一位小数（CPW 用） */
function money(value: unknown, digits = 0): string {
  const amount = toNumber(value)
  if (digits > 0) {
    return `¥${amount.toLocaleString('zh-CN', {
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    })}`
  }
  return `¥${Math.round(amount).toLocaleString('zh-CN')}`
}

/** 次数：负数 / NaN 归零 */
function wears(value: unknown): string {
  return Math.max(0, Math.round(toNumber(value))).toLocaleString('zh-CN')
}

function itemName(item: CpwItem | null): string {
  const name = item?.name
  if (typeof name === 'string' && name.trim()) {
    return name.trim()
  }
  return item ? '未命名单品' : '暂无单品'
}

function itemInitial(item: CpwItem | null): string {
  if (!item) {
    return '—'
  }
  const label = item.categoryLabel
  if (typeof label === 'string' && label.trim()) {
    return label.trim().slice(0, 1)
  }
  return itemName(item).slice(0, 1)
}

const cpw = computed(() => data.value?.cpw ?? null)

const cpwItems = computed<CpwItem[]>(() => {
  const raw = data.value?.cpwItems
  return Array.isArray(raw) ? raw.filter((item) => Boolean(item)) : []
})

/** cpw 升序：越便宜越靠前，顺带兜住 cpw 字段缺失 / 非数字 */
const cpwSorted = computed(() =>
  [...cpwItems.value].sort((a, b) => toNumber(a.cpw) - toNumber(b.cpw)),
)

const cpwRankingMax = computed(() =>
  cpwSorted.value.reduce((max, item) => Math.max(max, toNumber(item.cpw)), 0),
)

/** 横向排行取前 6 条 */
const cpwRanking = computed(() => cpwSorted.value.slice(0, 6))

const cpwBest = computed<CpwItem | null>(() => cpw.value?.bestValue ?? cpwSorted.value[0] ?? null)

const cpwWorst = computed<CpwItem | null>(() => cpw.value?.worstValue ?? cpwSorted.value.at(-1) ?? null)

/** 全 0 / 空对象视为「后端还没给数据」，走空态而不是显示 ¥0.0 */
const hasCpw = computed(() => {
  const overview = cpw.value
  if (!overview) {
    return false
  }
  return (
    toNumber(overview.totalValue) > 0 ||
    toNumber(overview.totalWears) > 0 ||
    toNumber(overview.avgCpw) > 0
  )
})

interface CpwHighlight {
  key: 'best' | 'worst'
  label: string
  item: CpwItem | null
  detail: string
  accent: boolean
}

const cpwHighlights = computed<CpwHighlight[]>(() => {
  if (!hasCpw.value) {
    return []
  }
  const best = cpwBest.value
  const worst = cpwWorst.value
  return [
    {
      key: 'best',
      label: '最划算',
      item: best,
      detail: best
        ? `${money(best.cpw, 1)} / 次 · 已穿 ${wears(best.wearCount)} 次`
        : '还缺价格或穿着记录',
      accent: false,
    },
    {
      key: 'worst',
      label: '最该处理',
      item: worst,
      detail: worst
        ? `已穿 ${wears(worst.wearCount)} 次，单次 ${money(worst.cpw, 1)}`
        : '还缺价格或穿着记录',
      accent: true,
    },
  ]
})

/** 条形长度 ∝ CPW：越短越划算，留 8% 最小可见宽度 */
function cpwBarWidth(item: CpwItem): string {
  const max = cpwRankingMax.value
  const value = Math.max(0, toNumber(item.cpw))
  const ratio = max > 0 ? Math.min(1, value / max) : 0
  return `${Math.round(8 + ratio * 92)}%`
}

/* ============================================================
 * 365 天穿着热力图：53 列 × 7 行（列 = 周，行 = 周一 → 周日）
 * 纯原生 DOM，配色全部走 CSS 变量，换季自动跟随 accent。
 * ============================================================ */
const HEAT_WEEKS = 53
const HEAT_CELL = 11
const HEAT_GAP = 2

function parseIsoDate(value: unknown): Date | null {
  if (typeof value !== 'string') {
    return null
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value.trim())
  if (!match) {
    return null
  }
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const date = new Date(year, month - 1, day)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return null
  }
  return date
}

function isoKey(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function addDays(date: Date, days: number): Date {
  const next = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  next.setDate(next.getDate() + days)
  return next
}

/** 周一 = 0 … 周日 = 6 */
function mondayIndex(date: Date): number {
  return (date.getDay() + 6) % 7
}

/** 4 档浓度 + 空格，色值来自季节令牌，不写死颜色 */
function heatLevelColor(count: number): string {
  if (count <= 0) {
    return 'color-mix(in oklab, var(--c-ink) 8%, transparent)'
  }
  if (count === 1) {
    return 'color-mix(in oklab, var(--c-accent) 22%, transparent)'
  }
  if (count === 2) {
    return 'color-mix(in oklab, var(--c-accent) 45%, transparent)'
  }
  if (count <= 4) {
    return 'color-mix(in oklab, var(--c-accent) 70%, transparent)'
  }
  return 'color-mix(in oklab, var(--c-accent) 100%, transparent)'
}

const heatLegend = [
  heatLevelColor(0),
  heatLevelColor(1),
  heatLevelColor(2),
  heatLevelColor(3),
  heatLevelColor(5),
]

const weekdayLabels = ['一', '', '三', '', '五', '', '']

interface HeatCell {
  key: string
  title: string
  style: CSSProperties
}

interface HeatMonthLabel {
  key: string
  text: string
  column: number
}

const heatMonthRowStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: `repeat(${HEAT_WEEKS}, ${HEAT_CELL}px)`,
  columnGap: `${HEAT_GAP}px`,
  height: '13px',
  marginBottom: '3px',
}

const heatGridStyle: CSSProperties = {
  display: 'grid',
  gridAutoFlow: 'column',
  gridTemplateColumns: `repeat(${HEAT_WEEKS}, ${HEAT_CELL}px)`,
  gridTemplateRows: `repeat(7, ${HEAT_CELL}px)`,
  gap: `${HEAT_GAP}px`,
}

const heatWeekdayStyle: CSSProperties = {
  display: 'grid',
  gridTemplateRows: `repeat(7, ${HEAT_CELL}px)`,
  rowGap: `${HEAT_GAP}px`,
}

const heatmap = computed(() => {
  // 显式依赖当前季节：换季时重算一次（色值本身是 CSS 变量，浏览器也会即时刷新）
  void theme.season

  const summary = data.value?.heatmapSummary ?? null
  const rawRecords = data.value?.wearHeatmap
  const records = Array.isArray(rawRecords) ? rawRecords : []

  const counts = new Map<string, number>()
  for (const entry of records) {
    const date = parseIsoDate(entry?.date)
    if (!date) {
      continue
    }
    const count = Math.round(toNumber(entry?.count))
    if (count <= 0) {
      continue
    }
    const key = isoKey(date)
    counts.set(key, (counts.get(key) ?? 0) + count)
  }

  const today = new Date()
  const todayDate = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const end = parseIsoDate(summary?.to) ?? todayDate
  // 对齐到本周日，再往前铺满 53 周（371 天），最后一列就是本周
  const start = addDays(end, 6 - mondayIndex(end) - (HEAT_WEEKS * 7 - 1))
  const todayKey = isoKey(todayDate)

  const cells: HeatCell[] = []
  const labels: HeatMonthLabel[] = []
  const wornKeys = new Set<string>()
  let activeFromGrid = 0
  let totalFromGrid = 0
  let streak = 0
  let maxStreak = 0

  for (let column = 0; column < HEAT_WEEKS; column += 1) {
    const weekStart = addDays(start, column * 7)

    for (let row = 0; row < 7; row += 1) {
      const date = addDays(weekStart, row)
      const key = isoKey(date)
      const count = counts.get(key) ?? 0
      const future = key > todayKey
      if (!future && count > 0) {
        wornKeys.add(key)
        activeFromGrid += 1
        totalFromGrid += count
        streak += 1
        maxStreak = Math.max(maxStreak, streak)
      } else if (!future) {
        streak = 0
      }
      cells.push({
        key,
        title: future ? '' : count > 0 ? `${key} · 穿了 ${count} 次` : `${key} · 没有记录`,
        style: { background: future ? 'transparent' : heatLevelColor(count) },
      })
    }

    // 月份标签：只标 1 / 4 / 7 / 10 月（该月 1 号落在这一列时打标）
    for (let row = 0; row < 7; row += 1) {
      const date = addDays(weekStart, row)
      const month = date.getMonth() + 1
      if (date.getDate() === 1 && (month === 1 || month === 4 || month === 7 || month === 10)) {
        labels.push({ key: `${month}-${column}`, text: `${month}月`, column })
        break
      }
    }
  }

  // 汇总优先用后端字段，缺了就按 365 天网格现算，保证不出现 NaN
  const activeDays = summary ? Math.round(toNumber(summary.activeDays)) : activeFromGrid
  const totalWears = summary ? Math.round(toNumber(summary.totalWears)) : totalFromGrid
  const maxStreakDays = summary ? Math.round(toNumber(summary.maxStreak)) : maxStreak

  return {
    cells,
    labels,
    hasRecords: wornKeys.size > 0 || counts.size > 0 || activeDays > 0,
    activeDays: Math.max(0, activeDays),
    totalWears: Math.max(0, totalWears),
    maxStreak: Math.max(0, maxStreakDays),
  }
})

/** 热力图容器默认滚到最右（最近日期） */
async function scrollHeatmapToEnd() {
  await nextTick()
  const el = heatScrollRef.value
  if (!el) {
    return
  }
  el.scrollLeft = el.scrollWidth
}

function disposeCharts() {
  charts.forEach((chart) => chart.dispose())
  charts = []
}

function initCharts() {
  disposeCharts()
  if (!data.value) {
    return
  }
  const palette = readChartPalette()
  const { axisLabel, splitLine, axisLine, legendText, tooltip } = chartStyles(palette)
  const category = data.value.categoryDistribution
  const hot = data.value.topWorn
  const tags = data.value.styleTags
  const trends = data.value.trends

  if (categoryRef.value) {
    const chart = echarts.init(categoryRef.value)
    chart.setOption({
      tooltip: { ...tooltip, trigger: 'item', formatter: '{b}：{c} 件（{d}%）' },
      legend: {
        bottom: 0,
        textStyle: legendText,
        itemWidth: 8,
        itemHeight: 8,
        icon: 'circle',
      },
      color: palette.pie,
      series: [
        {
          type: 'pie',
          radius: ['44%', '70%'],
          center: ['50%', '46%'],
          itemStyle: { borderColor: palette.bg, borderWidth: 2 },
          // 品类较多时外置标签会互相压叠，名称与数量交给图例 + tooltip
          label: { show: false },
          labelLine: { show: false },
          data: category.map((entry) => ({ name: entry.name, value: entry.value })),
        },
      ],
    })
    charts.push(chart)
  }

  if (topWornRef.value) {
    const chart = echarts.init(topWornRef.value)
    const items = [...hot].reverse()
    chart.setOption({
      tooltip: { ...tooltip, trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 8, right: 24, top: 12, bottom: 8, containLabel: true },
      xAxis: {
        type: 'value',
        axisLabel,
        splitLine,
        axisLine: { show: false },
      },
      yAxis: {
        type: 'category',
        data: items.map((item) => item.name),
        axisLabel: { ...axisLabel, width: 84, overflow: 'truncate' },
        axisLine,
        axisTick: { show: false },
      },
      series: [
        {
          type: 'bar',
          data: items.map((item) => item.wearCount),
          barWidth: 10,
          itemStyle: {
            borderRadius: [0, 6, 6, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: rgba(palette.accent, 0.28) },
              { offset: 1, color: palette.accent },
            ]),
          },
          label: { show: true, position: 'right', color: palette.label, fontSize: 10, formatter: '{c} 次' },
        },
      ],
    })
    charts.push(chart)
  }

  if (tagsRef.value) {
    const chart = echarts.init(tagsRef.value)
    chart.setOption({
      tooltip: { ...tooltip, trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 8, right: 12, top: 18, bottom: 8, containLabel: true },
      xAxis: {
        type: 'category',
        data: tags.map((tag) => tag.name),
        axisLabel: { ...axisLabel, interval: 0, rotate: 24 },
        axisLine,
        axisTick: { show: false },
      },
      yAxis: { type: 'value', axisLabel, splitLine, axisLine: { show: false } },
      series: [
        {
          type: 'bar',
          data: tags.map((tag) => tag.value),
          barWidth: 14,
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 1, 0, 0, [
              { offset: 0, color: rgba(palette.accent, 0.2) },
              { offset: 1, color: palette.accent },
            ]),
          },
        },
      ],
    })
    charts.push(chart)
  }

  if (valueRef.value) {
    const chart = echarts.init(valueRef.value)
    const values = [...data.value.valueByCategory].reverse()
    chart.setOption({
      tooltip: {
        ...tooltip,
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: unknown) => {
          const item = (params as Array<{ name: string; value: number }>)[0]
          return `${item.name}：¥${item.value.toLocaleString('zh-CN')}`
        },
      },
      grid: { left: 8, right: 24, top: 12, bottom: 8, containLabel: true },
      xAxis: { type: 'value', axisLabel, splitLine, axisLine: { show: false } },
      yAxis: {
        type: 'category',
        data: values.map((entry) => entry.name),
        axisLabel: { ...axisLabel, width: 84, overflow: 'truncate' },
        axisLine,
        axisTick: { show: false },
      },
      series: [
        {
          type: 'bar',
          data: values.map((entry) => entry.value),
          barWidth: 10,
          itemStyle: {
            borderRadius: [0, 6, 6, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: rgba(palette.ink3, 0.24) },
              { offset: 1, color: palette.ink3 },
            ]),
          },
          label: {
            show: true,
            position: 'right',
            color: palette.label,
            fontSize: 10,
            formatter: (params: unknown) => `¥${(params as { value: number }).value.toLocaleString('zh-CN')}`,
          },
        },
      ],
    })
    charts.push(chart)
  }

  if (trendRef.value) {
    const chart = echarts.init(trendRef.value)
    const trendSeries = (name: string, values: number[], color: string) => ({
      name,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      data: values,
      itemStyle: { color },
      lineStyle: { color, width: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: rgba(color, 0.24) },
          { offset: 1, color: rgba(color, 0) },
        ]),
      },
    })
    chart.setOption({
      tooltip: { ...tooltip, trigger: 'axis' },
      legend: {
        top: 0,
        right: 8,
        textStyle: legendText,
        itemWidth: 14,
        itemHeight: 8,
        icon: 'roundRect',
      },
      grid: { left: 8, right: 16, top: 34, bottom: 8, containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: trends.map((point) => point.date),
        axisLabel,
        axisLine,
      },
      yAxis: { type: 'value', minInterval: 1, axisLabel, splitLine, axisLine: { show: false } },
      series: [
        trendSeries('AI 会话', trends.map((point) => point.ai), palette.trend[0]!),
        trendSeries('社区发帖', trends.map((point) => point.community), palette.trend[1]!),
        trendSeries('记录穿着', trends.map((point) => point.wear), palette.trend[2]!),
      ],
    })
    charts.push(chart)
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await fetchDashboard()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '看板数据加载失败'
  } finally {
    loading.value = false
  }
  // 图表容器在 v-if="data" 之后才渲染，等 DOM 更新完再初始化 ECharts
  await nextTick()
  initCharts()
  // 热力图默认停在最右侧（最近日期）
  await scrollHeatmapToEnd()
}

function resize() {
  charts.forEach((chart) => chart.resize())
}

// 换季：先销毁旧实例，再按新的季节令牌重建，颜色跟着光线走
watch(
  () => theme.season,
  async () => {
    if (!data.value) {
      return
    }
    await nextTick()
    initCharts()
  },
)

onMounted(async () => {
  await load()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  disposeCharts()
})
</script>

<template>
  <div class="page-stack">
    <!-- ============ 页头 ============ -->
    <header class="flex flex-wrap items-end justify-between gap-[var(--space-4)] sm:gap-[var(--space-6)]">
      <div class="max-w-3xl flex-1">
        <p class="eyebrow">{{ theme.meta.en }} · {{ theme.meta.light }} · Insights</p>
        <h1 class="display mt-[var(--space-4)] text-balance text-[clamp(2rem,3.4vw,2.75rem)] leading-[1.2] text-mist-100">
          衣橱数据看板，
          <span class="accent-text">让穿搭习惯看得见</span>
        </h1>
        <p class="mt-[var(--space-3)] max-w-xl text-sm leading-[1.95] text-mist-400">
          衣橱规模、风格偏好、单品热度与近 14 天的活跃趋势，全部来自真实数据。换季时整页光线与图表配色一起切换，数据本身不变。
        </p>
      </div>

      <div class="action-row justify-end">
        <span class="chip cursor-default">{{ theme.meta.label }} · {{ theme.meta.name }}光线</span>
        <button type="button" class="btn-primary" :disabled="loading" @click="load">
          <svg
            class="size-4"
            :class="{ 'animate-spin': loading }"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
          >
            <path d="M20 12a8 8 0 1 1-2.3-5.6" stroke-linecap="round" />
            <path d="M20 4v4h-4" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          {{ loading ? '刷新中…' : '刷新数据' }}
        </button>
      </div>
    </header>

    <!-- ============ 错误提示 ============ -->
    <div
      v-if="error"
      class="glass-soft flex flex-wrap items-center gap-3 border border-rose-400/30 px-4 py-3 text-sm text-mist-100"
    >
      <svg class="size-4 shrink-0 text-gold-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 8v5M12 16h.01" stroke-linecap="round" />
      </svg>
      <span class="flex-1">
        {{ error }}
        <span class="text-mist-500">（请确认 Spring Boot 已在 8080 端口启动）</span>
      </span>
      <button type="button" class="btn-ghost !px-3 !py-1.5 !text-[11px]" @click="load">重试</button>
    </div>

    <template v-if="data">
      <!-- ============ 衣橱概览：10 张概览卡，按三组错开 ============ -->
      <section class="section-block">
        <header class="section-head">
          <div>
            <h2>衣橱概览</h2>
            <p>10 项核心指标，按衣橱资产 / 使用互动 / 推荐效果三组拆开看，先看总量再看结构。</p>
          </div>
          <div class="action-row justify-end">
            <span class="chip cursor-default">10 项指标</span>
            <span class="chip cursor-default">{{ data.overview.clothingCount }} 件在册</span>
          </div>
        </header>

        <!-- 三组卡片在桌面端依次下沉 16px（手机取消），组与组之间留 24px -->
        <div class="staggered">
          <div
            v-for="group in [
              { label: '衣橱资产', hint: '规模与价值', cards: overviewCards.slice(0, 4), note: '' },
              { label: '使用与互动', hint: 'AI · 社区 · 互动', cards: overviewCards.slice(4, 7), note: '互动 = 帖子获赞 + 评论，随发帖实时累计' },
              { label: '推荐效果', hint: '曝光与采纳', cards: overviewCards.slice(7), note: '采纳率 = 推荐采纳 ÷ 推荐曝光' },
            ]"
            :key="group.label"
            class="flex flex-col gap-[var(--space-3)]"
          >
            <div class="flex items-center gap-[var(--space-3)]">
              <p class="text-[10px] uppercase tracking-[0.24em] text-mist-500">{{ group.label }}</p>
              <span class="h-px flex-1 bg-ink-700/40" />
              <span class="text-[10px] text-mist-500">{{ group.hint }}</span>
            </div>

            <div class="grid grid-cols-2 gap-[var(--space-3)] lg:grid-cols-4 lg:gap-[var(--space-4)]">
              <article
                v-for="card in group.cards"
                :key="card.label"
                class="glass-soft relative overflow-hidden rounded-[var(--radius-md)] px-[var(--space-4)] py-[var(--space-3)]"
              >
                <span class="pointer-events-none absolute inset-x-0 top-0 h-px bg-gold-300/30" />
                <p class="text-[10px] uppercase tracking-[0.24em] text-mist-500">{{ card.label }}</p>
                <p class="mt-[var(--space-2)] flex items-baseline gap-1.5">
                  <span class="display text-[1.6rem] leading-none text-mist-100 tabular-nums">{{ card.value }}</span>
                  <span v-if="card.unit" class="text-[10px] text-mist-500">{{ card.unit }}</span>
                </p>
                <p class="mt-1.5 truncate text-[10px] text-mist-500">{{ card.hint }}</p>
              </article>

              <!-- 第三张卡之后补一块下沉说明，让三列组也铺满一行 -->
              <div
                v-if="group.note"
                class="surface-sunken flex items-center rounded-[var(--radius-md)] px-[var(--space-4)] py-[var(--space-3)] text-[10px] leading-[1.85] text-mist-500"
              >
                {{ group.note }}
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ============ CPW：单次穿着成本，独立成块 ============ -->
      <section class="section-block">
        <header class="section-head">
          <div>
            <h2>单次穿着成本 CPW</h2>
            <p>衣橱总值 ÷ 累计穿着次数：每穿一次的成本越低，这件衣服越值。</p>
          </div>
          <div class="action-row justify-end">
            <span class="chip cursor-default">前 6 件排行</span>
          </div>
        </header>

        <div v-if="hasCpw" class="grid gap-[var(--space-5)] sm:grid-cols-3">
          <!-- 平均值 -->
          <article class="glass-soft p-[var(--space-4)] sm:p-[var(--space-5)]">
            <p class="text-[10px] uppercase tracking-[0.24em] text-mist-500">平均单次穿着成本</p>
            <p class="mt-2 flex items-baseline gap-1.5">
              <span class="display text-[2rem] leading-none text-gold-300 tabular-nums">
                {{ money(cpw?.avgCpw, 1) }}
              </span>
            </p>
            <p class="mt-1 text-[10px] text-mist-500">每穿一次的成本</p>
            <p class="mt-3 flex flex-wrap items-center gap-x-1.5 text-[11px] text-mist-400 tabular-nums">
              <span>总值 {{ money(cpw?.totalValue) }}</span>
              <span class="text-mist-500">·</span>
              <span>累计 {{ wears(cpw?.totalWears) }} 次</span>
            </p>
          </article>

          <!-- 最划算 / 最该处理 -->
          <article
            v-for="highlight in cpwHighlights"
            :key="highlight.key"
            class="glass-soft flex items-start gap-[var(--space-3)] p-[var(--space-4)] sm:p-[var(--space-5)]"
            :class="{ 'ring-1 ring-inset ring-gold-300/45': highlight.accent }"
          >
            <span
              class="photo-tile grid size-12 shrink-0 place-items-center overflow-hidden rounded-xl ring-1 ring-inset ring-white/10"
            >
              <img
                v-if="highlight.item?.imageUrl"
                :src="highlight.item.imageUrl"
                :alt="itemName(highlight.item)"
                class="size-full object-contain p-1"
                loading="lazy"
              />
              <span v-else class="on-photo text-[10px]">{{ itemInitial(highlight.item) }}</span>
            </span>
            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center gap-1.5">
                <p class="text-[10px] uppercase tracking-[0.24em] text-mist-500">{{ highlight.label }}</p>
                <span
                  v-if="highlight.accent"
                  class="rounded-full border border-gold-300/40 bg-gold-300/10 px-1.5 py-0.5 text-[9px] leading-none text-gold-200"
                >
                  回本慢
                </span>
              </div>
              <p class="mt-1 truncate text-sm text-mist-100">{{ itemName(highlight.item) }}</p>
              <p class="mt-1 text-[10px] text-mist-400 tabular-nums">{{ highlight.detail }}</p>
            </div>
          </article>
        </div>

        <!-- 后端还没给 cpw 字段：明确空态，不白屏 -->
        <div v-else class="glass-soft flex flex-col items-center gap-[var(--space-2)] px-[var(--space-4)] py-[var(--space-6)] text-center">
          <p class="text-[11px] text-mist-300">暂无单次穿着成本数据</p>
          <p class="text-[10px] text-mist-500">等衣橱里记录价格与穿着次数后，这里会自动算出 CPW</p>
        </div>

        <!-- CPW 排行：条越短越划算 -->
        <article v-if="cpwRanking.length" class="glass-soft p-[var(--space-4)] sm:p-[var(--space-5)]">
          <header class="flex flex-wrap items-baseline justify-between gap-2">
            <h3 class="eyebrow">CPW 排行</h3>
            <span class="text-[10px] text-mist-500">取前 6 件 · 条越短越划算</span>
          </header>
          <ul class="mt-[var(--space-4)] space-y-[var(--space-2)]">
            <li
              v-for="(item, index) in cpwRanking"
              :key="`${index}-${item.id}`"
              class="flex items-center gap-[var(--space-2)]"
            >
              <span class="w-4 shrink-0 text-[10px] text-mist-500 tabular-nums">{{ index + 1 }}</span>
              <span class="w-20 shrink-0 truncate text-[11px] text-mist-300 sm:w-28">
                {{ itemName(item) }}
              </span>
              <span class="h-1.5 min-w-0 flex-1 overflow-hidden rounded-full bg-ink-800">
                <span
                  class="block h-full rounded-full bg-gradient-to-r from-gold-300/35 to-gold-300"
                  :style="{ width: cpwBarWidth(item) }"
                />
              </span>
              <span
                class="w-[4.5rem] shrink-0 whitespace-nowrap text-right text-[10px] text-mist-400 tabular-nums"
              >
                {{ money(item.cpw, 1) }} / 次
              </span>
            </li>
          </ul>
        </article>
      </section>

      <!-- ============ 穿着热力图：独立成块，原生 DOM，不额外引库 ============ -->
      <section class="section-block">
        <header class="section-head">
          <div>
            <h2>穿着热力图</h2>
            <p>近 365 天每一天的穿着密度：颜色越深，当天记录越多；横向滚动可以回看更早。</p>
          </div>
          <div v-if="heatmap.hasRecords" class="action-row justify-end">
            <span class="chip cursor-default">活跃 {{ heatmap.activeDays }} 天</span>
            <span class="chip cursor-default">最长连续 {{ heatmap.maxStreak }} 天</span>
            <span class="chip cursor-default">累计 {{ heatmap.totalWears }} 次</span>
          </div>
        </header>

        <div class="glass p-[var(--space-4)] sm:p-[var(--space-6)]">
          <template v-if="heatmap.hasRecords">
            <!-- 整块居中：内容比卡片窄时居中，比卡片宽时横向滚动 -->
            <div ref="heatScrollRef" class="overflow-x-auto pb-1">
              <div class="mx-auto w-max max-w-none">
                <div class="flex gap-2">
                  <!-- 星期标签：一 / 三 / 五 -->
                  <div class="shrink-0">
                    <div class="mb-[3px] h-[13px]" aria-hidden="true" />
                    <div class="text-[9px] leading-[11px] text-mist-500" :style="heatWeekdayStyle">
                      <span v-for="(label, index) in weekdayLabels" :key="index" :style="{ gridRow: index + 1 }">
                        {{ label }}
                      </span>
                    </div>
                  </div>

                  <div class="min-w-0">
                    <!-- 月份标签：1 / 4 / 7 / 10 月 -->
                    <div class="text-[9px] leading-[13px] text-mist-500" :style="heatMonthRowStyle">
                      <span
                        v-for="label in heatmap.labels"
                        :key="label.key"
                        :style="{ gridColumn: label.column + 1 }"
                      >
                        {{ label.text }}
                      </span>
                    </div>
                    <!-- 53 周 × 7 天：列 = 周，行 = 周一 → 周日 -->
                    <div :style="heatGridStyle">
                      <span
                        v-for="cell in heatmap.cells"
                        :key="cell.key"
                        class="block rounded-[2px]"
                        :style="cell.style"
                        :title="cell.title || undefined"
                      />
                    </div>
                  </div>
                </div>

                <!-- 图例：0 次 / 1 / 2 / 3-4 / 5+ -->
                <div class="mt-[var(--space-3)] flex items-center justify-end gap-1.5 text-[9px] leading-none text-mist-500">
                  <span>少</span>
                  <span
                    v-for="(color, index) in heatLegend"
                    :key="index"
                    class="size-[11px] rounded-[2px] ring-1 ring-inset ring-white/10"
                    :style="{ background: color }"
                  />
                  <span>多</span>
                </div>
              </div>
            </div>
          </template>

          <!-- 后端还没有热力图字段：明确空态 -->
          <div
            v-else
            class="rounded-[var(--radius-md)] border border-dashed border-ink-700/60 px-[var(--space-4)] py-[var(--space-6)] text-center"
          >
            <p class="text-[11px] text-mist-300">最近 365 天还没有穿着记录</p>
            <p class="mt-1 text-[10px] text-mist-500">在衣橱里记录几次「今天穿了什么」，这里就会亮起来</p>
          </div>
        </div>
      </section>

      <!-- ============ 结构与偏好：两块图表，区块内 24px ============ -->
      <section class="section-block">
        <header class="section-head">
          <div>
            <h2>结构与偏好</h2>
            <p>品类分布说明衣橱由什么构成，风格标签说明你偏爱的表达。</p>
          </div>
          <div class="action-row justify-end">
            <span class="chip cursor-default">{{ data.overview.clothingCount }} 件单品</span>
            <span class="chip cursor-default">来自单品标签</span>
          </div>
        </header>

        <div class="grid gap-[var(--space-5)] lg:grid-cols-2">
          <article class="glass-soft p-[var(--space-4)] sm:p-[var(--space-5)]">
            <header class="flex flex-wrap items-baseline justify-between gap-2">
              <h3 class="eyebrow">品类分布</h3>
              <span class="text-[10px] text-mist-500">{{ data.overview.clothingCount }} 件单品</span>
            </header>
            <div ref="categoryRef" class="mt-[var(--space-2)] h-64 w-full" />
          </article>

          <article class="glass-soft p-[var(--space-4)] sm:p-[var(--space-5)]">
            <header class="flex flex-wrap items-baseline justify-between gap-2">
              <h3 class="eyebrow">风格标签 Top 8</h3>
              <span class="text-[10px] text-mist-500">来自单品标签</span>
            </header>
            <div ref="tagsRef" class="mt-[var(--space-2)] h-64 w-full" />
            <p v-if="!data.styleTags.length" class="text-center text-[11px] text-mist-500">单品还没有风格标签</p>
          </article>
        </div>
      </section>

      <!-- ============ 单品与价值：热度 / 价值 / 趋势 ============ -->
      <section class="section-block">
        <header class="section-head">
          <div>
            <h2>单品与价值</h2>
            <p>穿得最多的单品、钱花在哪一类，以及近 14 天的活跃走势。</p>
          </div>
          <div class="action-row justify-end">
            <span class="chip cursor-default">近 14 天</span>
            <span class="chip cursor-default">按穿着次数</span>
          </div>
        </header>

        <div class="grid gap-[var(--space-5)] lg:grid-cols-2">
          <article class="glass-soft p-[var(--space-4)] sm:p-[var(--space-5)]">
            <header class="flex flex-wrap items-baseline justify-between gap-2">
              <h3 class="eyebrow">热门单品</h3>
              <span class="text-[10px] text-mist-500">按穿着次数</span>
            </header>
            <div ref="topWornRef" class="mt-[var(--space-2)] h-64 w-full" />
            <p v-if="!data.topWorn.length" class="text-center text-[11px] text-mist-500">还没有穿着记录</p>
          </article>

          <article class="glass-soft p-[var(--space-4)] sm:p-[var(--space-5)]">
            <header class="flex flex-wrap items-baseline justify-between gap-2">
              <h3 class="eyebrow">分类价值</h3>
              <span class="text-[10px] text-mist-500">按购入价合计</span>
            </header>
            <div ref="valueRef" class="mt-[var(--space-2)] h-64 w-full" />
            <p v-if="!data.valueByCategory.length" class="text-center text-[11px] text-mist-500">单品还没有记录价格</p>
          </article>

          <article class="glass-soft p-[var(--space-4)] sm:p-[var(--space-5)] lg:col-span-2">
            <header class="flex flex-wrap items-baseline justify-between gap-2">
              <h3 class="eyebrow">近 14 天活跃趋势</h3>
              <span class="text-[10px] text-mist-500">AI 会话 / 社区发帖 / 记录穿着</span>
            </header>
            <div ref="trendRef" class="mt-[var(--space-2)] h-64 w-full" />
          </article>
        </div>
      </section>

      <p class="text-center text-[11px] leading-relaxed text-mist-500">
        指标按当前账号的衣橱与行为实时聚合 · 切换季节只改变光线与配色，不改变任何数值。
      </p>
    </template>

    <div v-else class="grid gap-[var(--space-5)] lg:grid-cols-2">
      <div v-for="index in 4" :key="index" class="glass-soft h-72 p-[var(--space-5)]">
        <div class="skeleton h-3.5 w-1/4 rounded-full" />
        <div class="skeleton mt-[var(--space-5)] h-52 w-full rounded-xl" />
      </div>
    </div>
  </div>
</template>
