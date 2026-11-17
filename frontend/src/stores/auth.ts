import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  fetchMe,
  login as loginApi,
  register as registerApi,
  updateProfile as updateProfileApi,
  type AuthUser,
  type ProfilePayload,
} from '@/api/auth'

const TOKEN_KEY = 'qj.token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<AuthUser | null>(null)
  const loading = ref(false)
  const error = ref('')

  const isLoggedIn = computed(() => Boolean(token.value))
  const displayName = computed(() => user.value?.nickname || user.value?.username || '演示账号')

  function persist(sessionToken: string | null) {
    token.value = sessionToken
    if (sessionToken) {
      localStorage.setItem(TOKEN_KEY, sessionToken)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
  }

  async function login(username: string, password: string) {
    loading.value = true
    error.value = ''
    try {
      const session = await loginApi(username, password)
      persist(session.token)
      user.value = session.user
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : '登录失败'
      return false
    } finally {
      loading.value = false
    }
  }

  async function register(username: string, password: string, nickname?: string) {
    loading.value = true
    error.value = ''
    try {
      const session = await registerApi(username, password, nickname)
      persist(session.token)
      user.value = session.user
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : '注册失败'
      return false
    } finally {
      loading.value = false
    }
  }

  async function restore() {
    if (!token.value) {
      return
    }
    try {
      user.value = await fetchMe()
    } catch {
      persist(null)
      user.value = null
    }
  }

  function logout() {
    persist(null)
    user.value = null
  }

  /** 保存个人资料：身高/体重/体型/风格偏好会作为 AI 推荐的输入 */
  async function saveProfile(payload: ProfilePayload) {
    const updated = await updateProfileApi(payload)
    user.value = updated
    return updated
  }

  return {
    token,
    user,
    loading,
    error,
    isLoggedIn,
    displayName,
    login,
    register,
    restore,
    logout,
    saveProfile,
  }
})