<script setup lang="ts">
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

/**
 * 季节光场背景
 * ------------------------------------------------------------
 * 纯 CSS 实现，零图片、零依赖：
 *   - 三层缓慢漂移的色晕（季节主色）→ 玻璃卡片透过它产生"折射感"
 *   - 每个季节一组动机粒子：春·花瓣 / 夏·光尘 / 秋·落叶 / 冬·雪
 *   - 最后叠一层极淡的胶片颗粒，避免大面积渐变出现色带
 * 所有元素 pointer-events-none / aria-hidden，只做氛围。
 */

const theme = useThemeStore()
const meta = computed(() => theme.meta)

interface Particle {
  left: number
  size: number
  duration: number
  delay: number
  drift: number
  rotate: number
  opacity: number
}

/** 用固定种子生成，避免每次刷新粒子位置乱跳 */
function seeded(seed: number): () => number {
  let value = seed
  return () => {
    value = (value * 9301 + 49297) % 233280
    return value / 233280
  }
}

const rand = seeded(20261116)

const particles = computed<Particle[]>(() => {
  const count = meta.value.key === 'summer' ? 14 : 12
  return Array.from({ length: count }, () => ({
    left: Math.round(rand() * 1000) / 10,
    size: 0.55 + rand() * 0.9,
    duration: 16 + rand() * 18,
    delay: -Math.round(rand() * 30),
    drift: (rand() - 0.5) * 12,
    rotate: Math.round(rand() * 540),
    opacity: 0.25 + rand() * 0.45,
  }))
})
</script>

<template>
  <div class="season-sky" aria-hidden="true">
    <div class="sky-base" />
    <span class="blob blob-1" />
    <span class="blob blob-2" />
    <span class="blob blob-3" />

    <!-- 夏 · 正午：顶光光锥 -->
    <div v-if="meta.key === 'summer'" class="sun-rays" />

    <!-- 季节粒子：花瓣 / 光尘 / 落叶 / 雪 -->
    <div class="particles" :class="`particles-${meta.key}`">
      <span
        v-for="(particle, index) in particles"
        :key="index"
        class="particle"
        :style="{
          left: `${particle.left}%`,
          '--size': `${particle.size}`,
          '--duration': `${particle.duration}s`,
          '--delay': `${particle.delay}s`,
          '--drift': `${particle.drift}rem`,
          '--rot': `${particle.rotate}deg`,
          '--opacity': particle.opacity,
        }"
      />
    </div>

    <div class="grain" />
    <div class="vignette" />
  </div>
</template>

<style scoped>
.season-sky {
  position: fixed;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  pointer-events: none;
  background: var(--c-bg);
}

.sky-base {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(
      56rem 42rem at 4% -8%,
      color-mix(in oklab, var(--c-wash-3) 62%, transparent),
      transparent 62%
    ),
    radial-gradient(
      48rem 40rem at 98% -4%,
      color-mix(in oklab, var(--c-wash-1) 66%, transparent),
      transparent 64%
    ),
    radial-gradient(
      64rem 46rem at 50% 112%,
      color-mix(in oklab, var(--c-wash-2) 44%, transparent),
      transparent 68%
    );
}

/* 深色季节：色晕压暗一档，让玻璃上的文字更清楚 */
html[data-scheme='dark'] .sky-base {
  opacity: 0.85;
}

html[data-scheme='light'] .sky-base {
  opacity: 0.9;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(72px);
  opacity: 0.34;
  will-change: transform;
}

html[data-scheme='light'] .blob {
  opacity: 0.5;
}

.blob-1 {
  width: 40rem;
  height: 40rem;
  left: -10rem;
  top: 2rem;
  background: var(--c-wash-1);
  animation: drift-a 48s ease-in-out infinite alternate;
}

.blob-2 {
  width: 34rem;
  height: 34rem;
  right: -8rem;
  top: -8rem;
  background: var(--c-wash-2);
  animation: drift-b 56s ease-in-out infinite alternate;
}

.blob-3 {
  width: 38rem;
  height: 38rem;
  left: 42%;
  bottom: -18rem;
  background: var(--c-wash-3);
  opacity: 0.22;
  animation: drift-a 66s ease-in-out infinite alternate-reverse;
}

@keyframes drift-a {
  to {
    transform: translate3d(6rem, -3rem, 0) scale(1.12);
  }
}

@keyframes drift-b {
  to {
    transform: translate3d(-5rem, 4rem, 0) scale(1.08);
  }
}

/* ---------- 夏季光锥 ---------- */
.sun-rays {
  position: absolute;
  inset: -30% -10% auto;
  height: 90%;
  background: conic-gradient(
    from 200deg at 70% 0%,
    transparent 0deg,
    color-mix(in oklab, var(--c-wash-2) 30%, transparent) 8deg,
    transparent 18deg,
    transparent 30deg,
    color-mix(in oklab, var(--c-wash-1) 24%, transparent) 38deg,
    transparent 52deg
  );
  filter: blur(18px);
  opacity: 0.7;
  animation: rays 40s linear infinite alternate;
}

@keyframes rays {
  to {
    transform: rotate(6deg) translateX(-2%);
  }
}

/* ---------- 粒子 ---------- */
.particles {
  position: absolute;
  inset: 0;
}

.particle {
  position: absolute;
  top: -8vh;
  width: calc(var(--size) * 14px);
  height: calc(var(--size) * 8px);
  opacity: var(--opacity);
  border-radius: 60% 10% 60% 10%;
  animation: fall var(--duration) linear infinite;
  animation-delay: var(--delay);
  will-change: transform;
}

/* 春 · 花瓣：粉白、更圆、慢慢飘 */
.particles-spring .particle {
  background: linear-gradient(
    120deg,
    color-mix(in oklab, #f3cfd0 70%, white),
    color-mix(in oklab, #e0b7bd 70%, transparent)
  );
  border-radius: 65% 35% 60% 40%;
  filter: blur(0.2px);
}

/* 夏 · 光尘：小圆点，向上漂浮 */
.particles-summer .particle {
  width: calc(var(--size) * 5px);
  height: calc(var(--size) * 5px);
  border-radius: 50%;
  background: color-mix(in oklab, #fff7df 75%, transparent);
  box-shadow: 0 0 8px rgba(255, 244, 210, 0.5);
  animation-name: float-up;
}

/* 秋 · 落叶：焦糖斜片 */
.particles-autumn .particle {
  background: linear-gradient(
    120deg,
    color-mix(in oklab, #d69a63 80%, transparent),
    color-mix(in oklab, #a7623c 70%, transparent)
  );
  border-radius: 70% 20% 70% 10%;
}

/* 冬 · 雪：小白点，落得慢 */
.particles-winter .particle {
  width: calc(var(--size) * 4px);
  height: calc(var(--size) * 4px);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 0 10px rgba(220, 238, 255, 0.6);
  animation-name: snowfall;
}

@keyframes fall {
  0% {
    transform: translate3d(0, 0, 0) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: var(--opacity);
  }
  85% {
    opacity: var(--opacity);
  }
  100% {
    transform: translate3d(var(--drift), 112vh, 0) rotate(var(--rot));
    opacity: 0;
  }
}

@keyframes snowfall {
  0% {
    transform: translate3d(0, 0, 0);
    opacity: 0;
  }
  8% {
    opacity: var(--opacity);
  }
  100% {
    transform: translate3d(var(--drift), 108vh, 0);
    opacity: 0;
  }
}

@keyframes float-up {
  0% {
    transform: translate3d(0, 20vh, 0) scale(0.6);
    opacity: 0;
  }
  15% {
    opacity: var(--opacity);
  }
  100% {
    transform: translate3d(var(--drift), -90vh, 0) scale(1.1);
    opacity: 0;
  }
}

/* ---------- 颗粒 & 暗角 ---------- */
.grain {
  position: absolute;
  inset: 0;
  opacity: var(--c-grain);
  mix-blend-mode: overlay;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='160' height='160'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='.82' numOctaves='3' stitchTiles='stitch'/><feColorMatrix values='0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 .55 0'/></filter><rect width='160' height='160' filter='url(%23n)'/></svg>");
  background-size: 160px 160px;
}

.vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    120% 90% at 50% 0%,
    transparent 45%,
    color-mix(in oklab, var(--c-bg) 55%, transparent) 100%
  );
  opacity: 0.6;
}

html[data-scheme='light'] .vignette {
  opacity: 0.35;
}
</style>
