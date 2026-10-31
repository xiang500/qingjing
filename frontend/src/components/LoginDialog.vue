<script setup lang="ts">
import { ref, watch } from 'vue'
import BaseModal from './BaseModal.vue'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ (event: 'close'): void }>()

const auth = useAuthStore()
const mode = ref<'login' | 'register'>('login')
const username = ref('demo')
const password = ref('123456')
const nickname = ref('')

watch(
  () => props.open,
  (open) => {
    if (open) {
      mode.value = 'login'
      username.value = 'demo'
      password.value = '123456'
      nickname.value = ''
      auth.error = ''
    }
  },
)

async function submit() {
  const ok = mode.value === 'login'
    ? await auth.login(username.value.trim(), password.value)
    : await auth.register(username.value.trim(), password.value, nickname.value.trim() || undefined)
  if (ok) {
    emit('close')
  }
}
</script>

<template>
  <BaseModal
    :open="open"
    :title="mode === 'login' ? '登录轻境' : '创建账号'"
    :description="mode === 'login' ? '演示账号已预填，直接登录即可体验完整衣橱' : '注册后衣橱与对话记录会归属于你的账号'"
    width="sm"
    @close="emit('close')"
  >
    <form class="space-y-4" @submit.prevent="submit">
      <div>
        <label class="field-label" for="username">账号</label>
        <input id="username" v-model="username" class="field-input" autocomplete="username" placeholder="3-20 位字母或数字" />
      </div>
      <div>
        <label class="field-label" for="password">密码</label>
        <input
          id="password"
          v-model="password"
          type="password"
          class="field-input"
          autocomplete="current-password"
          placeholder="至少 6 位"
        />
      </div>
      <div v-if="mode === 'register'">
        <label class="field-label" for="nickname">昵称（可选）</label>
        <input id="nickname" v-model="nickname" class="field-input" placeholder="例如：小轻" />
      </div>

      <p v-if="auth.error" class="rounded-lg border border-rose-400/25 bg-rose-400/8 px-3 py-2 text-xs text-rose-100">
        {{ auth.error }}
      </p>

      <button type="submit" class="btn-primary w-full" :disabled="auth.loading">
        {{ auth.loading ? '处理中…' : mode === 'login' ? '登录' : '注册并进入' }}
      </button>

      <p class="text-center text-xs text-mist-500">
        {{ mode === 'login' ? '还没有账号？' : '已经有账号了？' }}
        <button
          type="button"
          class="text-gold-300 underline-offset-4 hover:underline"
          @click="mode = mode === 'login' ? 'register' : 'login'"
        >
          {{ mode === 'login' ? '去注册' : '去登录' }}
        </button>
      </p>
    </form>
  </BaseModal>
</template>