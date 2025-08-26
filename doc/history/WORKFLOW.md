# 项目工作流与开发计划

## 1. 引言

本文档概述了数据分析器应用的完整业务工作流，从数据源创建到报告生成。它还指出了当前实现中存在的差距，并提出了相应的开发计划来解决这些问题。

核心工作流旨在实现健壮性和可扩展性，允许用户配置各种数据源，异步运行分析任务，并检索详细的分析报告。

## 2. 核心业务工作流

端到端流程分为五个主要步骤：

### 第一步：创建和配置数据源

- **端点:** `POST /datasources`
- **描述:** 这是定义新数据源并将其配置存储在系统中的第一步。
- **请求体:** 一个 `DataSourceConfig` JSON 对象，包含名称、类型（例如 `POSTGRESQL`, `MYSQL`, `CSV`）、连接凭证和其他元数据等详细信息。
- **响应:** 一个 `DataSourceConfig` 对象，其中包含一个唯一的 `sourceId`，该 ID 将在所有后续步骤中使用。
- **状态:** **已实现**。

### 第二步：测试数据源连接

- **端点:** `POST /datasources/{sourceId}/test`
- **描述:** 验证在第一步中提供的配置是否正确，以及应用程序是否可以成功连接到目标数据源。
- **路径变量:** 从第一步获得的 `sourceId`。
- **响应:** 一个 JSON 对象，指示连接状态（`success`: true/false）、描述性消息和连接延迟。
- **状态:** **已实现**。

### 第三步：创建并启动分析任务

- **端点:** `POST /profiling/profiling-tasks`
- **描述:** 在一个或多个已配置的数据源上启动一个异步分析任务。
- **请求体:** 一个 `ProfilingTaskRequest` JSON 对象，指定任务名称、描述以及要分析的 `datasources` 列表。每个数据源对象都包含 `sourceId` 以及一个可选的 `tables` 和 `schemas` 列表，用于指定扫描范围。
- **响应:** 一个 `ProfilingTask` 对象，其中包含一个唯一的 `taskId`。
- **状态:** **已实现**。

### 第四步：监控任务状态

- **端点:** `GET /task-status/{taskId}`
- **描述:** 允许客户端轮询正在进行的分析任务的状态。
- **路径变量:** 从第三步获得的 `taskId`。
- **响应:** 一个 `TaskStatusResponse` 对象，详细说明任务的当前 `status`（例如 "RUNNING", "COMPLETED", "FAILED"）、进度信息（`processedDataSources` / `totalDataSources`）和时间戳。
- **状态:** **已实现**。

### 第五步：查询分析报告

- **描述:** 一旦任务状态变为 `COMPLETED`，客户端就可以使用以下端点检索生成的报告。
- **端点:**
    - `POST /api/reports/summary`: 获取给定 `dataSourceIds` 列表的轻量级摘要报告。
    - `POST /api/reports/detailed`: 获取给定 `dataSourceIds` 列表的详细、可过滤和分页的报告。
- **状态:** **已实现**。

### 工作流图

```mermaid
graph TD
    subgraph "步骤 1 & 2: 数据源管理"
        A[1. 创建数据源<br/>POST /datasources] -->|返回 sourceId| B;
        B[2. 测试连接<br/>POST /datasources/{sourceId}/test] -->|验证成功| C;
    end

    subgraph "步骤 3 & 4: 任务管理"
        C[3. 启动分析任务<br/>POST /profiling/profiling-tasks<br/>(传入 sourceId)] -->|返回 taskId| D;
        D[4. 轮询任务状态<br/>GET /task-status/{taskId}] -->|状态: COMPLETED| E;
    end

    subgraph "步骤 5: 报告查询"
        E[5. 查询报告<br/>POST /api/reports/summary<br/>POST /api/reports/detailed<br/>(传入 sourceId)] --> F[获取分析结果];
    end
```

## 3. 已发现问题与后续步骤

根据当前的实现，发现了以下问题。解决这些问题将提高系统的可用性和完整性。

### 问题一：缺少用于 Schema/Table 发现的端点

- **问题:** 当前工作流要求用户在创建分析任务时手动提供 schema 和 table 名称。没有 API 端点可以从给定的数据源中发现可用的 schema 和 table。
- **影响:** 这使得系统不够用户友好且更容易出错，因为用户必须通过外部工具来查找这些信息。
- **后续步骤:**
    - **实现 `GET /datasources/{sourceId}/schemas`:** 此端点将返回给定数据源的所有可用 schema 的列表。
    - **实现 `GET /datasources/{sourceId}/tables?schema={schemaName}`:** 此端点将返回指定 schema 内的 table 列表。

### 问题二：“获取所有任务”端点存在缺陷

- **问题:** 端点 `GET /profiling/profiling-tasks` 旨在检索所有分析任务，但错误地配置为需要一个 `taskId` 作为路径变量。
- **影响:** 目前没有功能可以列出系统中已创建的所有任务。
- **后续步骤:**
    - **重构 `ProfilingTaskController` 中的 `getAllProfilingTask`:** 移除 `@PathVariable` 并修改服务层以返回所有分析任务的分页列表。端点应为 `GET /profiling/profiling-tasks`。

### 问题三：缺少以任务为中心的报告查询 ✅ **已解决**

- **问题:** 报告目前是使用 `dataSourceIds` 进行查询的。虽然灵活，但对于希望查看刚运行的特定任务关联的所有报告的用户来说，这并不直观。
- **影响:** 工作流不够直接。用户必须先找出哪些数据源属于某个任务，然后使用这些 ID 来查询报告。
- **解决方案:**
    - **✅ 已实现 `GET /api/reports/task/{taskId}`:** 在 `ReportController` 中创建了新的便捷端点，用于检索给定 `taskId` 的所有报告。该端点提供任务中心的报告查询视图，简化了任务完成后的用户体验。
    - **✅ 服务层支持:** 在 `StructuredReportService` 接口中添加了 `getTaskReports(String taskId)` 方法，并在 `StructuredReportServiceImpl` 中提供了完整实现。
    - **✅ 保留原有功能:** 原有的基于数据源ID的报告查询功能完全保留，确保向后兼容性。

## 4. 结论

核心分析工作流定义明确且功能齐全。当前的开发重点应放在解决上述三个问题上。这些增强功能将显著提高系统的可用性，使其对最终用户更加直观和高效。