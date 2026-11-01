export interface ShareCardItem {
  name: string
  imageUrl?: string | null
  colorHex?: string | null
}

export interface ShareCardInput {
  title: string
  reason?: string
  score?: number
  occasion?: string
  items: ShareCardItem[]
}

const WIDTH = 1080
const HEIGHT = 1440

function loadImage(url: string): Promise<HTMLImageElement | null> {
  return new Promise((resolve) => {
    const image = new Image()
    image.crossOrigin = 'anonymous'
    image.onload = () => resolve(image)
    image.onerror = () => resolve(null)
    image.src = url
  })
}

function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

function wrap(ctx: CanvasRenderingContext2D, text: string, maxWidth: number): string[] {
  const lines: string[] = []
  let current = ''
  for (const char of text) {
    if (ctx.measureText(current + char).width > maxWidth) {
      lines.push(current)
      current = char
    } else {
      current += char
    }
  }
  if (current) {
    lines.push(current)
  }
  return lines
}

/**
 * 生成分享卡片：不依赖后端图片服务，纯 Canvas 绘制并导出 PNG。
 * 目的是让用户在社区之外（朋友圈 / 小红书）也能带走一张可读的搭配卡。
 */
export async function downloadShareCard(input: ShareCardInput, filename = 'qingjing-outfit.png') {
  const canvas = document.createElement('canvas')
  canvas.width = WIDTH
  canvas.height = HEIGHT
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    return
  }

  const background = ctx.createLinearGradient(0, 0, WIDTH, HEIGHT)
  background.addColorStop(0, '#0a0e16')
  background.addColorStop(0.55, '#111826')
  background.addColorStop(1, '#171f2e')
  ctx.fillStyle = background
  ctx.fillRect(0, 0, WIDTH, HEIGHT)

  const glow = ctx.createRadialGradient(WIDTH * 0.82, 120, 40, WIDTH * 0.82, 120, 620)
  glow.addColorStop(0, 'rgba(214, 189, 128, 0.22)')
  glow.addColorStop(1, 'rgba(214, 189, 128, 0)')
  ctx.fillStyle = glow
  ctx.fillRect(0, 0, WIDTH, 700)

  ctx.strokeStyle = 'rgba(214, 189, 128, 0.45)'
  ctx.lineWidth = 2
  roundRect(ctx, 36, 36, WIDTH - 72, HEIGHT - 72, 28)
  ctx.stroke()

  ctx.fillStyle = '#e3cd9b'
  ctx.font = '500 26px "HarmonyOS Sans SC", "PingFang SC", sans-serif'
  ctx.fillText('轻境 · LIGHTNESS', 88, 128)

  ctx.fillStyle = 'rgba(245, 242, 236, 0.55)'
  ctx.font = '22px "HarmonyOS Sans SC", "PingFang SC", sans-serif'
  ctx.fillText('AI 穿搭 · 3D 数字衣橱', 88, 168)

  const titleFont = '600 58px "Songti SC", "Noto Serif SC", serif'
  ctx.fillStyle = '#f5f2ec'
  ctx.font = titleFont
  const titleLines = wrap(ctx, input.title, WIDTH - 300)
  titleLines.slice(0, 2).forEach((line, index) => {
    ctx.fillText(line, 88, 300 + index * 74)
  })

  if (input.score != null) {
    ctx.strokeStyle = 'rgba(227, 205, 155, 0.85)'
    ctx.lineWidth = 3
    ctx.beginPath()
    ctx.arc(WIDTH - 168, 292, 76, 0, Math.PI * 2)
    ctx.stroke()
    ctx.fillStyle = '#e3cd9b'
    ctx.font = '600 54px "HarmonyOS Sans SC", sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(String(input.score), WIDTH - 168, 306)
    ctx.font = '20px "HarmonyOS Sans SC", sans-serif'
    ctx.fillStyle = 'rgba(245, 242, 236, 0.6)'
    ctx.fillText('契合度', WIDTH - 168, 340)
    ctx.textAlign = 'left'
  }

  const cardY = 430
  const cardW = 214
  const cardH = 268
  const gap = 22
  const items = input.items.slice(0, 4)
  const totalW = items.length * cardW + Math.max(0, items.length - 1) * gap
  let x = (WIDTH - totalW) / 2

  for (const item of items) {
    roundRect(ctx, x, cardY, cardW, cardH, 18)
    ctx.fillStyle = 'rgba(255, 255, 255, 0.05)'
    ctx.fill()
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.12)'
    ctx.lineWidth = 1.5
    ctx.stroke()

    if (item.imageUrl) {
      const image = await loadImage(item.imageUrl)
      if (image) {
        const size = Math.min(cardW - 24, cardH - 24)
        ctx.save()
        roundRect(ctx, x + 12, cardY + 12, cardW - 24, cardH - 24, 14)
        ctx.clip()
        ctx.drawImage(image, x + (cardW - size) / 2, cardY + (cardH - size) / 2, size, size)
        ctx.restore()
      }
    } else if (item.colorHex) {
      ctx.beginPath()
      ctx.arc(x + cardW / 2, cardY + cardH / 2, 52, 0, Math.PI * 2)
      ctx.fillStyle = item.colorHex
      ctx.fill()
    }

    ctx.fillStyle = 'rgba(245, 242, 236, 0.72)'
    ctx.font = '20px "HarmonyOS Sans SC", sans-serif'
    const label = item.name.length > 9 ? `${item.name.slice(0, 9)}…` : item.name
    ctx.fillText(label, x + 14, cardY + cardH + 30)
    x += cardW + gap
  }

  const reasonTop = cardY + cardH + 88
  if (input.reason) {
    ctx.fillStyle = 'rgba(245, 242, 236, 0.78)'
    ctx.font = '26px "HarmonyOS Sans SC", sans-serif'
    const lines = wrap(ctx, input.reason, WIDTH - 176).slice(0, 6)
    lines.forEach((line, index) => {
      ctx.fillText(line, 88, reasonTop + index * 44)
    })
  }

  if (input.occasion) {
    ctx.fillStyle = 'rgba(227, 205, 155, 0.9)'
    ctx.font = '24px "HarmonyOS Sans SC", sans-serif'
    ctx.fillText(`场合 · ${input.occasion}`, 88, HEIGHT - 168)
  }

  ctx.fillStyle = 'rgba(245, 242, 236, 0.4)'
  ctx.font = '22px "HarmonyOS Sans SC", sans-serif'
  ctx.fillText('由「轻境」AI 穿搭顾问生成 · 浏览器端实时渲染', 88, HEIGHT - 108)

  const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/png'))
  if (!blob) {
    return
  }
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 2000)
}