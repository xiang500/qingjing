import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  SEASON_AUTOPLAY_MS,
  isSeasonKey,
  seasonMeta,
  seasonOfMonth,
  type SeasonKey,
} from '@/theme/seasons'

const STORAGE_KEY = 'qingjing:season'
/** 四季循环顺序：春 → 夏 → 秋 → 冬 */
const CYCLE: SeasonKey[] = ['spring', 'summer', 'autumn', 'winter']

function nextOf(key: SeasonKey): SeasonKey {
  const index = CYCLE.indexOf(key)
  return CYCLE[(index + 1) % CYCLE.length]
}

function prefersReducedMotion(): boolean {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

type ViewTransitionDocument = Document & {
  startViewTransition?: (callback: () => void) => { finished: Promise<void> }
}

function readStoredSeason(): SeasonKey | null {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY)
    return isSeasonKey(stored) ? stored : null
  } catch {
    return null
  }
}

/**
 * 季节光场：把当前季节写到 <html data-season="…" data-scheme="…">，
 * 配色、玻璃材质、背景动机全部由 style.css 的季节令牌接管；
 * 同时同步 Element Plus 的 dark 类（秋冬是深色光线）。
 *
 * 季节现在是「自动播放」的：默认按当前月份起步，然后每 30 秒轮换到下一季。
 * 切换时优先用 View Transition 做整页交叉淡入（不支持则直接换色），
 * 减少深浅色突变带来的割裂感。
 */
export const useThemeStore = defineStore('theme', () => {
  const initial = readStoredSeason() ?? seasonOfMonth(new Date().getMonth() + 1)
  const season = ref<SeasonKey>(initial)
  const autoPlaying = ref(false)

  const meta = computed(() => seasonMeta(season.value))
  const isDark = computed(() => meta.value.scheme === 'dark')

  let timer: number | undefined

  function apply(key: SeasonKey, persist: boolean) {
    const root = document.documentElement
    const current = seasonMeta(key)
    root.dataset.season = key
    root.dataset.scheme = current.scheme
    root.classList.toggle('dark', current.scheme === 'dark')
    const themeColor = document.querySelector('meta[name="theme-color"]')
    if (themeColor) {
      themeColor.setAttribute('content', current.bgHex)
    }
    if (!persist) {
      return
    }
    try {
      window.localStorage.setItem(STORAGE_KEY, key)
    } catch {
      /* 隐私模式下写不进去也不影响使用 */
    }
  }

  /** 换季：DOM 属性变更包在 View Transition 里，浏览器不支持时直接切 */
  function commit(key: SeasonKey, options: { persist?: boolean; animate?: boolean } = {}) {
    const { persist = true, animate = true } = options
    const run = () => {
      season.value = key
      apply(key, persist)
    }
    const viewDocument = document as ViewTransitionDocument
    if (animate && typeof viewDocument.startViewTransition === 'function' && !prefersReducedMotion()) {
      viewDocument.startViewTransition(run)
    } else {
      run()
    }
  }

  function setSeason(key: SeasonKey) {
    commit(key)
  }

  /**
   * 开始四季自动轮播。
   * - 隐藏标签页时跳过这一拍，避免回来时连跳好几季
   * - 系统开启「减少动态效果」时不自动播放，尊重用户偏好
   */
  function startAutoPlay() {
    stopAutoPlay()
    if (prefersReducedMotion() || typeof window.setInterval !== 'function') {
      return
    }
    autoPlaying.value = true
    timer = window.setInterval(() => {
      if (document.hidden) {
        return
      }
      commit(nextOf(season.value), { persist: false })
    }, SEASON_AUTOPLAY_MS)
  }

  function stopAutoPlay() {
    if (timer !== undefined) {
      window.clearInterval(timer)
      timer = undefined
    }
    autoPlaying.value = false
  }

  /** 首屏挂载前调用一次，避免闪烁 */
  function init() {
    apply(season.value, true)
  }

  return {
    season,
    meta,
    isDark,
    autoPlaying,
    setSeason,
    startAutoPlay,
    stopAutoPlay,
    init,
  }
})
