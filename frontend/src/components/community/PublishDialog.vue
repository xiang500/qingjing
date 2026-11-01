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
        <label class="field-label" for="post-title">标题 *</label>
        <input id="post-title" v-model="title" class="field-input" placeholder="例如：雾蓝衬衫的通勤日" maxlength="60" />
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

      <div v-if="preview.length" class="flex gap-2">
        <div
          v-for="url in preview"
          :key="url"
          class="size-16 overflow-hidden rounded-lg border border-white/10 bg-ink-900/60"
        >
          <img :src="url" alt="" class="size-full object-contain p-1" />
        </div>
      </div>

      <div v-else>
        <label class="field-label" for="post-outfit">关联套装</label>
        <select id="post-outfit" v-model="outfitId" class="field-input">
          <option :value="0">不关联套装（仅文字分享）</option>
          <option v-for="outfit in outfits" :key="outfit.id" :value="outfit.id">
            {{ outfit.name }}（{{ outfit.itemNames.length }} 件）
          </option>
        </select>
      </div>

      <div>
        <label class="field-label" for="post-tags">标签（逗号分隔）</label>
        <input id="post-tags" v-model="tags" class="field-input" placeholder="通勤，秋季，法式" />
      </div>

      <p v-if="store.error" class="rounded-lg border border-rose-400/25 bg-rose-400/8 px-3 py-2 text-xs text-rose-100">
        {{ store.error }}
      </p>
    </form>

    <template #footer>
      <div class="flex justify-end gap-2">
        <button type="button" class="btn-ghost" @click="emit('close')">取消</button>
        <button type="button" class="btn-primary" :disabled="store.publishing" @click="submit">
          {{ store.publishing ? '发布中…' : '发布到社区' }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>