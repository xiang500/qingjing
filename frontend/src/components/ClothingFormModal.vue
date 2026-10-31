<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import BaseModal from './BaseModal.vue'
import { CATEGORY_LABELS, CATEGORY_ORDER, SEASON_LABELS, type CategoryCode, type Clothing, type ClothingPayload } from '@/types/clothing'

const props = defineProps<{
  open: boolean
  item: Clothing | null
  submitting: boolean
}>()

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'submit', payload: ClothingPayload): void
}>()

const createBlank = (): ClothingPayload => ({
  name: '',
  category: 'TOP',
  subCategory: '',
  color: '',
  colorHex: '#D8C9B0',
  season: ['SPRING', 'AUTUMN'],
  styleTags: [],
  material: '',
  brand: '',
  price: undefined,
  purchaseDate: '',
  favorite: false,
  note: '',
})

const form = reactive<ClothingPayload>(createBlank())
const tagText = ref('')
const nameError = ref('')

const title = computed(() => (props.item ? '编辑单品' : '录入新单品'))
const description = computed(() =>
  props.item ? `正在修改「${props.item.name}」的信息` : '录入一次，之后 AI 推荐和 3D 换装都会用到它',
)

watch(
  () => props.open,
  (open) => {
    if (!open) {
      return
    }
    nameError.value = ''
    if (props.item) {
      const item = props.item
      Object.assign(form, {
        name: item.name,
        category: item.category,
        subCategory: item.subCategory ?? '',
        color: item.color ?? '',
        colorHex: item.colorHex ?? '#D8C9B0',
        season: [...item.season],
        styleTags: [...item.styleTags],
        material: item.material ?? '',
        brand: item.brand ?? '',
        price: item.price ?? undefined,
        purchaseDate: item.purchaseDate ?? '',
        favorite: item.favorite,
        note: item.note ?? '',
      })
      tagText.value = item.styleTags.join('，')
    } else {
      Object.assign(form, createBlank())
      tagText.value = ''
    }
  },
  { immediate: true },
)

const seasonKeys = Object.keys(SEASON_LABELS)

function toggleSeason(season: string) {
  const index = form.season.indexOf(season)
  if (index >= 0) {
    form.season.splice(index, 1)
  } else {
    form.season.push(season)
  }
}

const toggleCategory = (code: CategoryCode) => {
  form.category = code
}

function handleSubmit() {
  if (!form.name.trim()) {
    nameError.value = '请填写单品名称'
    return
  }
  nameError.value = ''
  const payload: ClothingPayload = {
    ...form,
    name: form.name.trim(),
    price: form.price === undefined || Number.isNaN(form.price) ? undefined : Number(form.price),
    purchaseDate: form.purchaseDate || undefined,
    colorHex: form.colorHex || undefined,
    styleTags: tagText.value
      .split(/[,，]/)
      .map((tag) => tag.trim())
      .filter(Boolean),
  }
  emit('submit', payload)
}
</script>

<template>
  <BaseModal :open="open" :title="title" :description="description" width="lg" @close="emit('close')">
    <form class="space-y-5" @submit.prevent="handleSubmit">
      <div class="grid gap-4 sm:grid-cols-2">
        <div class="sm:col-span-2">
          <label class="field-label" for="name">单品名称 *</label>
          <input id="name" v-model="form.name" class="field-input" placeholder="例如：雾霾蓝宽松衬衫" />
          <p v-if="nameError" class="mt-1 text-xs text-rose-300">{{ nameError }}</p>
        </div>

        <div class="sm:col-span-2">
          <span class="field-label">分类 *</span>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="code in CATEGORY_ORDER"
              :key="code"
              type="button"
              class="chip"
              :class="{ 'chip-active': form.category === code }"
              @click="toggleCategory(code)"
            >
              {{ CATEGORY_LABELS[code] }}
            </button>
          </div>
        </div>

        <div>
          <label class="field-label" for="subCategory">子类</label>
          <input id="subCategory" v-model="form.subCategory" class="field-input" placeholder="衬衫 / 牛仔 / 西装…" />
        </div>

        <div>
          <label class="field-label" for="brand">品牌</label>
          <input id="brand" v-model="form.brand" class="field-input" placeholder="例如：COS" />
        </div>

        <div>
          <label class="field-label" for="color">颜色名</label>
          <input id="color" v-model="form.color" class="field-input" placeholder="例如：雾霾蓝" />
        </div>

        <div>
          <span class="field-label">主色</span>
          <div class="flex items-center gap-2">
            <input
              v-model="form.colorHex"
              type="color"
              class="size-10 cursor-pointer rounded-lg border border-white/10 bg-transparent"
              aria-label="选择主色"
            />
            <input v-model="form.colorHex" class="field-input font-mono text-xs" placeholder="#8CA3B8" />
          </div>
        </div>

        <div>
          <label class="field-label" for="material">材质</label>
          <input id="material" v-model="form.material" class="field-input" placeholder="棉 / 羊毛 / 天丝…" />
        </div>

        <div>
          <label class="field-label" for="price">购入价（元）</label>
          <input
            id="price"
            v-model.number="form.price"
            type="number"
            min="0"
            step="1"
            class="field-input"
            placeholder="259"
          />
        </div>

        <div>
          <label class="field-label" for="purchaseDate">购入日期</label>
          <input id="purchaseDate" v-model="form.purchaseDate" type="date" class="field-input" />
        </div>

        <div>
          <label class="field-label" for="imageUrl">图片地址</label>
          <input id="imageUrl" v-model="form.imageUrl" class="field-input" placeholder="留空则显示分类占位图" />
        </div>

        <div class="sm:col-span-2">
          <span class="field-label">适穿季节</span>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="season in seasonKeys"
              :key="season"
              type="button"
              class="chip"
              :class="{ 'chip-active': form.season.includes(season) }"
              @click="toggleSeason(season)"
            >
              {{ SEASON_LABELS[season] }}
            </button>
          </div>
        </div>

        <div class="sm:col-span-2">
          <label class="field-label" for="tags">风格标签（逗号分隔）</label>
          <input id="tags" v-model="tagText" class="field-input" placeholder="通勤，法式，简约" />
        </div>

        <div class="sm:col-span-2">
          <label class="field-label" for="note">备注</label>
          <textarea id="note" v-model="form.note" rows="2" class="field-input resize-none" placeholder="版型、搭配心得…" />
        </div>
      </div>
    </form>

    <template #footer>
      <div class="flex items-center justify-between gap-3">
        <label class="flex cursor-pointer items-center gap-2 text-xs text-mist-300">
          <input v-model="form.favorite" type="checkbox" class="size-4 accent-gold-400" />
          同时加入收藏
        </label>
        <div class="flex items-center gap-2">
          <button type="button" class="btn-ghost" @click="emit('close')">取消</button>
          <button type="button" class="btn-primary" :disabled="submitting" @click="handleSubmit">
            {{ submitting ? '保存中…' : '保存单品' }}
          </button>
        </div>
      </div>
    </template>
  </BaseModal>
</template>