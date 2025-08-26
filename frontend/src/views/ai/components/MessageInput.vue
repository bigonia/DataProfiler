<template>
  <div class="message-input">
    <el-card class="input-container" shadow="hover">
      <div class="input-wrapper">
        <!-- Text Input Area -->
        <div class="text-input-area">
          <el-input
            ref="inputRef"
            v-model="inputMessage"
            type="textarea"
            :placeholder="placeholder"
            :disabled="disabled"
            :autosize="{ minRows: 1, maxRows: 6 }"
            resize="none"
            class="message-textarea"
            @keydown="handleKeyDown"
            @input="handleInput"
          />
          
          <!-- Character Counter -->
          <div class="character-counter" v-if="showCharacterCount">
            <span :class="{ 'over-limit': isOverLimit }">
              {{ inputMessage.length }}/{{ maxLength }}
            </span>
          </div>
        </div>
        
        <!-- Action Buttons -->
        <div class="action-buttons">
          <!-- Stop Button (when streaming) -->
          <el-button
            v-if="loading"
            type="danger"
            :icon="VideoPause"
            circle
            size="large"
            @click="handleStop"
            class="stop-button"
          >
          </el-button>
          
          <!-- Send Button -->
          <el-button
            v-else
            type="primary"
            :icon="Promotion"
            circle
            size="large"
            :disabled="!canSend"
            @click="handleSend"
            class="send-button"
          >
          </el-button>
        </div>
      </div>
      
      <!-- Input Hints -->
      <div class="input-hints" v-if="showHints">
        <div class="hint-item">
          <el-icon><InfoFilled /></el-icon>
          <span>按 Enter 发送，Shift + Enter 换行</span>
        </div>
        <div class="hint-item" v-if="suggestedQuestions.length > 0">
          <span class="hint-label">建议问题:</span>
          <div class="suggested-questions">
            <el-tag
              v-for="question in suggestedQuestions"
              :key="question"
              type="info"
              size="small"
              class="question-tag"
              @click="selectSuggestedQuestion(question)"
            >
              {{ question }}
            </el-tag>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, VideoPause, InfoFilled } from '@element-plus/icons-vue'

// Props
interface Props {
  disabled?: boolean
  loading?: boolean
  placeholder?: string
  maxLength?: number
  showCharacterCount?: boolean
  showHints?: boolean
  suggestedQuestions?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  loading: false,
  placeholder: '请输入您的问题...',
  maxLength: 2000,
  showCharacterCount: true,
  showHints: true,
  suggestedQuestions: () => [
    '这个数据源有哪些表？',
    '数据质量如何？',
    '有哪些异常数据？',
    '表之间的关联关系是什么？',
    '数据分布情况如何？'
  ]
})

// Emits
interface Emits {
  (e: 'send', message: string): void
  (e: 'stop'): void
}

const emit = defineEmits<Emits>()

// Refs
const inputRef = ref()
const inputMessage = ref('')

// Computed
const canSend = computed(() => {
  return !props.disabled && 
         !props.loading && 
         inputMessage.value.trim().length > 0 && 
         !isOverLimit.value
})

const isOverLimit = computed(() => {
  return inputMessage.value.length > props.maxLength
})

// Methods
const handleSend = () => {
  if (!canSend.value) {
    if (isOverLimit.value) {
      ElMessage.warning(`消息长度不能超过 ${props.maxLength} 个字符`)
    } else if (inputMessage.value.trim().length === 0) {
      ElMessage.warning('请输入消息内容')
    }
    return
  }
  
  const message = inputMessage.value.trim()
  emit('send', message)
  inputMessage.value = ''
  
  // Focus back to input
  nextTick(() => {
    if (inputRef.value) {
      inputRef.value.focus()
    }
  })
}

const handleStop = () => {
  emit('stop')
}

const handleKeyDown = (event: KeyboardEvent) => {
  // Enter to send (without Shift)
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
  
  // Shift + Enter for new line (default behavior)
  // Ctrl/Cmd + Enter as alternative send
  if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
    event.preventDefault()
    handleSend()
  }
}

const handleInput = () => {
  // Auto-resize is handled by Element Plus autosize prop
  // Additional input validation can be added here
}

const selectSuggestedQuestion = (question: string) => {
  if (props.disabled || props.loading) {
    return
  }
  
  inputMessage.value = question
  
  // Focus to input and move cursor to end
  nextTick(() => {
    if (inputRef.value) {
      inputRef.value.focus()
      const textarea = inputRef.value.textarea || inputRef.value.$el.querySelector('textarea')
      if (textarea) {
        textarea.setSelectionRange(question.length, question.length)
      }
    }
  })
}

// Watch for disabled state changes to manage focus
watch(
  () => props.disabled,
  (newDisabled) => {
    if (!newDisabled) {
      nextTick(() => {
        if (inputRef.value) {
          inputRef.value.focus()
        }
      })
    }
  }
)
</script>

<style scoped>
.message-input {
  width: 100%;
}

.input-container {
  border-radius: 12px;
  overflow: hidden;
}

.input-container :deep(.el-card__body) {
  padding: 16px;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.text-input-area {
  flex: 1;
  position: relative;
}

.message-textarea {
  width: 100%;
}

.message-textarea :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding: 8px 12px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  background-color: #f8f9fa;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.message-textarea :deep(.el-textarea__inner):focus {
  background-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.message-textarea :deep(.el-textarea__inner):disabled {
  background-color: #f5f7fa;
  color: #c0c4cc;
}

.character-counter {
  position: absolute;
  bottom: 4px;
  right: 8px;
  font-size: 12px;
  color: #909399;
  pointer-events: none;
}

.character-counter .over-limit {
  color: #f56c6c;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.send-button,
.stop-button {
  width: 44px;
  height: 44px;
  transition: all 0.2s ease;
}

.send-button:hover:not(:disabled) {
  transform: scale(1.05);
}

.stop-button {
  animation: pulse 2s infinite;
}

.stop-button:hover {
  transform: scale(1.05);
  animation: none;
}

.input-hints {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.hint-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #909399;
}

.hint-item:last-child {
  margin-bottom: 0;
}

.hint-label {
  font-weight: 500;
  margin-right: 8px;
  flex-shrink: 0;
}

.suggested-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
}

.question-tag {
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.question-tag:hover {
  background-color: #409eff;
  color: white;
  transform: translateY(-1px);
}

/* Animations */
@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(245, 108, 108, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .input-container :deep(.el-card__body) {
    padding: 12px;
  }
  
  .input-wrapper {
    gap: 8px;
  }
  
  .send-button,
  .stop-button {
    width: 40px;
    height: 40px;
  }
  
  .input-hints {
    margin-top: 8px;
    padding-top: 8px;
  }
  
  .hint-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .hint-label {
    margin-right: 0;
  }
  
  .suggested-questions {
    margin-top: 4px;
  }
  
  .question-tag {
    font-size: 12px;
  }
}

/* Dark mode support (if needed) */
@media (prefers-color-scheme: dark) {
  .message-textarea :deep(.el-textarea__inner) {
    background-color: #2d2d2d;
    color: #ffffff;
  }
  
  .message-textarea :deep(.el-textarea__inner):focus {
    background-color: #1a1a1a;
  }
}
</style>