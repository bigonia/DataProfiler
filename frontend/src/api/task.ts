import request from './request'
import type { ProfilingTaskRequest, ProfilingTask, TaskStatusResponse } from '@/types'

export const taskApi = {
  // Create profiling task
  create: (data: ProfilingTaskRequest) => {
    return request.post<ProfilingTask>('/profiling/profiling-tasks', data)
  },

  // Get all tasks
  getAll: () => {
    return request.get<ProfilingTask[]>('/profiling/profiling-tasks/list')
  },

  // Get task status
  getStatus: (taskId: string | number) => {
    return request.get<TaskStatusResponse>(`/profiling/task-status/${taskId}`)
  },

  // Delete task
  delete: (taskId: string) => {
    return request.delete(`/profiling/profiling-tasks/${taskId}`)
  }
}