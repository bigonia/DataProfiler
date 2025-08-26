<template>
  <div class="task-selector">
    <el-select
      :model-value="modelValue"
      placeholder="请选择分析任务"
      :loading="loading"
      :disabled="disabled || loading || tasks.length === 0"
      clearable
      filterable
      class="selector"
      @update:model-value="handleChange"
    >
      <el-option
        v-for="task in tasks"
        :key="task.taskId"
        :label="task.name"
        :value="task.taskId"
      >
        <div class="option-content">
          <div class="option-main">
            <span class="option-name">{{ task.name }}</span>
            <el-tag
              :type="getTaskStatusTag(task.status)"
              size="small"
              class="option-tag"
            >
              {{ getTaskStatusLabel(task.status) }}
            </el-tag>
          </div>
          <div class="option-meta">
            <span class="option-time">{{ formatTime(task.createdAt) }}</span>
            <span class="option-duration" v-if="task.duration">
              耗时: {{ formatDuration(task.duration) }}
            </span>
          </div>
        </div>
      </el-option>
    </el-select>
    
    <!-- Task Info -->
    <div v-if="selectedTask" class="task-info">
      <div class="info-item">
        <span class="info-label">状态:</span>
        <el-tag :type="getTaskStatusTag(selectedTask.status)" size="small">
          {{ getTaskStatusLabel(selectedTask.status) }}
        </el-tag>
      </div>
      <div class="info-item">
        <span class="info-label">创建时间:</span>
        <span class="info-value">{{ formatTime(selectedTask.createdAt) }}</span>
      </div>
      <div class="info-item" v-if="selectedTask.completedAt">
        <span class="info-label">完成时间:</span>
        <span class="info-value">{{ formatTime(selectedTask.completedAt) }}</span>
      </div>
      <div class="info-item" v-if="selectedTask.duration">
        <span class="info-label">执行时长:</span>
        <span class="info-value">{{ formatDuration(selectedTask.duration) }}</span>
      </div>
      <div class="info-item" v-if="selectedTask.description">
        <span class="info-label">描述:</span>
        <span class="info-value">{{ selectedTask.description }}</span>
      </div>
    </div>
    
    <!-- Empty State -->
    <div v-if="!loading && !disabled && tasks.length === 0" class="empty-state">
      <el-empty
        description="暂无已完成的分析任务"
        :image-size="60"
      >
        <template #description>
          <span class="empty-description">请先运行数据剖析任务</span>
        </template>
      </el-empty>
    </div>
    
    <!-- Disabled State -->
    <div v-if="disabled && !loading" class="disabled-state">
      <el-alert
        title="请先选择数据源"
        type="info"
        :closable="false"
        show-icon
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProfilingTaskForAI } from '@/types'

// Props
interface Props {
  modelValue: string | null
  loading?: boolean
  disabled?: boolean
  tasks: ProfilingTaskForAI[]
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  disabled: false
})

// Emits
interface Emits {
  (e: 'update:modelValue', value: string | null): void
  (e: 'change', value: string | null): void
}

const emit = defineEmits<Emits>()

// Computed
const selectedTask = computed(() => {
  if (!props.modelValue) return null
  return props.tasks.find(task => task.taskId === props.modelValue) || null
})

// Methods
const handleChange = (value: string | null) => {
  emit('update:modelValue', value)
  emit('change', value)
}

const getTaskStatusLabel = (status: string): string => {
  const statusMap: Record<string, string> = {
    'COMPLETED': '已完成',
    'RUNNING': '运行中',
    'FAILED': '失败',
    'PENDING': '等待中',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

const getTaskStatusTag = (status: string): string => {
  const tagMap: Record<string, string> = {
    'COMPLETED': 'success',
    'RUNNING': 'primary',
    'FAILED': 'danger',
    'PENDING': 'warning',
    'CANCELLED': 'info'
  }
  return tagMap[status] || 'info'
}

const formatTime = (timestamp: string | number): string => {
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const formatDuration = (duration: number): string => {
  if (duration < 1000) {
    return `${duration}ms`
  }
  
  const seconds = Math.floor(duration / 1000)
  if (seconds < 60) {
    return `${seconds}秒`
  }
  
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  
  if (minutes < 60) {
    return remainingSeconds > 0 ? `${minutes}分${remainingSeconds}秒` : `${minutes}分钟`
  }
  
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  
  return remainingMinutes > 0 ? `${hours}小时${remainingMinutes}分钟` : `${hours}小时`
}
</script>

<style scoped>
.task-selector {
  width: 100%;
}

.selector {
  width: 100%;
}

.option-content {
  width: 100%;
}

.option-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.option-name {
  font-weight: 500;
  color: #303133;
  flex: 1;
  margin-right: 8px;
}

.option-tag {
  flex-shrink: 0;
}

.option-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.option-time,
.option-duration {
  white-space: nowrap;
}

.task-info {
  margin-top: 12px;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.info-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  min-width: 70px;
  margin-right: 8px;
  flex-shrink: 0;
}

.info-value {
  font-size: 13px;
  color: #303133;
  word-break: break-all;
  line-height: 1.4;
}

.empty-state,
.disabled-state {
  margin-top: 20px;
}

.empty-state {
  text-align: center;
}

.empty-description {
  color: #909399;
  font-size: 14px;
}

/* Responsive */
@media (max-width: 768px) {
  .option-main {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .option-name {
    margin-right: 0;
  }
  
  .option-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .info-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .info-label {
    min-width: auto;
    margin-right: 0;
  }
}
</style>