/**
 * 四季光场 · 季节元数据
 *
 * 每个季节对应一种「光」：春·晨光、夏·正午、秋·暖阳、冬·夜雪。
 * 春/夏走浅色纸感，秋/冬走深色夜感，配色令牌在 style.css 里按
 * html[data-season] / html[data-scheme] 切换，这里只放文案与色卡。
 */

export type SeasonKey = 'spring' | 'summer' | 'autumn' | 'winter'
export type SeasonScheme = 'light' | 'dark'

export interface SeasonSwatch {
  name: string
  hex: string
}

export interface SeasonMeta {
  key: SeasonKey
  /** 单字：春 / 夏 / 秋 / 冬 */
  label: string
  /** 季名：新绿 / 海盐 / 焦糖 / 雪雾 */
  name: string
  en: string
  /** 光线名：晨光 / 正午 / 暖阳 / 夜雪 */
  light: string
  scheme: SeasonScheme
  /** 浏览器地址栏 / 主题色，与 style.css 的 --c-bg 对应 */
  bgHex: string
  months: number[]
  /** 节气 */
  solarTerm: string
  /** 首屏大标题（第二行是强调色） */
  headline: [string, string]
  lede: string
  swatches: SeasonSwatch[]
  tags: string[]
}

export const SEASONS: SeasonMeta[] = [
  {
    key: 'spring',
    label: '春',
    name: '新绿',
    en: 'Spring',
    light: '晨光',
    scheme: 'light',
    bgHex: '#f7f4ec',
    months: [3, 4, 5],
    solarTerm: '谷雨',
    headline: ['把春天，', '穿在身上'],
    lede: '柳芽色的针织、被风掀起的裙摆，还有一件刚好挡风的薄外套。把这一季的颜色先收进衣橱，出门前的一分钟交给轻境。',
    swatches: [
      { name: '柳芽', hex: '#c9d8b4' },
      { name: '樱粉', hex: '#efd4cd' },
      { name: '雾蓝', hex: '#c3dbe0' },
      { name: '燕麦', hex: '#e4d9c4' },
    ],
    tags: ['轻薄', '叠穿', '晨光感'],
  },
  {
    key: 'summer',
    label: '夏',
    name: '海盐',
    en: 'Summer',
    light: '正午',
    scheme: 'light',
    bgHex: '#f6f7f0',
    months: [6, 7, 8],
    solarTerm: '小满',
    headline: ['让夏天，', '轻一点'],
    lede: '亚麻、真丝、被海风晒过的白。衣橱按材质和透气质感排好，AI 在你出门前给出不闷汗的那一套。',
    swatches: [
      { name: '海盐', hex: '#a9cfd8' },
      { name: '细沙', hex: '#eddfb9' },
      { name: '薄荷', hex: '#c4dfcb' },
      { name: '贝壳', hex: '#f5efe2' },
    ],
    tags: ['透气', '亚麻', '高饱和点缀'],
  },
  {
    key: 'autumn',
    label: '秋',
    name: '焦糖',
    en: 'Autumn',
    light: '暖阳',
    scheme: 'light',
    bgHex: '#f8efdf',
    months: [9, 10, 11],
    solarTerm: '霜降',
    headline: ['把秋天，', '穿在身上'],
    lede: '针织、风衣、苔绿与焦糖。降温之前，让轻境先把这一季的叠穿想好——记录、搭配、AI 推荐，都收在同一道光里。',
    swatches: [
      { name: '焦糖', hex: '#c8a06d' },
      { name: '苔绿', hex: '#8d9a6e' },
      { name: '赤陶', hex: '#c4714f' },
      { name: '米杏', hex: '#e6d8c3' },
    ],
    tags: ['叠穿', '大地色', '针织'],
  },
  {
    key: 'winter',
    label: '冬',
    name: '雪雾',
    en: 'Winter',
    light: '夜雪',
    scheme: 'dark',
    bgHex: '#0b1116',
    months: [12, 1, 2],
    solarTerm: '大雪',
    headline: ['把冬天，', '穿成一层暖'],
    lede: '羊毛大衣、围巾、手套和一件足够厚的内搭。雪落下来之前，先把这一季的保暖层次和配色排好。',
    swatches: [
      { name: '冰蓝', hex: '#aec9de' },
      { name: '雾紫', hex: '#cbc4d8' },
      { name: '烟灰', hex: '#9aa6b1' },
      { name: '雪白', hex: '#f2f5f7' },
    ],
    tags: ['保暖', '层次', '羊毛'],
  },
]

export const SEASON_KEYS = SEASONS.map((season) => season.key)

/** 四季自动轮播间隔：30 秒换一季，两分钟走完一轮 */
export const SEASON_AUTOPLAY_MS = 30_000

export function isSeasonKey(value: unknown): value is SeasonKey {
  return typeof value === 'string' && SEASON_KEYS.includes(value as SeasonKey)
}

export function seasonOfMonth(month: number): SeasonKey {
  const season = SEASONS.find((item) => item.months.includes(month))
  return season?.key ?? 'autumn'
}

export function seasonMeta(key: SeasonKey): SeasonMeta {
  return SEASONS.find((season) => season.key === key) ?? SEASONS[2]
}
