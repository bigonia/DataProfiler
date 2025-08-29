### AI上下文服务模块（AI-Context-Service）后端设计方案

#### 1\. 方案目标

在`DBCrawlerV3`后端服务中，创建一个新的服务模块，该模块能够：

1.  接收一个`taskId`作为输入。
2.  从数据库中加载与该`taskId`关联的、完整的“数据画像”报告（`StructuredReportDto`）。
3.  对这份详尽的JSON报告进行**智能压缩与格式化**，生成一段信息密度高、Token友好、LLM易于理解的Markdown文本。
4.  将生成的Markdown文本提供给上层AI调用服务（`AIService`），作为调用Dify工作流的核心上下文。

#### 2\. 模块设计与开发任务

我们将在您现有的分层架构中新增一个`AIContextService`，并对原有的`AIService`进行调整。

##### 2.1. 服务层 (Service Layer)

**任务1：创建 `AIContextService` 接口与实现**

这是本次新增的核心模块。它负责将复杂的数据画像DTO转换为LLM友好的字符串。

  * **创建接口 `AIContextService.java`:**

    ```java
    package com.dataprofiler.service;

    public interface AIContextService {

        /**
         * 根据任务ID构建用于LLM的Markdown格式上下文
         * @param taskId 剖析任务的ID
         * @return 格式化后的Markdown字符串上下文
         */
        String buildContextForLLM(String taskId);
    }
    ```

  * **创建实现类 `AIContextServiceImpl.java`:**

    ```java
    package com.dataprofiler.service.impl;

    import com.dataprofiler.dto.response.StructuredReportDto;
    import com.dataprofiler.service.AIContextService;
    import com.dataprofiler.service.StructuredReportService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;

    @Service
    public class AIContextServiceImpl implements AIContextService {

        private static final int MAX_CONTEXT_LENGTH = 30000; // 定义一个合理的上下文最大字符数，防止超长

        @Autowired
        private StructuredReportService structuredReportService;

        @Override
        public String buildContextForLLM(String taskId) {
            // 1. 从现有服务获取完整的结构化报告数据
            StructuredReportDto report = structuredReportService.getReportByTaskId(taskId); // 假设您有这个方法
            if (report == null) {
                throw new RuntimeException("Report not found for task ID: " + taskId);
            }

            // 2. 使用StringBuilder高效构建Markdown字符串
            StringBuilder contextBuilder = new StringBuilder();

            // 3. 调用私有方法，将DTO转换为Markdown
            appendReportSummary(contextBuilder, report);
            
            for (StructuredReportDto.SchemaReport schema : report.getSchemas()) {
                appendSchemaSummary(contextBuilder, schema);
                
                // 对表按行数降序排序，优先展示核心表
                schema.getTables().sort((t1, t2) -> t2.getRowCount().compareTo(t1.getRowCount()));

                for (StructuredReportDto.TableReport table : schema.getTables()) {
                    appendTableDetails(contextBuilder, table);
                }
            }

            // 4. 截断处理，防止上下文超长
            String finalContext = contextBuilder.toString();
            if (finalContext.length() > MAX_CONTEXT_LENGTH) {
                return finalContext.substring(0, MAX_CONTEXT_LENGTH) + "\n\n...[上下文内容过长，已被截断]";
            }
            
            return finalContext;
        }
        
        // --- 以下为辅助方法，用于将DTO的不同部分转换为Markdown ---

        private void appendReportSummary(StringBuilder sb, StructuredReportDto report) {
            sb.append("# 数据源分析报告摘要\n\n");
            sb.append(String.format("- **数据源名称**: %s\n", report.getDataSourceName()));
            sb.append(String.format("- **数据源类型**: %s\n", report.getDataSourceType()));
            sb.append(String.format("- **核心摘要**: 共 %d 个表, %d 个字段, 总行数约 %d。\n", 
                report.getSummary().getTotalTables(),
                report.getSummary().getTotalColumns(),
                report.getSummary().getTotalRows()));
            sb.append(String.format("- **整体数据质量评分**: %.2f\n", report.getSummary().getDataQualityScore()));
            sb.append("\n---\n");
        }
        
        private void appendSchemaSummary(StringBuilder sb, StructuredReportDto.SchemaReport schema) {
            sb.append(String.format("## 模式: `%s`\n", schema.getSchemaName()));
            sb.append(String.format("- 包含 %d 张表，总行数约 %d。\n", 
                schema.getStatistics().getTableCount(),
                schema.getStatistics().getTotalRows()));
        }

        private void appendTableDetails(StringBuilder sb, StructuredReportDto.TableReport table) {
            sb.append(String.format("\n### 表: `%s` (行数: %d)\n", table.getName(), table.getRowCount()));
            if (table.getComment() != null && !table.getComment().isEmpty()) {
                sb.append(String.format("**注释**: %s\n", table.getComment()));
            }
            sb.append("**字段列表:**\n");

            for (StructuredReportDto.ColumnReport column : table.getColumns()) {
                String pk_fk = column.isPrimaryKey() ? " (PK)" : (column.isForeignKey() ? " (FK)" : "");
                sb.append(String.format("- `%s` (`%s`%s): 空值率 %.2f%%, 唯一值率 %.2f%%. 注释: %s\n",
                    column.getName(),
                    column.getType(),
                    pk_fk,
                    column.getMetrics().getNullRate() * 100,
                    column.getMetrics().getDistinctRate() * 100,
                    column.getComment() != null ? column.getComment() : "无"
                ));
            }

            // 添加样本数据，使用您设计的紧凑格式
            if (table.getSampleRows() != null && table.getSampleRows().getRows() != null && !table.getSampleRows().getRows().isEmpty()) {
                sb.append("\n**样本数据 (前3行):**\n");
                sb.append("| ").append(String.join(" | ", table.getSampleRows().getHeaders())).append(" |\n");
                sb.append("|").append("---|".repeat(table.getSampleRows().getHeaders().size())).append("\n");
                for (int i = 0; i < Math.min(table.getSampleRows().getRows().size(), 3); i++) {
                     sb.append("| ").append(String.join(" | ", table.getSampleRows().getRows().get(i))).append(" |\n");
                }
            }
        }
    }
    ```

**任务3：改造 `AIServiceImpl` - 开发新的V2实现**

现在，`AIServiceV2Impl`不再简单地转发`taskId`，而是先调用`AIContextService`准备好上下文。

```java
// 在 AIServiceV2Impl.java 中
// ...
@Autowired
private AIContextService aiContextService; // 注入新创建的服务

@Override
@Async
public SseEmitter streamAnalysis(AnalysisRequest request) {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    try {
        // 1. 调用新服务，生成精简的Markdown上下文
        String preparedContext = aiContextService.buildContextForLLM(request.getTaskId());

        // 2. 构建Dify API请求，这次传递的是完整的上下文
        Map<String, String> inputs = Map.of(
            "profile_context", preparedContext, // 变量名与Dify工作流中一致
            "user_question", request.getQuestion()
        );
        DifyWorkflowRequest difyRequest = new DifyWorkflowRequest(inputs, request.getUserId(), true);

        // 3. 调用Dify并订阅流式结果 (这部分逻辑与之前相同)
        difyApiClient.invokeWorkflowStream(difyRequest)
            // ... .doOnNext, .doOnComplete, .doOnError 的逻辑不变 ...
            .subscribe();

    } catch (Exception e) {
        emitter.completeWithError(e);
    }
    return emitter;
}
```

### 总结

这份后端设计方案的**核心**，是新增一个`AIContextService`，**将数据画像的“智能压缩与格式化”逻辑收归后端**。

**这样做的好处显而易见：**

  * **架构解耦：** 您的AI服务(Dify)变成了一个纯粹的、无状态的推理引擎，它不关心数据从何而来，只负责处理给定的上下文。这使得更换LLM平台或模型变得轻而易举。
  * **性能提升：** 减少了`Dify -> 后端`的HTTP回调，所有数据准备都在服务内部完成，响应速度会更快。
  * **安全可控：** 您无需再为Dify配置回调API的安全策略，所有敏感的数据处理都在您自己的服务内部，降低了安全风险。
  * **可维护性强：** 核心的上下文处理逻辑由您熟悉的Java语言和Spring框架管理，可以进行单元测试、版本控制，完全纳入您现有的工程体系。

