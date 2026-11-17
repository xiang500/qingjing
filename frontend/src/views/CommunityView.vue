<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { useCommunityStore } from '@/stores/community'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import PostCard from '@/components/community/PostCard.vue'
import PublishDialog from '@/components/community/PublishDialog.vue'
import CommentsDialog from '@/components/community/CommentsDialog.vue'
import type { CommunityPost } from '@/types/community'

const store = useCommunityStore()
const theme = useThemeStore()
const auth = useAuthStore()
const { posts, total, page, pages, loading, error, isEmpty } = storeToRefs(store)
const route = useRoute()
const router = useRouter()

const publishOpen = ref(false)
const prefill = ref<{ title?: string; content?: string; images?: string[] }>({})
const commentPost = ref<CommunityPost | null>(null)
const commentsOpen = ref(false)
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
  if (!auth.isLoggedIn) {
    ElMessage.info('请先在右上角登录，登录后再发布搭配')
    return
  }
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

function openComments(post: CommunityPost) {
  commentPost.value = post
  commentsOpen.value = true
}

function onCommentsChanged(post: CommunityPost, count: number) {
  post.commentCount = count
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
  <div class="page-stack">
    <!-- ============ 区块一：社区首屏 · 发布搭配 ============ -->
    <section v-reveal class="section-block">
      <header class="section-head">
        <div class="min-w-0">
          <span class="eyebrow">{{ theme.meta.en }} · {{ theme.meta.light }} · Community</span>
          <h1 class="display mt-4 text-[clamp(1.9rem,4.6vw,3.2rem)] leading-[1.15] text-mist-100">
            灵感社区，<br />
            <span class="accent-text">把好搭配穿给更多人看</span>
          </h1>
          <p class="mt-5 max-w-[36rem] text-[13px] leading-[1.95] text-mist-400 sm:text-sm">
            每一条分享都关联真实衣橱单品。看看别人在{{ theme.meta.light }}里怎么穿，把喜欢的套装一键还原到自己的搭配画板，或者把今天这一身发出来。
          </p>
        </div>

        <!-- 首屏大动作：亮底主按钮 -->
        <div class="action-row">
          <button type="button" class="btn-primary" @click="openPublish">
            <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14" stroke-linecap="round" />
            </svg>
            发布搭配
          </button>
        </div>
      </header>

      <!-- 次要信息收进下沉面板 -->
      <div class="surface-sunken flex flex-wrap items-center gap-x-4 gap-y-2 p-4">
        <span class="text-[10px] uppercase tracking-[0.24em] text-mist-500">本季关键词</span>
        <span v-for="tag in theme.meta.tags" :key="tag" class="chip cursor-default">{{ tag }}</span>
        <span class="ml-auto text-[11px] tracking-[0.08em] text-mist-500">
          {{ theme.meta.label }} · {{ theme.meta.name }} · {{ theme.meta.solarTerm }}
        </span>
      </div>
    </section>

    <!-- ============ 区块二：社区脉搏 + 发布入口（独立于信息流） ============ -->
    <section v-reveal class="section-block">
      <header class="section-head">
        <div class="min-w-0">
          <span class="eyebrow">Community Pulse</span>
          <h2>灵感正在流动</h2>
          <p>本季光场里累积的分享，与此刻正在被翻看的这一页。</p>
        </div>
        <div class="action-row">
          <button type="button" class="btn-ghost" @click="openPublish">也发一条</button>
        </div>
      </header>

      <div class="grid gap-4 sm:grid-cols-3">
        <div class="glass-soft p-4">
          <p class="text-[10px] uppercase tracking-[0.2em] text-mist-500">All Posts</p>
          <p class="display mt-2 text-2xl leading-none text-mist-100 tabular-nums">{{ total }}</p>
          <p class="mt-2 text-[11px] text-mist-400">全部分享</p>
        </div>
        <div class="glass-soft p-4">
          <p class="text-[10px] uppercase tracking-[0.2em] text-mist-500">This Page</p>
          <p class="display mt-2 text-2xl leading-none text-mist-100 tabular-nums">{{ posts.length }}</p>
          <p class="mt-2 text-[11px] text-mist-400">本页展示</p>
        </div>
        <div class="glass-soft p-4">
          <p class="text-[10px] uppercase tracking-[0.2em] text-mist-500">Season Field</p>
          <p class="display mt-2 text-lg leading-none text-gold-300">
            {{ theme.meta.label }} · {{ theme.meta.name }}
          </p>
          <p class="mt-2 text-[11px] text-mist-400">{{ theme.meta.light }} · {{ theme.meta.solarTerm }}</p>
        </div>
      </div>

      <p class="surface-sunken p-4 text-xs leading-relaxed text-mist-400">
        发布时可以关联衣橱里的套装，其他人点「试穿此搭」就能在自己的画板上还原同一套。
      </p>
    </section>

    <!-- ============ 区块三：信息流（帖子墙 / 分页） ============ -->
    <section v-reveal class="section-block">
      <header class="section-head">
        <div class="min-w-0">
          <span class="eyebrow">Community Feed</span>
          <h2>大家的搭配</h2>
          <p>点赞、评论，或点「试穿此搭」把整套还原到自己的画板。</p>
        </div>
        <div class="action-row">
          <span class="chip cursor-default tabular-nums">
            共 {{ total }} 条 · 第 {{ page }} / {{ pages || 1 }} 页
          </span>
        </div>
      </header>

      <!-- 错误提示 -->
      <div
        v-if="error"
        class="flex items-start gap-3 rounded-[var(--radius-md)] border border-gold-300/35 bg-gold-300/10 px-4 py-3"
      >
        <svg
          class="mt-1 size-4 shrink-0 text-gold-300"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.6"
        >
          <circle cx="12" cy="12" r="9" />
          <path d="M12 8v5M12 16h.01" stroke-linecap="round" />
        </svg>
        <div class="min-w-0 flex-1">
          <p class="text-sm font-medium text-mist-200">社区数据加载失败</p>
          <p class="mt-1 text-xs leading-relaxed text-mist-400">{{ error }}</p>
        </div>
        <button type="button" class="chip shrink-0 px-3 py-1 text-[11px]" @click="store.load(1)">
          重试
        </button>
      </div>

      <!-- 加载骨架 -->
      <div v-if="loading && !posts.length" class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        <div v-for="index in 3" :key="index" class="glass-soft p-2">
          <div class="skeleton aspect-[4/5] w-full rounded-[var(--radius-sm)]" />
          <div class="space-y-2 px-2 pb-2 pt-3">
            <div class="flex items-center gap-3">
              <div class="skeleton size-7 rounded-full" />
              <div class="skeleton h-2.5 w-24 rounded" />
            </div>
            <div class="skeleton h-4 w-3/4 rounded" />
            <div class="skeleton h-3 w-1/2 rounded" />
          </div>
        </div>
      </div>

      <!-- 帖子墙 -->
      <div v-else-if="posts.length" class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        <PostCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
          @like="store.like(post)"
          @remove="remove(post)"
          @comments="openComments(post)"
          @try-on="tryOn"
        />
      </div>

      <!-- 空状态 -->
      <div
        v-else-if="isEmpty"
        class="glass flex flex-col items-center gap-4 px-6 py-16 text-center"
      >
        <span class="grid size-14 place-items-center rounded-full border border-gold-300/25 bg-gold-300/10">
          <svg
            class="size-6 text-gold-300"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.4"
          >
            <rect x="3.5" y="5" width="17" height="14" rx="3" />
            <circle cx="9" cy="10.5" r="1.6" />
            <path d="m5.5 17.5 4.2-3.8 2.8 2.3 2.8-2.3 3.2 3.8" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </span>
        <h3 class="display text-xl text-mist-100">还没有人分享</h3>
        <p class="max-w-md text-sm leading-relaxed text-mist-400">
          成为第一个把搭配发出来的人，顺便让 AI 的推荐被更多人看到。
        </p>
        <button type="button" class="btn-primary mt-2" @click="openPublish">发布第一条</button>
      </div>

      <!-- 分页 -->
      <nav v-if="pages > 1" class="flex items-center justify-center gap-2">
        <button
          type="button"
          class="chip"
          :class="{ 'opacity-40': page <= 1 }"
          :disabled="page <= 1"
          @click="store.load(page - 1)"
        >
          上一页
        </button>
        <span class="px-2 text-[11px] text-mist-500 tabular-nums">{{ page }} / {{ pages }}</span>
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
    </section>

    <PublishDialog
      :open="publishOpen"
      :prefill="prefill"
      @close="publishOpen = false"
      @published="flash('已发布到灵感社区')"
    />

    <CommentsDialog
      :open="commentsOpen"
      :post="commentPost"
      @close="commentsOpen = false"
      @changed="(count) => commentPost && onCommentsChanged(commentPost, count)"
    />

    <Transition name="fade">
      <div
        v-if="notice"
        class="glass fixed bottom-6 left-1/2 z-50 -translate-x-1/2 rounded-[var(--radius-pill)] px-5 py-2 text-sm text-mist-100"
        role="status"
      >
        {{ notice }}
      </div>
    </Transition>
  </div>
</template>