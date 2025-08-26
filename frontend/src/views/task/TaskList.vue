<template>
  <div class="task-list">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">Profiling Tasks</h2>
        <p class="page-description">Monitor and manage your data profiling tasks</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="$router.push('/tasks/create')">
          <el-icon><Plus /></el-icon>
          Create Task
        </el-button>
      </div>
    </div>
    
    <!-- Statistics Cards -->
    <div class="stats-row">
      <div class="stat-card" style="border-left: 4px solid #67c23a">
        <div class="stat-content">
          <div class="stat-value">{{ completedTasks }}</div>
          <div class="stat-label">Completed</div>
        </div>
        <div class="stat-icon">
          <el-icon color="#67c23a" size="24"><CircleCheck /></el-icon>
        </div>
      </div>
      
      <div class="stat-card" style="border-left: 4px solid #e6a23c">
        <div class="stat-content">
          <div class="stat-value">{{ runningTasks }}</div>
          <div class="stat-label">Running</div>
        </div>
        <div class="stat-icon">
          <el-icon color="#e6a23c" size="24"><Loading /></el-icon>
        </div>
      </div>
      
      <div class="stat-card" style="border-left: 4px solid #f56c6c">
        <div class="stat-content">
          <div class="stat-value">{{ failedTasks }}</div>
          <div class="stat-label">Failed</div>
        </div>
        <div class="stat-icon">
          <el-icon color="#f56c6c" size="24"><CircleClose /></el-icon>
        </div>
      </div>
      
      <div class="stat-card" style="border-left: 4px solid #909399">
        <div class="stat-content">
          <div class="stat-value">{{ pendingTasks }}</div>
          <div class="stat-label">Pending</div>
        </div>
        <div class="stat-icon">
          <el-icon color="#909399" size="24"><Clock /></el-icon>
        </div>
      </div>
    </div>
    
    <!-- Filters -->
    <div class="card">
      <div class="filter-section">
        <div class="filter-row">
          <div class="filter-item">
            <el-input
              v-model="searchQuery"
              placeholder="Search tasks..."
              clearable
              @input="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          
          <div class="filter-item">
            <el-select
              v-model="selectedStatus"
              placeholder="Filter by status"
              clearable
              @change="handleStatusFilter"
            >
              <el-option label="All Status" value="" />
              <el-option label="Pending" value="pending" />
              <el-option label="Running" value="running" />
              <el-option label="Completed" value="completed" />
              <el-option label="Failed" value="failed" />
            </el-select>
          </div>
          
          <div class="filter-item">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="To"
              start-placeholder="Start date"
              end-placeholder="End date"
              @change="handleDateFilter"
            />
          </div>
          
          <div class="filter-item">
            <el-button @click="refreshList">
              <el-icon><Refresh /></el-icon>
              Refresh
            </el-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Task Cards -->
    <div class="task-grid" v-loading="loading">
      <div
        v-for="task in filteredTasks"
        :key="task.id"
        class="task-card"
        :class="getTaskCardClass(task.status)"
      >
        <div class="card-header">
          <div class="task-info">
            <div class="task-title-row">
              <h3 class="task-name">{{ task.name || task.taskName }}</h3>
              <div class="task-status">
                <el-tag
                  :type="getStatusType(task.status)"
                  :effect="task.status === 'RUNNING' ? 'plain' : 'dark'"
                  :class="getStatusClass(task.status)"
                >
                  <el-icon v-if="task.status === 'RUNNING'" class="rotating">
                    <Loading />
                  </el-icon>
                  <el-icon v-else-if="task.status === 'COMPLETED'" class="status-icon">
                    <CircleCheck />
                  </el-icon>
                  <el-icon v-else-if="task.status === 'FAILED'" class="status-icon">
                    <CircleClose />
                  </el-icon>
                  <el-icon v-else-if="task.status === 'PENDING'" class="status-icon">
                    <Clock />
                  </el-icon>
                  {{ formatStatus(task.status) }}
                </el-tag>
              </div>
            </div>
            <p class="task-description">{{ task.description || 'No description' }}</p>
          </div>
        </div>
        
        <div class="card-content">
          <div class="task-details">
            <div class="detail-item">
              <span class="label">Task ID:</span>
              <span class="value task-id">{{ task.taskId }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Total Data Sources:</span>
              <span class="value">{{ task.totalDataSources || 0 }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Processed:</span>
              <span class="value">{{ task.processedDataSources || 0 }} / {{ task.totalDataSources || 0 }}</span>
            </div>
            <div class="detail-item">
              <span class="label">All Sources Processed:</span>
              <span class="value">
                <el-tag :type="task.allDataSourcesProcessed ? 'success' : 'warning'" size="small">
                  {{ task.allDataSourcesProcessed ? 'Yes' : 'No' }}
                </el-tag>
              </span>
            </div>
            <div class="detail-item">
              <span class="label">Created:</span>
              <span class="value">{{ formatDate(task.createdAt, 'datetime') }}</span>
            </div>
            <div v-if="task.completedAt" class="detail-item">
              <span class="label">Completed:</span>
              <span class="value">{{ formatDate(task.completedAt, 'datetime') }}</span>
            </div>
            <div v-if="task.completedAt && task.createdAt" class="detail-item">
              <span class="label">Duration:</span>
              <span class="value">{{ calculateDuration(task.createdAt, task.completedAt) }}</span>
            </div>
          </div>
          
          <!-- Task Info Section -->
          <div v-if="task.info" class="task-info-section">
            <h4 class="section-title">Task Information</h4>
            <div class="info-content">
              <el-alert
                :title="task.info"
                :type="task.status === 'COMPLETED' ? 'success' : task.status === 'FAILED' ? 'error' : 'info'"
                :closable="false"
                show-icon
              />
            </div>
          </div>
          
          <!-- Request Payload Summary -->
          <div v-if="task.requestPayload" class="payload-section">
            <h4 class="section-title">Configuration Summary</h4>
            <div class="payload-summary">
              <div class="summary-item">
                <span class="summary-label">Data Sources:</span>
                <span class="summary-value">{{ getDataSourcesFromPayload(task.requestPayload) }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">Total Schemas:</span>
                <span class="summary-value">{{ getSchemasCountFromPayload(task.requestPayload) }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">Total Tables:</span>
                <span class="summary-value">{{ getTablesCountFromPayload(task.requestPayload) }}</span>
              </div>
            </div>
            
            <!-- Expandable detailed payload -->
            <el-collapse class="payload-details">
              <el-collapse-item title="View Detailed Configuration" name="payload">
                <div class="payload-content">
                  <pre>{{ formatPayload(task.requestPayload) }}</pre>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
          
          <!-- Data Sources Information -->
          <div v-if="task.dataSources && task.dataSources.length > 0" class="data-sources-section">
            <h4 class="section-title">Data Sources</h4>
            <div class="data-sources-list">
              <div 
                v-for="dataSource in task.dataSources" 
                :key="dataSource.id" 
                class="data-source-item"
              >
                <div class="data-source-info">
                  <div class="data-source-header">
                    <span class="data-source-name">{{ dataSource.name }}</span>
                    <el-tag 
                      :type="dataSource.active ? 'success' : 'info'" 
                      size="small"
                    >
                      {{ dataSource.active ? 'Active' : 'Inactive' }}
                    </el-tag>
                  </div>
                  <div class="data-source-details">
                    <span class="data-source-type">{{ dataSource.type }}</span>
                    <span class="data-source-id">ID: {{ dataSource.sourceId }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- Progress Bar for Running Tasks -->
          <div v-if="task.status === 'running' && task.progress !== undefined" class="task-progress">
            <div class="progress-info">
              <span>Progress</span>
              <span>{{ task.progress }}%</span>
            </div>
            <el-progress
              :percentage="task.progress"
              :status="task.progress === 100 ? 'success' : undefined"
              :stroke-width="6"
            />
          </div>
          
          <!-- Error Message for Failed Tasks -->
          <div v-if="task.status === 'failed' && task.errorMessage" class="error-message">
            <el-alert
              :title="task.errorMessage"
              type="error"
              :closable="false"
              show-icon
            />
          </div>
        </div>
        
        <div class="card-footer">
          <div class="footer-info">
            <span v-if="task.duration" class="duration">
              Duration: {{ formatDuration(task.duration) }}
            </span>
          </div>
          
          <div class="footer-actions">
            <el-button
              v-if="task.status === 'completed'"
              type="success"
              size="small"
              @click="viewReport(task.taskId)"
            >
              <el-icon><View /></el-icon>
              View Report
            </el-button>
            
            <el-button
              v-if="task.status === 'running'"
              type="info"
              size="small"
              @click="viewTaskStatus(task.id)"
            >
              <el-icon><Monitor /></el-icon>
              Monitor
            </el-button>
            
            <el-dropdown @command="handleTaskAction">
              <el-button type="text" size="small">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="{ action: 'details', id: task.id }">
                    <el-icon><View /></el-icon>
                    View Details
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="task.status === 'completed'"
                    :command="{ action: 'report', id: task.taskId }"
                  >
                    <el-icon><DataAnalysis /></el-icon>
                    View Report
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="task.status === 'failed'"
                    :command="{ action: 'retry', id: task.id }"
                  >
                    <el-icon><RefreshRight /></el-icon>
                    Retry
                  </el-dropdown-item>
                  <el-dropdown-item
                    :command="{ action: 'delete', id: task.id }"
                    divided
                  >
                    <el-icon><Delete /></el-icon>
                    Delete
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Empty State -->
    <div v-if="!loading && filteredTasks.length === 0" class="empty-state">
      <el-icon class="empty-state-icon"><Operation /></el-icon>
      <div class="empty-state-text">
        <h3>No tasks found</h3>
        <p>{{ searchQuery ? 'Try adjusting your search criteria' : 'Create your first profiling task to get started' }}</p>
      </div>
      <el-button
        v-if="!searchQuery"
        type="primary"
        @click="$router.push('/tasks/create')"
      >
        <el-icon><Plus /></el-icon>
        Create Task
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  CircleCheck,
  Loading,
  CircleClose,
  Clock,
  Search,
  Refresh,
  View,
  Monitor,
  MoreFilled,
  RefreshRight,
  Delete,
  Operation,
  DataAnalysis
} from '@element-plus/icons-vue'
import { useTaskStore } from '@/stores/task'
import { useDataSourceStore } from '@/stores/datasource'
import { formatDate, formatDuration, debounce } from '@/utils'
import type { ProfilingTask } from '@/types'

const router = useRouter()
const taskStore = useTaskStore()
const dataSourceStore = useDataSourceStore()

const loading = ref(false)
const searchQuery = ref('')
const selectedStatus = ref('')
const dateRange = ref<[Date, Date] | null>(null)

const filteredTasks = computed(() => {
  let tasks = taskStore.tasks
  
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    tasks = tasks.filter(task => 
      task.taskName.toLowerCase().includes(query) ||
      task.description?.toLowerCase().includes(query)
    )
  }
  
  if (selectedStatus.value) {
    tasks = tasks.filter(task => task.status === selectedStatus.value)
  }
  
  if (dateRange.value) {
    const [start, end] = dateRange.value
    tasks = tasks.filter(task => {
      const createdDate = new Date(task.createdAt)
      return createdDate >= start && createdDate <= end
    })
  }
  
  return tasks.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
})

const completedTasks = computed(() => 
  taskStore.tasks.filter(task => task.status?.toUpperCase() === 'COMPLETED').length
)

const runningTasks = computed(() => 
  taskStore.tasks.filter(task => task.status?.toUpperCase() === 'RUNNING').length
)

const failedTasks = computed(() => 
  taskStore.tasks.filter(task => task.status?.toUpperCase() === 'FAILED').length
)

const pendingTasks = computed(() => 
  taskStore.tasks.filter(task => task.status?.toUpperCase() === 'PENDING').length
)

const getStatusType = (status: string) => {
  switch (status.toUpperCase()) {
    case 'COMPLETED':
      return 'success'
    case 'RUNNING':
      return 'warning'
    case 'FAILED':
      return 'danger'
    case 'PENDING':
    default:
      return 'info'
  }
}

const getStatusClass = (status: string) => {
  switch (status.toUpperCase()) {
    case 'COMPLETED':
      return 'status-completed'
    case 'RUNNING':
      return 'status-running'
    case 'FAILED':
      return 'status-failed'
    case 'PENDING':
      return 'status-pending'
    default:
      return ''
  }
}

const formatStatus = (status: string) => {
  return status.charAt(0).toUpperCase() + status.slice(1)
}

const getTaskCardClass = (status: string) => {
  const upperStatus = status.toUpperCase()
  return {
    'task-completed': upperStatus === 'COMPLETED',
    'task-running': upperStatus === 'RUNNING',
    'task-failed': upperStatus === 'FAILED',
    'task-pending': upperStatus === 'PENDING'
  }
}

const getDataSourceName = (dataSourceId: number) => {
  const dataSource = dataSourceStore.dataSources.find(ds => ds.id === dataSourceId)
  return dataSource?.name || `DataSource #${dataSourceId}`
}

// Calculate duration between two dates
const calculateDuration = (startTime: string, endTime: string) => {
  const start = new Date(startTime)
  const end = new Date(endTime)
  const diffMs = end.getTime() - start.getTime()
  
  const seconds = Math.floor(diffMs / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  
  if (hours > 0) {
    return `${hours}h ${minutes % 60}m ${seconds % 60}s`
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`
  } else {
    return `${seconds}s`
  }
}

// Extract data sources from request payload
const getDataSourcesFromPayload = (payload: string) => {
  try {
    const parsed = JSON.parse(payload)
    const datasources = parsed.datasources || {}
    return Object.keys(datasources).length
  } catch (error) {
    return 'N/A'
  }
}

// Extract schemas count from request payload
const getSchemasCountFromPayload = (payload: string) => {
  try {
    const parsed = JSON.parse(payload)
    const datasources = parsed.datasources || {}
    let totalSchemas = 0
    
    Object.values(datasources).forEach((ds: any) => {
      if (ds.schemas) {
        totalSchemas += Object.keys(ds.schemas).length
      }
    })
    
    return totalSchemas
  } catch (error) {
    return 'N/A'
  }
}

// Extract tables count from request payload
const getTablesCountFromPayload = (payload: string) => {
  try {
    const parsed = JSON.parse(payload)
    const datasources = parsed.datasources || {}
    let totalTables = 0
    
    Object.values(datasources).forEach((ds: any) => {
      if (ds.schemas) {
        Object.values(ds.schemas).forEach((tables: any) => {
          if (Array.isArray(tables)) {
            totalTables += tables.length
          }
        })
      }
    })
    
    return totalTables
  } catch (error) {
    return 'N/A'
  }
}

// Format payload for display
const formatPayload = (payload: string) => {
  try {
    const parsed = JSON.parse(payload)
    return JSON.stringify(parsed, null, 2)
  } catch (error) {
    return payload
  }
}

const handleSearch = debounce(() => {
  // Search is handled by computed property
}, 300)

const handleStatusFilter = () => {
  // Filter is handled by computed property
}

const handleDateFilter = () => {
  // Filter is handled by computed property
}

const refreshList = async () => {
  loading.value = true
  try {
    await taskStore.fetchTasks()
  } finally {
    loading.value = false
  }
}

const viewReport = async (taskId: string) => {
  try {
    // Navigate to reports page and filter by taskId
    router.push({
      path: '/reports',
      query: { taskId: taskId }
    })
  } catch (error) {
    console.error('Failed to navigate to report:', error)
    ElMessage.error('Failed to open report')
  }
}

const viewTaskStatus = (taskId: number) => {
  router.push(`/tasks/${taskId}/status`)
}

const handleTaskAction = async (command: { action: string; id: string | number }) => {
  const { action, id } = command
  
  switch (action) {
    case 'details':
      viewTaskStatus(typeof id === 'string' ? parseInt(id) : id)
      break
    case 'report':
      viewReport(typeof id === 'string' ? id : id.toString())
      break
    case 'retry':
      await retryTask(typeof id === 'string' ? parseInt(id) : id)
      break
    case 'delete':
      await deleteTask(typeof id === 'string' ? parseInt(id) : id)
      break
  }
}

const retryTask = async (taskId: number) => {
  try {
    const task = taskStore.tasks.find(t => t.id === taskId)
    if (!task) return
    
    await ElMessageBox.confirm(
      `Are you sure you want to retry the task "${task.taskName}"?`,
      'Confirm Retry',
      {
        confirmButtonText: 'Retry',
        cancelButtonText: 'Cancel',
        type: 'info'
      }
    )
    
    // In a real implementation, you would call the retry API
    ElMessage.success('Task retry initiated')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Failed to retry task')
    }
  }
}

const deleteTask = async (taskId: number) => {
  try {
    const task = taskStore.tasks.find(t => t.id === taskId)
    if (!task) return
    
    await ElMessageBox.confirm(
      `Are you sure you want to delete the task "${task.taskName}"? This action cannot be undone.`,
      'Confirm Delete',
      {
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    await taskStore.deleteTask(taskId)
    ElMessage.success('Task deleted successfully')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Failed to delete task')
    }
  }
}

// Auto-refresh running tasks
let refreshInterval: NodeJS.Timeout | null = null

const startAutoRefresh = () => {
  refreshInterval = setInterval(() => {
    if (runningTasks.value > 0) {
      // In a real implementation, you would fetch task status updates
      console.log('Auto-refreshing running tasks...')
    }
  }, 5000) // Refresh every 5 seconds
}

const stopAutoRefresh = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
    refreshInterval = null
  }
}

onMounted(async () => {
  try {
    await refreshList()
  } catch (error) {
    console.error('Failed to load task list:', error)
    ElMessage.error('Failed to load task list')
  }
  
  try {
    await dataSourceStore.fetchDataSources() // Load data sources for display
  } catch (error) {
    console.error('Failed to load data sources:', error)
    // Don't show error message for data sources as it's secondary data
  }
  
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.task-list {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.page-description {
  color: #606266;
  font-size: 14px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  color: #606266;
  font-size: 14px;
}

.filter-section {
  padding: 0;
}

.filter-row {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-item {
  min-width: 200px;
}

.filter-item:last-child {
  min-width: auto;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(450px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.task-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
  border-left: 4px solid #e4e7ed;
}

.task-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.15);
}

.task-card.task-completed {
  border-left-color: #67c23a;
}

.task-card.task-running {
  border-left-color: #e6a23c;
}

.task-card.task-failed {
  border-left-color: #f56c6c;
}

.task-card.task-pending {
  border-left-color: #909399;
}

.card-header {
  padding: 20px 20px 0;
}

.task-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.task-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  flex: 1;
  margin-right: 12px;
}

.task-description {
  color: #606266;
  font-size: 14px;
  margin: 0;
  line-height: 1.4;
}

.task-status {
  flex-shrink: 0;
}

.status-icon {
  margin-right: 4px;
}

.status-completed {
  background: linear-gradient(135deg, #67c23a, #85ce61) !important;
  border: none !important;
  color: white !important;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
  animation: completedPulse 2s ease-in-out;
}

.status-running {
  background: linear-gradient(135deg, #e6a23c, #f0c78a) !important;
  border: none !important;
  color: white !important;
  font-weight: 600;
}

.status-failed {
  background: linear-gradient(135deg, #f56c6c, #f89898) !important;
  border: none !important;
  color: white !important;
  font-weight: 600;
}

.status-pending {
  background: linear-gradient(135deg, #909399, #b1b3b8) !important;
  border: none !important;
  color: white !important;
  font-weight: 600;
}

@keyframes completedPulse {
  0% {
    box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
  }
  50% {
    box-shadow: 0 4px 16px rgba(103, 194, 58, 0.5);
  }
  100% {
    box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
  }
}

.card-content {
  padding: 16px 20px;
}

.task-details {
  margin-bottom: 16px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.detail-item:last-child {
  margin-bottom: 0;
}

.label {
  color: #909399;
  font-weight: 500;
}

.value {
  color: #303133;
  text-align: right;
  max-width: 60%;
  word-break: break-all;
}

.data-sources-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.data-sources-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.data-source-item {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 12px;
  border: 1px solid #ebeef5;
}

.data-source-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.data-source-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.data-source-name {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.data-source-details {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.data-source-type {
  font-weight: 500;
}

.task-id {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  word-break: break-all;
}

.task-info-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.info-content {
  margin-top: 8px;
}

.payload-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.payload-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.summary-value {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.payload-details {
  margin-top: 8px;
}

.payload-content {
  max-height: 300px;
  overflow-y: auto;
  background: #f8f9fa;
  border-radius: 4px;
  padding: 12px;
}

.payload-content pre {
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.task-progress {
  margin-top: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.error-message {
  margin-top: 16px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px 20px;
  border-top: 1px solid #ebeef5;
  margin-top: 16px;
  padding-top: 16px;
}

.duration {
  color: #909399;
  font-size: 12px;
}

.footer-actions {
  display: flex;
  gap: 8px;
  align-items: center;
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

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-state-icon {
  font-size: 64px;
  margin-bottom: 20px;
  color: #c0c4cc;
}

.empty-state-text h3 {
  font-size: 18px;
  color: #303133;
  margin-bottom: 8px;
}

.empty-state-text p {
  font-size: 14px;
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .stats-row {
    grid-template-columns: 1fr;
  }
  
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-item {
    min-width: auto;
  }
  
  .task-grid {
    grid-template-columns: 1fr;
  }
  
  .card-footer {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
}
</style>