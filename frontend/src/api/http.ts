import axios, { AxiosError, type AxiosRequestConfig } from 'axios'

/** 后端统一响应结构 */
interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 带业务错误码的异常，便于页面做差异化提示 */
export class ApiError extends Error {
  readonly code: number

  constructor(message: string, code: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const client = axios.create({
  baseURL: '/api',
  timeout: 15_000,
})

// 阶段一：演示用户身份由请求头携带；阶段二登录后换成 Authorization: Bearer <jwt>
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('qj.token')
  if (token) {
    // 登录后优先使用 JWT；阶段一、二的 X-User-Id 仅在没有 token 时作为演示回退
    config.headers.set('Authorization', `Bearer ${token}`)
  } else {
    const userId = localStorage.getItem('qj.userId') ?? '1'
    config.headers.set('X-User-Id', userId)
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResult<unknown>>) => {
    const body = error.response?.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new ApiError(body.message, body.code))
    }
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new ApiError('请求超时，请检查后端服务是否已启动', -1))
    }
    return Promise.reject(new ApiError(error.message || '网络异常', -1))
  },
)

/** 统一解包：只把 data 交给调用方 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await client.request<ApiResult<T>>(config)
  const body = response.data
  if (body.code !== 0) {
    throw new ApiError(body.message, body.code)
  }
  return body.data
}

export default client