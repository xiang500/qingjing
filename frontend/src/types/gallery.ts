/** 旋转画廊的单卡片数据（由衣橱单品或演示数据映射而来） */
export interface GalleryItem {
  id: number | string
  name: string
  /** 单品图；为空时卡片显示品类首字占位 */
  imageUrl?: string
  categoryLabel: string
  colorHex?: string | null
  /** 卡片副标题：品牌 · 材质 / 子类之类的一行摘要 */
  meta?: string
  /** 穿着次数：有值时显示在卡片右下角 */
  wearCount?: number
}
