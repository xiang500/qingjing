<script setup lang="ts">
const plans = [
  {
    title: '穿搭分享',
    en: 'Share Outfit',
    desc: '把 AI 生成的套装一键发布，附带场景、天气与搭配理由，自动生成分享卡片。',
  },
  {
    title: '点赞与收藏',
    en: 'Reactions',
    desc: '帖子点赞、收藏到灵感夹，热度数据反哺推荐权重——越用越懂你。',
  },
  {
    title: '同款试穿',
    en: 'Try the Look',
    desc: '看到喜欢的搭配，直接在自己的 3D 数字人身上换装，减少冲动下单与退货。',
  },
]
</script>

<template>
  <div class="space-y-6">
    <section class="max-w-2xl">
      <p class="text-[11px] uppercase tracking-[0.4em] text-gold-400/80">Community</p>
      <h1 class="mt-2 font-display text-3xl leading-tight text-mist-100 sm:text-4xl">
        灵感社区，
        <span class="text-gold-200">把好搭配分享出去</span>
      </h1>
      <p class="mt-3 text-sm leading-relaxed text-mist-500">
        社区表结构（post / post_like / comment）已在第一阶段建好，
        接口与页面将在阶段四随试衣镜一起上线。
      </p>
    </section>

    <section class="grid gap-4 md:grid-cols-3">
      <article v-for="plan in plans" :key="plan.title" class="glass-card flex flex-col gap-2 p-5">
        <span class="text-[10px] uppercase tracking-[0.28em] text-mist-500">{{ plan.en }}</span>
        <h2 class="font-display text-lg text-mist-100">{{ plan.title }}</h2>
        <p class="text-sm leading-relaxed text-mist-500">{{ plan.desc }}</p>
      </article>
    </section>

    <section class="glass-card flex flex-col items-center gap-3 px-6 py-14 text-center">
      <span class="grid size-14 place-items-center rounded-full border border-white/10 bg-white/[0.03]">
        <svg class="size-6 text-mist-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4">
          <path d="M8 11a3 3 0 1 0 3-3 3 3 0 0 0-3 3Zm5.5 7v-1.2A4.8 4.8 0 0 0 8.7 12H7.3A4.8 4.8 0 0 0 2.5 16.8V18" stroke-linecap="round" />
          <path d="M16 11h6M19 8v6" stroke-linecap="round" />
        </svg>
      </span>
      <h2 class="font-display text-xl text-mist-100">社区正在准备中</h2>
      <p class="max-w-md text-sm text-mist-500">
        先完善衣橱与 AI 推荐，让分享出去的每一套搭配都值得被收藏。
      </p>
      <RouterLink to="/" class="btn-primary mt-2">回到数字衣橱</RouterLink>
    </section>
  </div>
</template>