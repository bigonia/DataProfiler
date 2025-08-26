import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { taskApi } from '@/api'
import type { ProfilingTaskRequest, ProfilingTask, TaskStatusResponse } from '@/types'
import { ElMessage } from 'element-plus'

export const useTaskStore = defineStore('task', () => {
  // State
  const tasks = ref<ProfilingTask[]>([])
  const currentTask = ref<ProfilingTask | null>(null)
  const taskStatuses = ref<Record<string, TaskStatusResponse>>({})
  const loading = ref(false)
  const pollingIntervals = ref<Record<string, NodeJS.Timeout>>({})

  // Getters
  const taskCount = computed(() => tasks.value.length)
  const runningTasks = computed(() => 
    tasks.value.filter(task => task.status === 'RUNNING' || task.status === 'PENDING')
  )
  const completedTasks = computed(() => 
    tasks.value.filter(task => task.status === 'COMPLETED')
  )
  const failedTasks = computed(() => 
    tasks.value.filter(task => task.status === 'FAILED')
  )

  // Actions
  const createTask = async (data: ProfilingTaskRequest) => {
    try {
      loading.value = true
      const response = await taskApi.create(data)
      tasks.value.unshift(response)
      currentTask.value = response
      
      // Start polling for task status
      startPolling(response.id!)
      
      ElMessage.success('Profiling task created successfully')
      return response
    } catch (error) {
      console.error('Failed to create task:', error)
      ElMessage.error('Failed to create profiling task')
      throw error
    } finally {
      loading.value = false
    }
  }

  const getTaskStatus = async (taskId: number) => {
    try {
      const response = await taskApi.getStatus(taskId)
      const taskIdStr = taskId.toString()
      taskStatuses.value[taskIdStr] = response
      
      // Update task in the list
      const taskIndex = tasks.value.findIndex(task => task.id === taskId)
      if (taskIndex !== -1) {
        tasks.value[taskIndex].status = response.status
        tasks.value[taskIndex].progress = response.progress
        if (response.errorDetails) {
          tasks.value[taskIndex].errorMessage = response.errorDetails
        }
      }
      
      // Update current task if it matches
      if (currentTask.value?.id === taskId) {
        currentTask.value.status = response.status
        currentTask.value.progress = response.progress
        if (response.errorDetails) {
          currentTask.value.errorMessage = response.errorDetails
        }
      }
      
      // Stop polling if task is completed or failed
      if (response.status === 'COMPLETED' || response.status === 'FAILED') {
        stopPolling(taskIdStr)
        
        if (response.status === 'COMPLETED') {
          ElMessage.success(`Task ${response.taskId} completed successfully`)
        } else {
          ElMessage.error(`Task ${response.taskId} failed: ${response.errorDetails || 'Unknown error'}`)
        }
      }
      
      return response
    } catch (error) {
      console.error('Failed to get task status:', error)
      throw error
    }
  }

  const deleteTask = async (taskId: number) => {
    try {
      loading.value = true
      await taskApi.delete(taskId.toString())
      
      // Remove from tasks list
      tasks.value = tasks.value.filter(task => task.id !== taskId)
      
      // Clear current task if it matches
      if (currentTask.value?.id === taskId) {
        currentTask.value = null
      }
      
      // Stop polling and clean up
      const taskIdStr = taskId.toString()
      stopPolling(taskIdStr)
      delete taskStatuses.value[taskIdStr]
      
      ElMessage.success('Task deleted successfully')
    } catch (error) {
      console.error('Failed to delete task:', error)
      ElMessage.error('Failed to delete task')
      throw error
    } finally {
      loading.value = false
    }
  }

  const startPolling = (taskId: number, interval: number = 2000) => {
    // Clear existing interval if any
    const taskIdStr = taskId.toString()
    stopPolling(taskIdStr)
    
    const intervalId = setInterval(async () => {
      try {
        await getTaskStatus(taskId)
      } catch (error) {
        console.error('Polling error:', error)
        stopPolling(taskIdStr)
      }
    }, interval)
    
    pollingIntervals.value[taskIdStr] = intervalId
  }

  const stopPolling = (taskId: string) => {
    const intervalId = pollingIntervals.value[taskId]
    if (intervalId) {
      clearInterval(intervalId)
      delete pollingIntervals.value[taskId]
    }
  }

  const stopAllPolling = () => {
    Object.keys(pollingIntervals.value).forEach(taskId => {
      stopPolling(taskId)
    })
  }

  const setCurrentTask = (task: ProfilingTask | null) => {
    currentTask.value = task
  }

  const fetchTasks = async () => {
    try {
      loading.value = true
      const response = await taskApi.getAll()
      
      // Handle different response formats with defensive checks
      if (Array.isArray(response)) {
        tasks.value = response
      } else if (response && typeof response === 'object' && Array.isArray(response.content)) {
        tasks.value = response.content
      } else {
        tasks.value = []
      }
      
      return response
    } catch (error) {
      console.error('Failed to fetch tasks:', error)
      ElMessage.error('Failed to load tasks')
      tasks.value = []
      throw error
    } finally {
      loading.value = false
    }
  }

  const addTask = (task: ProfilingTask) => {
    const existingIndex = tasks.value.findIndex(t => t.id === task.id)
    if (existingIndex !== -1) {
      tasks.value[existingIndex] = task
    } else {
      tasks.value.unshift(task)
    }
  }

  const clearTasks = () => {
    stopAllPolling()
    tasks.value = []
    currentTask.value = null
    taskStatuses.value = {}
  }

  // Cleanup on store destruction
  const cleanup = () => {
    stopAllPolling()
  }

  return {
    // State
    tasks,
    currentTask,
    taskStatuses,
    loading,
    
    // Getters
    taskCount,
    runningTasks,
    completedTasks,
    failedTasks,
    
    // Actions
    createTask,
    fetchTasks,
    getTaskStatus,
    deleteTask,
    startPolling,
    stopPolling,
    stopAllPolling,
    setCurrentTask,
    addTask,
    clearTasks,
    cleanup
  }
})