import { request } from './http'
import type { DashboardData } from '@/types/dashboard'

export function fetchDashboard() {
  return request<DashboardData>({ url: '/dashboard', method: 'GET' })
}
