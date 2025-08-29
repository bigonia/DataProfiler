<template>
  <div class="message-bubble" :class="messageClasses">
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
        
        <div class="message-time">{{ formatTime(Number(message.timestamp)) }}</div>
      </div>
      <div class="user-avatar">
        <el-icon><User /></el-icon>
      </div>
    </div>

    <!-- AI Message -->
    <div v-else class="ai-message">
      <div class="ai-avatar">
        <el-icon><ChatDotRound /></el-icon>
      </div>
      
      <div class="message-content">
        <!-- Message Header -->
        <div class="message-header" v-if="message.type || messageTypeBadge">
          <el-tag :type="getMessageTypeTag(message.type || messageTypeBadge?.type)" size="small">
            {{ getMessageTypeLabel(message.type) || messageTypeBadge?.text }}
          </el-tag>
        </div>
        
        <!-- Agent Thoughts -->
        <div v-if="agentThoughts.length > 0" class="agent-thoughts">
          <div class="thoughts-header" @click="toggleThoughts">
            <el-icon class="thought-icon"><ChatDotRound /></el-icon>
            <span class="thoughts-title">AI 思维过程</span>
            <el-icon class="expand-icon" :class="{ 'expanded': showThoughts }">
              <ArrowDown />
            </el-icon>
          </div>
          
          <transition name="slide-down">
            <div v-show="showThoughts" class="thoughts-content">
              <div
                v-for="(thought, index) in agentThoughts"
                :key="thought.id"
                class="thought-item"
              >
                <div class="thought-header">
                  <span class="thought-position">步骤 {{ index + 1 }}</span>
                  <span class="thought-tool" v-if="thought.tool">{{ thought.tool }}</span>
                </div>
                <div class="thought-content">{{ thought.thought }}</div>
                <div v-if="thought.observation" class="thought-observation">
                  <strong>观察:</strong> {{ thought.observation }}
                </div>
              </div>
            </div>
          </transition>
        </div>
        
        <!-- Workflow Process -->
        <div v-if="workflowProcess" class="workflow-section">
          <WorkflowVisualization
            :workflow-process="workflowProcess"
            :auto-expand="false"
          />
        </div>
        
        <!-- Message Body -->
        <div class="message-body">
          <!-- Error State -->
          <div v-if="message.type === 'error'" class="error-content">
            <el-alert
              :title="message.content"
              type="error"
              :closable="false"
              show-icon
            />
          </div>
          
          <!-- Normal Message with Enhanced Markdown -->
          <div v-else class="message-text" :class="{ 'streaming': isStreaming }">
            <div 
              class="markdown-content"
              v-html="renderedMarkdown"
            ></div>
            
            <!-- Streaming Indicator -->
            <div v-if="isStreaming" class="streaming-indicator">
              <el-icon class="spinning"><Loading /></el-icon>
              <span>AI 正在思考...</span>
            </div>
          </div>
        </div>
        
        <!-- Citations -->
        <div v-if="citations.length > 0" class="citations-section">
          <div class="citations-header">
            <el-icon><Link /></el-icon>
            <span>引用资料</span>
          </div>
          <div class="citations-list">
            <div
              v-for="citation in citations"
              :key="citation.id"
              class="citation-item"
              @click="openCitation(citation)"
            >
              <div class="citation-content">
                <div class="citation-title">{{ citation.title }}</div>
                <div class="citation-snippet">{{ citation.content }}</div>
              </div>
              <div class="citation-score">{{ (citation.score * 100).toFixed(1) }}%</div>
            </div>
          </div>
        </div>
        
        <!-- Message Files -->
        <div v-if="messageFiles.length > 0" class="message-files">
          <div class="files-header">
            <el-icon><Folder /></el-icon>
            <span>相关文件</span>
          </div>
          <div class="files-list">
            <div
              v-for="file in messageFiles"
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
        
        <!-- Message Footer -->
        <div class="message-footer">
          <div class="message-meta">
            <span class="message-time">{{ formatTime(message.timestamp) }}</span>
            <span v-if="message.tokens" class="token-count">
              <el-icon><Coin /></el-icon>
              {{ message.tokens }}
            </span>
          </div>
          
          <div class="message-actions">
            <el-tooltip content="复制" placement="top">
              <el-button
                type="text"
                size="small"
                :icon="DocumentCopy"
                @click="handleCopyMessage"
                class="action-btn"
              />
            </el-tooltip>
            
            <el-tooltip content="重新生成" placement="top">
              <el-button
                type="text"
                size="small"
                :icon="Refresh"
                @click="handleRegenerateMessage"
                :disabled="isStreaming"
                class="action-btn"
              />
            </el-tooltip>
            
            <el-tooltip content="点赞" placement="top">
              <el-button
                type="text"
                size="small"
                :icon="Star"
                @click="handleLikeMessage"
                :class="{ 'liked': message.liked }"
                class="action-btn like-btn"
              />
            </el-tooltip>
            
            <el-tooltip content="语音播报" placement="top">
              <el-button
                type="text"
                size="small"
                :icon="Microphone"
                @click="handleTTS"
                :disabled="isStreaming"
                class="action-btn"
              />
            </el-tooltip>
            
            <el-tooltip content="添加标注" placement="top">
              <el-button
                type="text"
                size="small"
                :icon="EditPen"
                @click="showAnnotationDialog = true"
                class="action-btn"
              />
            </el-tooltip>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Annotation Dialog -->
    <el-dialog
      v-model="showAnnotationDialog"
      title="添加标注"
      width="400px"
      :before-close="() => { annotationText = ''; showAnnotationDialog = false }"
    >
      <el-input
        v-model="annotationText"
        type="textarea"
        :rows="4"
        placeholder="请输入您的标注内容..."
        maxlength="500"
        show-word-limit
      />
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAnnotationDialog = false">取消</el-button>
          <el-button type="primary" @click="handleAddAnnotation" :disabled="!annotationText.trim()">
            添加标注
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { ElMessage, ElAvatar, ElTag, ElButton, ElDialog, ElInput } from 'element-plus'
import { 
  ChatDotRound, 
  User, 
  DocumentCopy, 
  Refresh, 
  Star, 
  Coin,
  Document,
  ArrowDown,
  Loading,
  Link,
  Folder,
  Download,
  Microphone,
  EditPen
} from '@element-plus/icons-vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
import WorkflowVisualization from '@/components/chat/WorkflowVisualization.vue'
import type { Message, AgentThought, WorkflowProcess, Citation, MessageFile } from '@/types'

// Props
interface Props {
  message: Message
  isStreaming?: boolean
  rawStreamData?: string // Store original backend response for debugging
  agentThoughts?: AgentThought[]
  workflowProcess?: WorkflowProcess | null
  citations?: Citation[]
  messageFiles?: MessageFile[]
}

const props = withDefaults(defineProps<Props>(), {
  isStreaming: false,
  rawStreamData: '',
  agentThoughts: () => [],
  workflowProcess: null,
  citations: () => [],
  messageFiles: () => []
})

// Emits
interface Emits {
  (e: 'copy-message', content: string): void
  (e: 'regenerate', messageId: string): void
  (e: 'like-message', messageId: string): void
  (e: 'stream-complete', data: { messageId: string, rawData: string, processedContent: string }): void
  (e: 'tts', content: string): void
  (e: 'add-annotation', messageId: string, annotation: string): void
}

const emit = defineEmits<Emits>()

// Local state
const originalRawData = ref<string>('')
const processedContent = ref<string>('')
const showThoughts = ref(false)
const showAnnotationDialog = ref(false)
const annotationText = ref('')

// Enhanced markdown configuration for streaming content
marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: false,
  mangle: false,
  pedantic: false,
  sanitize: false,
  smartypants: false,
  highlight: (code, lang) => {
    if (!code || !code.trim()) return code;
    try {
      const language = hljs.getLanguage(lang) ? lang : 'plaintext';
      return hljs.highlight(code, { language }).value;
    } catch (error) {
      console.warn('Code highlighting failed:', error);
      return code;
    }
  }
})

// Process streaming chunk data to reconstruct markdown content
const processStreamingData = (rawData: string): string => {
  if (!rawData) return ''
  
  try {
    // Split by lines and process each event:chunk pair
    const lines = rawData.split('\n')
    let content = ''
    
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i].trim()
      
      // Look for event:chunk lines
      if (line === 'event:chunk') {
        const nextLine = lines[i + 1]
        if (nextLine && nextLine.startsWith('data:')) {
          const chunkData = nextLine.substring(5) // Remove 'data:' prefix
          
          // Empty chunk data represents line breaks
          if (chunkData === '') {
            content += '\n'
          } else {
            content += chunkData
          }
        }
        i++ // Skip the data line as we've processed it
      }
    }
    
    return content
  } catch (error) {
    console.error('Error processing streaming data:', error)
    return rawData
  }
}

// Process message content for markdown rendering
const processMessageContent = (content: string, isRawStream: boolean = false): string => {
  if (!content) return ''
  
  let processedText = content
  
  // If this is raw streaming data, process it first
  if (isRawStream) {
    processedText = processStreamingData(content)
  }
  
  // Additional processing for better markdown rendering
  processedText = processedText
    // Normalize line breaks
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    // Handle multiple consecutive line breaks
    .replace(/\n{3,}/g, '\n\n')
    // Trim leading/trailing whitespace
    .trim()
  
  return processedText
}

// Computed
const messageClasses = computed(() => ({
  'user-message-wrapper': props.message.role === 'user',
  'ai-message-wrapper': props.message.role === 'assistant',
  'error-message': props.message.type === 'error',
  'streaming-message': props.isStreaming
}))

// Enhanced markdown rendering with streaming data support
const renderedMarkdown = computed(() => {
  if (!props.message.content) return ''
  
  try {
    // Determine if we're dealing with raw streaming data or processed content
    const isRawStreamData = props.rawStreamData || props.message.content.includes('event:chunk')
    
    // Process the content appropriately
    const processedText = processMessageContent(
      props.rawStreamData || props.message.content, 
      isRawStreamData
    )
    
    // Store processed content for debugging
    processedContent.value = processedText
    
    // Convert to HTML using marked
    const rawHtml = marked(processedText)
    
    // Sanitize the HTML
    return DOMPurify.sanitize(rawHtml, {
      ALLOWED_TAGS: [
        'p', 'br', 'strong', 'em', 'u', 's', 'code', 'pre', 
        'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
        'ul', 'ol', 'li', 'blockquote', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
        'a', 'img', 'div', 'span', 'i', 'b', 'del', 'ins', 'sub', 'sup',
        'hr', 'mark', 'small', 'cite', 'q', 'dfn', 'abbr', 'time', 'kbd', 'samp', 'var'
      ],
      ALLOWED_ATTR: [
        'href', 'target', 'rel', 'src', 'alt', 'title', 'class', 'id', 'style',
        'data-*', 'aria-*', 'role', 'tabindex', 'lang', 'dir'
      ],
      ALLOW_DATA_ATTR: true,
      ALLOW_ARIA_ATTR: true,
      KEEP_CONTENT: true,
      SANITIZE_DOM: false
     })
   } catch (error) {
     console.error('Enhanced markdown parsing error:', error)
     console.error('Raw content:', props.message.content)
     console.error('Raw stream data:', props.rawStreamData)
     
     // Fallback: return escaped content with basic line break handling
     return props.message.content
       .replace(/&/g, '&amp;')
       .replace(/</g, '&lt;')
       .replace(/>/g, '&gt;')
       .replace(/\n\n/g, '</p><p>')
       .replace(/\n/g, '<br>')
       .replace(/^/, '<p>')
       .replace(/$/, '</p>')
   }
 })

// Watch for streaming completion and emit debug data
watch(
  () => props.isStreaming,
  (newStreaming, oldStreaming) => {
    // When streaming stops (from true to false), emit debug data
    if (oldStreaming && !newStreaming) {
      const debugData = {
        messageId: props.message.id,
        rawData: props.rawStreamData || props.message.content,
        processedContent: processedContent.value
      }
      
      // Console output for debugging
      console.group(`🔍 AI Message Debug - ${props.message.id}`)
      console.log('📥 Raw Backend Data:', debugData.rawData)
      console.log('⚙️ Processed Content:', debugData.processedContent)
      console.log('🎨 Rendered HTML:', renderedMarkdown.value)
      console.groupEnd()
      
      // Emit event for parent component
      emit('stream-complete', debugData)
    }
  }
)

// Store original raw data when component mounts
onMounted(() => {
  originalRawData.value = props.rawStreamData || props.message.content
})

// Methods
const formatTime = (timestamp?: number | string) => {
  if (!timestamp) return ''
  
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  
  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getMessageTypeTag = (type: string) => {
  switch (type) {
    case 'error': return 'danger'
    case 'warning': return 'warning'
    case 'success': return 'success'
    default: return 'info'
  }
}

const getMessageTypeLabel = (type: string) => {
  switch (type) {
    case 'error': return '错误'
    case 'warning': return '警告'
    case 'success': return '成功'
    case 'system': return '系统'
    default: return '信息'
  }
}

const handleCopyMessage = async () => {
  try {
    await navigator.clipboard.writeText(props.message.content)
    ElMessage.success('已复制到剪贴板')
    emit('copy-message', props.message.content)
  } catch (error) {
    console.error('Copy failed:', error)
    ElMessage.error('复制失败')
  }
}

const handleRegenerateMessage = () => {
  emit('regenerate', props.message.id)
}

const handleLikeMessage = () => {
  emit('like-message', props.message.id)
}

const handleTTS = () => {
  emit('tts', props.message.content)
}

const handleAddAnnotation = () => {
  if (annotationText.value.trim()) {
    emit('add-annotation', props.message.id, annotationText.value.trim())
    annotationText.value = ''
    showAnnotationDialog.value = false
    ElMessage.success('标注已添加')
  }
}

const toggleThoughts = () => {
  showThoughts.value = !showThoughts.value
}

const openCitation = (citation: Citation) => {
  if (citation.url) {
    window.open(citation.url, '_blank')
  }
}

const downloadFile = (file: MessageFile) => {
  if (file.url) {
    const link = document.createElement('a')
    link.href = file.url
    link.download = file.name
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// Computed properties
const messageTypeBadge = computed(() => {
  if (props.message.type) {
    return {
      type: getMessageTypeTag(props.message.type),
      text: getMessageTypeLabel(props.message.type)
    }
  }
  return null
})
</script>

<style scoped>
.message-bubble {
  margin-bottom: 16px;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* User Message */
.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
  gap: 8px;
}

.user-message .message-content {
  background: linear-gradient(135deg, #409eff, #67c23a);
  color: white;
  padding: 12px 16px;
  border-radius: 16px 16px 4px 16px;
  max-width: 70%;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.user-message .message-text {
  font-size: 14px;
  line-height: 1.5;
  word-wrap: break-word;
}

.user-message .message-time {
  font-size: 11px;
  opacity: 0.8;
  margin-top: 4px;
  text-align: right;
}

.user-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  flex-shrink: 0;
}

/* AI Message */
.ai-message {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.ai-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  flex-shrink: 0;
  margin-top: 2px;
}

.ai-message .message-content {
  flex: 1;
  max-width: 95%;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 4px 16px 16px 16px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  transition: box-shadow 0.2s;
  min-width: 0; /* Crucial for flexbox wrapping */
}

.ai-message .message-content:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* Message Header */
.message-header {
  padding: 8px 12px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

/* Message Body */
.message-body {
  padding: 12px;
}

.message-text {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  word-wrap: break-word;
}

.message-text.streaming::after {
  content: '▋';
  color: #409eff;
  animation: blink 1s infinite;
  margin-left: 2px;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* Enhanced Markdown Styles for Streaming Content */
.markdown-content {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
  line-height: 1.7;
  overflow-wrap: break-word;
  word-wrap: break-word;
  word-break: break-word;
  max-width: 100%;
  min-width: 0;
  white-space: pre-wrap;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  /* Enhanced spacing for better readability */
  letter-spacing: 0.01em;
  color: #2c3e50;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin: 20px 0 12px 0;
  font-weight: 600;
  color: #303133;
  line-height: 1.3;
}

.markdown-content :deep(h1:first-child),
.markdown-content :deep(h2:first-child),
.markdown-content :deep(h3:first-child),
.markdown-content :deep(h4:first-child),
.markdown-content :deep(h5:first-child),
.markdown-content :deep(h6:first-child) {
  margin-top: 0;
}

.markdown-content :deep(p) {
  margin: 14px 0;
  line-height: 1.8;
  text-align: left;
  color: #34495e;
  font-size: 14px;
}

.markdown-content :deep(p:first-child) {
  margin-top: 0;
}

.markdown-content :deep(p:last-child) {
  margin-bottom: 0;
}

/* Enhanced paragraph spacing for streaming content */
.markdown-content :deep(p + p) {
  margin-top: 18px;
}

.markdown-content :deep(p:empty) {
  margin: 10px 0;
  min-height: 1.2em;
}

/* Handle streaming artifacts - empty paragraphs */
.markdown-content :deep(p:empty:first-child) {
  display: none;
}

.markdown-content :deep(p:empty:last-child) {
  display: none;
}

.markdown-content :deep(h1) { font-size: 20px; }
.markdown-content :deep(h2) { font-size: 18px; }
.markdown-content :deep(h3) { font-size: 16px; }
.markdown-content :deep(h4) { font-size: 15px; }
.markdown-content :deep(h5) { font-size: 14px; }
.markdown-content :deep(h6) { font-size: 13px; color: #606266; }

.markdown-content :deep(strong) {
  font-weight: 600;
  color: #303133;
}

.markdown-content :deep(em) {
  font-style: italic;
  color: #606266;
}

.markdown-content :deep(code) {
  background: #f8f9fa;
  color: #d73a49;
  padding: 3px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'Consolas', 'Monaco', 'SF Mono', 'Courier New', monospace;
  font-weight: 500;
  border: 1px solid #e1e4e8;
  word-break: break-all;
}

/* Enhanced Code Block Styles for Streaming Content */
.markdown-content :deep(pre) {
  margin: 16px 0;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', 'SF Mono', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre;
  word-wrap: normal;
  word-break: normal;
  background: #f6f8fa;
  border: 1px solid #e1e4e8;
  max-width: 100%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  position: relative;
}

.markdown-content :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
  border-radius: 0;
  font-size: inherit;
  white-space: pre;
  word-break: normal;
  border: none;
  font-weight: normal;
}

.markdown-content :deep(pre code.hljs) {
  padding: 0;
  background: transparent;
  color: inherit;
  border-radius: 0;
}

/* Code block language indicator */
.markdown-content :deep(pre::before) {
  content: attr(data-lang);
  position: absolute;
  top: 8px;
  right: 12px;
  font-size: 11px;
  color: #6a737d;
  text-transform: uppercase;
  font-weight: 600;
  letter-spacing: 0.5px;
}

/* List Styles */
.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.markdown-content :deep(li) {
  margin: 4px 0;
  line-height: 1.5;
}

/* Blockquote Styles */
.markdown-content :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 12px;
  background: #f8f9fa;
  border-left: 3px solid #409eff;
  border-radius: 0 4px 4px 0;
  color: #606266;
  font-style: italic;
}

/* Table Styles */
.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin: 12px 0;
  overflow-x: auto;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  display: block;
}

.markdown-content :deep(th) {
  background: #f8f9fa;
  padding: 8px 12px;
  text-align: left;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
}

.markdown-content :deep(td) {
  padding: 8px 12px;
  border-bottom: 1px solid #f0f2f5;
  color: #606266;
}

.markdown-content :deep(tr:hover) {
  background: #f8f9fa;
}

/* Link Styles */
.markdown-content :deep(a) {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
}

.markdown-content :deep(a:hover) {
  color: #67c23a;
  text-decoration: underline;
}

/* Image Styles */
.markdown-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 8px 0;
}

/* Error Content */
.error-content {
  margin: 8px 0;
}

/* Message Footer */
.message-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f8f9fa;
  border-top: 1px solid #f0f2f5;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: #909399;
}

.message-time {
  font-weight: 500;
}

.token-count {
  display: flex;
  align-items: center;
  gap: 2px;
  background: #e9ecef;
  padding: 2px 6px;
  border-radius: 10px;
}

.message-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}

.ai-message:hover .message-actions {
  opacity: 1;
}

.action-btn {
  width: 28px;
  height: 28px;
  color: #909399;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #e9ecef;
  color: #606266;
}

.like-btn.liked {
  color: #f56c6c;
  background: #fef0f0;
}

.like-btn.liked:hover {
  background: #fde2e2;
  color: #f56c6c;
}

/* Agent Thoughts */
.agent-thoughts {
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
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

.thought-icon {
  color: var(--el-color-primary);
}

.thoughts-title {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.expand-icon {
  transition: transform 0.2s;
}

.expand-icon.expanded {
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
  border-radius: 4px;
  border-left: 3px solid var(--el-color-primary);
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

.thought-position {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.thought-tool {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  background: var(--el-bg-color);
  padding: 2px 6px;
  border-radius: 10px;
}

.thought-content {
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.thought-observation {
  font-size: 12px;
  color: var(--el-text-color-regular);
  font-style: italic;
}

/* Workflow Section */
.workflow-section {
  margin-bottom: 12px;
}

/* Streaming Indicator */
.streaming-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 12px;
  background: var(--el-color-primary-light-9);
  border-radius: 6px;
  font-size: 12px;
  color: var(--el-color-primary);
}

.streaming-indicator .spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Citations Section */
.citations-section {
  margin-top: 12px;
  padding: 12px;
  background: var(--el-bg-color-page);
  border-radius: 6px;
  border: 1px solid var(--el-border-color-lighter);
}

.citations-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.citations-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.citation-item {
  display: flex;
  align-items: center;
  padding: 8px;
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

.files-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.files-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
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

/* Slide Down Transition */
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
  .user-message .message-content,
  .ai-message .message-content {
    max-width: 85%;
  }
  
  .message-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .message-actions {
    opacity: 1;
  }
  
  .thoughts-content {
    padding: 8px;
  }
  
  .citations-section {
    padding: 8px;
  }
}

@media (max-width: 480px) {
  .user-message .message-content,
  .ai-message .message-content {
    max-width: 95%;
  }
  
  .message-body {
    padding: 8px;
  }
  
  .message-footer {
    padding: 6px 8px;
  }
  
  .thought-item {
    padding: 6px;
  }
  
  .citation-item {
    padding: 6px;
  }
}
</style>