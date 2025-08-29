<template>
  <div class="ai-analysis-container">
    <!-- Header -->

    
    <!-- Main Content -->
    <div class="analysis-content">
      <!-- Left Panel: Settings -->
      <div class="settings-panel">
        <div class="settings-card">
          <!-- Task Selection -->
          <div class="setting-section">
            <div class="section-header">
              <el-icon><FolderOpened /></el-icon>
              <span class="section-title">分析任务</span>
            </div>
            
            <TaskSelector
              v-model="selectedTaskId"
              :loading="isLoadingTasks"
              :tasks="profilingTasks"
              @change="handleTaskChange"
              class="task-selector"
            />
            
            <!-- Task Info -->
            <div v-if="selectedTask" class="task-info">
              <div class="info-item">
                <span class="info-label">任务名称:</span>
                <span class="info-value">{{ selectedTask.name }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">数据源:</span>
                <span class="info-value">{{ selectedTask.dataSource }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">完成时间:</span>
                <span class="info-value">{{ formatTime(selectedTask.completedAt) }}</span>
              </div>
            </div>
          </div>
          
          <!-- Workflow Status Section -->
          <div class="setting-section">
            <div class="section-header">
              <el-icon><Operation /></el-icon>
              <span class="section-title">AI工作流状态</span>
            </div>
            
            <div class="workflow-status-card">
              <WorkflowDisplay 
                class="workflow-visualization" 
              />
            </div>
          </div>

        </div>
      </div>
      
      <!-- Right Panel: Chat Interface -->
      <div class="chat-panel">
        <!-- Chat Window -->
        <div class="chat-window-wrapper">
          <ChatWindow
            :messages="messages"
            :is-streaming="isStreaming"
            :empty-message="getEmptyMessage()"
            @clear-messages="handleClearMessages"
            @regenerate="handleRegenerateMessage"
            @like-message="handleLikeMessage"
            @dislike-message="handleDislikeMessage"
            @tts="handleTTS"
            @add-annotation="handleAddAnnotation"
            class="chat-window"
          />
        </div>
        
        <!-- Message Input -->
        <div class="input-wrapper">
          <MessageInput
            :disabled="!canStartAnalysis"
            :loading="isStreaming"
            :placeholder="getInputPlaceholder()"
            :auto-focus="!!selectedTaskId"
            :suggested-questions="suggestedQuestions"
            :show-suggested-questions="showSuggestedQuestions"
            :uploaded-files="uploadedFiles"
            :max-file-size="maxFileSize"
            :allowed-file-types="allowedFileTypes"
            @send="handleSendMessage"
            @voice-input="handleVoiceInput"
            @file-upload="handleFileUpload"
            @file-remove="handleFileRemove"
            @suggested-question="handleSuggestedQuestion"
            @focus="handleInputFocus"
            @blur="handleInputBlur"
            @clear="handleInputClear"
            class="message-input"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  CircleCheckFilled,
  CircleCloseFilled,
  FolderOpened,
  Operation,
  Lightning,
  Refresh,
  Delete
} from '@element-plus/icons-vue'

// Components
import TaskSelector from './components/TaskSelector.vue'
import ChatWindow from './components/ChatWindow.vue'
import MessageInput from './components/MessageInput.vue'
import WorkflowDisplay from './components/WorkflowDisplay.vue'

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

// Reactive data
const uploadedFiles = ref<File[]>([])
const suggestedQuestions = ref<string[]>([
  '分析数据质量情况',
  '查看数据分布特征',
  '识别异常数据',
  '生成数据报告'
])
const showSuggestedQuestions = ref(true)
const maxFileSize = ref(10 * 1024 * 1024) // 10MB
const allowedFileTypes = ref(['.csv', '.xlsx', '.json', '.txt'])

// Computed
const currentWorkflowData = computed(() => {
  const lastMessage = messages.value[messages.value.length - 1]
  if (lastMessage && lastMessage.role === 'assistant' && lastMessage.workflowNodes) {
    return {
      nodes: lastMessage.workflowNodes,
      status: getWorkflowStatus(lastMessage.workflowNodes),
      progress: calculateWorkflowProgress(lastMessage.workflowNodes)
    }
  }
  return null
})

const getWorkflowStatus = (nodes: any[]) => {
  if (nodes.length === 0) return 'pending'
  
  const hasRunning = nodes.some(node => node.status === 'running')
  const hasFailed = nodes.some(node => node.status === 'failed')
  const allCompleted = nodes.every(node => node.status === 'succeeded')
  
  if (hasRunning) return 'running'
  if (hasFailed) return 'failed'
  if (allCompleted) return 'completed'
  return 'pending'
}

const calculateWorkflowProgress = (nodes: any[]) => {
  if (nodes.length === 0) return 0
  
  const completedNodes = nodes.filter(node => 
    node.status === 'succeeded' || node.status === 'failed'
  ).length
  
  return Math.round((completedNodes / nodes.length) * 100)
}

const getEmptyMessage = () => {
  if (!selectedTaskId.value) {
    return '请选择一个已完成的分析任务开始对话'
  }
  if (!aiServiceAvailable.value) {
    return 'AI服务暂时不可用，请稍后再试'
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
  return '输入您的问题...'
}

// Methods
const formatTime = (timestamp?: number) => {
  if (!timestamp) return '未知'
  
  const date = new Date(timestamp)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Event Handlers
const handleTaskChange = (taskId: string | null) => {
  aiStore.setSelectedTask(taskId)
  if (taskId) {
    ElMessage.success('已选择分析任务，可以开始对话了')
  }
}

const handleSendMessage = async (message: string, files?: File[]) => {
  if (!message.trim()) {
    ElMessage.warning('请输入问题内容')
    return
  }
  
  if (!canStartAnalysis.value) {
    ElMessage.warning('请先选择分析任务')
    return
  }
  
  try {
    await aiStore.startAnalysis(message, files)
  } catch (error) {
    console.error('Failed to send message:', error)
    ElMessage.error('发送消息失败，请重试')
  }
}

const handleVoiceInput = () => {
  ElMessage.info('语音输入功能开发中...')
}

const handleFileUpload = (files: File[]) => {
  uploadedFiles.value.push(...files)
  ElMessage.success(`已上传 ${files.length} 个文件`)
}

const handleFileRemove = (fileIndex: number) => {
  uploadedFiles.value.splice(fileIndex, 1)
}

const handleSuggestedQuestion = (question: string) => {
  handleSendMessage(question)
  showSuggestedQuestions.value = false
}

const handleInputFocus = () => {
  // Handle input focus event
}

const handleInputBlur = () => {
  // Handle input blur event
}

const handleInputClear = () => {
  // Input cleared
}

const handleLikeMessage = (messageId: string) => {
  console.log('Like message:', messageId)
  // TODO: Implement message rating
}

const handleDislikeMessage = (messageId: string) => {
  console.log('Dislike message:', messageId)
  // TODO: Implement message rating
}

const handleTTS = async (content: string) => {
  // TTS service not implemented yet
  ElMessage.info('语音播放功能开发中...')
}

const handleAddAnnotation = (messageId: string, annotation: string) => {
  if (!annotation.trim()) {
    ElMessage.warning('标注内容不能为空')
    return
  }
  
  // Annotation service not implemented yet
  ElMessage.info('标注功能开发中...')
  console.log('Annotation request:', { messageId, annotation })
}

const handleRefreshTasks = async () => {
  try {
    await aiStore.fetchAllCompletedTasks()
    ElMessage.success('任务列表已刷新')
  } catch (error) {
    ElMessage.error('刷新任务失败')
  }
}

const handleClearMessages = async () => {
  if (messages.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(
      '确定要清空所有对话记录吗？此操作不可撤销。',
      '清空对话',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    aiStore.clearMessages()
    ElMessage.success('对话记录已清空')
  } catch {
    // User cancelled
  }
}

const handleRegenerateMessage = (messageId: string) => {
  // Regenerate message logic
  ElMessage.info('重新生成功能开发中...')
}

// Lifecycle
onMounted(async () => {
  try {
    await Promise.all([
      aiStore.fetchAllCompletedTasks(),
      aiStore.checkAIServiceHealth()
    ])
  } catch (error) {
    console.error('Failed to initialize:', error)
  }
})

onUnmounted(() => {
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
  background: #f8f9fa;
  overflow: hidden;
}

/* Header */
.analysis-header {
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  padding: 16px 24px;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1400px;
  margin: 0 auto;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.title-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: #909399;
  line-height: 1.2;
}

.status-indicator {
  display: flex;
  align-items: center;
}

/* Main Content */
.analysis-content {
  flex: 1;
  display: flex;
  /* align-items: center; */
  justify-content: center;
  gap: 24px;
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  min-height: 0;
  height: 90vh;
}

/* Settings Panel */
.settings-panel {
  width: 320px;
  flex-shrink: 0;
}

.settings-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  padding: 20px;
  height: fit-content;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;
}

.settings-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.setting-section {
  margin-bottom: 24px;
}

.setting-section:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.section-header .el-icon {
  font-size: 16px;
  color: #409eff;
}

/* Task Info */
.task-info {
  margin-top: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #f0f2f5;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.info-value {
  font-size: 12px;
  color: #303133;
  font-weight: 500;
  text-align: right;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Quick Actions */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn {
  width: 100%;
  justify-content: flex-start;
}

/* Workflow Status Card */
.workflow-status-card {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
  padding: 12px;
}

.workflow-status-card::-webkit-scrollbar {
  width: 6px;
}

.workflow-status-card::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.workflow-status-card::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.workflow-status-card::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* Chat Panel */
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  max-height: 70vh;
}

/* Analysis Content Layout */
.analysis-content {
  display: flex;
  gap: 24px;
  padding: 24px;
  height: calc(100vh - 80px);
  overflow: hidden;
}

.settings-panel {
  width: 350px;
  flex-shrink: 0;
  overflow-y: auto;
}

.chat-window-wrapper {
  flex: 1;
  min-height: 0;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;
  margin-bottom: 16px;
}

.chat-window-wrapper:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.chat-window {
  height: 100%;
}

.input-wrapper {
  flex-shrink: 0;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;
}

.input-wrapper:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.message-input {
  border: none;
  background: transparent;
}

/* Responsive Design */
@media (max-width: 1200px) {
  .analysis-content {
    flex-direction: column;
    gap: 16px;
    height: auto;
    min-height: calc(100vh - 80px);
  }
  
  .settings-panel {
    width: 100%;
    overflow-y: visible;
  }
  
  .settings-card {
    padding: 16px;
  }
  
  .chat-panel {
    max-height: none;
  }
}

@media (max-width: 768px) {
  .analysis-header {
    padding: 12px 16px;
  }
  
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .title-section {
    gap: 8px;
  }
  
  .title-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
  
  .page-title {
    font-size: 20px;
  }
  
  .page-subtitle {
    font-size: 13px;
  }
  
  .analysis-content {
    padding: 16px;
    gap: 12px;
  }
  
  .settings-card {
    padding: 12px;
  }
  
  .setting-section {
    margin-bottom: 16px;
  }
}

@media (max-width: 480px) {
  .analysis-header {
    padding: 8px 12px;
  }
  
  .analysis-content {
    padding: 12px;
  }
  
  .quick-actions {
    flex-direction: row;
  }
  
  .action-btn {
    flex: 1;
    font-size: 12px;
  }
}

/* Dark mode support */
@media (prefers-color-scheme: dark) {
  .ai-analysis-container {
    background: #1a1a1a;
  }
  
  .analysis-header {
    background: #2a2a2a;
    border-bottom-color: #333;
  }
  
  .page-title {
    color: #e4e7ed;
  }
  
  .page-subtitle {
    color: #909399;
  }
  
  .settings-card,
  .chat-window-wrapper,
  .input-wrapper {
    background: #2a2a2a;
    border-color: #333;
  }
  
  .section-title {
    color: #e4e7ed;
  }
  
  .task-info {
    background: #333;
    border-color: #444;
  }
  
  .info-label {
    color: #909399;
  }
  
  .info-value {
    color: #e4e7ed;
  }
}
</style>