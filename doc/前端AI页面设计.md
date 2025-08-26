### 前端设计方案

#### 1\. 方案目标

开发一个“AI智能分析”前端页面，该页面允许用户选择一个**已完成的剖析任务（Profiling Task）**，并围绕该任务的“数据画像”结果进行自然语言提问，最终以流式响应的方式展示AI的分析报告。

#### 2\. 技术选型确认

  * **核心框架：** Vue 3 (Composition API) + TypeScript
  * **UI 组件库：** Element Plus
  * **状态管理：** Pinia
  * **API 通信：** `axios`（常规请求） + `EventSource`（流式接收）
  * **Markdown渲染：** `marked.js` 或 `markdown-it` (+ DOMPurify 用于安全)

#### 3\. 核心功能与页面设计

##### 3.1. 页面布局与核心组件（已调整）

布局保持不变，但**左侧上下文设置区的交互逻辑将发生关键变化**，从一步选择变为两步。

```
┌──────────────────────────────────────────────────────────────┐
│ [AI智能分析]                                                 │ Header
├──────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────┐ ┌─────────────────────────────┐ │
│ │ ❶ **分析范围设置** │ │ ❷ **对话窗口 (Chat Window)**│ │ Main Content
│ │                          │ │                             │ │
│ │ 步骤1: 选择数据源        │ │ [请先选择一个分析任务...]   │ │
│ │ [ (Select Dropdown)    ] │ │                             │ │
│ │                          │ │                             │ │
│ │ 步骤2: 选择分析任务      │ │                             │ │
│ │ [ (Select Dropdown)    ] │ │                             │ │
│ │                          │ │                             │ │
│ │                          │ └─────────────────────────────┘ │
│ └──────────────────────────┘ ┌─────────────────────────────┐ │
│                              │ ❸ **输入区域 (Input Area)** │ │
│                              │ [ (Textarea Input)      ] [发送]│ │
│                              └─────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

1.  **分析范围设置区:**
      * **数据源选择器 (`el-select`):** 用户首先选择要分析的数据源。
      * **分析任务选择器 (`el-select`):** 当用户选定一个数据源后，此选择器会**被动态填充**该数据源下所有\*\*已完成（COMPLETED）\*\*的剖析任务列表。此选择器应显示任务名称、任务ID和完成时间，以帮助用户识别。
2.  **对话窗口:** 保持不变。
3.  **输入区域:** 保持不变，但在用户完成\*\*第二步（选择分析任务）\*\*后才可用。

##### 3.2. 交互流程 (User Experience Flow) - **已更新**

1.  **进入页面：**
      * 调用API获取数据源列表，填充`数据源选择器`。
      * `分析任务选择器`和`输入区域`均处于禁用状态。
2.  **选择数据源：**
      * 用户选择一个数据源。
      * 系统立即调用一个新的后端API（例如 `GET /api/v1/tasks?dataSourceId=...&status=COMPLETED`），获取该数据源下所有已完成的任务列表。
      * `分析任务选择器`被填充并变为可用状态。
3.  **选择分析任务：**
      * 用户在`分析任务选择器`中选择一个具体的`taskId`。
      * `输入区域`变为可用状态，`placeholder`提示用户“可以开始提问”。
4.  **发送问题：**
      * 用户在`输入区域`输入问题，点击“发送”。
      * 前端将\*\*`question`**和**`selectedTaskId`\*\*作为参数，调用后端的流式API `POST /api/v1/ai/analyze`。
5.  **接收与渲染：**
      * 流程不变，`EventSource`开始接收SSE流，并在对话窗口中实时渲染“打字机”效果。
6.  **完成与错误处理：**
      * 流程不变，完成后或出错时，`输入区域`恢复可用状态。

#### 4\. 状态管理 (State Management - Pinia) - **已更新**

`store/aiAnalysis.ts` 的 `state` 需要扩展以管理任务列表。

```typescript
// store/aiAnalysis.ts
import { defineStore } from 'pinia';

// ... Message interface

// 新增Task接口定义
interface ProfilingTask {
  taskId: string;
  name: string;
  completedAt: string;
}

export const useAIAnalysisStore = defineStore('aiAnalysis', {
  state: () => ({
    dataSources: [] as { id: string; name: string }[],
    selectedDataSourceId: null as string | null,
    
    profilingTasks: [] as ProfilingTask[], // 新增：存储任务列表
    selectedTaskId: null as string | null,     // 新增：存储选中的任务ID
    
    messages: [] as Message[],
    isStreaming: false,
    isLoadingTasks: false, // 新增：用于显示加载任务列表的loading状态
  }),
  actions: {
    // Action 1: 获取数据源列表
    async fetchDataSources() { /* ... */ },
    
    // Action 2: (新增) 根据数据源获取任务列表
    async fetchTasksForDataSource(dataSourceId: string) {
      this.isLoadingTasks = true;
      this.profilingTasks = []; // 清空旧列表
      this.selectedTaskId = null; // 重置任务选择
      try {
        // 调用后端API: GET /api/v1/tasks?dataSourceId=...
        const tasks = await api.getTasks(dataSourceId); 
        this.profilingTasks = tasks;
      } finally {
        this.isLoadingTasks = false;
      }
    },
    
    // Action 3: (修改) 发送分析请求
    async startAnalysis(question: string) {
      if (!this.selectedTaskId) return; // 校验条件变为taskId

      this.isStreaming = true;
      this.messages.push({ role: 'user', content: question, id: Date.now().toString() });

      // 调用流式API，传递 question 和 selectedTaskId
      api.startAnalysisStream({
        question,
        taskId: this.selectedTaskId,
        // ...
      });
    },
  },
});
```

#### 5\. API 通信层设计 (`api/aiService.ts`) - **已更新**

需要新增一个API函数，并修改现有函数。

```typescript
// api/aiService.ts

// ...

// 新增函数：获取任务列表
export async function getTasks(dataSourceId: string): Promise<ProfilingTask[]> {
  const response = await axios.get(`/api/v1/tasks?dataSourceId=${dataSourceId}&status=COMPLETED`);
  return response.data;
}

// 修改函数：请求体变为taskId
interface AnalysisRequest {
  question: string;
  taskId: string;
  userId: string;
}

export function startAnalysisStream(request: AnalysisRequest) {
  // EventSource实现保持不变，但URL的参数或请求体需要调整
  // 假设后端接口改为接收JSON Body
  // EventSource 不支持 POST，所以后端需要调整为GET，或前端改用fetch API来处理流
  // 以下为使用 fetch 的示例
  
  const store = useAIAnalysisStore();
  // ... 添加AI消息气泡等准备工作
  
  fetch('/api/v1/ai/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request)
  }).then(response => {
      const reader = response.body.getReader();
      // ... 处理流式读取 ...
  });
}
```

**注意:** 原生`EventSource`只支持GET请求。如果您的后端`analyze`接口是POST，前端需要使用`fetch` API来处理流式响应体。这在现代浏览器中是标准做法。

#### 6\. 组件化开发建议 - **已更新**

  * `AIAnalysisView.vue`: 负责整体布局和状态协调。当`DataSourceSelector`选择变化时，触发`fetchTasksForDataSource` action。
  * `DataSourceSelector.vue`: 保持不变。
  * **`TaskSelector.vue` (新增):** 一个新组件，接收`tasks`数组和`isLoadingTasks`作为props，展示任务列表。当用户选择一个任务时，通过`emit`事件将`taskId`传递给父组件。
  * `ChatWindow.vue`, `MessageBubble.vue`, `MessageInput.vue`: 基本保持不变。

#### 7\. 建议的开发步骤

1.  **后端先行：** 确保后端已提供`GET /api/v1/tasks`接口，用于根据数据源ID查询已完成的任务列表。同时确认`POST /api/v1/ai/analyze`接口接收的参数是`taskId`。
2.  **前端状态管理：** 按照4.0的定义，更新Pinia Store。
3.  **API层更新：** 按照5.0的定义，更新`api/aiService.ts`。
4.  **组件开发：** 开发新增的`TaskSelector.vue`组件。
5.  **页面集成：** 在主视图`AIAnalysisView.vue`中，实现“数据源选择 -\> 加载任务列表 -\> 任务选择”的二级联动逻辑。
6.  **联调测试：** 重点测试二级联动的正确性，以及最终传递给`startAnalysisStream`的`taskId`是否正确。
