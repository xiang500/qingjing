<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useWardrobeStore } from '@/stores/wardrobe'
import StatOverview from '@/components/StatOverview.vue'
import FilterBar from '@/components/FilterBar.vue'
import ClothingCard from '@/components/ClothingCard.vue'
import SkeletonGrid from '@/components/SkeletonGrid.vue'
import EmptyState from '@/components/EmptyState.vue'
import BaseModal from '@/components/BaseModal.vue'
import ClothingFormModal from '@/components/ClothingFormModal.vue'
import type { CategoryCode, Clothing, ClothingPayload } from '@/types/clothing'

const store = useWardrobeStore()
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
  editingItem.value = null
  formOpen.value = true
}

function openEdit(item: Clothing) {
  editingItem.value = item
  formOpen.value = true
}

async function handleSubmit(payload: ClothingPayload) {
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
  if (!pendingDelete.value) {
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
  await store.markWorn(item)
  flash(`已记录「${item.name}」的穿着`)
}

async function handleFavorite(item: Clothing) {
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
  <div class="space-y-6">
    <section class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
      <div class="max-w-2xl">
        <p class="text-[11px] uppercase tracking-[0.4em] text-gold-400/80">Wardrobe Overview</p>
        <h1 class="mt-2 font-display text-3xl leading-tight text-mist-100 sm:text-4xl">
          把衣橱搬进浏览器，
          <span class="text-gold-200">让每一件都被看见</span>
        </h1>
        <p class="mt-3 text-sm leading-relaxed text-mist-500">
          记录单品、统计穿着、沉淀风格标签。这些数据会成为下一阶段 AI 穿搭建议与 3D 换装的输入。
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <button type="button" class="btn-ghost" :disabled="loading" @click="store.refresh()">
          <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
            <path d="M20 12a8 8 0 1 1-2.3-5.6" stroke-linecap="round" />
            <path d="M20 4v4h-4" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          刷新
        </button>
        <button type="button" class="btn-primary" @click="openCreate">
          <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5v14M5 12h14" stroke-linecap="round" />
          </svg>
          录入单品
        </button>
      </div>
    </section>

    <StatOverview :stats="stats" :categories="categories" :loading="summaryLoading" />

    <section
      v-if="heroOutfit"
      class="glass-card relative overflow-hidden p-5 sm:p-6"
    >
      <span class="pointer-events-none absolute -right-10 -top-16 size-48 rounded-full bg-gold-400/10 blur-3xl" />
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div class="min-w-0 flex-1">
          <div class="flex items-center gap-2 text-[11px] uppercase tracking-[0.3em] text-gold-300">
            <span class="inline-flex size-1.5 rounded-full bg-gold-300" />
            AI 套装推荐
            <span v-if="heroOutfit.score != null" class="rounded-full border border-gold-400/30 px-2 py-0.5 text-[10px] text-gold-200">
              匹配度 {{ heroOutfit.score }}
            </span>
          </div>
          <h2 class="mt-2 font-display text-xl text-mist-100">{{ heroOutfit.name }}</h2>
          <p class="mt-1 line-clamp-2 text-sm text-mist-500">{{ heroOutfit.aiReason }}</p>
          <div class="mt-3 flex flex-wrap gap-1.5">
            <span
              v-for="name in heroOutfit.itemNames"
              :key="name"
              class="rounded-md border border-white/10 bg-white/[0.04] px-2 py-0.5 text-[11px] text-mist-300"
            >
              {{ name }}
            </span>
          </div>
        </div>
        <RouterLink
          :to="{ path: '/assistant', query: { q: `参考「${heroOutfit.name}」，再帮我搭一套今天能穿的通勤搭配` } }"
          class="btn-ghost shrink-0 self-start lg:self-center"
        >
          让 AI 再搭一套
          <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="m9 6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </RouterLink>
      </div>
    </section>

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
      class="flex items-start gap-3 rounded-2xl border border-rose-400/25 bg-rose-400/8 px-4 py-3 text-sm text-rose-100"
    >
      <svg class="mt-0.5 size-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 8v5M12 16h.01" stroke-linecap="round" />
      </svg>
      <div class="flex-1">
        <p class="font-medium">衣橱数据加载失败</p>
        <p class="mt-0.5 text-xs text-rose-200/80">{{ error }}（请确认 Spring Boot 已在 8080 端口启动）</p>
      </div>
      <button type="button" class="text-xs text-rose-200 underline-offset-4 hover:underline" @click="store.loadList()">
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

    <nav v-if="pages > 1" class="flex items-center justify-center gap-2 pt-2">
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
          <button
            type="button"
            class="btn-primary"
            :disabled="deleting"
            @click="confirmDelete"
          >
            {{ deleting ? '移除中…' : '确认移除' }}
          </button>
        </div>
      </template>
    </BaseModal>

    <Transition name="fade">
      <div
        v-if="notice"
        class="fixed bottom-6 left-1/2 z-50 -translate-x-1/2 rounded-full border border-gold-400/30 bg-ink-900/95 px-5 py-2.5 text-sm text-gold-100 shadow-xl backdrop-blur"
        role="status"
      >
        {{ notice }}
      </div>
    </Transition>
  </div>
</template>