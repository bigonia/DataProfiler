<template>
  <div class="chat-window">
    <!-- Chat Header -->
    <div class="chat-header">
      <div class="header-left">
        <div class="chat-icon">
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <div class="header-info">
          <h3 class="chat-title">AI智能分析</h3>
          <p class="chat-subtitle">基于数据剖析的智能问答助手</p>
        </div>
      </div>
      <div class="header-actions">
        <el-tooltip content="清空对话" placement="bottom">
          <el-button
            type="text"
            :icon="Delete"
            :disabled="messages.length === 0"
            @click="handleClearMessages"
            class="clear-btn"
          />
        </el-tooltip>
      </div>
    </div>

    <!-- Messages Container -->
    <div class="messages-container" ref="messagesContainerRef">
      <!-- Empty State -->
      <div v-if="messages.length === 0" class="empty-state">
        <div class="empty-icon">
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <h4 class="empty-title">开始智能对话</h4>
        <p class="empty-description">{{ emptyMessage }}</p>
        <div class="suggestion-cards">
          <div 
            v-for="suggestion in suggestions" 
            :key="suggestion.text"
            class="suggestion-card"
            @click="$emit('send-suggestion', suggestion.text)"
          >
            <el-icon class="suggestion-icon">
              <component :is="suggestion.icon" />
            </el-icon>
            <span class="suggestion-text">{{ suggestion.text }}</span>
          </div>
        </div>
      </div>

      <!-- Messages List -->
      <div v-else class="messages-list">
        <EnhancedMessageBubble
          v-for="message in messages"
          :key="message.id"
          :message="message"
          :is-streaming="isStreaming && message.isStreaming"
          @copy-message="handleCopyMessage"
          @regenerate="handleRegenerateMessage"
          @like-message="handleLikeMessage"
          @dislike-message="handleDislikeMessage"
          @tts="handleTTS"
          @add-annotation="handleAddAnnotation"
        />
        

      </div>
    </div>

    <!-- Scroll to Bottom Button -->
    <transition name="fade">
      <el-button
        v-if="showScrollButton"
        class="scroll-btn"
        type="primary"
        :icon="ArrowDown"
        circle
        @click="scrollToBottom"
      />
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, onMounted, onUnmounted, computed } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { 
  ChatDotRound, 
  Delete, 
  ArrowDown, 
  DataAnalysis, 
  TrendCharts, 
  Warning 
} from '@element-plus/icons-vue'
import type { Message } from '@/types'
import MessageBubble from './MessageBubble.vue'
import EnhancedMessageBubble from '@/components/chat/EnhancedMessageBubble.vue'

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
  (e: 'send-suggestion', suggestion: string): void
  (e: 'copy-message', content: string): void
  (e: 'regenerate-message', messageId: string): void
}

const emit = defineEmits<Emits>()

// Refs
const messagesContainerRef = ref<HTMLElement>()
const showScrollButton = ref(false)

// Computed
const suggestions = computed(() => [
  { icon: DataAnalysis, text: '分析数据质量情况' },
  { icon: TrendCharts, text: '查看数据分布特征' },
  { icon: Warning, text: '识别异常数据' }
])

// Methods
const scrollToBottom = (smooth = true) => {
  if (!messagesContainerRef.value) return
  
  messagesContainerRef.value.scrollTo({
    top: messagesContainerRef.value.scrollHeight,
    behavior: smooth ? 'smooth' : 'auto'
  })
}

const checkScrollPosition = () => {
  if (!messagesContainerRef.value) return
  
  const { scrollTop, scrollHeight, clientHeight } = messagesContainerRef.value
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
    ElMessage.success('对话记录已清空')
  } catch {
    // User cancelled
  }
}

const handleCopyMessage = (content: string) => {
  emit('copy-message', content)
}

const handleRegenerateMessage = (messageId: string) => {
  emit('regenerate-message', messageId)
}

// Auto scroll to bottom when new messages arrive
watch(
  () => [props.messages.length, props.isStreaming],
  async () => {
    await nextTick()
    
    if (!messagesContainerRef.value) return
    
    const { scrollTop, scrollHeight, clientHeight } = messagesContainerRef.value
    const isNearBottom = scrollHeight - scrollTop - clientHeight < 150
    
    if (isNearBottom || props.messages.length === 1) {
      setTimeout(() => scrollToBottom(), 100)
    }
  },
  { flush: 'post' }
)

// Setup scroll listener
onMounted(() => {
  if (messagesContainerRef.value) {
    messagesContainerRef.value.addEventListener('scroll', checkScrollPosition)
  }
})

onUnmounted(() => {
  if (messagesContainerRef.value) {
    messagesContainerRef.value.removeEventListener('scroll', checkScrollPosition)
  }
})
</script>

<style scoped>
.chat-window {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

/* Chat Header */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: #ffffff;
  border-bottom: 1px solid #f0f2f5;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chat-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.chat-subtitle {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.clear-btn {
  color: #909399;
  transition: color 0.2s;
}

.clear-btn:hover:not(:disabled) {
  color: #f56c6c;
}

.clear-btn:disabled {
  opacity: 0.5;
}

/* Messages Container */
.messages-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px;
  background: #fafbfc;
  width: 100%;
  max-width: 100%;
}

.messages-container::-webkit-scrollbar {
  width: 4px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 2px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

/* Empty State */
.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px 20px;
}

.empty-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 28px;
  margin-bottom: 16px;
}

.empty-title {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.empty-description {
  margin: 0 0 24px 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.suggestion-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  max-width: 300px;
}

.suggestion-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: #606266;
}

.suggestion-card:hover {
  border-color: #409eff;
  background: #f0f9ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.suggestion-icon {
  font-size: 16px;
  color: #409eff;
}

.suggestion-text {
  flex: 1;
  text-align: left;
}

/* Messages List */
.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
}



/* Scroll Button */
.scroll-btn {
  position: absolute;
  bottom: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 10;
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.9);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .chat-header {
    padding: 16px 20px;
  }
  
  .messages-container {
    padding: 12px;
  }
  
  .suggestion-cards {
    max-width: 100%;
  }
  
  .scroll-btn {
    bottom: 16px;
    right: 16px;
    width: 36px;
    height: 36px;
  }
}
</style>