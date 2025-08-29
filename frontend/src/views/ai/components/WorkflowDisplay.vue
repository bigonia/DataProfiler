<template>
  <div class="workflow-display">
    <div class="workflow-nodes" v-if="hasWorkflowNodes">
      <div 
        v-for="(node, index) in displayNodes" 
        :key="node.node_id || index"
        class="workflow-node"
        :class="getNodeClasses(node)"
      >
        <!-- Node Header -->
        <div class="node-header">
          <div class="node-info">
            <!-- Node Type Icon -->
            <el-icon class="node-type-icon" :class="getNodeTypeIconClass(node.node_type)">
              <component :is="getNodeTypeIcon(node.node_type)" />
            </el-icon>
            
            <!-- Node Details -->
            <div class="node-details">
              <div class="node-title">{{ getNodeTitle(node) }}</div>
              <div class="node-type">{{ formatNodeType(node.node_type) }}</div>
            </div>
          </div>
          
          <!-- Status Text -->
          <div class="node-status">
            <span class="status-text" :class="getStatusTextClass(node.status)">
              {{ getStatusText(node.status) }}
            </span>
          </div>
        </div>
        
        <!-- Simplified Node Metrics -->
        <div class="node-metrics" v-if="hasNodeMetrics(node)">
          <!-- Execution Time -->
          <div v-if="getExecutionTime(node)" class="metric-item">
            <el-icon class="metric-icon"><Timer /></el-icon>
            <span class="metric-value">{{ getExecutionTime(node) }}</span>
          </div>
          
          <!-- Token Usage -->
          <div v-if="node.execution_metadata?.total_tokens" class="metric-item">
            <el-icon class="metric-icon"><Coin /></el-icon>
            <span class="metric-value">{{ formatTokens(node.execution_metadata.total_tokens) }}</span>
          </div>
          
          <!-- Cost (if available) -->
          <div v-if="node.execution_metadata?.total_price" class="metric-item">
            <el-icon class="metric-icon"><Money /></el-icon>
            <span class="metric-value">{{ formatCost(node.execution_metadata.total_price, node.execution_metadata.currency) }}</span>
          </div>
        </div>
        
        <!-- Error Message (if any) -->
        <div v-if="node.error" class="node-error">
          <el-alert
            :title="node.error"
            type="error"
            :closable="false"
            show-icon
            size="small"
          />
        </div>
      </div>
    </div>
    
    <!-- Empty State for streaming -->
    <div v-if="hasWorkflowNodes && displayNodes.length === 0" class="empty-state">
      <div class="empty-animation">
        <el-icon class="empty-icon rotating"><Loading /></el-icon>
      </div>
      <span class="empty-text">等待AI工作流启动...</span>
    </div>
    
    <!-- No data state -->
    <div v-if="!hasWorkflowNodes" class="no-data-state">
      <div class="no-data-content">
        <el-icon class="no-data-icon"><DataAnalysis /></el-icon>
        <div class="no-data-text">
          <p class="no-data-title">暂无工作流数据</p>
          <p class="no-data-description">开始AI分析后，这里将显示详细的工作流执行状态</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useAIAnalysisStore } from '@/stores'
import { 
  Operation, Timer, Coin, Money,
  ChatDotRound, Document, DataAnalysis, Setting
} from '@element-plus/icons-vue'
import type { WorkflowNodeData } from '@/types'

// Store
const aiStore = useAIAnalysisStore()
const { messages, isStreaming } = storeToRefs(aiStore)

// Computed
const hasWorkflowNodes = computed(() => {
  return isStreaming.value || currentWorkflowNodes.value.length > 0
})

const currentWorkflowNodes = computed(() => {
  const lastMessage = messages.value[messages.value.length - 1]
  if (lastMessage && lastMessage.role === 'assistant' && lastMessage.workflowNodes) {
    return lastMessage.workflowNodes
  }
  return []
})

const workflowStatus = computed(() => {
  const nodes = currentWorkflowNodes.value
  if (nodes.length === 0) return ''
  
  const hasRunning = nodes.some(node => node.status === 'running')
  const hasFailed = nodes.some(node => node.status === 'failed')
  const allCompleted = nodes.every(node => node.status === 'succeeded')
  
  if (hasRunning) return 'running'
  if (hasFailed) return 'failed'
  if (allCompleted) return 'completed'
  return 'pending'
})

const displayNodes = computed(() => {
  // Group nodes by node_id to merge start/end states and avoid duplication
  const nodeMap = new Map()
  
  currentWorkflowNodes.value
    .filter(node => node.status !== 'waiting')
    .forEach(node => {
      const nodeId = node.node_id
      if (nodeMap.has(nodeId)) {
        // Merge with existing node, keeping the latest status and combining metadata
        const existing = nodeMap.get(nodeId)
        const merged = {
          ...existing,
          ...node,
          // Keep the earliest start time and latest end time
          created_at: Math.min(existing.created_at || Infinity, node.created_at || Infinity),
          finished_at: Math.max(existing.finished_at || 0, node.finished_at || 0),
          // Merge execution metadata
          execution_metadata: {
            ...existing.execution_metadata,
            ...node.execution_metadata
          }
        }
        nodeMap.set(nodeId, merged)
      } else {
        nodeMap.set(nodeId, { ...node })
      }
    })
  
  return Array.from(nodeMap.values())
    .sort((a, b) => {
      // Sort by index if available, otherwise by creation time
      if (a.index !== undefined && b.index !== undefined) {
        return a.index - b.index
      }
      if (a.created_at && b.created_at) {
        return a.created_at - b.created_at
      }
      return 0
    })
})

// Methods
const getWorkflowStatusType = (status: string) => {
  switch (status) {
    case 'completed': return 'success'
    case 'failed': return 'danger'
    case 'running': return 'warning'
    default: return 'info'
  }
}

const getWorkflowStatusText = (status: string) => {
  switch (status) {
    case 'completed': return '执行完成'
    case 'failed': return '执行失败'
    case 'running': return '执行中'
    case 'pending': return '等待中'
    default: return '未知状态'
  }
}



const getStatusText = (status?: string) => {
  switch (status) {
    case 'succeeded': return '完成'
    case 'failed': return '失败'
    case 'running': return '执行中'
    default: return '等待中'
  }
}

const getStatusTextClass = (status?: string) => {
  return {
    'text-success': status === 'succeeded',
    'text-error': status === 'failed',
    'text-running': status === 'running',
    'text-pending': !status || status === 'pending'
  }
}

const getNodeClasses = (node: WorkflowNodeData) => {
  return {
    'node-success': node.status === 'succeeded',
    'node-error': node.status === 'failed',
    'node-running': node.status === 'running',
    'node-pending': !node.status || node.status === 'pending',
    // Add node type classes for color differentiation
    'node-type-llm': node.node_type === 'llm',
    'node-type-code': node.node_type === 'code',
    'node-type-tool': node.node_type === 'tool',
    'node-type-knowledge': node.node_type === 'knowledge-retrieval'
  }
}

const getNodeTypeIcon = (nodeType?: string) => {
  switch (nodeType) {
    case 'llm': return ChatDotRound
    case 'code': return Document
    case 'tool': return Setting
    case 'knowledge-retrieval': return DataAnalysis
    default: return Operation
  }
}

const getNodeTypeIconClass = (nodeType?: string) => {
  return {
    'icon-llm': nodeType === 'llm',
    'icon-code': nodeType === 'code',
    'icon-tool': nodeType === 'tool',
    'icon-knowledge': nodeType === 'knowledge-retrieval'
  }
}

const formatNodeType = (nodeType?: string) => {
  const typeMap: Record<string, string> = {
    'llm': 'LLM节点',
    'code': '代码执行',
    'tool': '工具调用',
    'knowledge-retrieval': '知识检索',
    'start': '开始节点',
    'end': '结束节点'
  }
  return typeMap[nodeType || ''] || nodeType || '未知类型'
}

const getNodeTitle = (node: WorkflowNodeData) => {
  return node.title || node.node_id || '未命名节点'
}

// Unified execution time calculation to avoid duplication
const getExecutionTime = (node: WorkflowNodeData) => {
  // Priority: elapsed_time > calculated from timestamps
  if (node.elapsed_time) {
    return formatDuration(node.elapsed_time)
  }
  
  if (node.created_at && node.finished_at) {
    const duration = (node.finished_at - node.created_at) * 1000 // Convert to milliseconds
    return formatDuration(duration)
  }
  
  return null
}

const formatDuration = (seconds: number) => {
  // Input is already in seconds, no need to convert from milliseconds
  if (seconds < 60) {
    return `${seconds.toFixed(2)}s`
  }
  if (seconds < 3600) {
    return `${(seconds / 60).toFixed(1)}min`
  }
  return `${(seconds / 3600).toFixed(1)}h`
}

const formatTokens = (tokens: number) => {
  if (tokens >= 1000) {
    return `${(tokens / 1000).toFixed(1)}K tokens`
  }
  return `${tokens} tokens`
}

const formatCost = (price: number | string | undefined, currency?: string) => {
  const currencySymbol = currency === 'USD' ? '$' : '¥'
  const numPrice = typeof price === 'number' ? price : parseFloat(String(price || 0))
  return `${currencySymbol}${numPrice.toFixed(4)}`
}

const hasNodeMetrics = (node: WorkflowNodeData) => {
  return getExecutionTime(node) || 
         node.execution_metadata?.total_tokens || 
         node.execution_metadata?.total_price
}
</script>

<style scoped>
.workflow-display {
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  min-height: 80px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.workflow-display:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}



.workflow-nodes {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.workflow-node {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 10px 12px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.workflow-node::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: #e2e8f0;
  transition: all 0.3s ease;
}

.workflow-node:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.workflow-node.node-success::before {
  background: linear-gradient(135deg, #10b981, #059669);
}

.workflow-node.node-error::before {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.workflow-node.node-running::before {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  animation: pulse-glow 2s ease-in-out infinite;
}

.workflow-node.node-pending::before {
  background: linear-gradient(135deg, #6b7280, #4b5563);
}

/* Node type color differentiation */
.workflow-node.node-type-llm {
  border-left: 4px solid #667eea;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.02), rgba(118, 75, 162, 0.02));
}

.workflow-node.node-type-code {
  border-left: 4px solid #f59e0b;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.02), rgba(217, 119, 6, 0.02));
}

.workflow-node.node-type-tool {
  border-left: 4px solid #10b981;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.02), rgba(5, 150, 105, 0.02));
}

.workflow-node.node-type-knowledge {
  border-left: 4px solid #ef4444;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.02), rgba(220, 38, 38, 0.02));
}

/* Enhanced hover effects for different node types */
.workflow-node.node-type-llm:hover {
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.workflow-node.node-type-code:hover {
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.15);
}

.workflow-node.node-type-tool:hover {
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.15);
}

.workflow-node.node-type-knowledge:hover {
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.15);
}

@keyframes pulse-glow {
  0%, 100% {
    opacity: 1;
    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.4);
  }
  50% {
    opacity: 0.8;
    box-shadow: 0 0 0 8px rgba(245, 158, 11, 0);
  }
}

.node-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.node-info {
  display: flex;
  align-items: center;
  gap: 8px;
}



.node-type-icon {
  font-size: 28px;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
  min-width: 52px;
  min-height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.node-type-icon.icon-llm {
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
}

.node-type-icon.icon-code {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
}

.node-type-icon.icon-tool {
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
}

.node-type-icon.icon-knowledge {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

.node-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.node-title {
  font-weight: 600;
  color: #1e293b;
  font-size: 13px;
  line-height: 1.2;
}

.node-type {
  font-size: 11px;
  color: #64748b;
  font-weight: 500;
}

.node-status {
  display: flex;
  align-items: center;
}

.status-text {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.status-text.text-success {
  color: #059669;
  background: #d1fae5;
}

.status-text.text-error {
  color: #dc2626;
  background: #fee2e2;
}

.status-text.text-running {
  color: #d97706;
  background: #fef3c7;
  animation: pulse-text 2s ease-in-out infinite;
}

.status-text.text-pending {
  color: #4b5563;
  background: #f3f4f6;
}

@keyframes pulse-text {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.node-metrics {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #f8fafc;
  padding: 4px 8px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}

.metric-item:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.metric-icon {
  font-size: 12px;
  color: #64748b;
}

.metric-value {
  font-size: 11px;
  font-weight: 600;
  color: #374151;
}

.node-error {
  margin-top: 12px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #64748b;
}

.empty-animation {
  margin-bottom: 16px;
}

.empty-icon {
  font-size: 48px;
  color: #667eea;
  opacity: 0.6;
}

.empty-text {
  font-size: 15px;
  font-weight: 500;
}

.no-data-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  min-height: 200px;
}

.no-data-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 300px;
}

.no-data-icon {
  font-size: 64px;
  color: #cbd5e1;
  margin-bottom: 20px;
  opacity: 0.8;
}

.no-data-text {
  color: #64748b;
}

.no-data-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #374151;
}

.no-data-description {
  font-size: 14px;
  line-height: 1.5;
  margin: 0;
  opacity: 0.8;
}

/* Responsive Design */
@media (max-width: 768px) {
  .workflow-display {
    padding: 16px;
  }
  
  .workflow-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .node-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .node-metrics {
    gap: 8px;
  }
  
  .metric-item {
    padding: 4px 8px;
  }
}

@media (max-width: 480px) {
  .workflow-display {
    padding: 12px;
  }
  
  .workflow-node {
    padding: 12px;
  }
  
  .node-info {
    gap: 8px;
  }
  
  .node-status-icon {
    width: 28px;
    height: 28px;
  }
  
  .status-icon {
    font-size: 14px;
  }
  
  .node-type-icon {
    font-size: 16px;
    padding: 6px;
  }
}
</style>