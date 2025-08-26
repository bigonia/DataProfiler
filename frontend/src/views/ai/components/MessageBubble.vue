<template>
  <div class="message-bubble" :class="messageClasses">
    <!-- User Message -->
    <div v-if="message.role === 'user'" class="user-message">
      <div class="message-content">
        <div class="message-text">{{ message.content }}</div>
        <div class="message-time">{{ formatTime(message.timestamp) }}</div>
      </div>
      <div class="message-avatar">
        <el-avatar :size="32" :icon="User" />
      </div>
    </div>
    
    <!-- AI Message -->
    <div v-else class="ai-message">
      <div class="message-avatar">
        <el-avatar :size="32" class="ai-avatar">
          <el-icon><ChatDotRound /></el-icon>
        </el-avatar>
      </div>
      <div class="message-content">
        <div class="message-header">
          <span class="ai-label">AI助手</span>
          <el-tag v-if="message.type" :type="getMessageTypeTag(message.type)" size="small">
            {{ getMessageTypeLabel(message.type) }}
          </el-tag>
        </div>
        
        <!-- Message Text with Markdown Support -->
        <div class="message-text" v-html="formattedContent"></div>
        
        <!-- Workflow Nodes Display -->
        <WorkflowNodeDisplay 
          v-if="message.workflowNodes && message.workflowNodes.length > 0"
          :nodes="message.workflowNodes"
        />
        
        <!-- Error State -->
        <div v-if="message.type === 'error'" class="error-content">
          <el-alert
            :title="message.content"
            type="error"
            :closable="false"
            show-icon
          />
        </div>
        
        <!-- Message Actions -->
        <div class="message-actions">
          <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          <div class="action-buttons">
            <el-button
              type="text"
              size="small"
              @click="copyMessage"
              :icon="DocumentCopy"
            >
              复制
            </el-button>
            <el-button
              v-if="message.type !== 'error'"
              type="text"
              size="small"
              @click="regenerateMessage"
              :icon="Refresh"
            >
              重新生成
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { User, ChatDotRound, DocumentCopy, Refresh } from '@element-plus/icons-vue'
import type { Message } from '@/types'
import WorkflowNodeDisplay from './WorkflowNodeDisplay.vue'

// Props
interface Props {
  message: Message
}

const props = defineProps<Props>()

// Emits
interface Emits {
  (e: 'regenerate', messageId: string): void
}

const emit = defineEmits<Emits>()

// Computed
const messageClasses = computed(() => {
  return {
    'is-user': props.message.role === 'user',
    'is-ai': props.message.role === 'assistant',
    'is-error': props.message.type === 'error',
    'is-streaming': props.message.type === 'streaming'
  }
})

const formattedContent = computed(() => {
  if (props.message.type === 'error') {
    return ''
  }
  
  let content = props.message.content
  
  // Simple markdown-like formatting
  content = content
    // Bold text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    // Italic text
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    // Code blocks
    .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    // Inline code
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // Line breaks
    .replace(/\n/g, '<br>')
  
  return content
})

// Methods
const formatTime = (timestamp: number): string => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  // Less than 1 minute
  if (diff < 60000) {
    return '刚刚'
  }
  
  // Less than 1 hour
  if (diff < 3600000) {
    const minutes = Math.floor(diff / 60000)
    return `${minutes}分钟前`
  }
  
  // Less than 1 day
  if (diff < 86400000) {
    const hours = Math.floor(diff / 3600000)
    return `${hours}小时前`
  }
  
  // More than 1 day
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getMessageTypeLabel = (type: string): string => {
  const typeMap: Record<string, string> = {
    'analysis': '分析结果',
    'suggestion': '建议',
    'warning': '警告',
    'error': '错误',
    'streaming': '实时响应'
  }
  return typeMap[type] || type
}

const getMessageTypeTag = (type: string): string => {
  const tagMap: Record<string, string> = {
    'analysis': 'primary',
    'suggestion': 'success',
    'warning': 'warning',
    'error': 'danger',
    'streaming': 'info'
  }
  return tagMap[type] || 'info'
}

const copyMessage = async () => {
  try {
    await navigator.clipboard.writeText(props.message.content)
    ElMessage.success('消息已复制到剪贴板')
  } catch (error) {
    console.error('Failed to copy message:', error)
    ElMessage.error('复制失败')
  }
}

const regenerateMessage = () => {
  emit('regenerate', props.message.id)
}
</script>

<style scoped>
.message-bubble {
  width: 100%;
  margin-bottom: 4px;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 12px;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 12px;
}

.message-avatar {
  flex-shrink: 0;
}

.ai-avatar {
  background-color: #409eff;
  color: white;
}

.message-content {
  max-width: 70%;
  min-width: 120px;
}

.user-message .message-content {
  background-color: #409eff;
  color: white;
  padding: 12px 16px;
  border-radius: 18px 18px 4px 18px;
  position: relative;
}

.ai-message .message-content {
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  padding: 12px 16px;
  border-radius: 18px 18px 18px 4px;
  position: relative;
}

.message-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.ai-label {
  font-size: 12px;
  font-weight: 500;
  color: #409eff;
}

.message-text {
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.user-message .message-text {
  color: white;
}

.ai-message .message-text {
  color: #303133;
}

.message-text :deep(strong) {
  font-weight: 600;
}

.message-text :deep(em) {
  font-style: italic;
}

.message-text :deep(code) {
  background-color: rgba(0, 0, 0, 0.1);
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.message-text :deep(pre) {
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 12px;
  margin: 8px 0;
  overflow-x: auto;
}

.message-text :deep(pre code) {
  background-color: transparent;
  padding: 0;
  border-radius: 0;
  font-size: 13px;
  line-height: 1.4;
}

.error-content {
  margin-top: 8px;
}

.message-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.user-message .message-time {
  color: rgba(255, 255, 255, 0.8);
  margin-top: 4px;
  text-align: right;
}

.action-buttons {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.ai-message:hover .action-buttons {
  opacity: 1;
}

/* Animation for streaming messages */
.is-streaming .message-text::after {
  content: '▋';
  animation: blink 1s infinite;
  color: #409eff;
}

@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}

/* Responsive */
@media (max-width: 768px) {
  .message-content {
    max-width: 85%;
  }
  
  .user-message,
  .ai-message {
    gap: 8px;
  }
  
  .message-avatar {
    display: none;
  }
  
  .user-message .message-content {
    border-radius: 18px 18px 4px 18px;
  }
  
  .ai-message .message-content {
    border-radius: 18px 18px 18px 4px;
  }
  
  .message-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .action-buttons {
    opacity: 1;
  }
}
</style>