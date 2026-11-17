import type { Directive, DirectiveBinding } from 'vue'

/**
 * 滚动显现指令（Scroll Reveal）
 * ------------------------------------------------------------
 * 用法：
 *   <section v-reveal>...</section>                     默认：进入视口时淡入上移
 *   <section v-reveal="{ delay: 120 }">...</section>    延迟 120ms
 *   <section v-reveal="{ y: 32, once: false }">...</section>  位移更大，且离开视口后复位（再进入会重新显现）
 *   <section v-reveal="180">...</section>               只给数字 = delay
 *
 * 设计取舍：
 * - 用原生 IntersectionObserver，不引第三方动画库（scrollreveal 会额外增加依赖）
 * - 首屏已可见的元素不做隐藏，避免"闪一下"再出现
 * - `prefers-reduced-motion: reduce` 时完全跳过动画
 */

interface RevealOptions {
  /** 进入前的纵向位移（px） */
  y: number
  /** 延迟（ms） */
  delay: number
  /** 只显现一次（默认 true）；false 时离开视口会复位 */
  once: boolean
  /** 触发阈值 0~1 */
  threshold: number
}

const CLASS_BASE = 'reveal'
const CLASS_IN = 'reveal-in'
const CLASS_OUT = 'reveal-out'

const reduceMotion =
  typeof window !== 'undefined' &&
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

let observer: IntersectionObserver | null = null
const optionsMap = new WeakMap<Element, RevealOptions>()

function normalize(value: DirectiveBinding['value']): RevealOptions {
  if (typeof value === 'number') {
    return { y: 16, delay: value, once: true, threshold: 0.12 }
  }
  const config = (value ?? {}) as Partial<RevealOptions>
  return {
    y: config.y ?? 16,
    delay: config.delay ?? 0,
    once: config.once ?? true,
    threshold: config.threshold ?? 0.12,
  }
}

function ensureObserver(): IntersectionObserver {
  if (observer) {
    return observer
  }
  observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        const element = entry.target as HTMLElement
        const options = optionsMap.get(element) ?? { y: 16, delay: 0, once: true, threshold: 0.12 }
        if (entry.isIntersecting) {
          element.classList.remove(CLASS_OUT)
          element.classList.add(CLASS_IN)
          if (options.once) {
            observer?.unobserve(element)
          }
        } else if (!options.once) {
          element.classList.remove(CLASS_IN)
          element.classList.add(CLASS_OUT)
        }
      }
    },
    { threshold: 0.12, rootMargin: '0px 0px -8% 0px' },
  )
  return observer
}

export const vReveal: Directive<HTMLElement, number | Partial<RevealOptions>> = {
  mounted(element, binding) {
    if (reduceMotion) {
      element.classList.add(CLASS_IN)
      return
    }
    const options = normalize(binding.value)
    optionsMap.set(element, options)

    // 首屏已经可见的元素直接显示，避免加载时"闪一下"
    const rect = element.getBoundingClientRect()
    const belowFold = rect.top > window.innerHeight * 0.92
    if (!belowFold) {
      element.classList.add(CLASS_IN)
      return
    }

    element.classList.add(CLASS_BASE)
    if (options.delay) {
      element.style.transitionDelay = `${options.delay}ms`
    }
    if (options.y) {
      element.style.setProperty('--reveal-y', `${options.y}px`)
    }
    ensureObserver().observe(element)
  },

  unmounted(element) {
    observer?.unobserve(element)
    optionsMap.delete(element)
  },
}

export default vReveal