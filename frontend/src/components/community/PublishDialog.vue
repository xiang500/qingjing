<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import { fetchOutfits } from '@/api/clothing'
import { useCommunityStore } from '@/stores/community'
import type { Outfit } from '@/types/clothing'

const props = defineProps<{
  open: boolean
  prefill?: { title?: string; content?: string; images?: string[] }
}>()

const emit = defineEmits<{ (event: 'close'): void; (event: 'published'): void }>()

const store = useCommunityStore()
const outfits = ref<Outfit[]>([])
const title = ref('')
const content = ref('')
const tags = ref('')
const outfitId = ref(0)
const images = ref<string[]>([])

const preview = computed(() => images.value.slice(0, 4))

watch(
  () => props.open,
  async (open) => {
    if (!open) {
      return
    }
    title.value = props.prefill?.title ?? ''
    content.value = props.prefill?.content ?? ''
    images.value = props.prefill?.images ?? []
    tags.value = '穿搭,轻境'
    outfitId.value = 0
    store.error = ''
    if (!outfits.value.length) {
      try {
        outfits.value = await fetchOutfits()
      } catch {
        outfits.value = []
      }
    }
    if (!images.value.length && outfits.value.length) {
      outfitId.value = outfits.value[0].id
    }
  },
)

async function submit() {
  const ok = await store.publish({
    title: title.value.trim(),
    content: content.value.trim(),
    outfitId: outfitId.value || undefined,
    tags: tags.value.split(/[,，]/).map((tag) => tag.trim()).filter(Boolean),
    images: images.value,
  })
  if (ok) {
    emit('published')
    emit('close')
  }
}
</script>

<template>
  <BaseModal
    :open="open"
    title="分享一套穿搭"
    description="发布后会出现在灵感社区，其他人可以直接试穿同款搭配"
    width="md"
    @close="emit('close')"
  >
    <form class="space-y-4" @submit.prevent="submit">
      <div>
        <label class="field-label" for="post-title">
          标题 <span class="text-gold-300">*</span>
        </label>
        <input
          id="post-title"
          v-model="title"
          class="field-input"
          placeholder="例如：雾蓝衬衫的通勤日"
          maxlength="60"
        />
      </div>

      <div>
        <label class="field-label" for="post-content">搭配心得</label>
        <textarea
          id="post-content"
          v-model="content"
          rows="3"
          class="field-input resize-none"
          placeholder="为什么这样搭？什么场合穿？"
          maxlength="500"
        />
      </div>

      <!-- 预置图片：统一放在深色影棚底上 -->
      <div v-if="preview.length" class="space-y-2">
        <p class="field-label flex items-center justify-between">
          <span>已选图片</span>
          <span class="tracking-normal text-mist-500">{{ preview.length }} 张</span>
        </p>
        <div class="flex flex-wrap gap-2">
          <div
            v-for="url in preview"
            :key="url"
            class="photo-tile size-16 overflow-hidden rounded-xl"
          >
            <img :src="url" alt="" class="p-1.5" />
          </div>
        </div>
      </div>

      <div v-else>
        <label class="field-label" for="post-outfit">关联套装</label>
        <div class="relative">
          <select
            id="post-outfit"
            v-model="outfitId"
            class="field-input cursor-pointer appearance-none pr-9"
          >
            <option :value="0">不关联套装（仅文字分享）</option>
            <option v-for="outfit in outfits" :key="outfit.id" :value="outfit.id">
              {{ outfit.name }}（{{ outfit.itemNames.length }} 件）
            </option>
          </select>
          <svg
            class="pointer-events-none absolute right-3 top-1/2 size-3.5 -translate-y-1/2 text-mist-500"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="m6 9 6 6 6-6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <p class="mt-1.5 text-[10px] leading-relaxed text-mist-500">
          关联后，其他人可以在社区里一键试穿这套搭配。
        </p>
      </div>

      <div>
        <label class="field-label" for="post-tags">标签（逗号分隔）</label>
        <input id="post-tags" v-model="tags" class="field-input" placeholder="通勤，秋季，法式" />
      </div>

      <div class="glass-soft flex items-start gap-2.5 px-3 py-2.5">
        <svg
          class="mt-0.5 size-3.5 shrink-0 text-gold-300"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
        >
          <path d="M12 3.5 13.9 9l5.6 1.9-5.6 1.9L12 18.4l-1.9-5.6L4.5 11 10.1 9 12 3.5Z" stroke-linejoin="round" />
        </svg>
        <p class="text-[11px] leading-relaxed text-mist-400">
          发布小贴士：标题写清场景（通勤 / 约会 / 周末），正文说清配色与单品，更容易被试穿。
        </p>
      </div>

      <p
        v-if="store.error"
        class="rounded-xl border border-gold-300/35 bg-gold-300/10 px-3 py-2 text-xs leading-relaxed text-mist-200"
      >
        {{ store.error }}
      </p>
    </form>

    <template #footer>
      <div class="flex items-center justify-between gap-3">
        <span class="hidden text-[10px] text-mist-500 sm:inline">发布后将展示在社区信息流中</span>
        <div class="ml-auto flex justify-end gap-2">
          <button type="button" class="btn-ghost" @click="emit('close')">取消</button>
          <button type="button" class="btn-primary" :disabled="store.publishing" @click="submit">
            {{ store.publishing ? '发布中…' : '发布到社区' }}
          </button>
        </div>
      </div>
    </template>
  </BaseModal>
</template>
