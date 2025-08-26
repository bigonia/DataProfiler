import request from './request'
import type { DataSourceConfig, ProfilingTask, AnalysisRequest, AnalysisStreamResponse } from '@/types'

// AI Analysis API Service
export const aiApi = {
  // Get all completed tasks for AI analysis
  getAllCompletedTasks: (): Promise<ProfilingTask[]> => {
    return request.get('/profiling/profiling-tasks/list')
  },

  // Check AI service health
  checkHealth: (): Promise<{ available: boolean; timestamp: number }> => {
    return request.get('/v1/ai/health')
  },

  // Get AI service information
  getServiceInfo: (): Promise<{
    service: string
    version: string
    provider: string
    features: string[]
    supported_formats: string[]
    max_context_length: number
    timeout_minutes: number
  }> => {
    return request.get('/v1/ai/info')
  },

  // Start streaming analysis using fetch API (since EventSource doesn't support POST)
  startAnalysisStream: async (
    request: AnalysisRequest,
    onMessage: (data: AnalysisStreamResponse) => void,
    onError: (error: Error) => void,
    onComplete: () => void
  ): Promise<void> => {
    try {
      const response = await fetch('/api/v1/ai/analyze', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
        },
        body: JSON.stringify(request)
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('Failed to get response reader')
      }

      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = ''

      try {
        while (true) {
          const { done, value } = await reader.read()
          
          if (done) {
            onComplete()
            break
          }

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || '' // Keep incomplete line in buffer

          for (const line of lines) {
            if (line.trim() === '') {
              // Empty line indicates end of event
              currentEvent = ''
              continue
            }
            
            // Parse SSE format: "event: eventName" and "data: content"
            if (line.startsWith('event:')) {
              currentEvent = line.substring(6).trim()
            } else if (line.startsWith('data:')) {
              const dataContent = line.substring(5).trim()
              
              if (dataContent === '[DONE]') {
                onComplete()
                return
              }
              
              try {
                // Create structured response based on event type
                let responseData: AnalysisStreamResponse
                
                switch (currentEvent) {
                  case 'status':
                  case 'connected':
                  case 'started':
                  case 'finished':
                  case 'complete':
                    responseData = {
                      type: 'status',
                      event: currentEvent,
                      content: dataContent
                    }
                    break
                  case 'progress':
                    // Try to parse as JSON for workflow node information
                    try {
                      const nodeData = JSON.parse(dataContent)
                      responseData = {
                        type: 'progress',
                        event: currentEvent,
                        content: dataContent,
                        nodeData: nodeData
                      }
                    } catch {
                      responseData = {
                        type: 'progress',
                        event: currentEvent,
                        content: dataContent
                      }
                    }
                    break
                  case 'chunk':
                    responseData = {
                      type: 'content',
                      event: currentEvent,
                      content: dataContent
                    }
                    break
                  case 'error':
                    responseData = {
                      type: 'error',
                      event: currentEvent,
                      error: dataContent
                    }
                    break
                  default:
                    // Default to content type for unknown events
                    responseData = {
                      type: 'content',
                      event: currentEvent || 'data',
                      content: dataContent
                    }
                }
                
                onMessage(responseData)
              } catch (parseError) {
                console.warn('Failed to process SSE message:', line, parseError)
                // Send as raw content if parsing fails
                onMessage({
                  type: 'content',
                  event: currentEvent || 'data',
                  content: dataContent
                })
              }
            }
          }
        }
      } finally {
        reader.releaseLock()
      }
    } catch (error) {
      console.error('Stream analysis error:', error)
      onError(error instanceof Error ? error : new Error('Unknown streaming error'))
    }
  }
}

export default aiApi