/** 单品图：上传图直接用；内置 SVG 统一取 /catalog（透明底 + 裁边），
 *  这样放在固定的深色「影棚」底上，浅色季节也不会糊成一片。 */
export function catalogImage(imageUrl?: string): string {
  if (!imageUrl) {
    return ''
  }
  const name = imageUrl.split('/').pop() ?? ''
  return name.endsWith('.svg') ? `/catalog/${name}` : imageUrl
}

/** 真实照片（jpg/png/webp 或来自上传/素材目录）：应当铺满裁剪，而不是等比留白 */
export function isPhotoImage(url?: string): boolean {
  if (!url) return false
  const trimmed = url.split('?')[0]
  if (/\.(jpe?g|png|webp|avif)$/i.test(trimmed)) return true
  return /\/(items|looks|uploads)\//.test(trimmed)
}
