import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { aiApi } from '@/api'
import { reportApi } from '@/api'
import type { DataSourceConfig, ProfilingTask, AnalysisRequest, Message, AnalysisStreamResponse, WorkflowNodeData, TextChunkData } from '@/types'
import { ElMessage } from 'element-plus'

// Helper function to format workflow node information (optimized and simplified)
const formatWorkflowNodeInfo = (nodeData: WorkflowNodeData): string => {
  if (!nodeData.node_id) return ''
  
  const title = nodeData.title || nodeData.node_id
  const status = nodeData.status || 'running'
  
  // Simplified status display
  let statusIcon = '⏳'
  if (status === 'succeeded') {
    statusIcon = '✅'
  } else if (status === 'failed') {
    statusIcon = '❌'
  }
  
  // Compact format with essential info only
  let info = `${statusIcon} **${title}**`
  
  // Add execution time if available and significant
  if (nodeData.elapsed_time && nodeData.elapsed_time > 100) {
    const timeInSeconds = (nodeData.elapsed_time / 1000).toFixed(1)
    info += ` (${timeInSeconds}s)`
  }
  
  return info + '\n'
}

export const useAIAnalysisStore = defineStore('aiAnalysis', () => {
  // State
  const profilingTasks = ref<ProfilingTask[]>([])
  const selectedTaskId = ref<string | null>(null)
  const messages = ref<Message[]>([])
  const isStreaming = ref(false)
  const isLoadingTasks = ref(false)
  const aiServiceAvailable = ref(false)
  const currentStreamController = ref<AbortController | null>(null)

  // Getters
  const selectedTask = computed(() => 
    profilingTasks.value.find(task => task.taskId === selectedTaskId.value)
  )
  
  const canStartAnalysis = computed(() => 
    selectedTaskId.value && !isStreaming.value
  )
  
  const lastUserMessage = computed(() => 
    messages.value.filter(m => m.role === 'user').pop()
  )
  
  const lastAssistantMessage = computed(() => 
    messages.value.filter(m => m.role === 'assistant').pop()
  )

  // Actions
  // Removed fetchDataSources - no longer needed as we only work with tasks

  const fetchAllCompletedTasks = async () => {
    try {
      isLoadingTasks.value = true
      profilingTasks.value = [] // Clear old tasks
      selectedTaskId.value = null // Reset task selection
      
      const response = await aiApi.getAllCompletedTasks()
      profilingTasks.value = response || []
      
      if (profilingTasks.value.length === 0) {
        ElMessage.warning('No completed tasks found')
      }
    } catch (error) {
      console.error('Failed to fetch completed tasks:', error)
      ElMessage.error('Failed to load profiling tasks')
      profilingTasks.value = []
    } finally {
      isLoadingTasks.value = false
    }
  }

  const checkAIServiceHealth = async () => {
    try {
      const response = await aiApi.checkHealth()
      aiServiceAvailable.value = response.available
      return response.available
    } catch (error) {
      console.error('AI service health check failed:', error)
      aiServiceAvailable.value = false
      return false
    }
  }

  const startAnalysis = async (question: string) => {
    if (!selectedTaskId.value) {
      ElMessage.error('Please select a task first')
      return
    }

    if (isStreaming.value) {
      ElMessage.warning('Analysis is already in progress')
      return
    }

    // Add user message
    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: question,
      timestamp: new Date().toISOString()
    }
    messages.value.push(userMessage)

    // Add assistant message with initial loading content
    const assistantMessage: Message = {
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      content: 'AI正在分析中...\n\n',
      timestamp: new Date().toISOString(),
      isStreaming: true,
      workflowNodes: []
    }
    messages.value.push(assistantMessage)

    isStreaming.value = true
    currentStreamController.value = new AbortController()

    const analysisRequest: AnalysisRequest = {
      question,
      taskId: selectedTaskId.value,
      userId: 'current-user' // TODO: Get from auth store
    }

    try {
      await aiApi.startAnalysisStream(
        analysisRequest,
        // onMessage callback
        (data: AnalysisStreamResponse) => {
          const lastMessage = messages.value[messages.value.length - 1]
          if (lastMessage && lastMessage.role === 'assistant') {
            switch (data.type) {
              case 'status':
                // Handle status events (connected, started, finished)
                if (data.event === 'finished') {
                  // Fetch analysis report after completion
                  fetchAnalysisReport(lastMessage)
                }
                break
              case 'content':
                // Handle content chunks (legacy)
                lastMessage.content += data.content || ''
                break
              case 'chunk':
                // Handle text chunks for typewriter effect
                if (data.chunkData) {
                  // Use delta for incremental text or text for full chunk
                  const chunkText = data.chunkData.delta || data.chunkData.text || data.content || ''
                  lastMessage.content += chunkText
                } else {
                  // Fallback to content field
                  lastMessage.content += data.content || ''
                }
                // Force reactivity update for markdown re-rendering
                lastMessage.timestamp = new Date().toISOString()
                break
              case 'progress':
              case 'node_update':
                 // Handle workflow node progress
                 if (data.nodeData) {
                   // Store node data for later display
                   if (!lastMessage.workflowNodes) {
                     lastMessage.workflowNodes = []
                   }
                   lastMessage.workflowNodes.push(data.nodeData)
                   // Note: Node status is now displayed in WorkflowDisplay component
                   // No need to add formatted node info to message content
                 }
                 break
              case 'error':
                lastMessage.content += `\n\n**Error:** ${data.error}`
                lastMessage.hasError = true
                break
            }
          }
        },
        // onError callback
        (error: Error) => {
          console.error('Streaming analysis error:', error)
          const lastMessage = messages.value[messages.value.length - 1]
          if (lastMessage && lastMessage.role === 'assistant') {
            lastMessage.content += `\n\n**Error:** ${error.message}`
            lastMessage.hasError = true
            lastMessage.isStreaming = false
          }
          ElMessage.error(`Analysis failed: ${error.message}`)
          isStreaming.value = false
        },
        // onComplete callback
        () => {
          const lastMessage = messages.value[messages.value.length - 1]
          if (lastMessage && lastMessage.role === 'assistant') {
            lastMessage.isStreaming = false
            
            // Output AI report content to console when analysis completes
            console.group('🤖 AI Analysis Report - Task ID:', selectedTaskId.value)
            console.log('📝 Question:', question)
            console.log('📊 Full AI Response:')
            console.log(lastMessage.content)
            
            if (lastMessage.workflowNodes && lastMessage.workflowNodes.length > 0) {
              console.log('🔄 Workflow Execution Details:')
              lastMessage.workflowNodes.forEach((node, index) => {
                console.log(`  ${index + 1}. ${node.title || node.node_id}:`, {
                  status: node.status,
                  elapsed_time: node.elapsed_time ? `${(node.elapsed_time / 1000).toFixed(2)}s` : 'N/A',
                  node_type: node.node_type,
                  outputs: node.outputs
                })
              })
            }
            
            console.log('⏰ Analysis completed at:', new Date().toLocaleString())
            console.groupEnd()
          }
          isStreaming.value = false
          currentStreamController.value = null
        }
      )
    } catch (error) {
      console.error('Failed to start analysis:', error)
      ElMessage.error('Failed to start analysis')
      isStreaming.value = false
      currentStreamController.value = null
      
      // Update the last assistant message with error
      const lastMessage = messages.value[messages.value.length - 1]
      if (lastMessage && lastMessage.role === 'assistant') {
        lastMessage.content = `**Error:** Failed to start analysis - ${error instanceof Error ? error.message : 'Unknown error'}`
        lastMessage.hasError = true
        lastMessage.isStreaming = false
      }
    }
  }

  const stopAnalysis = () => {
    if (currentStreamController.value) {
      currentStreamController.value.abort()
      currentStreamController.value = null
    }
    
    isStreaming.value = false
    
    // Update the last assistant message
    const lastMessage = messages.value[messages.value.length - 1]
    if (lastMessage && lastMessage.role === 'assistant' && lastMessage.isStreaming) {
      lastMessage.content += '\n\n**Analysis stopped by user**'
      lastMessage.isStreaming = false
    }
    
    ElMessage.info('Analysis stopped')
  }

  const clearMessages = () => {
    messages.value = []
  }

  // Fetch analysis report after AI analysis completion
  const fetchAnalysisReport = async (message: Message) => {
    if (!selectedTaskId.value) {
      message.content += '❌ 无法获取分析报告：未选择任务\n\n'
      return
    }

    try {
      // Get task summary reports
      const summaryResponse = await reportApi.getSummaryByTaskId({
        taskId: selectedTaskId.value,
        page: 0,
        pageSize: 10
      })

      if (summaryResponse.data && summaryResponse.data.length > 0) {
        message.content += '📊 **数据分析报告**\n\n'
        
        summaryResponse.data.forEach((report, index) => {
          message.content += `### ${index + 1}. ${report.dataSourceName || report.dataSourceId}\n\n`
          message.content += `**数据源类型**: ${report.dataSourceType}\n`
          message.content += `**Schema数量**: ${report.schemas?.length || 0}\n`
          
          if (report.statistics) {
            message.content += `**总表数**: ${report.statistics.totalTables || 0}\n`
            message.content += `**总列数**: ${report.statistics.totalColumns || 0}\n`
            message.content += `**总记录数**: ${report.statistics.totalRows || 0}\n`
          }
          
          if (report.schemas && report.schemas.length > 0) {
            message.content += `\n**Schema详情**:\n`
            report.schemas.forEach(schema => {
              message.content += `- **${schema.schemaName}**: ${schema.tableCount || 0}个表\n`
            })
          }
          
          message.content += '\n---\n\n'
        })
        
        message.content += '💡 **分析建议**: 基于以上数据剖析结果，您可以进一步询问具体的数据质量问题、表结构分析或数据分布情况。\n\n'
      } else {
        // message.content += '\r\n⚠️ 暂无分析报告数据，可能分析任务尚未完成或数据为空。\n\n'
      }
    } catch (error) {
      console.error('Failed to fetch analysis report:', error)
      // message.content += `❌ 获取分析报告失败: ${error instanceof Error ? error.message : '未知错误'}\n\n`
    }
  }

  // Removed setSelectedDataSource - no longer needed

  const setSelectedTask = (taskId: string | null) => {
    selectedTaskId.value = taskId
  }

  const addMessage = (message: Message) => {
    messages.value.push(message)
  }

  const removeMessage = (messageId: string) => {
    messages.value = messages.value.filter(m => m.id !== messageId)
  }

  const reset = () => {
    // Stop any ongoing analysis
    stopAnalysis()
    
    // Clear all state
    dataSources.value = []
    selectedDataSourceId.value = null
    profilingTasks.value = []
    selectedTaskId.value = null
    messages.value = []
    isStreaming.value = false
    isLoadingDataSources.value = false
    isLoadingTasks.value = false
    aiServiceAvailable.value = false
  }

  return {
    // State
    profilingTasks,
    selectedTaskId,
    messages,
    isStreaming,
    isLoadingTasks,
    aiServiceAvailable,
    
    // Getters
    selectedTask,
    canStartAnalysis,
    lastUserMessage,
    lastAssistantMessage,
    
    // Actions
    fetchAllCompletedTasks,
    checkAIServiceHealth,
    startAnalysis,
    stopAnalysis,
    clearMessages,
    setSelectedTask,
    addMessage,
    removeMessage,
    reset
  }
})