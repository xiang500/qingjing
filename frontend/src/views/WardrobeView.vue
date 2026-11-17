<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { RouterLink } from 'vue-router'
import { useWardrobeStore } from '@/stores/wardrobe'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import StatOverview from '@/components/StatOverview.vue'
import FilterBar from '@/components/FilterBar.vue'
import ClothingCard from '@/components/ClothingCard.vue'
import SkeletonGrid from '@/components/SkeletonGrid.vue'
import EmptyState from '@/components/EmptyState.vue'
import BaseModal from '@/components/BaseModal.vue'
import ClothingFormModal from '@/components/ClothingFormModal.vue'
import RotatingGallery from '@/components/RotatingGallery.vue'
import type { CategoryCode, Clothing, ClothingPayload } from '@/types/clothing'
import type { GalleryItem } from '@/types/gallery'
import { catalogImage } from '@/utils/image'

const store = useWardrobeStore()
const theme = useThemeStore()
const auth = useAuthStore()

/**
 * 未登录时不做写操作：后端会用缺省演示账号兜底，
 * 放任写入会把数据记到别人账号上，也容易在切换账号后串数据。
 */
function guardLogin(): boolean {
  if (auth.isLoggedIn) {
    return false
  }
  ElMessage.info('请先在右上角登录，登录后再管理衣橱')
  return true
}
const { items, stats, categories, outfits, total, pages, loading, summaryLoading, error, filters, hasActiveFilter, isEmpty } =
  storeToRefs(store)

const formOpen = ref(false)
const editingItem = ref<Clothing | null>(null)
const submitting = ref(false)
const pendingDelete = ref<Clothing | null>(null)
const deleting = ref(false)
const notice = ref('')
let noticeTimer: number | undefined

const keywordModel = ref(filters.value.keyword)
let searchTimer: number | undefined

const heroOutfit = computed(() => outfits.value[0] ?? null)

/** 当前季节在衣橱里的单品数（按后端返回的 season 编码统计） */
const seasonCount = computed(() => {
  const code = theme.meta.key.toUpperCase()
  return items.value.filter((item) => item.season.includes(code)).length
})

/** 今日推荐套装里的单品缩略图，最多取 3 件 */
const outfitThumbs = computed(() => {
  if (!heroOutfit.value) {
    return []
  }
  return heroOutfit.value.itemIds
    .map((id) => items.value.find((item) => item.id === id))
    .filter((item): item is Clothing => Boolean(item?.imageUrl))
    .slice(0, 3)
    .map((item) => catalogImage(item.imageUrl))
})

const yearLabel = computed(() => new Date().getFullYear())

/** 旋转画廊卡片：优先用衣橱真实单品；没有数据时组件内部会退化为演示卡片 */
const galleryItems = computed<GalleryItem[]>(() =>
  items.value
    .filter((item) => item.imageUrl)
    .map((item) => ({
      id: item.id,
      name: item.name,
      imageUrl: item.imageUrl,
      categoryLabel: item.categoryLabel,
      colorHex: item.colorHex,
      meta: [item.brand, item.subCategory ?? item.material].filter(Boolean).join(' · '),
      wearCount: item.wearCount,
    })),
)

const pageNumbers = computed(() => {
  const totalPages = Math.max(1, pages.value)
  const current = filters.value.page
  const start = Math.max(1, Math.min(current - 1, totalPages - 2))
  const end = Math.min(totalPages, start + 2)
  return Array.from({ length: end - start + 1 }, (_, index) => start + index)
})

function flash(message: string) {
  notice.value = message
  window.clearTimeout(noticeTimer)
  noticeTimer = window.setTimeout(() => {
    notice.value = ''
  }, 2600)
}

watch(keywordModel, (value) => {
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    void store.setKeyword(value)
  }, 320)
})

watch(
  () => filters.value.keyword,
  (value) => {
    if (value !== keywordModel.value) {
      keywordModel.value = value
    }
  },
)

function openCreate() {
  if (guardLogin()) {
    return
  }
  editingItem.value = null
  formOpen.value = true
}

function openEdit(item: Clothing) {
  if (guardLogin()) {
    return
  }
  editingItem.value = item
  formOpen.value = true
}

async function handleSubmit(payload: ClothingPayload) {
  if (guardLogin()) {
    return
  }
  submitting.value = true
  try {
    await store.save(payload, editingItem.value?.id)
    formOpen.value = false
    flash(editingItem.value ? '单品信息已更新' : '新单品已收入衣橱')
  } catch (err) {
    flash(err instanceof Error ? err.message : '保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

async function confirmDelete() {
  if (!pendingDelete.value || guardLogin()) {
    return
  }
  deleting.value = true
  try {
    await store.remove(pendingDelete.value.id)
    pendingDelete.value = null
    flash('单品已从衣橱移除')
  } catch (err) {
    flash(err instanceof Error ? err.message : '删除失败')
  } finally {
    deleting.value = false
  }
}

async function handleWear(item: Clothing) {
  if (guardLogin()) {
    return
  }
  await store.markWorn(item)
  flash(`已记录「${item.name}」的穿着`)
}

async function handleFavorite(item: Clothing) {
  if (guardLogin()) {
    return
  }
  await store.toggleFavorite(item)
}

function resetFilters() {
  keywordModel.value = ''
  filters.value.favorite = false
  void store.setCategory('')
}

function changeCategory(code: CategoryCode | '') {
  void store.setCategory(code)
}

function changeSort(value: 'latest' | 'wearCount' | 'priceAsc' | 'priceDesc') {
  void store.setSort(value)
}

function changeFavorite(value: boolean) {
  void store.setFavoriteOnly(value)
}

onMounted(() => {
  void store.init()
})

onBeforeUnmount(() => {
  window.clearTimeout(searchTimer)
  window.clearTimeout(noticeTimer)
})
</script>

<template>
  <div class="page-stack">
    <!-- ============ 区块一：季节宣言 · 首屏大动作 ============ -->
    <section v-reveal class="section-block">
      <header class="section-head">
        <div class="min-w-0">
          <span class="eyebrow">{{ theme.meta.en }} · {{ theme.meta.light }} · {{ theme.meta.solarTerm }}</span>
          <h1 class="display mt-4 text-[clamp(2.15rem,4.4vw,3.6rem)] leading-[1.14] text-mist-100">
            {{ theme.meta.headline[0] }}<br />
            <span class="accent-text">{{ theme.meta.headline[1] }}</span>
          </h1>
          <p class="mt-5 max-w-[36rem] text-[13px] leading-[1.95] text-mist-400 sm:text-sm">
            {{ theme.meta.lede }}
          </p>
        </div>

        <!-- 首屏大动作：亮底主按钮在前，次动作统一压暗 -->
        <div class="action-row">
          <button type="button" class="btn-primary" @click="openCreate">
            <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14" stroke-linecap="round" />
            </svg>
            录入单品
          </button>
          <RouterLink
            :to="{ path: '/assistant', query: { q: '帮我按现在的季节搭一套今天能穿的' } }"
            class="btn-ghost"
          >
            让 AI 帮我搭一套
            <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="m9 6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </RouterLink>
        </div>
      </header>

      <div class="grid gap-4 lg:grid-cols-[minmax(0,1fr)_22rem] lg:items-stretch lg:gap-5">
        <!-- 今日推荐：信息性内容压进下沉面板，与右侧玻璃色卡形成明暗对比 -->
        <div class="surface-sunken order-2 flex flex-col justify-between gap-5 p-5 sm:p-6 lg:order-1">
          <div class="min-w-0">
            <p class="text-[10px] uppercase tracking-[0.3em] text-mist-500">Today's Pick</p>
            <template v-if="heroOutfit">
              <h2 class="display mt-2 text-xl text-mist-100">{{ heroOutfit.name }}</h2>
              <p class="mt-2 max-w-[42rem] text-xs leading-[1.9] text-mist-400">{{ heroOutfit.aiReason }}</p>
            </template>
            <p v-else class="mt-2 text-xs leading-relaxed text-mist-500">
              先录入几件单品，轻境会在这里给出今天的整套建议。
            </p>
          </div>

          <div v-if="heroOutfit" class="flex flex-wrap items-center gap-3">
            <div class="flex gap-2">
              <span
                v-for="(thumb, index) in outfitThumbs"
                :key="index"
                class="photo-tile block size-16 shrink-0 rounded-[var(--radius-sm)]"
              >
                <img :src="thumb" :alt="heroOutfit.itemNames[index] ?? ''" class="size-full object-contain p-2" />
              </span>
              <RouterLink
                :to="{ path: '/assistant', query: { q: `参考「${heroOutfit.name}」，再帮我搭一套今天能穿的` } }"
                class="grid size-16 shrink-0 place-items-center rounded-[var(--radius-sm)] border border-dashed border-ink-700/70 text-mist-500 transition-colors hover:border-gold-300/60 hover:text-gold-200"
                aria-label="让 AI 再搭一套"
              >
                <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path d="M12 5v14M5 12h14" stroke-linecap="round" />
                </svg>
              </RouterLink>
            </div>
            <div class="action-row ml-auto">
              <RouterLink
                :to="{ path: '/mirror', query: { ids: heroOutfit.itemIds.join(',') } }"
                class="btn-ghost"
              >
                试穿这套
              </RouterLink>
            </div>
          </div>
        </div>

        <!-- 季节色卡：本季配色 + 关键词 -->
        <aside class="glass order-1 flex flex-col gap-4 p-5 sm:p-6 lg:order-2">
          <div class="flex items-start justify-between gap-3">
            <div>
              <p class="text-[10px] uppercase tracking-[0.32em] text-mist-500">Season Palette</p>
              <h2 class="display mt-2 text-xl text-mist-100">
                {{ theme.meta.label }} · {{ theme.meta.name }}
                <span class="text-[11px] tracking-[0.18em] text-mist-500">{{ yearLabel }}</span>
              </h2>
            </div>
            <span class="season-pill">{{ seasonCount }} 件本季单品</span>
          </div>

          <div class="grid grid-cols-4 gap-2">
            <div v-for="swatch in theme.meta.swatches" :key="swatch.name">
              <span
                class="block h-11 rounded-[var(--radius-xs)] ring-1 ring-inset ring-black/10"
                :style="{ background: swatch.hex }"
              />
              <p class="mt-2 text-[10px] tracking-[0.06em] text-mist-500">{{ swatch.name }}</p>
            </div>
          </div>

          <div class="mt-auto flex flex-wrap gap-2 border-t border-ink-700/50 pt-4">
            <span v-for="tag in theme.meta.tags" :key="tag" class="chip cursor-default">{{ tag }}</span>
          </div>
        </aside>
      </div>
    </section>

    <!-- ============ 区块二：衣橱一览（画廊 / 数据 / AI 推荐） ============ -->
    <section v-reveal class="section-block">
      <header class="section-head">
        <div class="min-w-0">
          <span class="eyebrow">Wardrobe Overview</span>
          <h2>衣橱一览</h2>
          <p>旋转画廊看陈列，数据概览看结构，AI 推荐看今天穿什么。</p>
        </div>
        <div class="action-row">
          <button
            type="button"
            class="btn-ghost"
            :disabled="loading"
            aria-label="刷新衣橱"
            title="刷新衣橱"
            @click="store.refresh()"
          >
            <svg
              class="size-4"
              :class="{ 'animate-spin': loading }"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
            >
              <path d="M20 12a8 8 0 1 1-2.3-5.6" stroke-linecap="round" />
              <path d="M20 4v4h-4" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            刷新衣橱
          </button>
        </div>
      </header>

      <!-- 同级三块依次错开，形成节奏 -->
      <div class="staggered">
        <RotatingGallery :items="galleryItems" title="本季衣橱" label="Wardrobe Gallery" />

        <StatOverview :stats="stats" :categories="categories" :loading="summaryLoading" />

        <section v-if="heroOutfit" class="glass relative overflow-hidden p-5 sm:p-6">
          <span
            class="pointer-events-none absolute inset-y-0 left-0 w-px bg-gradient-to-b from-transparent via-gold-300/70 to-transparent"
          />
          <header class="section-head">
            <div class="min-w-0">
              <span class="eyebrow">AI Outfit Pick</span>
              <h2>{{ heroOutfit.name }}</h2>
              <p class="line-clamp-2">{{ heroOutfit.aiReason }}</p>
            </div>
            <div class="action-row">
              <RouterLink
                :to="{ path: '/mirror', query: { ids: heroOutfit.itemIds.join(',') } }"
                class="btn-primary"
              >
                在工作台试穿
              </RouterLink>
              <RouterLink
                :to="{ path: '/assistant', query: { q: `参考「${heroOutfit.name}」，再帮我搭一套今天能穿的通勤搭配` } }"
                class="btn-ghost"
              >
                让 AI 再搭一套
                <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="m9 6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </RouterLink>
            </div>
          </header>
          <div class="flex flex-wrap items-center gap-2">
            <span v-if="heroOutfit.score != null" class="chip cursor-default text-gold-200">
              匹配度 {{ heroOutfit.score }}
            </span>
            <span v-for="name in heroOutfit.itemNames" :key="name" class="chip cursor-default">
              {{ name }}
            </span>
          </div>
        </section>
      </div>
    </section>

    <!-- ============ 区块三：全部单品（筛选 / 网格 / 分页） ============ -->
    <section v-reveal class="section-block">
      <header class="section-head">
        <div class="min-w-0">
          <span class="eyebrow">The Collection</span>
          <h2>全部单品</h2>
          <p>按品类、穿着频率与价格筛选；点开任意一件，可以记录穿着、收藏或修改信息。</p>
        </div>
      </header>

      <FilterBar
        :categories="categories"
        :category="filters.category"
        :keyword="keywordModel"
        :favorite="filters.favorite"
        :sort="filters.sort"
        :total="total"
        :has-active-filter="hasActiveFilter"
        @update:category="changeCategory"
        @update:keyword="(value) => (keywordModel = value)"
        @update:favorite="changeFavorite"
        @update:sort="changeSort"
        @reset="resetFilters"
      />

      <div
        v-if="error"
        class="flex items-start gap-3 rounded-[var(--radius-md)] border border-rose-400/30 bg-rose-400/10 px-4 py-3 text-sm text-rose-200"
      >
        <svg class="mt-1 size-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 8v5M12 16h.01" stroke-linecap="round" />
        </svg>
        <div class="flex-1">
          <p class="font-medium">衣橱数据加载失败</p>
          <p class="mt-1 text-xs text-rose-200/80">{{ error }}（请确认 Spring Boot 已在 8080 端口启动）</p>
        </div>
        <button type="button" class="text-xs text-rose-100 underline-offset-4 hover:underline" @click="store.loadList()">
          重试
        </button>
      </div>

      <SkeletonGrid v-if="loading && items.length === 0" />

      <TransitionGroup
        v-else-if="items.length"
        tag="div"
        name="fade"
        class="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-4"
      >
        <ClothingCard
          v-for="item in items"
          :key="item.id"
          :item="item"
          @favorite="handleFavorite(item)"
          @wear="handleWear(item)"
          @edit="openEdit(item)"
          @remove="pendingDelete = item"
        />
      </TransitionGroup>

      <EmptyState
        v-else-if="isEmpty"
        :title="hasActiveFilter ? '没有匹配的单品' : '衣橱还是空的'"
        :description="
          hasActiveFilter
            ? '试试更换分类、清空关键词，或取消「仅看收藏」。'
            : '录入第一件单品，轻境就能开始为你计算穿搭组合与衣橱利用率。'
        "
        :action-label="hasActiveFilter ? '清空筛选' : '录入第一件单品'"
        @action="hasActiveFilter ? resetFilters() : openCreate()"
      />

      <nav v-if="pages > 1" class="flex items-center justify-center gap-2">
        <button
          type="button"
          class="chip"
          :disabled="filters.page <= 1"
          :class="{ 'opacity-40': filters.page <= 1 }"
          @click="store.goToPage(filters.page - 1)"
        >
          上一页
        </button>
        <button
          v-for="page in pageNumbers"
          :key="page"
          type="button"
          class="chip min-w-9 justify-center"
          :class="{ 'chip-active': page === filters.page }"
          @click="store.goToPage(page)"
        >
          {{ page }}
        </button>
        <button
          type="button"
          class="chip"
          :disabled="filters.page >= pages"
          :class="{ 'opacity-40': filters.page >= pages }"
          @click="store.goToPage(filters.page + 1)"
        >
          下一页
        </button>
      </nav>
    </section>

    <ClothingFormModal
      :open="formOpen"
      :item="editingItem"
      :submitting="submitting"
      @close="formOpen = false"
      @submit="handleSubmit"
    />

    <BaseModal
      :open="pendingDelete !== null"
      title="移除单品"
      description="删除后该单品不再参与 AI 推荐，历史统计也会同步更新。"
      width="sm"
      @close="pendingDelete = null"
    >
      <p class="text-sm text-mist-300">
        确认要将
        <span class="text-mist-100">「{{ pendingDelete?.name }}」</span>
        从衣橱中移除吗？
      </p>
      <template #footer>
        <div class="flex justify-end gap-2">
          <button type="button" class="btn-ghost" @click="pendingDelete = null">再想想</button>
          <button type="button" class="btn-primary" :disabled="deleting" @click="confirmDelete">
            {{ deleting ? '移除中…' : '确认移除' }}
          </button>
        </div>
      </template>
    </BaseModal>

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

<style scoped>
.season-pill {
  flex: none;
  border-radius: var(--radius-pill);
  border: 1px solid color-mix(in oklab, var(--c-accent) 32%, transparent);
  background: color-mix(in oklab, var(--c-accent) 12%, transparent);
  padding: var(--space-1) var(--space-3);
  font-size: 0.7rem;
  letter-spacing: 0.04em;
  color: var(--c-accent-strong);
  white-space: nowrap;
}
</style>