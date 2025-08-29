<template>
  <div class="test-markdown-page">
    <div class="test-header">
      <h1>Markdown渲染测试页面</h1>
      <p>测试新的流式数据Markdown渲染功能</p>
    </div>
    
    <div class="test-content">
      <div class="test-section">
        <h2>测试1: 普通Markdown内容</h2>
        <MessageBubble 
          :message="normalMessage" 
          :is-streaming="false"
          @stream-complete="handleStreamComplete"
        />
      </div>
      
      <div class="test-section">
        <h2>测试2: 模拟流式数据</h2>
        <MessageBubble 
          :message="streamMessage" 
          :raw-stream-data="rawStreamData"
          :is-streaming="isStreaming"
          @stream-complete="handleStreamComplete"
        />
        <el-button @click="toggleStreaming" :type="isStreaming ? 'danger' : 'primary'">
          {{ isStreaming ? '停止流式' : '开始流式' }}
        </el-button>
      </div>
      
      <div class="test-section">
        <h2>测试3: 包含代码块的内容</h2>
        <MessageBubble 
          :message="codeMessage" 
          :is-streaming="false"
          @stream-complete="handleStreamComplete"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElButton } from 'element-plus'
import MessageBubble from './components/MessageBubble.vue'
import type { Message } from '@/types'

// Test data
const isStreaming = ref(false)

const normalMessage: Message = {
  id: 'test-1',
  role: 'assistant',
  content: `# 数据库分析报告\n\n## 概述\n\n这是一个**测试消息**，包含了各种Markdown元素：\n\n- 列表项1\n- 列表项2\n- 列表项3\n\n### 重要信息\n\n> 这是一个引用块，用于显示重要信息。\n\n普通段落文本，测试换行和格式化效果。`,
  timestamp: Date.now()
}

const streamMessage: Message = {
  id: 'test-2',
  role: 'assistant',
  content: '流式渲染测试消息',
  timestamp: Date.now()
}

const codeMessage: Message = {
  id: 'test-3',
  role: 'assistant',
  content: `# 代码示例\n\n以下是一个JavaScript代码示例：\n\n\`\`\`javascript\nfunction processStreamingData(rawData) {\n  const lines = rawData.split('\\n')\n  let content = ''\n  \n  for (let i = 0; i < lines.length; i++) {\n    if (lines[i] === 'event:chunk') {\n      const dataLine = lines[i + 1]\n      if (dataLine && dataLine.startsWith('data:')) {\n        const chunkData = dataLine.substring(5)\n        content += chunkData === '' ? '\\n' : chunkData\n      }\n    }\n  }\n  \n  return content\n}\n\`\`\`\n\n还有内联代码：\`console.log('Hello World')\``,
  timestamp: Date.now()
}

// Simulate raw streaming data
const rawStreamData = `event:chunk
data:##

event:chunk
data: 数据

event:chunk
data:库

event:chunk
data:分析

event:chunk
data:报告

event:chunk
data:

event:chunk
data:

event:chunk
data:###

event:chunk
data: **

event:chunk
data:第一步

event:chunk
data:：

event:chunk
data:数据

event:chunk
data:全

event:chunk
data:览

event:chunk
data:**

event:chunk
data:

event:chunk
data:

event:chunk
data:通过

event:chunk
data:对

event:chunk
data:数据库

event:chunk
data:的

event:chunk
data:深度

event:chunk
data:剖析

event:chunk
data:。`

// Methods
const toggleStreaming = () => {
  isStreaming.value = !isStreaming.value
}

const handleStreamComplete = (data: any) => {
  console.log('Stream completed:', data)
}
</script>

<style scoped>
.test-markdown-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.test-header {
  text-align: center;
  margin-bottom: 40px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.test-header h1 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.test-header p {
  color: #6c757d;
  font-size: 16px;
}

.test-section {
  margin-bottom: 40px;
  padding: 20px;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  background: white;
}

.test-section h2 {
  color: #495057;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e9ecef;
}

.el-button {
  margin-top: 10px;
}
</style>