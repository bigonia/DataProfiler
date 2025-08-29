# MessageBubble 组件 - 增强版 Markdown 渲染

## 概述

`MessageBubble` 组件已经完全重构，专门针对AI对话中的流式数据渲染进行了优化。新版本能够正确处理后端返回的Server-Sent Events (SSE) 格式数据，并提供完整的调试支持。

## 主要特性

### 1. 流式数据处理
- **自动识别流式数据格式**: 自动检测 `event:chunk` 格式的数据
- **空chunk处理**: 正确处理空的 `data:` 行，将其转换为换行符
- **实时渲染**: 支持流式内容的实时Markdown渲染

### 2. 增强的Markdown渲染
- **优化的样式**: 专为AI对话优化的视觉样式
- **代码高亮**: 使用 highlight.js 提供语法高亮
- **响应式设计**: 适配不同屏幕尺寸
- **安全渲染**: 使用 DOMPurify 确保内容安全

### 3. 调试功能
- **原始数据保存**: 保留完整的后端响应数据
- **控制台输出**: 流式完成时自动输出调试信息
- **处理过程追踪**: 显示数据处理的每个步骤

## 使用方法

### 基本用法

```vue
<template>
  <MessageBubble 
    :message="message" 
    :is-streaming="isStreaming"
    @stream-complete="handleStreamComplete"
  />
</template>

<script setup>
import MessageBubble from './components/MessageBubble.vue'

const message = {
  id: 'msg-1',
  role: 'assistant',
  content: 'AI回复内容',
  timestamp: Date.now()
}

const isStreaming = ref(false)

const handleStreamComplete = (data) => {
  console.log('流式完成:', data)
}
</script>
```

### 处理流式数据

```vue
<template>
  <MessageBubble 
    :message="message" 
    :raw-stream-data="rawStreamData"
    :is-streaming="isStreaming"
    @stream-complete="handleStreamComplete"
  />
</template>

<script setup>
// 原始流式数据示例
const rawStreamData = `event:chunk
data:##

event:chunk
data: 标题

event:chunk
data:

event:chunk
data:

event:chunk
data:内容段落`

const message = {
  id: 'msg-1',
  role: 'assistant',
  content: '', // 可以为空，会使用 rawStreamData
  timestamp: Date.now()
}
</script>
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `message` | `Message` | - | 消息对象 |
| `isStreaming` | `boolean` | `false` | 是否正在流式传输 |
| `rawStreamData` | `string` | `''` | 原始流式数据 |

## Events

| 事件名 | 参数 | 说明 |
|--------|------|------|
| `copy-message` | `content: string` | 复制消息内容 |
| `regenerate` | `messageId: string` | 重新生成消息 |
| `like-message` | `messageId: string` | 点赞消息 |
| `stream-complete` | `data: StreamCompleteData` | 流式传输完成 |

### StreamCompleteData 接口

```typescript
interface StreamCompleteData {
  messageId: string      // 消息ID
  rawData: string        // 原始后端数据
  processedContent: string // 处理后的内容
}
```

## 流式数据格式

后端返回的流式数据应该遵循以下格式：

```
event:chunk
data:文本内容

event:chunk
data:

event:chunk
data:更多内容
```

**重要说明**:
- 空的 `data:` 行代表换行符
- 每个 `event:chunk` 后必须跟一个 `data:` 行
- 组件会自动处理这种格式并重构为完整的Markdown内容

## 调试功能

### 控制台输出

当流式传输完成时，组件会在控制台输出详细的调试信息：

```
🔍 AI Message Debug - msg-123
📥 Raw Backend Data: event:chunk\ndata:##\n...
⚙️ Processed Content: ## 标题\n\n内容段落
🎨 Rendered HTML: <h2>标题</h2><p>内容段落</p>
```

### 事件监听

```javascript
const handleStreamComplete = (data) => {
  console.log('消息ID:', data.messageId)
  console.log('原始数据:', data.rawData)
  console.log('处理后内容:', data.processedContent)
}
```

## 样式定制

组件提供了丰富的CSS变量用于样式定制：

```css
.markdown-content {
  --text-color: #2c3e50;
  --code-bg: #f8f9fa;
  --code-color: #d73a49;
  --pre-bg: #f6f8fa;
  --border-color: #e1e4e8;
}
```

## 最佳实践

1. **性能优化**: 对于长文本，建议使用 `rawStreamData` 属性而不是直接修改 `message.content`
2. **错误处理**: 监听 `stream-complete` 事件来处理渲染错误
3. **调试**: 在开发环境中启用控制台输出来调试渲染问题
4. **安全性**: 组件已内置XSS防护，但仍建议验证输入数据

## 故障排除

### 常见问题

1. **Markdown不渲染**: 检查数据格式是否正确
2. **换行丢失**: 确保空的 `data:` 行被正确处理
3. **代码高亮失效**: 检查语言标识是否正确
4. **样式异常**: 检查CSS类名冲突

### 调试步骤

1. 打开浏览器开发者工具
2. 查看控制台的调试输出
3. 检查 `rawData` 和 `processedContent` 的差异
4. 验证HTML渲染结果

## 测试

访问 `/test-markdown` 路由可以测试组件的各种功能：

- 普通Markdown渲染
- 流式数据处理
- 代码块高亮
- 调试功能

## 更新日志

### v2.0.0 (当前版本)
- 完全重构Markdown渲染逻辑
- 新增流式数据处理支持
- 新增调试功能
- 优化样式和性能
- 新增测试页面

### v1.0.0
- 基础Markdown渲染
- 基本样式支持