import { computed, reactive, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createClothing,
  deleteClothing,
  fetchCategories,
  fetchClothingPage,
  fetchOutfits,
  fetchWardrobeStats,
  markClothingWorn,
  toggleClothingFavorite,
  updateClothing,
} from '@/api/clothing'
import type {
  CategoryCode,
  CategoryMeta,
  Clothing,
  ClothingPayload,
  Outfit,
  WardrobeStats,
} from '@/types/clothing'

type SortKey = 'latest' | 'wearCount' | 'priceAsc' | 'priceDesc'

interface Filters {
  category: CategoryCode | ''
  keyword: string
  favorite: boolean
  sort: SortKey
  page: number
  size: number
}

export const useWardrobeStore = defineStore('wardrobe', () => {
  const items = ref<Clothing[]>([])
  const stats = ref<WardrobeStats | null>(null)
  const categories = ref<CategoryMeta[]>([])
  const outfits = ref<Outfit[]>([])

  const total = ref(0)
  const pages = ref(0)
  const loading = ref(false)
  const summaryLoading = ref(false)
  const error = ref('')

  const filters = reactive<Filters>({
    category: '',
    keyword: '',
    favorite: false,
    sort: 'latest',
    page: 1,
    size: 12,
  })

  const hasActiveFilter = computed(
    () => Boolean(filters.category) || filters.keyword.trim() !== '' || filters.favorite,
  )

  const isEmpty = computed(() => !loading.value && items.value.length === 0)

  async function loadList() {
    loading.value = true
    error.value = ''
    try {
      const page = await fetchClothingPage({
        category: filters.category,
        keyword: filters.keyword,
        favorite: filters.favorite || undefined,
        sort: filters.sort,
        page: filters.page,
        size: filters.size,
      })
      items.value = page.records
      total.value = page.total
      pages.value = page.pages
      if (filters.page > 1 && page.records.length === 0) {
        filters.page = Math.max(1, page.pages)
        await loadList()
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '衣橱加载失败'
      items.value = []
      total.value = 0
      pages.value = 0
    } finally {
      loading.value = false
    }
  }

  async function loadSummary() {
    summaryLoading.value = true
    try {
      const [statsData, categoryData, outfitData] = await Promise.all([
        fetchWardrobeStats(),
        fetchCategories(),
        fetchOutfits(),
      ])
      stats.value = statsData
      categories.value = categoryData
      outfits.value = outfitData
    } catch {
      // 概览失败不阻塞主列表，静默降级
    } finally {
      summaryLoading.value = false
    }
  }

  async function init() {
    await Promise.all([loadSummary(), loadList()])
  }

  async function refresh() {
    await Promise.all([loadSummary(), loadList()])
  }

  async function setCategory(category: CategoryCode | '') {
    filters.category = category
    filters.page = 1
    await loadList()
  }

  async function setKeyword(keyword: string) {
    filters.keyword = keyword
    filters.page = 1
    await loadList()
  }

  async function setFavoriteOnly(value: boolean) {
    filters.favorite = value
    filters.page = 1
    await loadList()
  }

  async function setSort(sort: SortKey) {
    filters.sort = sort
    filters.page = 1
    await loadList()
  }

  async function goToPage(page: number) {
    filters.page = Math.min(Math.max(1, page), Math.max(1, pages.value))
    await loadList()
  }

  async function toggleFavorite(item: Clothing) {
    const target = items.value.find((candidate) => candidate.id === item.id)
    const previous = target?.favorite
    if (target) {
      target.favorite = !target.favorite
    }
    try {
      const updated = await toggleClothingFavorite(item.id)
      if (target) {
        target.favorite = updated.favorite
      }
      await loadSummary()
    } catch (err) {
      if (target && previous !== undefined) {
        target.favorite = previous
      }
      error.value = err instanceof Error ? err.message : '操作失败'
    }
  }

  async function markWorn(item: Clothing) {
    try {
      const updated = await markClothingWorn(item.id)
      const target = items.value.find((candidate) => candidate.id === item.id)
      if (target) {
        target.wearCount = updated.wearCount
        target.lastWornAt = updated.lastWornAt
      }
      await loadSummary()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '记录失败'
    }
  }

  async function remove(id: number) {
    await deleteClothing(id)
    if (items.value.length === 1 && filters.page > 1) {
      filters.page -= 1
    }
    await refresh()
  }

  async function save(payload: ClothingPayload, id?: number) {
    if (id) {
      await updateClothing(id, payload)
    } else {
      await createClothing(payload)
    }
    await refresh()
  }

  return {
    items,
    stats,
    categories,
    outfits,
    total,
    pages,
    loading,
    summaryLoading,
    error,
    filters,
    hasActiveFilter,
    isEmpty,
    init,
    refresh,
    loadList,
    loadSummary,
    setCategory,
    setKeyword,
    setFavoriteOnly,
    setSort,
    goToPage,
    toggleFavorite,
    markWorn,
    remove,
    save,
  }
})