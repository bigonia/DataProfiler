<template>
  <div class="workflow-status-card" v-if="hasActiveWorkflow">
    <el-card class="status-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-info">
            <el-icon class="workflow-icon" :class="getStatusIconClass()">
              <component :is="getStatusIcon()" />
            </el-icon>
            <span class="header-title">AI工作流</span>
          </div>
          <el-button
            type="text"
            size="small"
            @click="toggleExpanded"
            :icon="expanded ? ArrowDown : ArrowUp"
          />
        </div>
      </template>
      
      <!-- Workflow Progress -->
      <div class="workflow-progress">
        <div class="progress-info">
          <span class="progress-text">{{ getProgressText() }}</span>
          <el-tag :type="getOverallStatusType()" size="small">
            {{ getOverallStatusText() }}
          </el-tag>
        </div>
        
        <el-progress
          :percentage="progressPercentage"
          :status="getProgressStatus()"
          :stroke-width="6"
          class="progress-bar"
        />
      </div>
      
      <!-- Expanded Content -->
      <div v-if="expanded" class="expanded-content">
        <div class="workflow-nodes">
          <div 
            v-for="(node, index) in recentNodes" 
            :key="node.node_id || index"
            class="workflow-node"
            :class="getNodeClasses(node)"
          >
            <div class="node-info">
              <el-icon class="node-icon" :class="getNodeIconClass(node)">
                <component :is="getNodeIcon(node)" />
              </el-icon>
              <div class="node-details">
                <div class="node-title">{{ node.title || node.node_id }}</div>
                <div class="node-status">{{ getStatusText(node.status) }}</div>
              </div>
            </div>
            <div v-if="node.elapsed_time" class="node-time">
              {{ formatDuration(node.elapsed_time) }}
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useAIAnalysisStore } from '@/stores'
import { 
  ArrowUp, ArrowDown, Loading, Check, Close, Warning,
  Operation, ChatDotRound, Document, DataAnalysis, Setting
} from '@element-plus/icons-vue'
import type { WorkflowNodeData } from '@/types'

// Store
const aiStore = useAIAnalysisStore()
const { messages, isStreaming } = storeToRefs(aiStore)

// State
const expanded = ref(false)

// Computed
const hasActiveWorkflow = computed(() => {
  return isStreaming.value || currentWorkflowNodes.value.length > 0
})

const currentWorkflowNodes = computed(() => {
  const lastMessage = messages.value[messages.value.length - 1]
  if (lastMessage && lastMessage.role === 'assistant' && lastMessage.workflowNodes) {
    return lastMessage.workflowNodes
  }
  return []
})

const recentNodes = computed(() => {
  return currentWorkflowNodes.value.slice(-5) // Show last 5 nodes
})

const progressPercentage = computed(() => {
  const nodes = currentWorkflowNodes.value
  if (nodes.length === 0) return 0
  
  const completedNodes = nodes.filter(node => 
    node.status === 'succeeded' || node.status === 'failed'
  ).length
  
  return Math.round((completedNodes / nodes.length) * 100)
})

// Methods
const toggleExpanded = () => {
  expanded.value = !expanded.value
}

const getOverallStatusType = () => {
  const nodes = currentWorkflowNodes.value
  const hasError = nodes.some(node => node.status === 'failed')
  const hasRunning = nodes.some(node => node.status === 'running')
  const allCompleted = nodes.every(node => 
    node.status === 'succeeded' || node.status === 'failed'
  )
  
  if (hasError) return 'danger'
  if (hasRunning || isStreaming.value) return 'warning'
  if (allCompleted && nodes.length > 0) return 'success'
  return 'info'
}

const getOverallStatusText = () => {
  const nodes = currentWorkflowNodes.value
  const hasError = nodes.some(node => node.status === 'failed')
  const hasRunning = nodes.some(node => node.status === 'running')
  const allCompleted = nodes.every(node => 
    node.status === 'succeeded' || node.status === 'failed'
  )
  
  if (hasError) return '执行失败'
  if (hasRunning || isStreaming.value) return '执行中'
  if (allCompleted && nodes.length > 0) return '执行完成'
  return '准备中'
}

const getProgressText = () => {
  const nodes = currentWorkflowNodes.value
  const completedNodes = nodes.filter(node => 
    node.status === 'succeeded' || node.status === 'failed'
  ).length
  
  return `${completedNodes}/${nodes.length} 节点完成`
}

const getProgressStatus = () => {
  const hasError = currentWorkflowNodes.value.some(node => node.status === 'failed')
  if (hasError) return 'exception'
  if (isStreaming.value) return undefined
  return 'success'
}

const getStatusIcon = () => {
  const type = getOverallStatusType()
  switch (type) {
    case 'success': return Check
    case 'danger': return Close
    case 'warning': return Loading
    default: return Operation
  }
}

const getStatusIconClass = () => {
  const type = getOverallStatusType()
  return {
    'icon-success': type === 'success',
    'icon-error': type === 'danger',
    'icon-running': type === 'warning'
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
</script>

<style scoped>
.workflow-status-card {
  position: fixed;
  bottom: 24px;
  left: 24px;
  width: 360px;
  z-index: 1000;
  animation: slideInLeft 0.3s ease-out;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.workflow-status-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #409eff, #67c23a, #e6a23c, #f56c6c);
  background-size: 200% 100%;
  animation: gradientShift 3s ease-in-out infinite;
  z-index: 1;
  border-radius: 12px 12px 0 0;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.workflow-status-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 16px 50px rgba(0, 0, 0, 0.2);
}

.status-card {
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(20px);
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.3);
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.05) 0%, rgba(103, 194, 58, 0.05) 100%);
}

.header-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.workflow-icon {
  font-size: 18px;
  color: #409eff;
}

.workflow-icon.icon-success {
  color: #67c23a;
}

.workflow-icon.icon-error {
  color: #f56c6c;
}

.node-icon.icon-running {
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.15);
  animation: spin 2s linear infinite;
}

.header-title {
  font-weight: 600;
  color: #303133;
}

.workflow-progress {
  margin: 20px 0;
  padding: 0 24px;
}

.progress-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.progress-text {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.progress-bar {
  margin-bottom: 0;
  border-radius: 8px;
}

.expanded-content {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
  margin-top: 16px;
}

.workflow-nodes {
  max-height: 240px;
  overflow-y: auto;
  padding: 0 24px 20px;
}

.workflow-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  margin-bottom: 10px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  border-left: 4px solid #e4e7ed;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(5px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.workflow-node:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateX(4px) translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.workflow-node.node-success {
  border-left-color: #67c23a;
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.08), rgba(103, 194, 58, 0.03));
}

.workflow-node.node-error {
  border-left-color: #f56c6c;
  background: linear-gradient(135deg, rgba(245, 108, 108, 0.08), rgba(245, 108, 108, 0.03));
}

.workflow-node.node-running {
  border-left-color: #e6a23c;
  background: linear-gradient(135deg, rgba(230, 162, 60, 0.08), rgba(230, 162, 60, 0.03));
  animation: nodeRunning 2s ease-in-out infinite;
}

@keyframes nodeRunning {
  0%, 100% { box-shadow: 0 2px 8px rgba(230, 162, 60, 0.1); }
  50% { box-shadow: 0 4px 16px rgba(230, 162, 60, 0.2); }
}

.node-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.node-icon {
  font-size: 24px;
  color: #909399;
  padding: 8px;
  border-radius: 6px;
  background: rgba(144, 147, 153, 0.1);
  min-width: 40px;
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.node-icon.icon-success {
  color: #67c23a;
  background: rgba(103, 194, 58, 0.15);
}

.node-icon.icon-error {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.15);
}

.node-icon.icon-running {
  color: #e6a23c;
}

.node-details {
  flex: 1;
}

.node-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.3;
  margin-bottom: 2px;
}

.node-status {
  font-size: 12px;
  color: #909399;
  line-height: 1.2;
  font-weight: 500;
}

.node-time {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  font-weight: 500;
  padding: 2px 8px;
  background: rgba(144, 147, 153, 0.1);
  border-radius: 6px;
}

/* Animations */
@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

/* Scrollbar */
.workflow-nodes::-webkit-scrollbar {
  width: 4px;
}

.workflow-nodes::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 2px;
}

.workflow-nodes::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}

.workflow-nodes::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* Responsive */
@media (max-width: 768px) {
  .workflow-status-card {
    left: 10px;
    bottom: 10px;
    width: 280px;
  }
}
</style>