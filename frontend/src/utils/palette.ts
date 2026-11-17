/**
 * 搭配色彩评估（纯函数，无 Vue / DOM 依赖）。
 *
 * 页面和评测脚本共用这一份实现，保证「页面显示的结论」和「评测跑出来的结论」一致。
 * 设计原则：所有结论都能追溯到可复现的数值，不依赖大模型。
 */

export interface PackagePalette {
  harmony: number
  hueSpan: number
  lightSpan: number
  tone: string
  reason: string
  tags: string[]
  complete: boolean
  missing: string[]
  colorCount: number
}

export interface PaletteItem {
  category: string
  colorHex?: string | null
  styleTags?: string[]
}

interface ColorEntry {
  h: number
  s: number
  l: number
  /** chroma = max-min，判断「是否是彩色」比 HSL 的 s 更可靠：
   *  接近白色的浅色 s 会被放大，但 chroma 依然很低 */
  chroma: number
}

export function hexToHsl(hex?: string | null): ColorEntry | null {
  if (!hex) {
    return null
  }
  const value = hex.replace('#', '')
  if (value.length !== 6) {
    return null
  }
  const r = parseInt(value.slice(0, 2), 16) / 255
  const g = parseInt(value.slice(2, 4), 16) / 255
  const b = parseInt(value.slice(4, 6), 16) / 255
  if (Number.isNaN(r) || Number.isNaN(g) || Number.isNaN(b)) {
    return null
  }
  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  const l = (max + min) / 2
  const d = max - min
  if (d === 0) {
    return { h: 0, s: 0, l, chroma: 0 }
  }
  const s = d / (1 - Math.abs(2 * l - 1))
  let h: number
  if (max === r) {
    h = ((g - b) / d) % 6
  } else if (max === g) {
    h = (b - r) / d + 2
  } else {
    h = (r - g) / d + 4
  }
  return { h: (h * 60 + 360) % 360, s, l, chroma: d }
}

/**
 * 色相跨度：取色相环上的最短覆盖弧。
 * 直接 max-min 会把「350° 与 10°」算成 340°，实际上两者只差 20°。
 */
export function hueSpanOf(hues: number[]): number {
  if (hues.length <= 1) {
    return 0
  }
  const sorted = [...hues].sort((a, b) => a - b)
  let maxGap = -1
  for (let i = 0; i < sorted.length; i++) {
    const next = i === sorted.length - 1 ? sorted[0] + 360 : sorted[i + 1]
    maxGap = Math.max(maxGap, next - sorted[i])
  }
  return Math.round(360 - maxGap)
}

export function evaluatePalette(items: PaletteItem[]): PackagePalette {
  const colors = items
    .map((item) => hexToHsl(item.colorHex))
    .filter((value): value is ColorEntry => value !== null)

  const categories = new Set(items.map((item) => item.category))
  const missing: string[] = []
  if (!categories.has('DRESS')) {
    // 外套也视作「有上装」，否则穿长风衣时会被判成没穿上衣
    if (!categories.has('TOP') && !categories.has('OUTER')) {
      missing.push('上装')
    }
    if (!categories.has('BOTTOM')) {
      missing.push('下装')
    }
  }
  if (!categories.has('SHOES')) {
    missing.push('鞋履')
  }
  const complete = items.length > 0 && missing.length === 0

  if (!colors.length) {
    return {
      harmony: 0,
      hueSpan: 0,
      lightSpan: 0,
      tone: '待评估',
      reason: items.length
        ? '已选单品都没有色值，无法评估配色，建议补全颜色信息。'
        : '还没有选中任何单品，先挑一件开始。',
      tags: [],
      complete: false,
      missing,
      colorCount: 0,
    }
  }

  // 只有真正有彩度的颜色才参与色相统计：白色、米白、灰这类低彩度色
  // 如果按 HSL 的 s 判断会被误当成彩色，进而把色相跨度算歪
  const chromatic = colors.filter((c) => c.chroma > 0.06 && c.l > 0.05 && c.l < 0.97)
  const hues = chromatic.map((c) => c.h)
  const hueSpan = hueSpanOf(hues)

  const lights = colors.map((c) => c.l)
  const lightSpan = Math.max(...lights) - Math.min(...lights)

  let harmony = 78
  let tone = '邻近色'
  let reason = ''
  if (chromatic.length === 0) {
    harmony = 86
    tone = '中性色'
    reason = '整套都是黑白灰米这类中性色，最不容易出错，靠材质和明度拉开层次就行。'
  } else if (hues.length <= 1 || hueSpan < 24) {
    harmony = 88
    tone = '同色系'
    reason = `色相跨度仅 ${hueSpan}°，整套是同一色系，最稳也最显质感。`
  } else if (hueSpan < 90) {
    harmony = 90
    tone = '邻近色'
    reason = `色相跨度 ${hueSpan}°，属于邻近色搭配，过渡自然。`
  } else if (hueSpan < 150) {
    harmony = 74
    tone = '对比色'
    reason = `色相跨度 ${hueSpan}°，是对比配色，需要用面积主次控制，否则容易乱。`
  } else {
    harmony = 82
    tone = '互补色'
    reason = `色相跨度 ${hueSpan}°，接近互补色，视觉冲击强，适合做点缀色。`
  }

  if (lightSpan > 0.32) {
    harmony += 6
    reason += ' 明度落差大，有层次。'
  } else if (lightSpan < 0.1) {
    harmony -= 12
    reason += ' 全身明度几乎一致，会显得平，建议换一件深浅差明显的。'
  } else {
    harmony += 2
    reason += ' 明度有过渡。'
  }

  if (complete) {
    harmony += 4
  }

  const tags = [...new Set(items.flatMap((item) => item.styleTags ?? []))].slice(0, 4)

  return {
    harmony: Math.max(20, Math.min(99, Math.round(harmony))),
    hueSpan,
    lightSpan: Math.round(lightSpan * 100) / 100,
    tone,
    reason,
    tags,
    complete,
    missing,
    colorCount: colors.length,
  }
}