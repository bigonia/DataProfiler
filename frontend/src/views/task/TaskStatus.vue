<template>
  <div class="task-status">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <el-button text @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          Back
        </el-button>
        <div class="title-section">
          <h2 class="page-title">{{ task?.taskName || 'Task Status' }}</h2>
          <p class="page-description">Monitor task execution progress and details</p>
        </div>
      </div>
      
      <div class="header-right">
        <el-button @click="refreshStatus" :loading="loading">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
        
        <el-dropdown v-if="task" @command="handleAction">
          <el-button type="primary">
            Actions
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-if="task.status === 'completed'"
                command="viewReport"
              >
                <el-icon><View /></el-icon>
                View Report
              </el-dropdown-item>
              <el-dropdown-item
                v-if="task.status === 'running'"
                command="stop"
                divided
              >
                <el-icon><VideoPause /></el-icon>
                Stop Task
              </el-dropdown-item>
              <el-dropdown-item
                v-if="task.status === 'failed'"
                command="retry"
              >
                <el-icon><RefreshRight /></el-icon>
                Retry Task
              </el-dropdown-item>
              <el-dropdown-item command="delete" divided>
                <el-icon><Delete /></el-icon>
                Delete Task
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    
    <div v-if="loading && !task" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>
    
    <div v-else-if="task" class="task-content">
      <!-- Status Overview -->
      <div class="status-overview">
        <div class="status-card" :class="getStatusClass(task.status)">
          <div class="status-header">
            <div class="status-info">
              <el-icon class="status-icon" :class="{ rotating: task.status === 'running' }">
                <component :is="getStatusIcon(task.status)" />
              </el-icon>
              <div>
                <h3 class="status-title">{{ formatStatus(task.status) }}</h3>
                <p class="status-subtitle">{{ getStatusDescription(task.status) }}</p>
              </div>
            </div>
            
            <el-tag
              :type="getStatusType(task.status)"
              size="large"
              :effect="task.status === 'running' ? 'plain' : 'dark'"
            >
              {{ formatStatus(task.status) }}
            </el-tag>
          </div>
          
          <!-- Progress Bar for Running Tasks -->
          <div v-if="task.status === 'running' && task.progress !== undefined" class="progress-section">
            <div class="progress-info">
              <span>Overall Progress</span>
              <span>{{ task.progress }}%</span>
            </div>
            <el-progress
              :percentage="task.progress"
              :status="task.progress === 100 ? 'success' : undefined"
              :stroke-width="8"
            />
            <div v-if="task.currentStep" class="current-step">
              <span>Current Step: {{ task.currentStep }}</span>
            </div>
          </div>
          
          <!-- Error Message for Failed Tasks -->
          <div v-if="task.status === 'failed' && task.errorMessage" class="error-section">
            <el-alert
              :title="task.errorMessage"
              type="error"
              :closable="false"
              show-icon
            >
              <template v-if="task.errorDetails">
                <details class="error-details">
                  <summary>Error Details</summary>
                  <pre>{{ task.errorDetails }}</pre>
                </details>
              </template>
            </el-alert>
          </div>
        </div>
      </div>
      
      <!-- Task Information -->
      <div class="info-grid">
        <div class="info-card">
          <h3 class="card-title">
            <el-icon><InfoFilled /></el-icon>
            Task Information
          </h3>
          <div class="info-content">
            <div class="info-item">
              <span class="label">Task ID:</span>
              <span class="value">{{ task.id }}</span>
            </div>
            <div class="info-item">
              <span class="label">Task Name:</span>
              <span class="value">{{ task.taskName }}</span>
            </div>
            <div class="info-item">
              <span class="label">Description:</span>
              <span class="value">{{ task.description || 'No description' }}</span>
            </div>
            <div class="info-item">
              <span class="label">Priority:</span>
              <el-tag :type="getPriorityType(task.priority)" size="small">
                {{ formatPriority(task.priority) }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="label">Created:</span>
              <span class="value">{{ formatDate(task.createdAt, 'datetime') }}</span>
            </div>
            <div v-if="task.startedAt" class="info-item">
              <span class="label">Started:</span>
              <span class="value">{{ formatDate(task.startedAt, 'datetime') }}</span>
            </div>
            <div v-if="task.completedAt" class="info-item">
              <span class="label">Completed:</span>
              <span class="value">{{ formatDate(task.completedAt, 'datetime') }}</span>
            </div>
            <div v-if="task.duration" class="info-item">
              <span class="label">Duration:</span>
              <span class="value">{{ formatDuration(task.duration) }}</span>
            </div>
          </div>
        </div>
        
        <div class="info-card">
          <h3 class="card-title">
            <el-icon><Coin /></el-icon>
            Data Source
          </h3>
          <div class="info-content">
            <div class="info-item">
              <span class="label">Name:</span>
              <span class="value">{{ getDataSourceName(task.dataSourceId) }}</span>
            </div>
            <div class="info-item">
              <span class="label">Type:</span>
              <span class="value">{{ getDataSourceType(task.dataSourceId) }}</span>
            </div>
            <div class="info-item">
              <span class="label">Tables:</span>
              <span class="value">
                {{ task.tableNames?.length ? `${task.tableNames.length} tables` : 'All tables' }}
              </span>
            </div>
            <div v-if="task.tableNames?.length" class="table-list">
              <el-tag
                v-for="table in task.tableNames"
                :key="table"
                size="small"
                class="table-tag"
              >
                {{ table }}
              </el-tag>
            </div>
          </div>
        </div>
        
        <div class="info-card">
          <h3 class="card-title">
            <el-icon><Setting /></el-icon>
            Configuration
          </h3>
          <div class="info-content">
            <div class="info-item">
              <span class="label">Profile Types:</span>
              <div class="value">
                <el-tag
                  v-for="type in task.profileTypes"
                  :key="type"
                  size="small"
                  class="profile-tag"
                >
                  {{ formatProfileType(type) }}
                </el-tag>
              </div>
            </div>
            <div class="info-item">
              <span class="label">Sample Size:</span>
              <span class="value">{{ formatSampleSize(task.sampleSize) }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Execution Log -->
      <div class="log-section">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">
              <el-icon><Document /></el-icon>
              Execution Log
            </h3>
            <el-button text @click="refreshLog" :loading="loadingLog">
              <el-icon><Refresh /></el-icon>
              Refresh Log
            </el-button>
          </div>
          
          <div class="log-content">
            <div v-if="loadingLog" class="loading-state">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>Loading execution log...</span>
            </div>
            
            <div v-else-if="executionLog.length === 0" class="empty-log">
              <el-icon><Warning /></el-icon>
              <span>No execution log available</span>
            </div>
            
            <div v-else class="log-entries">
              <div
                v-for="(entry, index) in executionLog"
                :key="index"
                class="log-entry"
                :class="getLogEntryClass(entry.level)"
              >
                <div class="log-timestamp">{{ formatDate(entry.timestamp, 'time') }}</div>
                <div class="log-level">
                  <el-tag :type="getLogLevelType(entry.level)" size="small">
                    {{ entry.level.toUpperCase() }}
                  </el-tag>
                </div>
                <div class="log-message">{{ entry.message }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else class="error-state">
      <el-result
        icon="error"
        title="Task Not Found"
        sub-title="The requested task could not be found or you don't have permission to view it."
      >
        <template #extra>
          <el-button type="primary" @click="$router.push('/tasks')">
            Back to Tasks
          </el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowDown,
  Refresh,
  View,
  VideoPause,
  RefreshRight,
  Delete,
  CircleCheck,
  Loading,
  CircleClose,
  Clock,
  InfoFilled,
  Coin,
  Setting,
  Document,
  Warning
} from '@element-plus/icons-vue'
import { useTaskStore } from '@/stores/task'
import { useDataSourceStore } from '@/stores/datasource'
import { formatDate, formatDuration } from '@/utils'
import type { ProfilingTask, LogEntry } from '@/types'

const route = useRoute()
const router = useRouter()
const taskStore = useTaskStore()
const dataSourceStore = useDataSourceStore()

const loading = ref(false)
const loadingLog = ref(false)
const task = ref<ProfilingTask | null>(null)
const executionLog = ref<LogEntry[]>([])

const taskId = computed(() => parseInt(route.params.id as string))

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'completed':
      return CircleCheck
    case 'running':
      return Loading
    case 'failed':
      return CircleClose
    case 'pending':
    default:
      return Clock
  }
}

const getStatusClass = (status: string) => {
  return {
    'status-completed': status === 'completed',
    'status-running': status === 'running',
    'status-failed': status === 'failed',
    'status-pending': status === 'pending'
  }
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'completed':
      return 'success'
    case 'running':
      return 'warning'
    case 'failed':
      return 'danger'
    case 'pending':
    default:
      return 'info'
  }
}

const formatStatus = (status: string) => {
  return status.charAt(0).toUpperCase() + status.slice(1)
}

const getStatusDescription = (status: string) => {
  switch (status) {
    case 'completed':
      return 'Task completed successfully'
    case 'running':
      return 'Task is currently running'
    case 'failed':
      return 'Task failed with errors'
    case 'pending':
      return 'Task is waiting to be executed'
    default:
      return 'Unknown status'
  }
}

const getPriorityType = (priority: string) => {
  switch (priority) {
    case 'high':
      return 'danger'
    case 'normal':
      return 'primary'
    case 'low':
      return 'info'
    default:
      return 'info'
  }
}

const formatPriority = (priority: string) => {
  return priority.charAt(0).toUpperCase() + priority.slice(1)
}

const formatProfileType = (type: string) => {
  const types: Record<string, string> = {
    basic: 'Basic Statistics',
    advanced: 'Advanced Analysis',
    quality: 'Data Quality',
    patterns: 'Pattern Analysis'
  }
  return types[type] || type
}

const formatSampleSize = (size: string) => {
  if (size === 'all') return 'All rows'
  return `${parseInt(size).toLocaleString()} rows`
}

const getDataSourceName = (dataSourceId: number) => {
  const dataSource = dataSourceStore.dataSources.find(ds => ds.id === dataSourceId)
  return dataSource?.name || `DataSource #${dataSourceId}`
}

const getDataSourceType = (dataSourceId: number) => {
  const dataSource = dataSourceStore.dataSources.find(ds => ds.id === dataSourceId)
  return dataSource?.type || 'Unknown'
}

const getLogEntryClass = (level: string) => {
  return {
    'log-error': level === 'error',
    'log-warning': level === 'warning',
    'log-info': level === 'info',
    'log-debug': level === 'debug'
  }
}

const getLogLevelType = (level: string) => {
  switch (level) {
    case 'error':
      return 'danger'
    case 'warning':
      return 'warning'
    case 'info':
      return 'primary'
    case 'debug':
      return 'info'
    default:
      return 'info'
  }
}

const loadTask = async () => {
  loading.value = true
  try {
    // In a real implementation, you would fetch the task from the API
    // For now, we'll get it from the store or create mock data
    const existingTask = taskStore.tasks.find(t => t.id === taskId.value)
    if (existingTask) {
      task.value = existingTask
    } else {
      // Mock task data for demonstration
      task.value = {
        id: taskId.value,
        taskName: 'Sample Profiling Task',
        description: 'Profiling user and order tables',
        dataSourceId: 1,
        tableNames: ['users', 'orders'],
        profileTypes: ['basic', 'quality'],
        sampleSize: '10000',
        priority: 'normal',
        status: 'running',
        progress: 65,
        currentStep: 'Analyzing data quality patterns',
        createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
        startedAt: new Date(Date.now() - 1.5 * 60 * 60 * 1000).toISOString(),
        completedAt: null,
        duration: null,
        errorMessage: null,
        errorDetails: null
      }
    }
  } catch (error) {
    ElMessage.error('Failed to load task details')
  } finally {
    loading.value = false
  }
}

const loadExecutionLog = async () => {
  loadingLog.value = true
  try {
    // Mock execution log data
    executionLog.value = [
      {
        timestamp: new Date(Date.now() - 90 * 60 * 1000).toISOString(),
        level: 'info',
        message: 'Task started successfully'
      },
      {
        timestamp: new Date(Date.now() - 85 * 60 * 1000).toISOString(),
        level: 'info',
        message: 'Connected to data source: PostgreSQL Database'
      },
      {
        timestamp: new Date(Date.now() - 80 * 60 * 1000).toISOString(),
        level: 'info',
        message: 'Starting analysis of table: users'
      },
      {
        timestamp: new Date(Date.now() - 75 * 60 * 1000).toISOString(),
        level: 'info',
        message: 'Completed basic statistics for users table'
      },
      {
        timestamp: new Date(Date.now() - 70 * 60 * 1000).toISOString(),
        level: 'warning',
        message: 'Found 15 null values in email column'
      },
      {
        timestamp: new Date(Date.now() - 65 * 60 * 1000).toISOString(),
        level: 'info',
        message: 'Starting analysis of table: orders'
      },
      {
        timestamp: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
        level: 'info',
        message: 'Analyzing data quality patterns...'
      }
    ]
  } catch (error) {
    ElMessage.error('Failed to load execution log')
  } finally {
    loadingLog.value = false
  }
}

const refreshStatus = async () => {
  await loadTask()
}

const refreshLog = async () => {
  await loadExecutionLog()
}

const handleAction = async (command: string) => {
  if (!task.value) return
  
  switch (command) {
    case 'viewReport':
      router.push({
        path: '/reports',
        query: { taskId: task.value.id }
      })
      break
    case 'stop':
      await stopTask()
      break
    case 'retry':
      await retryTask()
      break
    case 'delete':
      await deleteTask()
      break
  }
}

const stopTask = async () => {
  if (!task.value) return
  
  try {
    await ElMessageBox.confirm(
      'Are you sure you want to stop this task? This action cannot be undone.',
      'Confirm Stop',
      {
        confirmButtonText: 'Stop Task',
        cancelButtonText: 'Cancel',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    // In a real implementation, you would call the stop API
    ElMessage.success('Task stop request sent')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Failed to stop task')
    }
  }
}

const retryTask = async () => {
  if (!task.value) return
  
  try {
    await ElMessageBox.confirm(
      'Are you sure you want to retry this task?',
      'Confirm Retry',
      {
        confirmButtonText: 'Retry',
        cancelButtonText: 'Cancel',
        type: 'info'
      }
    )
    
    // In a real implementation, you would call the retry API
    ElMessage.success('Task retry initiated')
    await refreshStatus()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Failed to retry task')
    }
  }
}

const deleteTask = async () => {
  if (!task.value) return
  
  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete the task "${task.value.taskName}"? This action cannot be undone.`,
      'Confirm Delete',
      {
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    await taskStore.deleteTask(task.value.id)
    ElMessage.success('Task deleted successfully')
    router.push('/tasks')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Failed to delete task')
    }
  }
}

// Auto-refresh for running tasks
let refreshInterval: NodeJS.Timeout | null = null

const startAutoRefresh = () => {
  refreshInterval = setInterval(() => {
    if (task.value?.status === 'running') {
      refreshStatus()
      refreshLog()
    }
  }, 10000) // Refresh every 10 seconds
}

const stopAutoRefresh = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
    refreshInterval = null
  }
}

onMounted(() => {
  loadTask()
  loadExecutionLog()
  dataSourceStore.fetchDataSources()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.task-status {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.title-section {
  margin-left: 8px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.page-description {
  color: #606266;
  font-size: 14px;
  margin: 0;
}

.header-right {
  display: flex;
  gap: 12px;
}

.loading-container {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.status-overview {
  margin-bottom: 20px;
}

.status-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 24px;
  border-left: 4px solid #e4e7ed;
}

.status-card.status-completed {
  border-left-color: #67c23a;
}

.status-card.status-running {
  border-left-color: #e6a23c;
}

.status-card.status-failed {
  border-left-color: #f56c6c;
}

.status-card.status-pending {
  border-left-color: #909399;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.status-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-icon {
  font-size: 32px;
}

.status-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.status-subtitle {
  color: #606266;
  margin: 0;
}

.progress-section {
  margin-top: 20px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.current-step {
  margin-top: 8px;
  font-size: 14px;
  color: #909399;
}

.error-section {
  margin-top: 20px;
}

.error-details {
  margin-top: 12px;
}

.error-details summary {
  cursor: pointer;
  font-weight: 500;
  margin-bottom: 8px;
}

.error-details pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.info-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  font-size: 14px;
}

.label {
  color: #909399;
  font-weight: 500;
  min-width: 100px;
}

.value {
  color: #303133;
  text-align: right;
  flex: 1;
  margin-left: 16px;
}

.table-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.table-tag,
.profile-tag {
  margin: 0;
}

.log-section {
  margin-bottom: 20px;
}

.card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #ebeef5;
}

.log-content {
  padding: 20px 24px;
  max-height: 400px;
  overflow-y: auto;
}

.loading-state,
.empty-log {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #909399;
}

.log-entries {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.log-entry {
  display: grid;
  grid-template-columns: 80px 80px 1fr;
  gap: 12px;
  align-items: center;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 14px;
  border-left: 3px solid #e4e7ed;
}

.log-entry.log-error {
  background: #fef0f0;
  border-left-color: #f56c6c;
}

.log-entry.log-warning {
  background: #fdf6ec;
  border-left-color: #e6a23c;
}

.log-entry.log-info {
  background: #f0f9ff;
  border-left-color: #409eff;
}

.log-entry.log-debug {
  background: #f5f7fa;
  border-left-color: #909399;
}

.log-timestamp {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}

.log-level {
  display: flex;
  justify-content: center;
}

.log-message {
  color: #303133;
}

.rotating {
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.error-state {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 40px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .header-right {
    align-self: stretch;
  }
  
  .status-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .log-entry {
    grid-template-columns: 1fr;
    gap: 4px;
  }
  
  .log-timestamp,
  .log-level {
    justify-self: start;
  }
}
</style>