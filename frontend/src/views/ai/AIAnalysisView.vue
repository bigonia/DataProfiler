<template>
  <div class="ai-analysis-container">
    <PageHeader title="AI智能分析" subtitle="基于数据剖析结果进行智能问答分析" />
    
    <div class="analysis-content">
      <!-- Left Panel: Analysis Scope Settings -->
      <div class="analysis-settings">
        <el-card class="settings-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon><Setting /></el-icon>
              <span>分析范围设置</span>
            </div>
          </template>
          
          <!-- Task Selection -->
          <div class="setting-section">
            <label class="setting-label">选择分析任务</label>
            <TaskSelector
              v-model="selectedTaskId"
              :loading="isLoadingTasks"
              :tasks="profilingTasks"
              @change="handleTaskChange"
            />
          </div>
          
          <!-- AI Service Status -->
          <div class="service-status">
            <el-alert
              v-if="!aiServiceAvailable"
              title="AI服务不可用"
              type="warning"
              description="请检查AI服务连接状态"
              show-icon
              :closable="false"
            />
            <el-alert
              v-else-if="selectedTaskId"
              title="准备就绪"
              type="success"
              description="可以开始AI分析"
              show-icon
              :closable="false"
            />
          </div>
        </el-card>
      </div>
      
      <!-- Right Panel: Chat Interface -->
      <div class="chat-interface">
        <!-- Chat Window -->
        <div class="chat-window-container">
          <ChatWindow
            :messages="messages"
            :is-streaming="isStreaming"
            :empty-message="getEmptyMessage()"
          />
        </div>
        
        <!-- Message Input -->
        <div class="message-input-container">
          <MessageInput
            :disabled="!canStartAnalysis"
            :loading="isStreaming"
            :placeholder="getInputPlaceholder()"
            @send="handleSendMessage"
            @stop="handleStopAnalysis"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useAIAnalysisStore } from '@/stores'
import { ElMessage } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'

// Components
import PageHeader from '@/components/PageHeader.vue'
import TaskSelector from './components/TaskSelector.vue'
import ChatWindow from './components/ChatWindow.vue'
import MessageInput from './components/MessageInput.vue'

// Store
const aiStore = useAIAnalysisStore()
const {
  profilingTasks,
  selectedTaskId,
  messages,
  isStreaming,
  isLoadingTasks,
  aiServiceAvailable,
  canStartAnalysis,
  selectedTask
} = storeToRefs(aiStore)

// Computed
const getEmptyMessage = () => {
  if (!selectedTaskId.value) {
    return '请选择一个已完成的分析任务...'
  }
  return '开始与AI对话，分析您的数据！'
}

const getInputPlaceholder = () => {
  if (!selectedTaskId.value) {
    return '请先选择分析任务'
  }
  if (!aiServiceAvailable.value) {
    return 'AI服务不可用'
  }
  if (isStreaming.value) {
    return 'AI正在分析中...'
  }
  return '请输入您的问题...'
}

// Event Handlers
const handleTaskChange = (taskId: string | null) => {
  aiStore.setSelectedTask(taskId)
}

const handleSendMessage = async (message: string) => {
  if (!message.trim()) {
    ElMessage.warning('请输入问题内容')
    return
  }
  
  if (!canStartAnalysis.value) {
    ElMessage.warning('请先选择分析任务')
    return
  }
  
  try {
    await aiStore.startAnalysis(message)
  } catch (error) {
    console.error('Failed to send message:', error)
  }
}

const handleStopAnalysis = () => {
  aiStore.stopAnalysis()
}

// Lifecycle
onMounted(async () => {
  // Initialize data
  await Promise.all([
    aiStore.fetchAllCompletedTasks(),
    aiStore.checkAIServiceHealth()
  ])
})

onUnmounted(() => {
  // Clean up any ongoing streams
  if (isStreaming.value) {
    aiStore.stopAnalysis()
  }
})
</script>

<style scoped>
.ai-analysis-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.analysis-content {
  flex: 1;
  display: flex;
  gap: 20px;
  padding: 20px;
  min-height: 0;
}

.analysis-settings {
  width: 320px;
  flex-shrink: 0;
}

.settings-card {
  height: fit-content;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
}

.setting-section {
  margin-bottom: 24px;
}

.setting-section:last-of-type {
  margin-bottom: 16px;
}

.setting-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}

.service-status {
  margin-top: 20px;
}

.chat-interface {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-window-container {
  flex: 1;
  min-height: 0;
  margin-bottom: 16px;
}

.message-input-container {
  flex-shrink: 0;
}

/* Responsive Design */
@media (max-width: 1200px) {
  .analysis-content {
    flex-direction: column;
  }
  
  .analysis-settings {
    width: 100%;
  }
  
  .settings-card {
    max-height: none;
  }
}

@media (max-width: 768px) {
  .analysis-content {
    padding: 12px;
    gap: 12px;
  }
}
</style>