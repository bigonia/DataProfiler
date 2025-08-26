<template>
  <div class="chat-window">
    <el-card class="chat-container" shadow="hover">
      <template #header>
        <div class="chat-header">
          <div class="header-title">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI智能分析对话</span>
          </div>
          <div class="header-actions">
            <el-button
              v-if="messages.length > 0"
              type="text"
              size="small"
              @click="handleClearMessages"
            >
              <el-icon><Delete /></el-icon>
              清空对话
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- Messages Area -->
      <div class="messages-area" ref="messagesAreaRef">
        <!-- Empty State -->
        <div v-if="messages.length === 0" class="empty-state">
          <el-empty :description="emptyMessage" :image-size="120">
            <template #image>
              <el-icon class="empty-icon"><ChatDotRound /></el-icon>
            </template>
          </el-empty>
        </div>
        
        <!-- Messages List -->
        <div v-else class="messages-list">
          <MessageBubble
            v-for="message in messages"
            :key="message.id"
            :message="message"
            class="message-item"
          />
          
          <!-- Streaming Indicator -->
          <div v-if="isStreaming" class="streaming-indicator">
            <div class="streaming-bubble">
              <div class="streaming-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <span class="streaming-text">AI正在思考中...</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Scroll to Bottom Button -->
      <transition name="fade">
        <el-button
          v-if="showScrollButton"
          class="scroll-button"
          type="primary"
          :icon="ArrowDown"
          circle
          size="small"
          @click="scrollToBottom"
        />
      </transition>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { ChatDotRound, Delete, ArrowDown } from '@element-plus/icons-vue'
import type { Message } from '@/types'
import MessageBubble from './MessageBubble.vue'

// Props
interface Props {
  messages: Message[]
  isStreaming?: boolean
  emptyMessage?: string
}

const props = withDefaults(defineProps<Props>(), {
  isStreaming: false,
  emptyMessage: '开始与AI对话，分析您的数据！'
})

// Emits
interface Emits {
  (e: 'clear-messages'): void
}

const emit = defineEmits<Emits>()

// Refs
const messagesAreaRef = ref<HTMLElement>()
const showScrollButton = ref(false)

// Methods
const scrollToBottom = (smooth = true) => {
  if (!messagesAreaRef.value) return
  
  const scrollOptions: ScrollToOptions = {
    top: messagesAreaRef.value.scrollHeight,
    behavior: smooth ? 'smooth' : 'auto'
  }
  
  messagesAreaRef.value.scrollTo(scrollOptions)
}

const checkScrollPosition = () => {
  if (!messagesAreaRef.value) return
  
  const { scrollTop, scrollHeight, clientHeight } = messagesAreaRef.value
  const isNearBottom = scrollHeight - scrollTop - clientHeight < 100
  
  showScrollButton.value = !isNearBottom && props.messages.length > 0
}

const handleClearMessages = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空所有对话记录吗？此操作不可恢复。',
      '确认清空',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    emit('clear-messages')
  } catch {
    // User cancelled
  }
}

// Auto scroll to bottom when new messages arrive
watch(
  () => [props.messages.length, props.isStreaming],
  async () => {
    await nextTick()
    
    // Auto scroll to bottom if user is near the bottom
    if (!messagesAreaRef.value) return
    
    const { scrollTop, scrollHeight, clientHeight } = messagesAreaRef.value
    const isNearBottom = scrollHeight - scrollTop - clientHeight < 150
    
    if (isNearBottom || props.messages.length === 1) {
      scrollToBottom()
    }
  },
  { flush: 'post' }
)

// Setup scroll listener
onMounted(() => {
  if (messagesAreaRef.value) {
    messagesAreaRef.value.addEventListener('scroll', checkScrollPosition)
  }
})

onUnmounted(() => {
  if (messagesAreaRef.value) {
    messagesAreaRef.value.removeEventListener('scroll', checkScrollPosition)
  }
})
</script>

<style scoped>
.chat-window {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-container :deep(.el-card__body) {
  flex: 1;
  padding: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  min-height: 0;
  position: relative;
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-icon {
  font-size: 120px;
  color: #c0c4cc;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  animation: fadeInUp 0.3s ease-out;
}

.streaming-indicator {
  display: flex;
  justify-content: flex-start;
  margin-top: 8px;
}

.streaming-bubble {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background-color: #f5f7fa;
  border-radius: 18px;
  border: 1px solid #e4e7ed;
  max-width: 200px;
}

.streaming-dots {
  display: flex;
  gap: 4px;
}

.streaming-dots span {
  width: 6px;
  height: 6px;
  background-color: #409eff;
  border-radius: 50%;
  animation: streamingDots 1.4s infinite ease-in-out;
}

.streaming-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.streaming-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

.streaming-text {
  font-size: 13px;
  color: #606266;
}

.scroll-button {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 10;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

/* Animations */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes streamingDots {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Scrollbar Styling */
.messages-area::-webkit-scrollbar {
  width: 6px;
}

.messages-area::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.messages-area::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.messages-area::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* Responsive */
@media (max-width: 768px) {
  .messages-area {
    padding: 12px;
  }
  
  .messages-list {
    gap: 12px;
  }
  
  .scroll-button {
    bottom: 16px;
    right: 16px;
  }
  
  .header-title span {
    display: none;
  }
}
</style>