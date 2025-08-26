<template>
  <div class="workflow-display">
    <div class="workflow-header">
      <el-icon class="workflow-icon"><Operation /></el-icon>
      <span class="workflow-title">AI工作流执行过程</span>
      <el-tag :type="getOverallStatusType()" size="small">
        {{ getOverallStatusText() }}
      </el-tag>
    </div>
    
    <div class="workflow-nodes">
      <div 
        v-for="(node, index) in sortedNodes" 
        :key="node.node_id || index"
        class="workflow-node"
        :class="getNodeClasses(node)"
      >
        <!-- Node Header -->
        <div class="node-header">
          <div class="node-info">
            <el-icon class="node-icon" :class="getNodeIconClass(node)">
              <component :is="getNodeIcon(node)" />
            </el-icon>
            <div class="node-details">
              <div class="node-title">{{ node.title || node.node_id }}</div>
              <div class="node-type">{{ formatNodeType(node.node_type) }}</div>
            </div>
          </div>
          <el-tag :type="getStatusType(node.status)" size="small">
            {{ getStatusText(node.status) }}
          </el-tag>
        </div>
        
        <!-- Node Content -->
        <div class="node-content" v-if="showNodeDetails">
          <!-- Execution Time -->
          <div v-if="node.elapsed_time" class="node-metric">
            <el-icon><Timer /></el-icon>
            <span>执行时间: {{ formatDuration(node.elapsed_time) }}</span>
          </div>
          
          <!-- Token Usage -->
          <div v-if="node.execution_metadata?.total_tokens" class="node-metric">
            <el-icon><Coin /></el-icon>
            <span>Token使用: {{ node.execution_metadata.total_tokens }}</span>
          </div>
          
          <!-- Cost -->
          <div v-if="node.execution_metadata?.total_price" class="node-metric">
            <el-icon><Money /></el-icon>
            <span>成本: {{ node.execution_metadata.total_price }} {{ node.execution_metadata.currency || 'USD' }}</span>
          </div>
          
          <!-- Error Message -->
          <div v-if="node.error" class="node-error">
            <el-alert
              :title="node.error"
              type="error"
              :closable="false"
              show-icon
            />
          </div>
        </div>
        
        <!-- Connection Line -->
        <div v-if="index < sortedNodes.length - 1" class="connection-line"></div>
      </div>
    </div>
    
    <!-- Toggle Details Button -->
    <div class="workflow-actions">
      <el-button 
        type="text" 
        size="small" 
        @click="showNodeDetails = !showNodeDetails"
      >
        <el-icon><component :is="showNodeDetails ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
        {{ showNodeDetails ? '隐藏详情' : '显示详情' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { 
  Operation, Timer, Coin, Money, ArrowUp, ArrowDown,
  Check, Loading, Close, Warning, ChatDotRound, 
  Document, DataAnalysis, Setting
} from '@element-plus/icons-vue'
import type { WorkflowNodeData } from '@/types'

// Props
interface Props {
  nodes: WorkflowNodeData[]
}

const props = defineProps<Props>()

// State
const showNodeDetails = ref(false)

// Computed
const sortedNodes = computed(() => {
  return [...props.nodes].sort((a, b) => {
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
const getOverallStatusType = () => {
  const hasError = props.nodes.some(node => node.status === 'failed')
  const hasRunning = props.nodes.some(node => node.status === 'running')
  const allCompleted = props.nodes.every(node => node.status === 'succeeded')
  
  if (hasError) return 'danger'
  if (hasRunning) return 'warning'
  if (allCompleted) return 'success'
  return 'info'
}

const getOverallStatusText = () => {
  const hasError = props.nodes.some(node => node.status === 'failed')
  const hasRunning = props.nodes.some(node => node.status === 'running')
  const allCompleted = props.nodes.every(node => node.status === 'succeeded')
  
  if (hasError) return '执行失败'
  if (hasRunning) return '执行中'
  if (allCompleted) return '执行完成'
  return '准备中'
}

const getStatusType = (status?: string) => {
  switch (status) {
    case 'succeeded': return 'success'
    case 'failed': return 'danger'
    case 'running': return 'warning'
    default: return 'info'
  }
}

const getStatusText = (status?: string) => {
  switch (status) {
    case 'succeeded': return '成功'
    case 'failed': return '失败'
    case 'running': return '运行中'
    default: return '等待中'
  }
}

const getNodeClasses = (node: WorkflowNodeData) => {
  return {
    'node-success': node.status === 'succeeded',
    'node-error': node.status === 'failed',
    'node-running': node.status === 'running'
  }
}

const getNodeIcon = (node: WorkflowNodeData) => {
  switch (node.node_type) {
    case 'llm': return ChatDotRound
    case 'code': return Document
    case 'tool': return Setting
    case 'knowledge-retrieval': return DataAnalysis
    default: return Operation
  }
}

const getNodeIconClass = (node: WorkflowNodeData) => {
  return {
    'icon-success': node.status === 'succeeded',
    'icon-error': node.status === 'failed',
    'icon-running': node.status === 'running'
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

const formatDuration = (ms: number) => {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${(ms / 60000).toFixed(1)}min`
}
</script>

<style scoped>
.workflow-display {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 16px;
  margin: 12px 0;
}

.workflow-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e9ecef;
}

.workflow-icon {
  color: #409eff;
  font-size: 18px;
}

.workflow-title {
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.workflow-nodes {
  position: relative;
}

.workflow-node {
  position: relative;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.workflow-node:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.workflow-node.node-success {
  border-left: 4px solid #67c23a;
}

.workflow-node.node-error {
  border-left: 4px solid #f56c6c;
}

.workflow-node.node-running {
  border-left: 4px solid #e6a23c;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.node-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.node-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.node-icon {
  font-size: 20px;
  color: #909399;
}

.node-icon.icon-success {
  color: #67c23a;
}

.node-icon.icon-error {
  color: #f56c6c;
}

.node-icon.icon-running {
  color: #e6a23c;
}

.node-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.node-title {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.node-type {
  font-size: 12px;
  color: #909399;
}

.node-content {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.node-metric {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #606266;
}

.node-metric .el-icon {
  font-size: 14px;
  color: #909399;
}

.node-error {
  margin-top: 8px;
}

.connection-line {
  position: absolute;
  left: 50%;
  bottom: -12px;
  width: 2px;
  height: 8px;
  background: #e4e7ed;
  transform: translateX(-50%);
}

.workflow-actions {
  text-align: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e9ecef;
}
</style>