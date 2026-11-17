/** 数据看板接口类型（与后端 DashboardVO 对齐） */

export interface DashboardOverview {
  clothingCount: number
  outfitCount: number
  wearTotal: number
  favoriteCount: number
  unwornCount: number
  aiSessions: number
  postCount: number
  interactions: number
  recommendShown: number
  recommendAdopted: number
  adoptRate: number
  totalValue: number
}

export interface NamedValue {
  name: string
  value: number
}

export interface HotItem {
  id: number
  name: string
  imageUrl?: string | null
  categoryLabel: string
  colorHex?: string | null
  wearCount: number
}

export interface TrendPoint {
  date: string
  ai: number
  community: number
  wear: number
}

/** 单次穿着成本（CPW = Cost Per Wear）单品 */
export interface CpwItem {
  id: number
  name: string
  categoryLabel: string
  imageUrl?: string | null
  colorHex?: string | null
  price: number
  wearCount: number
  cpw: number
}

/** CPW 概览：衣橱总值 / 累计穿着 / 平均值 / 两个极值 */
export interface CpwOverview {
  totalValue: number
  totalWears: number
  avgCpw: number
  bestValue: CpwItem | null
  worstValue: CpwItem | null
}

/** 热力图单日记录（date = yyyy-MM-dd） */
export interface WearHeatmapDay {
  date: string
  count: number
}

/** 热力图汇总：活跃天数 / 最长连续 / 累计次数 / 区间 */
export interface HeatmapSummary {
  activeDays: number
  maxStreak: number
  totalWears: number
  from: string
  to: string
}

export interface DashboardData {
  overview: DashboardOverview
  categoryDistribution: NamedValue[]
  styleTags: NamedValue[]
  valueByCategory: NamedValue[]
  topWorn: HotItem[]
  trends: TrendPoint[]
  /**
   * 以下字段由后端追加，旧版接口可能整段缺失或返回 null，
   * 因此声明为可选；页面按「缺失 = 空态」处理，不依赖真实数据。
   */
  cpw?: CpwOverview | null
  cpwItems?: CpwItem[] | null
  wearHeatmap?: WearHeatmapDay[] | null
  heatmapSummary?: HeatmapSummary | null
}
