<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { useCommunityStore } from '@/stores/community'
import PostCard from '@/components/community/PostCard.vue'
import PublishDialog from '@/components/community/PublishDialog.vue'
import type { CommunityPost } from '@/types/community'

const store = useCommunityStore()
const { posts, total, page, pages, loading, error, isEmpty } = storeToRefs(store)
const route = useRoute()
const router = useRouter()

const publishOpen = ref(false)
const prefill = ref<{ title?: string; content?: string; images?: string[] }>({})
const notice = ref('')
let noticeTimer: number | undefined

function flash(message: string) {
  notice.value = message
  window.clearTimeout(noticeTimer)
  noticeTimer = window.setTimeout(() => {
    notice.value = ''
  }, 2600)
}

function openPublish() {
  prefill.value = {}
  publishOpen.value = true
}

function tryOn(ids: number[]) {
  void router.push({ path: '/mirror', query: { ids: ids.join(',') } })
}

async function remove(post: CommunityPost) {
  await store.remove(post)
  flash('已删除这条分享')
}

function consumeQuery() {
  if (!route.query.publish) {
    return
  }
  prefill.value = {
    title: typeof route.query.title === 'string' ? route.query.title : '',
    content: typeof route.query.content === 'string' ? route.query.content : '',
    images: typeof route.query.images === 'string'
      ? route.query.images.split('|').filter(Boolean)
      : [],
  }
  publishOpen.value = true
  void router.replace({ path: '/community' })
}

watch(() => route.query.publish, consumeQuery)

onMounted(async () => {
  await store.load(1)
  consumeQuery()
})
</script>

<template>
  <div class="space-y-5">
    <section class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
      <div class="max-w-2xl">
        <p class="text-[11px] uppercase tracking-[0.4em] text-gold-400/80">Community</p>
        <h1 class="mt-2 font-display text-3xl leading-tight text-mist-100 sm:text-4xl">
          灵感社区，
          <span class="text-gold-200">把好搭配穿给更多人看</span>
        </h1>
        <p class="mt-3 text-sm leading-relaxed text-mist-500">
          每一条分享都关联真实衣橱单品，点「试穿此搭」就能在 3D 数字人身上还原。
        </p>
      </div>
      <div class="flex items-center gap-3">
        <span class="text-xs text-mist-500">共 {{ total }} 条分享</span>
        <button type="button" class="btn-primary" @click="openPublish">
          <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5v14M5 12h14" stroke-linecap="round" />
          </svg>
          分享穿搭
        </button>
      </div>
    </section>

    <div
      v-if="error"
      class="flex items-start gap-3 rounded-2xl border border-rose-400/25 bg-rose-400/8 px-4 py-3 text-sm text-rose-100"
    >
      <span class="flex-1">{{ error }}</span>
      <button type="button" class="text-xs underline-offset-4 hover:underline" @click="store.load(1)">重试</button>
    </div>

    <div v-if="loading && !posts.length" class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      <div v-for="index in 3" :key="index" class="glass-card overflow-hidden">
        <div class="skeleton aspect-[4/3] w-full" />
        <div class="space-y-2 p-4">
          <div class="skeleton h-3 w-1/3 rounded" />
          <div class="skeleton h-4 w-3/4 rounded" />
          <div class="skeleton h-3 w-1/2 rounded" />
        </div>
      </div>
    </div>

    <div v-else-if="posts.length" class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      <PostCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
        @like="store.like(post)"
        @remove="remove(post)"
        @try-on="tryOn"
      />
    </div>

    <div v-else-if="isEmpty" class="glass-card flex flex-col items-center gap-3 px-6 py-16 text-center">
      <span class="grid size-14 place-items-center rounded-full border border-gold-400/25 bg-gold-400/8">
        <svg class="size-6 text-gold-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4">
          <path d="M4 7h16v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7Z" stroke-linejoin="round" />
          <path d="M8 7V5h8v2" stroke-linecap="round" />
        </svg>
      </span>
      <h2 class="font-display text-xl text-mist-100">还没有人分享</h2>
      <p class="max-w-md text-sm text-mist-500">成为第一个把搭配发出来的人，顺便让 AI 的推荐被更多人看到。</p>
      <button type="button" class="btn-primary mt-1" @click="openPublish">发布第一条</button>
    </div>

    <nav v-if="pages > 1" class="flex items-center justify-center gap-2 pt-2">
      <button
        type="button"
        class="chip"
        :class="{ 'opacity-40': page <= 1 }"
        :disabled="page <= 1"
        @click="store.load(page - 1)"
      >
        上一页
      </button>
      <span class="px-2 text-xs text-mist-500">{{ page }} / {{ pages }}</span>
      <button
        type="button"
        class="chip"
        :class="{ 'opacity-40': page >= pages }"
        :disabled="page >= pages"
        @click="store.load(page + 1)"
      >
        下一页
      </button>
    </nav>

    <PublishDialog
      :open="publishOpen"
      :prefill="prefill"
      @close="publishOpen = false"
      @published="flash('已发布到灵感社区')"
    />

    <Transition name="fade">
      <div
        v-if="notice"
        class="fixed bottom-6 left-1/2 z-50 -translate-x-1/2 rounded-full border border-gold-400/30 bg-ink-900/95 px-5 py-2.5 text-sm text-gold-100 shadow-xl backdrop-blur"
      >
        {{ notice }}
      </div>
    </Transition>
  </div>
</template>