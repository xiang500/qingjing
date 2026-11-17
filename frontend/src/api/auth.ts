export interface AuthUser {
  id: number
  username: string
  nickname: string
  /** ADMIN 管理员 / USER 普通用户；旧后端没有该字段时按普通用户处理 */
  role?: 'ADMIN' | 'USER' | string
  avatarUrl?: string | null
  gender?: number
  heightCm?: number
  weightKg?: number
  bodyType?: string
  stylePreference: string[]
}

export interface AuthSession {
  token: string
  expiresIn: number
  user: AuthUser
}

interface Envelope<T> {
  code: number
  msg?: string
  message?: string
  data: T
}

async function post<T>(url: string, body: unknown): Promise<T> {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  const payload = (await response.json()) as Envelope<T>
  if (payload.code !== 0) {
    throw new Error(payload.msg ?? payload.message ?? '请求失败')
  }
  return payload.data
}

export function login(username: string, password: string) {
  return post<AuthSession>('/api/auth/login', { username, password })
}

export function register(username: string, password: string, nickname?: string) {
  return post<AuthSession>('/api/auth/register', { username, password, nickname })
}

export async function fetchMe(): Promise<AuthUser> {
  const token = localStorage.getItem('qj.token')
  const response = await fetch('/api/auth/me', {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  const payload = (await response.json()) as Envelope<AuthUser>
  if (payload.code !== 0) {
    throw new Error(payload.msg ?? payload.message ?? '请求失败')
  }
  return payload.data
}

export interface ProfilePayload {
  nickname?: string
  avatarUrl?: string
  gender?: number
  heightCm?: number
  weightKg?: number
  bodyType?: string
  stylePreference?: string[]
}

/** 更新个人资料（身高/体重/体型/风格偏好是 AI 推荐的输入） */
export async function updateProfile(payload: ProfilePayload): Promise<AuthUser> {
  const token = localStorage.getItem('qj.token')
  const response = await fetch('/api/auth/profile', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
  })
  const body = (await response.json()) as Envelope<AuthUser>
  if (body.code !== 0) {
    throw new Error(body.msg ?? body.message ?? '保存失败')
  }
  return body.data
}