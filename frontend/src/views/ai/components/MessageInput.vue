<template>
  <div class="message-input-container">
    <!-- Input Area -->
    <div class="input-wrapper" :class="{ 'focused': isFocused, 'expanded': isExpanded }">
      <!-- Main Input -->
      <div class="input-main">
        <el-input
          ref="inputRef"
          v-model="inputMessage"
          type="textarea"
          :placeholder="placeholder"
          :rows="inputRows"
          :maxlength="maxLength"
          :disabled="isLoading"
          resize="none"
          @focus="handleFocus"
          @blur="handleBlur"
          @keydown="handleKeydown"
          @input="handleInput"
          class="message-textarea"
        />
        
        <!-- Character Counter -->
        <div v-if="showCounter" class="char-counter" :class="{ 'warning': isNearLimit }">
          {{ inputMessage.length }}/{{ maxLength }}
        </div>
      </div>
      
      <!-- Action Buttons -->
      <div class="action-buttons">
        <!-- File Upload -->
        <el-tooltip content="上传文件" placement="top">
          <el-button
            type="text"
            :icon="Paperclip"
            @click="handleFileUpload"
            :disabled="isLoading"
            class="action-btn upload-btn"
          />
        </el-tooltip>
        
        <!-- Voice Input -->
        <el-tooltip content="语音输入" placement="top">
          <el-button
            type="text"
            :icon="Microphone"
            @click="handleVoiceInput"
            :disabled="isLoading"
            :class="{ 'recording': isRecording }"
            class="action-btn voice-btn"
          />
        </el-tooltip>
        
        <!-- Clear Input -->
        <el-tooltip content="清空" placement="top" v-if="inputMessage.trim()">
          <el-button
            type="text"
            :icon="Delete"
            @click="handleClear"
            :disabled="isLoading"
            class="action-btn clear-btn"
          />
        </el-tooltip>
        
        <!-- Send Button -->
        <el-tooltip :content="sendTooltip" placement="top">
          <el-button
            type="primary"
            :icon="isLoading ? Loading : Promotion"
            @click="handleSend"
            :disabled="!canSend"
            :loading="isLoading"
            class="send-btn"
          >
            {{ sendButtonText }}
          </el-button>
        </el-tooltip>
      </div>
    </div>
    
    <!-- Quick Actions -->
    <div v-if="showQuickActions" class="quick-actions">
      <div class="quick-action-item" @click="insertQuickText('请帮我分析一下')">
        <el-icon><DataAnalysis /></el-icon>
        <span>分析</span>
      </div>
      
      <div class="quick-action-item" @click="insertQuickText('请总结一下')">
        <el-icon><Document /></el-icon>
        <span>总结</span>
      </div>
      
      <div class="quick-action-item" @click="insertQuickText('请解释一下')">
        <el-icon><QuestionFilled /></el-icon>
        <span>解释</span>
      </div>
      
      <div class="quick-action-item" @click="insertQuickText('请优化一下')">
        <el-icon><Tools /></el-icon>
        <span>优化</span>
      </div>
    </div>
    
    <!-- Suggested Questions -->
    <!-- <div v-if="showSuggestedQuestions && suggestedQuestions.length > 0" class="suggested-questions">
      <div class="suggested-title">
        <el-icon><QuestionFilled /></el-icon>
        <span>建议问题</span>
      </div>
      <div class="suggested-list">
        <div 
          v-for="(question, index) in suggestedQuestions" 
          :key="index"
          class="suggested-item"
          @click="handleSuggestedQuestion(question)"
        >
          {{ question }}
        </div>
      </div>
    </div> -->
    
    <!-- File Upload Dialog -->
    <input
      ref="fileInputRef"
      type="file"
      multiple
      accept=".txt,.md,.json,.csv,.xlsx,.pdf,.doc,.docx"
      @change="handleFileChange"
      style="display: none;"
    />
    
    <!-- Upload Progress -->
    <div v-if="uploadProgress.length > 0" class="upload-progress">
      <div v-for="file in uploadProgress" :key="file.id" class="upload-item">
        <div class="upload-info">
          <el-icon><Document /></el-icon>
          <span class="file-name">{{ file.name }}</span>
          <span class="file-size">{{ formatFileSize(file.size) }}</span>
        </div>
        
        <el-progress
          :percentage="file.progress"
          :status="file.status"
          :stroke-width="4"
          class="upload-progress-bar"
        />
        
        <el-button
          type="text"
          :icon="Close"
          @click="removeUploadFile(file.id)"
          class="remove-btn"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Paperclip,
  Microphone,
  Delete,
  Promotion,
  Loading,
  DataAnalysis,
  Document,
  QuestionFilled,
  Tools,
  Close
} from '@element-plus/icons-vue'

// Props
interface Props {
  placeholder?: string
  maxLength?: number
  disabled?: boolean
  loading?: boolean
  showQuickActions?: boolean
  autoFocus?: boolean
  suggestedQuestions?: string[]
  showSuggestedQuestions?: boolean
  uploadedFiles?: File[]
  maxFileSize?: number
  allowedFileTypes?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '输入您的问题...',
  maxLength: 2000,
  disabled: false,
  loading: false,
  showQuickActions: true,
  autoFocus: false,
  suggestedQuestions: () => [],
  showSuggestedQuestions: true,
  uploadedFiles: () => [],
  maxFileSize: 10 * 1024 * 1024, // 10MB
  allowedFileTypes: () => ['.txt', '.md', '.json', '.csv', '.xlsx', '.pdf', '.doc', '.docx', '.png', '.jpg', '.jpeg']
})

// Emits
interface Emits {
  (e: 'send', message: string, files?: File[]): void
  (e: 'voice-input'): void
  (e: 'file-upload', files: File[]): void
  (e: 'file-remove', fileId: string): void
  (e: 'clear'): void
  (e: 'suggested-question', question: string): void
  (e: 'focus'): void
  (e: 'blur'): void
}

const emit = defineEmits<Emits>()

// Refs
const inputRef = ref()
const fileInputRef = ref()

// State
const inputMessage = ref('')
const isFocused = ref(false)
const isRecording = ref(false)
const uploadProgress = ref<Array<{
  id: string
  name: string
  size: number
  progress: number
  status: 'success' | 'exception' | 'warning' | ''
}>>([])

// Computed
const isLoading = computed(() => props.loading || props.disabled)

const inputRows = computed(() => {
  const lines = inputMessage.value.split('\n').length
  return Math.min(Math.max(lines, 1), 6)
})

const isExpanded = computed(() => inputRows.value > 1 || isFocused.value)

const canSend = computed(() => {
  return inputMessage.value.trim().length > 0 && !isLoading.value
})

const showCounter = computed(() => {
  return inputMessage.value.length > props.maxLength * 0.8
})

const isNearLimit = computed(() => {
  return inputMessage.value.length > props.maxLength * 0.9
})

const sendButtonText = computed(() => {
  if (isLoading.value) return '发送中'
  return '发送'
})

const sendTooltip = computed(() => {
  if (!canSend.value && !isLoading.value) {
    return '请输入消息内容'
  }
  return 'Ctrl+Enter 快速发送'
})

// Methods
const handleFocus = () => {
  isFocused.value = true
  emit('focus')
}

const handleBlur = () => {
  isFocused.value = false
  emit('blur')
}

const handleInput = () => {
  // Auto-resize logic is handled by computed inputRows
}

const handleKeydown = (event: KeyboardEvent) => {
  // Ctrl+Enter to send
  if (event.ctrlKey && event.key === 'Enter') {
    event.preventDefault()
    handleSend()
    return
  }
  
  // Shift+Enter for new line (default behavior)
  if (event.shiftKey && event.key === 'Enter') {
    return
  }
  
  // Enter to send (can be configured)
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

const handleSend = () => {
  if (!canSend.value) return
  
  const message = inputMessage.value.trim()
  const files = uploadProgress.value
    .filter(f => f.status === 'success')
    .map(f => f as any) // Convert to File objects if needed
  
  emit('send', message, files.length > 0 ? files : undefined)
  
  // Clear input and files
  inputMessage.value = ''
  uploadProgress.value = []
  
  // Focus back to input
  nextTick(() => {
    inputRef.value?.focus()
  })
}

const handleClear = () => {
  inputMessage.value = ''
  uploadProgress.value = []
  emit('clear')
  
  nextTick(() => {
    inputRef.value?.focus()
  })
}

const handleVoiceInput = () => {
  if (isRecording.value) {
    // Stop recording
    isRecording.value = false
    ElMessage.info('语音输入已停止')
  } else {
    // Start recording
    isRecording.value = true
    ElMessage.info('开始语音输入...')
    emit('voice-input')
    
    // Simulate recording stop after 5 seconds
    setTimeout(() => {
      isRecording.value = false
    }, 5000)
  }
}

const handleFileUpload = () => {
  fileInputRef.value?.click()
}

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  
  if (files.length === 0) return
  
  // Validate file size and type
  const validFiles = files.filter(file => {
    // Check file size
    if (file.size > props.maxFileSize) {
      ElMessage.error(`文件 ${file.name} 超过${formatFileSize(props.maxFileSize)}限制`)
      return false
    }
    
    // Check file type
    const fileExt = '.' + file.name.split('.').pop()?.toLowerCase()
    if (!props.allowedFileTypes.includes(fileExt)) {
      ElMessage.error(`不支持的文件类型: ${fileExt}`)
      return false
    }
    
    return true
  })
  
  if (validFiles.length === 0) return
  
  // Add files to upload progress
  validFiles.forEach(file => {
    const fileItem = {
      id: Date.now() + Math.random().toString(),
      name: file.name,
      size: file.size,
      progress: 0,
      status: '' as const
    }
    
    uploadProgress.value.push(fileItem)
    
    // Simulate upload progress
    simulateUpload(fileItem)
  })
  
  emit('file-upload', validFiles)
  
  // Clear file input
  target.value = ''
}

const simulateUpload = (fileItem: any) => {
  const interval = setInterval(() => {
    fileItem.progress += Math.random() * 20
    
    if (fileItem.progress >= 100) {
      fileItem.progress = 100
      fileItem.status = 'success'
      clearInterval(interval)
      ElMessage.success(`文件 ${fileItem.name} 上传成功`)
    }
  }, 200)
}

const removeUploadFile = (fileId: string) => {
  const index = uploadProgress.value.findIndex(f => f.id === fileId)
  if (index > -1) {
    uploadProgress.value.splice(index, 1)
    emit('file-remove', fileId)
  }
}

const handleSuggestedQuestion = (question: string) => {
  emit('suggested-question', question)
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

const insertQuickText = (text: string) => {
  if (inputMessage.value.trim()) {
    inputMessage.value += ' ' + text
  } else {
    inputMessage.value = text
  }
  
  nextTick(() => {
    inputRef.value?.focus()
  })
}

// Auto focus
watch(() => props.autoFocus, (newVal) => {
  if (newVal) {
    nextTick(() => {
      inputRef.value?.focus()
    })
  }
}, { immediate: true })
</script>

<style scoped>
.message-input-container {
  background: #ffffff;
  border-top: 1px solid #e4e7ed;
  padding: 16px;
}

/* Input Wrapper */
.input-wrapper {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 12px;
  transition: all 0.3s ease;
  overflow: hidden;
}

.input-wrapper.focused {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.input-wrapper.expanded {
  border-radius: 16px;
}

/* Input Main */
.input-main {
  position: relative;
  padding: 12px 16px;
}

.message-textarea {
  border: none;
  box-shadow: none;
}

.message-textarea :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding: 0;
  font-size: 14px;
  line-height: 1.5;
  color: #303133;
  background: transparent;
  resize: none;
  min-height: 20px;
}

.message-textarea :deep(.el-textarea__inner):focus {
  border: none;
  box-shadow: none;
}

.message-textarea :deep(.el-textarea__inner)::placeholder {
  color: #c0c4cc;
  font-size: 14px;
}

/* Character Counter */
.char-counter {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 11px;
  color: #909399;
  background: rgba(255, 255, 255, 0.9);
  padding: 2px 6px;
  border-radius: 8px;
  backdrop-filter: blur(4px);
  transition: color 0.2s;
}

.char-counter.warning {
  color: #e6a23c;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f8f9fa;
  border-top: 1px solid #f0f2f5;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  color: #606266;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn:hover {
  background: #e9ecef;
  color: #409eff;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.voice-btn.recording {
  color: #f56c6c;
  background: #fef0f0;
  animation: recording-pulse 1.5s ease-in-out infinite;
}

@keyframes recording-pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

.clear-btn:hover {
  color: #f56c6c;
  background: #fef0f0;
}

.send-btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 8px;
  font-weight: 500;
  margin-left: auto;
  transition: all 0.2s;
}

.send-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.send-btn:disabled {
  transform: none;
  box-shadow: none;
}

/* Quick Actions */
.quick-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding: 0 4px;
}

.quick-action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 16px;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.quick-action-item:hover {
  background: #409eff;
  color: white;
  border-color: #409eff;
  transform: translateY(-1px);
}

.quick-action-item .el-icon {
  font-size: 14px;
}

/* Suggested Questions */
.suggested-questions {
  margin-top: 16px;
  padding: 12px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 12px;
}

.suggested-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
}

.suggested-title .el-icon {
  font-size: 14px;
  color: #909399;
}

.suggested-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.suggested-item {
  padding: 8px 12px;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  font-size: 13px;
  color: #303133;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.4;
}

.suggested-item:hover {
  background: #409eff;
  color: white;
  border-color: #409eff;
  transform: translateX(4px);
}

/* Upload Progress */
.upload-progress {
  margin-top: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.upload-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f2f5;
}

.upload-item:last-child {
  border-bottom: none;
}

.upload-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
}

.upload-progress-bar {
  flex: 1;
  margin: 0 8px;
}

.remove-btn {
  width: 24px;
  height: 24px;
  color: #909399;
  transition: color 0.2s;
}

.remove-btn:hover {
  color: #f56c6c;
}

/* Responsive */
@media (max-width: 768px) {
  .message-input-container {
    padding: 12px;
  }
  
  .action-buttons {
    gap: 6px;
    padding: 6px 12px;
  }
  
  .action-btn {
    width: 28px;
    height: 28px;
  }
  
  .send-btn {
    height: 32px;
    padding: 0 12px;
    font-size: 13px;
  }
  
  .quick-actions {
    flex-wrap: wrap;
    gap: 6px;
  }
  
  .quick-action-item {
    padding: 4px 8px;
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .message-input-container {
    padding: 8px;
  }
  
  .input-main {
    padding: 8px 12px;
  }
  
  .action-buttons {
    padding: 4px 8px;
  }
  
  .upload-progress {
    padding: 8px;
  }
  
  .upload-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .upload-progress-bar {
    width: 100%;
    margin: 0;
  }
}

/* Dark mode support */
@media (prefers-color-scheme: dark) {
  .message-input-container {
    background: #1a1a1a;
    border-top-color: #333;
  }
  
  .input-wrapper {
    background: #2a2a2a;
    border-color: #444;
  }
  
  .input-wrapper.focused {
    border-color: #409eff;
  }
  
  .message-textarea :deep(.el-textarea__inner) {
    background: transparent;
    color: #e4e7ed;
  }
  
  .action-buttons {
    background: #333;
    border-top-color: #444;
  }
  
  .quick-action-item {
    background: #333;
    border-color: #444;
    color: #e4e7ed;
  }
  
  .upload-progress {
    background: #333;
    border-color: #444;
  }
}
</style>