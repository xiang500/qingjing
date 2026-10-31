export interface AuthUser {
  id: number
  username: string
  nickname: string
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
  message: string
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
    throw new Error(payload.message)
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
    throw new Error(payload.message)
  }
  return payload.data
}