# 多数据源优化架构文档

## 概述

本文档记录了将ProfilingTask与DataSourceConfig的关系从一对一改为一对多的架构优化，以支持单个任务处理多个数据源的需求。

## 架构变更

### 1. 实体层变更

#### ProfilingTask实体
- **变更前**: 使用`@ManyToOne`关系维护单个`DataSourceConfig`
- **变更后**: 使用`@ManyToMany`关系维护`List<DataSourceConfig>`
- **新增字段**:
  - `totalDataSources`: 任务包含的数据源总数
  - `processedDataSources`: 已处理完成的数据源数量

```java
// 变更前
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "data_source_id")
private DataSourceConfig dataSourceConfig;

// 变更后
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "profiling_task_datasource",
    joinColumns = @JoinColumn(name = "task_id"),
    inverseJoinColumns = @JoinColumn(name = "datasource_id")
)
private List<DataSourceConfig> dataSourceConfigs = new ArrayList<>();

@Column(name = "total_data_sources")
private Integer totalDataSources;

@Column(name = "processed_data_sources")
private Integer processedDataSources = 0;
```

### 2. Repository层变更

#### ProfilingTaskRepository
- **移除方法**: `findByDataSourceConfig(DataSourceConfig dataSourceConfig)`
- **新增方法**:
  - `findByDataSourceConfigsContaining(@Param("dataSourceConfig") DataSourceConfig dataSourceConfig)`
  - `findByDataSourceId(@Param("dataSourceId") String dataSourceId)`

### 3. Service层变更

#### ProfilingServiceImpl
- **任务创建逻辑**: 支持多数据源配置的批量加载和验证
- **执行策略**: 从并行处理改为串行处理，确保任务状态的准确跟踪
- **进度跟踪**: 实时更新已处理数据源数量

## 核心功能变更

### 1. 任务创建流程

```java
// 支持多数据源的任务创建
List<DataSourceConfig> dataSourceConfigs = new ArrayList<>();
for (String dataSourceId : request.getDatasources().keySet()) {
    DataSourceConfig dataSource = dataSourceService.getDataSourceBySourceId(dataSourceId);
    if (dataSource != null) {
        dataSourceConfigs.add(dataSource);
    }
}
task.setDataSourceConfigs(dataSourceConfigs);
task.setTotalDataSources(dataSourceConfigs.size());
```

### 2. 串行执行策略

**变更原因**: 确保任务状态和进度的准确跟踪

**实现方式**:
- 顺序处理每个数据源
- 每完成一个数据源后更新进度计数器
- 即使单个数据源失败也继续处理其他数据源
- 所有数据源处理完成后统一更新任务状态

```java
for (Map.Entry<String, ProfilingTaskRequest.DataSourceScope> entry : dataSources.entrySet()) {
    // 处理单个数据源
    RawProfileDataDto result = processSingleDataSource(dataSourceId, scope, taskId);
    
    // 更新进度
    processedCount++;
    task.setProcessedDataSources(processedCount);
    profilingTaskRepository.save(task);
}
```

### 3. 进度跟踪机制

- **总数跟踪**: `totalDataSources`字段记录任务包含的数据源总数
- **进度跟踪**: `processedDataSources`字段实时更新已处理数量
- **状态更新**: 每处理完一个数据源后更新任务状态信息
- **完成判断**: 通过`isAllDataSourcesProcessed()`方法判断是否全部完成

## 对后续流程的影响分析

### 1. 报告组装服务 (ReportAssemblyService)

**影响**: 无需变更

**原因**: 
- 接口设计已支持多数据源: `assembleReport(List<RawProfileDataDto> rawDataList, String taskId)`
- 实现逻辑按数据源独立处理，天然支持多数据源场景
- 每个数据源生成独立的结构化报告

### 2. 结构化报告服务 (StructuredReportService)

**影响**: 接口签名微调

**变更内容**:
- `saveReports`方法增加`taskId`参数以支持批量保存
- 新增缺失的接口方法实现
- 保持原有的多报告批量处理能力

### 3. 报告存储和检索

**影响**: 无需变更

**原因**:
- StructuredReport实体已支持按taskId存储多个报告
- Repository查询方法支持按taskId检索所有相关报告
- 分页和过滤功能保持不变

### 4. 指标计算流程

**影响**: 增强能力

**优势**:
- 支持跨数据源的聚合指标计算
- 可以生成任务级别的汇总统计
- 保持单数据源指标计算的独立性

## 数据库变更

### 新增关联表

```sql
CREATE TABLE profiling_task_datasource (
    task_id BIGINT NOT NULL,
    datasource_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, datasource_id),
    FOREIGN KEY (task_id) REFERENCES profiling_task(id),
    FOREIGN KEY (datasource_id) REFERENCES data_source_config(id)
);
```

### ProfilingTask表字段新增

```sql
ALTER TABLE profiling_task 
ADD COLUMN total_data_sources INT DEFAULT 0,
ADD COLUMN processed_data_sources INT DEFAULT 0;
```

## 兼容性考虑

### 1. 向后兼容
- 单数据源任务仍然正常工作
- 现有API接口保持不变
- 数据库迁移脚本确保现有数据正常转换

### 2. 性能影响
- 串行处理可能增加总体执行时间
- 但提供了更好的资源控制和错误隔离
- 进度跟踪提供了更好的用户体验

## 测试建议

### 1. 单元测试
- ProfilingTask实体的多数据源关系映射
- Repository查询方法的正确性
- Service层的串行处理逻辑

### 2. 集成测试
- 多数据源任务的端到端执行
- 进度跟踪的准确性
- 错误处理和恢复机制

### 3. 性能测试
- 大量数据源的处理性能
- 内存使用情况
- 数据库连接池的影响

## 总结

本次多数据源优化通过以下方式实现了架构升级:

1. **实体关系优化**: 从一对一改为一对多，支持任务级别的多数据源管理
2. **执行策略调整**: 采用串行处理确保状态跟踪的准确性
3. **进度监控增强**: 实时跟踪处理进度，提供更好的用户体验
4. **向后兼容**: 保持现有功能的完整性，平滑升级

该优化为系统提供了更强的扩展性和更好的任务管理能力，为后续的高级功能（如跨数据源分析、批量处理等）奠定了基础。