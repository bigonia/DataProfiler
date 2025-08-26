import { request } from '@/utils/request'

// Dashboard statistics interfaces
export interface DashboardStats {
  dataSourceCount: number
  fileCount: number
  taskCount: number
  reportCount: number
  taskStatusDistribution: StatusCount[]
  dataSourceTypesDistribution: TypeCount[]
  recentActivities: Activity[]
}

export interface StatusCount {
  name: string
  value: number
}

export interface TypeCount {
  name: string
  value: number
}

export interface Activity {
  id: number
  type: string
  title: string
  description: string
  timestamp: string
  status: string
}

/**
 * Get comprehensive dashboard statistics
 */
export const getDashboardStats = (): Promise<DashboardStats> => {
  return request({
    url: '/dashboard/stats',
    method: 'GET'
  })
}

/**
 * Get task status distribution
 */
export const getTaskStatusDistribution = (): Promise<StatusCount[]> => {
  return request({
    url: '/dashboard/task-status',
    method: 'GET'
  })
}

/**
 * Get data source types distribution
 */
export const getDataSourceTypesDistribution = (): Promise<TypeCount[]> => {
  return request({
    url: '/dashboard/datasource-types',
    method: 'GET'
  })
}

/**
 * Get recent activities
 */
export const getRecentActivities = (limit: number = 10): Promise<Activity[]> => {
  return request({
    url: '/dashboard/recent-activities',
    method: 'GET',
    params: { limit }
  })
}