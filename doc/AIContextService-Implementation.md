# AIContextService 模块实现文档

## 概述

AIContextService 模块负责将结构化的数据库分析报告转换为适合大语言模型（LLM）处理的 Markdown 格式上下文。该模块提供智能压缩和格式化功能，确保在令牌限制内保留关键信息。

## 架构设计

### 后端架构

```
AIContextController
       ↓
AIContextService (Interface)
       ↓
AIContextServiceImpl
       ↓
StructuredReportService
```

### 前端架构

```
AIContextViewer (React Component)
       ↓
useAIContext (Custom Hook)
       ↓
aiContextApi (API Layer)
```

## 模块组件

### 后端组件

#### 1. AIContextService 接口

**文件位置**: `src/main/java/com/dataprofiler/service/AIContextService.java`

**主要方法**:
- `buildContextForLLM(String taskId)`: 构建默认长度的 LLM 上下文
- `buildContextForLLM(String taskId, int maxLength)`: 构建指定长度限制的 LLM 上下文
- `canBuildContext(String taskId)`: 检查是否可以为指定任务构建上下文
- `getEstimatedContextLength(String taskId)`: 获取预估的上下文长度

#### 2. AIContextServiceImpl 实现类

**文件位置**: `src/main/java/com/dataprofiler/service/impl/AIContextServiceImpl.java`

**核心功能**:
- 从 StructuredReportService 获取报告数据
- 将结构化报告转换为 Markdown 格式
- 应用智能压缩算法保持在长度限制内
- 提供详细的日志记录和错误处理

**配置参数**:
- `DEFAULT_MAX_LENGTH`: 默认最大长度 (8000 字符)
- `MAX_SAMPLE_ROWS`: 最大样本行数 (3 行)
- `MAX_COLUMNS_PER_TABLE`: 每个表的最大列数 (10 列)

#### 3. AIContextController 控制器

**文件位置**: `src/main/java/com/dataprofiler/controller/AIContextController.java`

**API 端点**:
- `GET /api/ai-context/build/{taskId}`: 构建 AI 上下文
- `GET /api/ai-context/check/{taskId}`: 检查上下文可用性
- `GET /api/ai-context/metadata/{taskId}`: 获取上下文元数据

### 前端组件

#### 1. aiContextApi API 层

**文件位置**: `src/api/aiContext.ts`

**主要功能**:
- 封装后端 API 调用
- 提供类型安全的接口定义
- 实现自动优化的上下文构建

#### 2. useAIContext Hook

**文件位置**: `src/hooks/useAIContext.ts`

**状态管理**:
- `loading`: 加载状态
- `context`: 生成的上下文内容
- `contextLength`: 上下文长度
- `metadata`: 上下文元数据
- `error`: 错误信息

**操作方法**:
- `buildContext()`: 构建上下文
- `buildOptimizedContext()`: 构建优化上下文
- `checkContext()`: 检查上下文可用性
- `getMetadata()`: 获取元数据

#### 3. AIContextViewer 组件

**文件位置**: `src/components/AIContext/AIContextViewer.tsx`

**主要特性**:
- 任务 ID 输入和验证
- 上下文生成和预览
- 统计信息显示
- 复制和下载功能
- 设置面板（长度控制）
- 响应式设计

## 使用方法

### 后端使用

```java
@Autowired
private AIContextService aiContextService;

// 构建默认上下文
String context = aiContextService.buildContextForLLM("task-123");

// 构建指定长度的上下文
String context = aiContextService.buildContextForLLM("task-123", 5000);

// 检查可用性
boolean canBuild = aiContextService.canBuildContext("task-123");
```

### 前端使用

#### 使用 Hook

```typescript
import { useAIContext } from '../hooks/useAIContext';

const MyComponent = () => {
  const { buildOptimizedContext, context, loading, error } = useAIContext();
  
  const handleGenerate = async () => {
    await buildOptimizedContext('task-123');
  };
  
  return (
    <div>
      <button onClick={handleGenerate} disabled={loading}>
        Generate Context
      </button>
      {context && <pre>{context}</pre>}
    </div>
  );
};
```

#### 使用组件

```typescript
import { AIContextViewer } from '../components/AIContext';

const ReportPage = () => {
  return (
    <AIContextViewer
      taskId="task-123"
      onContextGenerated={(context) => console.log('Generated:', context)}
      showTaskIdInput={false}
      defaultMaxLength={10000}
    />
  );
};
```

## Markdown 格式规范

生成的 Markdown 上下文包含以下结构：

```markdown
# Database Profiling Report

## Basic Information
- **Task ID**: task-123
- **Data Source ID**: ds-456
- **Data Source Type**: MySQL
- **Database**: ecommerce
- **Generated At**: 2024-01-15 10:30:00

## Summary
- **Total Tables**: 15
- **Total Columns**: 120
- **Total Rows**: 1,250,000

## Tables

### users (public)
**Description**: User account information
**Row Count**: 50,000

#### Columns
| Column | Type | Primary Key | Comment | Null Count | Distinct Count |
|--------|------|-------------|---------|------------|----------------|
| id | bigint | Yes | User ID | 0 | 50000 |
| email | varchar(255) | No | Email address | 0 | 50000 |
| created_at | timestamp | No | Creation time | 0 | 45000 |

#### Sample Data
| id | email | created_at |
|----|-------|------------|
| 1 | user1@example.com | 2024-01-01 10:00:00 |
| 2 | user2@example.com | 2024-01-01 11:00:00 |
| 3 | user3@example.com | 2024-01-01 12:00:00 |

---
```

## 智能压缩策略

当上下文长度超过限制时，系统采用以下压缩策略：

1. **列数限制**: 每个表最多显示 10 列
2. **样本行限制**: 每个表最多显示 3 行样本数据
3. **值截断**: 长值截断为 50 字符
4. **简单截断**: 超出部分用省略号替代

## 配置选项

### 后端配置

可以通过修改 `AIContextServiceImpl` 中的常量来调整行为：

```java
private static final int DEFAULT_MAX_LENGTH = 8000;
private static final int MAX_SAMPLE_ROWS = 3;
private static final int MAX_COLUMNS_PER_TABLE = 10;
```

### 前端配置

组件支持以下配置属性：

- `taskId`: 任务 ID（可选）
- `onContextGenerated`: 上下文生成回调
- `showTaskIdInput`: 是否显示任务 ID 输入框
- `defaultMaxLength`: 默认最大长度

## 错误处理

### 后端错误处理

- 报告不存在：抛出 `RuntimeException`
- 数据访问错误：记录日志并抛出异常
- 转换错误：记录详细错误信息

### 前端错误处理

- 网络错误：显示错误提示
- 验证错误：输入验证和提示
- 状态管理：错误状态的清理和重置

## 性能考虑

1. **缓存策略**: 可以考虑在服务层添加缓存
2. **异步处理**: 大型报告可以考虑异步生成
3. **分页支持**: 超大报告可以考虑分页处理
4. **压缩优化**: 可以实现更智能的压缩算法

## 扩展点

1. **自定义格式**: 支持不同的输出格式（JSON、XML 等）
2. **模板系统**: 支持自定义 Markdown 模板
3. **多语言支持**: 支持不同语言的上下文生成
4. **AI 优化**: 基于 AI 的智能压缩和格式化

## 测试建议

### 单元测试

- 测试 Markdown 转换逻辑
- 测试压缩算法
- 测试错误处理

### 集成测试

- 测试完整的上下文生成流程
- 测试 API 端点
- 测试前后端集成

### 性能测试

- 测试大型报告的处理性能
- 测试并发请求处理
- 测试内存使用情况

## 维护说明

1. **日志监控**: 关注上下文生成的性能和错误率
2. **版本兼容**: 确保与 StructuredReportService 的兼容性
3. **文档更新**: 及时更新 API 文档和使用说明
4. **依赖管理**: 定期更新相关依赖包