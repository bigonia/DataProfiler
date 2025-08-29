<template>
  <div class="enhanced-message-bubble" :class="messageClasses">
    <!-- User Message -->
    <div v-if="message.role === 'user'" class="user-message">
      <div class="message-content">
        <div class="message-text">{{ message.content }}</div>
        
        <!-- User Files -->
        <div v-if="message.files && message.files.length > 0" class="message-files">
          <div
            v-for="file in message.files"
            :key="file.id"
            class="file-item"
          >
            <el-icon><Document /></el-icon>
            <span class="file-name">{{ file.name }}</span>
            <span class="file-size">({{ formatFileSize(file.size) }})</span>
          </div>
        </div>
      </div>
      
      <div class="message-meta">
        <span class="message-time">{{ formatTime(message.created_at) }}</span>
      </div>
    </div>
    
    <!-- Assistant Message -->
    <div v-else class="assistant-message">
      <!-- AI Avatar -->
      <div class="ai-avatar">
        <el-icon><Robot /></el-icon>
      </div>
      
      <div class="message-body">
        <!-- Message Type Badge -->
        <div v-if="messageTypeBadge" class="message-type-badge">
          <el-tag :type="messageTypeBadge.type" size="small">
            {{ messageTypeBadge.text }}
          </el-tag>
        </div>
        
        <!-- Agent Thoughts -->
        <div v-if="agentThoughts.length > 0" class="agent-thoughts">
          <div class="thoughts-header" @click="toggleThoughts">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI 思维过程</span>
            <el-icon class="toggle-icon" :class="{ expanded: showThoughts }">
              <ArrowDown />
            </el-icon>
          </div>
          
          <transition name="slide-down">
            <div v-show="showThoughts" class="thoughts-content">
              <div
                v-for="(thought, index) in agentThoughts"
                :key="index"
                class="thought-item"
              >
                <div class="thought-header">
                  <span class="thought-title">{{ thought.tool || '分析' }}</span>
                  <span class="thought-time">{{ formatTime(thought.created_at) }}</span>
                </div>
                <div class="thought-content">{{ thought.thought }}</div>
                
                <!-- Tool Call Details -->
                <div v-if="thought.tool_input" class="tool-details">
                  <div class="tool-input">
                    <strong>输入:</strong>
                    <pre>{{ formatToolInput(thought.tool_input) }}</pre>
                  </div>
                  <div v-if="thought.observation" class="tool-output">
                    <strong>输出:</strong>
                    <pre>{{ thought.observation }}</pre>
                  </div>
                </div>
              </div>
            </div>
          </transition>
        </div>
        
        <!-- Workflow Process -->
        <div v-if="workflowProcess && showWorkflowTrace" class="workflow-process">
          <div class="workflow-header">
            <el-icon><Share /></el-icon>
            <span>工作流执行</span>
          </div>
          
          <div class="workflow-nodes">
            <div
              v-for="node in workflowProcess.tracing"
              :key="node.node_id"
              class="workflow-node"
              :class="getNodeStatusClass(node.status)"
            >
              <div class="node-icon">
                <el-icon v-if="node.status === 'running'"><Loading /></el-icon>
                <el-icon v-else-if="node.status === 'succeeded'"><Check /></el-icon>
                <el-icon v-else-if="node.status === 'failed'"><Close /></el-icon>
                <el-icon v-else><Clock /></el-icon>
              </div>
              
              <div class="node-content">
                <div class="node-title">{{ node.node_title }}</div>
                <div class="node-type">{{ node.node_type }}</div>
                
                <div v-if="node.status === 'running'" class="node-progress">
                  <el-progress
                    :percentage="getNodeProgress(node)"
                    :show-text="false"
                    :stroke-width="4"
                  />
                </div>
                
                <div v-if="node.execution_metadata" class="node-metadata">
                  <span>耗时: {{ formatDuration(node.execution_metadata.elapsed_time) }}</span>
                  <span v-if="node.execution_metadata.total_tokens">
                    Tokens: {{ node.execution_metadata.total_tokens }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Error Message -->
        <div v-if="message.status === 'error'" class="error-message">
          <el-alert
            :title="errorTitle"
            :description="errorDescription"
            type="error"
            :closable="false"
            show-icon
          />
        </div>
        
        <!-- Message Content -->
        <div v-else class="message-content">
          <!-- Streaming Indicator -->
          <div v-if="isStreaming" class="streaming-indicator">
            <div class="typing-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
          
          <!-- Rendered Content -->
          <div
            v-if="renderedContent"
            class="rendered-content"
            v-html="renderedContent"
          ></div>
          
          <!-- Citations -->
          <div v-if="message.retriever_resources && message.retriever_resources.length > 0" class="citations">
            <div class="citations-header">
              <el-icon><Link /></el-icon>
              <span>参考资料</span>
            </div>
            <div class="citations-list">
              <div
                v-for="(resource, index) in message.retriever_resources"
                :key="index"
                class="citation-item"
                @click="openCitation(resource)"
              >
                <div class="citation-content">
                  <div class="citation-title">{{ resource.document_name }}</div>
                  <div class="citation-snippet">{{ resource.content }}</div>
                </div>
                <div class="citation-score">
                  {{ Math.round(resource.score * 100) }}%
                </div>
              </div>
            </div>
          </div>
          
          <!-- Message Files -->
          <div v-if="message.files && message.files.length > 0" class="message-files">
            <div
              v-for="file in message.files"
              :key="file.id"
              class="file-item"
              @click="downloadFile(file)"
            >
              <el-icon><Document /></el-icon>
              <span class="file-name">{{ file.name }}</span>
              <span class="file-size">({{ formatFileSize(file.size) }})</span>
              <el-icon class="download-icon"><Download /></el-icon>
            </div>
          </div>
        </div>
        
        <!-- Message Actions -->
        <div v-if="!isStreaming && message.content" class="message-actions">
          <div class="message-meta">
            <span class="message-time">{{ formatTime(message.created_at) }}</span>
            <span v-if="message.usage" class="token-usage">
              Tokens: {{ message.usage.total_tokens }}
            </span>
          </div>
          
          <div class="action-buttons">
            <!-- Copy Button -->
            <el-button
              type="text"
              size="small"
              @click="handleCopy"
              :loading="copyLoading"
            >
              <el-icon><DocumentCopy /></el-icon>
            </el-button>
            
            <!-- Regenerate Button -->
            <el-button
              type="text"
              size="small"
              @click="handleRegenerate"
              :disabled="isResponding"
            >
              <el-icon><Refresh /></el-icon>
            </el-button>
            
            <!-- Like Button -->
            <el-button
              type="text"
              size="small"
              @click="handleLike"
              :class="{ liked: message.feedback?.rating === 'like' }"
            >
              <el-icon><ThumbsUp /></el-icon>
            </el-button>
            
            <!-- Dislike Button -->
            <el-button
              type="text"
              size="small"
              @click="handleDislike"
              :class="{ disliked: message.feedback?.rating === 'dislike' }"
            >
              <el-icon><CircleClose /></el-icon>
            </el-button>
            
            <!-- TTS Button -->
            <el-button
              v-if="enableTTS"
              type="text"
              size="small"
              @click="handleTTS"
              :loading="ttsLoading"
            >
              <el-icon><Microphone /></el-icon>
            </el-button>
            
            <!-- Annotation Button -->
            <el-button
              type="text"
              size="small"
              @click="showAnnotationDialog = true"
            >
              <el-icon><EditPen /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Annotation Dialog -->
    <el-dialog
      v-model="showAnnotationDialog"
      title="添加标注"
      width="500px"
    >
      <el-input
        v-model="annotationContent"
        type="textarea"
        :rows="4"
        placeholder="请输入标注内容..."
        maxlength="500"
        show-word-limit
      />
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAnnotationDialog = false">取消</el-button>
          <el-button
            type="primary"
            @click="handleAddAnnotation"
            :disabled="!annotationContent.trim()"
          >
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document,
  ChatDotRound,
  ArrowDown,
  Share,
  Loading,
  Check,
  Close,
  Clock,
  Link,
  Download,
  DocumentCopy,
  Refresh,
  CircleClose,
  Microphone,
  EditPen
} from '@element-plus/icons-vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import type {
  Message,

} from '@/types'

// Props
interface Props {
  message: Message
  isStreaming?: boolean
  showWorkflowTrace?: boolean
  enableTTS?: boolean
  isResponding?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isStreaming: false,
  showWorkflowTrace: true,
  enableTTS: false,
  isResponding: false
})

// Emits
interface Emits {
  'copy-message': [content: string]
  'regenerate': [messageId: string]
  'like-message': [messageId: string, rating: 'like' | 'dislike']
  'add-annotation': [messageId: string, content: string]
  'remove-annotation': [messageId: string]
  'stream-complete': []
  'tts-play': [content: string]
}

const emit = defineEmits<Emits>()

// Local state
const showThoughts = ref(false)
const copyLoading = ref(false)
const ttsLoading = ref(false)
const showAnnotationDialog = ref(false)
const annotationContent = ref('')

// Computed properties
const messageClasses = computed(() => ({
  'user-message-wrapper': props.message.role === 'user',
  'assistant-message-wrapper': props.message.role === 'assistant',
  'streaming': props.isStreaming,
  'error': props.message.status === 'error'
}))

const agentThoughts = computed(() => {
  return props.message.agent_thoughts || []
})

const workflowProcess = computed(() => {
  return props.message.workflow_run_id ? props.message.workflow_process : null
})

const messageTypeBadge = computed(() => {
  if (props.message.status === 'error') {
    return { type: 'danger', text: '错误' }
  }
  if (props.isStreaming) {
    return { type: 'info', text: '生成中' }
  }
  if (props.message.workflow_run_id) {
    return { type: 'success', text: '工作流' }
  }
  return null
})

const errorTitle = computed(() => {
  return props.message.error?.title || '处理错误'
})

const errorDescription = computed(() => {
  return props.message.error?.message || '消息处理时发生错误，请重试'
})

const renderedContent = computed(() => {
  if (!props.message.content) return ''
  
  try {
    // Configure marked with highlight.js
    marked.setOptions({
      highlight: (code, lang) => {
        if (lang && hljs.getLanguage(lang)) {
          try {
            return hljs.highlight(code, { language: lang }).value
          } catch (err) {
            console.warn('Highlight error:', err)
          }
        }
        return hljs.highlightAuto(code).value
      },
      breaks: true,
      gfm: true
    })
    
    // Convert markdown to HTML
    const html = marked(props.message.content)
    
    // Sanitize HTML
    return DOMPurify.sanitize(html, {
      ALLOWED_TAGS: [
        'p', 'br', 'strong', 'em', 'u', 's', 'code', 'pre',
        'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
        'ul', 'ol', 'li', 'blockquote',
        'a', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td'
      ],
      ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class']
    })
  } catch (error) {
    console.error('Markdown render error:', error)
    return props.message.content
  }
})

// Methods
const toggleThoughts = () => {
  showThoughts.value = !showThoughts.value
}

const getNodeStatusClass = (status: string) => ({
  'node-pending': status === 'pending',
  'node-running': status === 'running',
  'node-succeeded': status === 'succeeded',
  'node-failed': status === 'failed'
})

const getNodeProgress = (node: any) => {
  // Calculate progress based on node status and metadata
  if (node.status === 'succeeded') return 100
  if (node.status === 'failed') return 0
  if (node.status === 'running') {
    // Estimate progress based on elapsed time
    const elapsed = node.execution_metadata?.elapsed_time || 0
    return Math.min(90, elapsed * 10) // Max 90% for running
  }
  return 0
}

const formatTime = (timestamp: string | number) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatDuration = (seconds: number) => {
  if (seconds < 1) return `${Math.round(seconds * 1000)}ms`
  if (seconds < 60) return `${seconds.toFixed(1)}s`
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = Math.round(seconds % 60)
  return `${minutes}m ${remainingSeconds}s`
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatToolInput = (input: any) => {
  if (typeof input === 'string') return input
  return JSON.stringify(input, null, 2)
}

const handleCopy = async () => {
  copyLoading.value = true
  try {
    await navigator.clipboard.writeText(props.message.content)
    emit('copy-message', props.message.content)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    console.error('Copy failed:', error)
    ElMessage.error('复制失败')
  } finally {
    copyLoading.value = false
  }
}

const handleRegenerate = () => {
  emit('regenerate', props.message.id)
}

const handleLike = () => {
  const rating = props.message.feedback?.rating === 'like' ? null : 'like'
  emit('like-message', props.message.id, rating as 'like')
}

const handleDislike = () => {
  const rating = props.message.feedback?.rating === 'dislike' ? null : 'dislike'
  emit('like-message', props.message.id, rating as 'dislike')
}

const handleTTS = async () => {
  ttsLoading.value = true
  try {
    emit('tts-play', props.message.content)
    // TODO: Implement actual TTS
    ElMessage.info('TTS功能开发中...')
  } catch (error) {
    console.error('TTS error:', error)
    ElMessage.error('语音播放失败')
  } finally {
    ttsLoading.value = false
  }
}

const handleAddAnnotation = () => {
  if (annotationContent.value.trim()) {
    emit('add-annotation', props.message.id, annotationContent.value.trim())
    showAnnotationDialog.value = false
    annotationContent.value = ''
    ElMessage.success('标注已添加')
  }
}

const openCitation = (resource: RetrieverResource) => {
  // TODO: Implement citation viewer
  console.log('Open citation:', resource)
}

const downloadFile = (file: MessageFile) => {
  // TODO: Implement file download
  console.log('Download file:', file)
}

// Watch for streaming completion
watch(
  () => props.isStreaming,
  (newVal, oldVal) => {
    if (oldVal && !newVal) {
      // Streaming stopped
      emit('stream-complete')
    }
  }
)
</script>

<style scoped>
.enhanced-message-bubble {
  margin-bottom: 16px;
}

/* User Message Styles */
.user-message {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  max-width: 80%;
  margin-left: auto;
}

.user-message .message-content {
  background: var(--el-color-primary);
  color: white;
  padding: 12px 16px;
  border-radius: 18px 18px 4px 18px;
  max-width: 100%;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  box-sizing: border-box;
}

.user-message .message-text {
  line-height: 1.5;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

.user-message .message-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

/* Assistant Message Styles */
.assistant-message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: 100%;
}

.ai-avatar {
  width: 32px;
  height: 32px;
  background: var(--el-color-primary-light-8);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 4px;
}

.ai-avatar .el-icon {
  color: var(--el-color-primary);
  font-size: 16px;
}

.message-body {
  flex: 1;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}

.message-type-badge {
  margin-bottom: 8px;
}

/* Agent Thoughts */
.agent-thoughts {
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
}

.thoughts-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--el-bg-color-page);
  cursor: pointer;
  transition: background-color 0.2s;
}

.thoughts-header:hover {
  background: var(--el-color-primary-light-9);
}

.thoughts-header .toggle-icon {
  margin-left: auto;
  transition: transform 0.2s;
}

.thoughts-header .toggle-icon.expanded {
  transform: rotate(180deg);
}

.thoughts-content {
  padding: 12px;
  background: var(--el-bg-color);
}

.thought-item {
  margin-bottom: 12px;
  padding: 8px;
  background: var(--el-bg-color-page);
  border-radius: 6px;
}

.thought-item:last-child {
  margin-bottom: 0;
}

.thought-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.thought-title {
  font-weight: 600;
  color: var(--el-color-primary);
}

.thought-time {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.thought-content {
  font-size: 14px;
  line-height: 1.5;
  color: var(--el-text-color-regular);
  margin-bottom: 8px;
}

.tool-details {
  font-size: 12px;
}

.tool-input,
.tool-output {
  margin-bottom: 8px;
}

.tool-input pre,
.tool-output pre {
  background: var(--el-bg-color-darker);
  padding: 8px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 4px 0 0 0;
}

/* Workflow Process */
.workflow-process {
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 12px;
  background: var(--el-bg-color-page);
}

.workflow-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.workflow-nodes {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.workflow-node {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  background: var(--el-bg-color);
  border-radius: 6px;
  border-left: 3px solid var(--el-border-color-light);
}

.workflow-node.node-running {
  border-left-color: var(--el-color-primary);
}

.workflow-node.node-succeeded {
  border-left-color: var(--el-color-success);
}

.workflow-node.node-failed {
  border-left-color: var(--el-color-danger);
}

.node-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.node-content {
  flex: 1;
}

.node-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.node-type {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-bottom: 4px;
}

.node-progress {
  margin: 4px 0;
}

.node-metadata {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

/* Message Content */
.message-content {
  background: var(--el-bg-color-page);
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 8px;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  max-width: 100%;
  box-sizing: border-box;
}

.streaming-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.typing-dots {
  display: flex;
  gap: 4px;
}

.typing-dots span {
  width: 6px;
  height: 6px;
  background: var(--el-color-primary);
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

.rendered-content {
  line-height: 1.6;
  color: var(--el-text-color-primary);
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  max-width: 100%;
}

/* Markdown Styles */
.rendered-content :deep(h1),
.rendered-content :deep(h2),
.rendered-content :deep(h3),
.rendered-content :deep(h4),
.rendered-content :deep(h5),
.rendered-content :deep(h6) {
  margin: 16px 0 8px 0;
  font-weight: 600;
}

.rendered-content :deep(p) {
  margin: 8px 0;
}

.rendered-content :deep(code) {
  background: var(--el-bg-color-darker);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.9em;
  word-wrap: break-word;
  word-break: break-all;
}

.rendered-content :deep(pre) {
  background: var(--el-bg-color-darker);
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 12px 0;
  max-width: 100%;
  box-sizing: border-box;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.rendered-content :deep(pre code) {
  background: none;
  padding: 0;
}

.rendered-content :deep(blockquote) {
  border-left: 3px solid var(--el-color-primary);
  padding-left: 12px;
  margin: 12px 0;
  color: var(--el-text-color-regular);
}

.rendered-content :deep(ul),
.rendered-content :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.rendered-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
}

.rendered-content :deep(th),
.rendered-content :deep(td) {
  border: 1px solid var(--el-border-color-light);
  padding: 8px 12px;
  text-align: left;
}

.rendered-content :deep(th) {
  background: var(--el-bg-color-page);
  font-weight: 600;
}

/* Citations */
.citations {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.citations-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.citations-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.citation-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 8px 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.citation-item:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.citation-content {
  flex: 1;
}

.citation-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 2px;
}

.citation-snippet {
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.citation-score {
  font-size: 12px;
  color: var(--el-color-primary);
  font-weight: 600;
  margin-left: 8px;
}

/* Message Files */
.message-files {
  margin-top: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  margin-bottom: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.file-item:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.file-name {
  flex: 1;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.file-size {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.download-icon {
  color: var(--el-color-primary);
}

/* Message Actions */
.message-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.message-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.action-buttons {
  display: flex;
  gap: 4px;
}

.action-buttons .el-button {
  padding: 4px;
  min-height: auto;
}

.action-buttons .el-button.liked {
  color: var(--el-color-success);
}

.action-buttons .el-button.disliked {
  color: var(--el-color-danger);
}

/* Error Message */
.error-message {
  margin-bottom: 8px;
}

/* Transitions */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.slide-down-enter-from,
.slide-down-leave-to {
  max-height: 0;
  opacity: 0;
}

.slide-down-enter-to,
.slide-down-leave-from {
  max-height: 500px;
  opacity: 1;
}

/* Responsive */
@media (max-width: 768px) {
  .user-message {
    max-width: 90%;
  }
  
  .assistant-message {
    flex-direction: column;
    align-items: stretch;
  }
  
  .ai-avatar {
    align-self: flex-start;
  }
  
  .message-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .action-buttons {
    align-self: flex-end;
  }
}
</style>