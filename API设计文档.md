### **智能数据剖析与处理平台 - API设计文档 (基于OpenAPI v3)**

#### **1. 概述**

本文档定义了“智能数据剖析与处理平台”的RESTful API。所有API遵循OpenAPI v3.0规范，旨在为前后端分离开发提供一个清晰、无歧义的“单一事实来源 (Single Source of Truth)”。

通过此规范，您可以：

  * **生成交互式API文档 (Swagger UI)**：方便前后端开发人员在线浏览、测试API。
  * **生成客户端与服务端代码**: 为多种语言自动生成调用API的客户端SDK或服务端代码存根，提升开发效率。

本文档将使用 **YAML** 格式进行描述，因为它比JSON更具可读性。

-----

#### **2. OpenAPI 核心定义 (oas-spec.yaml)**

```yaml
openapi: 3.0.3
info:
  title: 智能数据剖析与处理平台 API
  description: |-
    用于管理数据源、发起统一的数据剖析任务（支持单源与多源）、并以分层和可压缩载荷的方式获取结构化分析报告的API。
    核心流程：
    1.  配置一个或多个数据源 (`POST /data-sources`)。
    2.  发起剖析任务 (`POST /profiling-tasks`)，获取任务ID。
    3.  根据任务ID轮询任务状态。
    4.  任务成功后，通过分层查询接口获取报告:
        -   获取摘要报告 (`POST /reports/summary`) 以了解概况。
        -   获取详细报告 (`POST /reports/details`) 进行深度分析。
  version: 1.0.0

servers:
  - url: http://localhost:8080/api/v1
    description: 本地开发服务器
  - url: https://XXX/api/v1
    description: 生产环境服务器

tags:
  - name: DataSource
    description: 数据源管理相关接口
  - name: File
    description: 文件管理相关接口
  - name: Profiling
    description: 数据剖析任务相关接口
  - name: Report
    description: 分析报告获取相关接口

paths:
  # --- DataSource Endpoints ---
  /datasources:
    post:
      tags: [DataSource]
      summary: 创建一个新的数据源配置
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DataSourceConfig'
      responses:
        '201':
          description: 数据源创建成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DataSourceConfig'
        '400': { $ref: '#/components/responses/BadRequest' }
        '500': { $ref: '#/components/responses/InternalServerError' }
    get:
      tags: [DataSource]
      summary: 获取所有活跃的数据源
      responses:
        '200':
          description: 成功返回数据源列表
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/DataSourceConfig'

  /datasources/{id}:
    get:
      tags: [DataSource]
      summary: 根据ID获取数据源
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: 成功返回数据源
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DataSourceConfig'
        '404': { $ref: '#/components/responses/NotFound' }
    put:
      tags: [DataSource]
      summary: 更新数据源配置
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DataSourceConfig'
      responses:
        '200':
          description: 数据源更新成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DataSourceConfig'
        '400': { $ref: '#/components/responses/BadRequest' }
        '404': { $ref: '#/components/responses/NotFound' }
    delete:
      tags: [DataSource]
      summary: 删除数据源（软删除）
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      responses:
        '204':
          description: 数据源删除成功
        '404': { $ref: '#/components/responses/NotFound' }

  /datasources/{id}/activate:
    post:
      tags: [DataSource]
      summary: 激活数据源
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: 数据源激活成功
        '404': { $ref: '#/components/responses/NotFound' }

  /datasources/{id}/test:
    post:
      tags: [DataSource]
      summary: 测试数据源连接
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: 连接测试完成
          content:
            application/json:
              schema:
                type: object
                properties:
                  success:
                    type: boolean
                  message:
                    type: string
        '404': { $ref: '#/components/responses/NotFound' }

  /datasources/test:
    post:
      tags: [DataSource]
      summary: 使用配置测试数据源连接
      description: 使用提供的配置测试连接，不保存配置
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DataSourceConfig'
      responses:
        '200':
          description: 连接测试完成
          content:
            application/json:
              schema:
                type: object
                properties:
                  success:
                    type: boolean
                  message:
                    type: string

  /datasources/type/{type}:
    get:
      tags: [DataSource]
      summary: 根据类型获取数据源
      parameters:
        - name: type
          in: path
          required: true
          schema:
            type: string
            enum: [MYSQL, POSTGRESQL, SQLITE, FILE]
      responses:
        '200':
          description: 成功返回指定类型的数据源
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/DataSourceConfig'

  /datasources/search:
    get:
      tags: [DataSource]
      summary: 搜索数据源
      parameters:
        - name: q
          in: query
          required: true
          schema:
            type: string
          description: 搜索关键词
      responses:
        '200':
          description: 搜索完成
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/DataSourceConfig'

  /datasources/statistics:
    get:
      tags: [DataSource]
      summary: 获取数据源统计信息
      responses:
        '200':
          description: 成功返回统计信息
          content:
            application/json:
              schema:
                type: object
                properties:
                  totalCount:
                    type: integer
                  activeCount:
                    type: integer
                  typeDistribution:
                    type: object
                    additionalProperties:
                      type: integer

  # --- File Management Endpoints ---
  /files/upload:
    post:
      tags: [File]
      summary: 上传文件并创建数据源
      description: 上传文件（CSV、Excel等）并自动创建基于文件的数据源
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                file:
                  type: string
                  format: binary
      responses:
        '201':
          description: 文件上传成功并创建数据源
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DataSourceConfig'
        '400': { $ref: '#/components/responses/BadRequest' }
        '500': { $ref: '#/components/responses/InternalServerError' }

  /files/health:
    get:
      tags: [File]
      summary: 文件服务健康检查
      responses:
        '200':
          description: 服务健康
          content:
            application/json:
              schema:
                type: string
                example: "File service is healthy"

  # --- Profiling Task Endpoints ---
  /profiling/profiling-tasks:
    post:
      tags: [Profiling]
      summary: 发起一个统一的剖析任务（支持单源或多源）
      description: 异步接口。接收一个或多个数据源的剖析请求，调用成功后立即返回一个任务ID。
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProfilingTaskRequest'
      responses:
        '201':
          description: 任务已成功创建
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProfilingTask'
        '400': { $ref: '#/components/responses/BadRequest' }
        '500': { $ref: '#/components/responses/InternalServerError' }

  /profiling/task-status/{taskId}:
    get:
      tags: [Profiling]
      summary: 获取任务状态
      description: 根据任务ID获取当前状态和进度信息
      parameters:
        - name: taskId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: 成功返回任务状态
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TaskStatusResponse'
        '404': { $ref: '#/components/responses/NotFound' }
        '400': { $ref: '#/components/responses/BadRequest' }

  /profiling/profiling-tasks/health:
    get:
      tags: [Profiling]
      summary: 剖析服务健康检查
      responses:
        '200':
          description: 服务健康
          content:
            application/json:
              schema:
                type: string
                example: "Profiling service is healthy"

  # --- Report Endpoints ---
  /reports/summary:
    post:
      tags: [Report]
      summary: 摘要查询 - 获取轻量级的元数据报告
      description: 提供一个或多个数据源的宏观概览，返回表目录、行数、列数等轻量级信息。
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ReportSummaryRequest'
      responses:
        '200':
          description: 成功返回摘要报告
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/ReportSummaryDto'
        '400': { $ref: '#/components/responses/BadRequest' }
        '500': { $ref: '#/components/responses/InternalServerError' }

  /reports/detailed:
    post:
      tags: [Report]
      summary: 详细查询 - 获取完整的细粒度剖析报告
      description: 支持对多个数据源、Schema和表进行批量、精确的查询，并提供可选的载荷结构压缩。
      parameters:
        - name: format
          in: query
          required: false
          schema:
            type: string
            enum: [object, compact]
            default: object
          description: "定义返回的JSON结构。`object`为键值对数组，`compact`为header-rows结构。"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DetailedReportRequest'
      responses:
        '200':
          description: 成功返回详细报告
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/StructuredReportDto'
        '400': { $ref: '#/components/responses/BadRequest' }
        '500': { $ref: '#/components/responses/InternalServerError' }

  /reports/task/{taskId}:
    get:
      tags: [Report]
      summary: 根据任务ID获取报告
      description: 获取指定剖析任务生成的所有报告
      parameters:
        - name: taskId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: 成功返回报告列表
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/StructuredReport'
        '404': { $ref: '#/components/responses/NotFound' }

  /reports/datasource/{dataSourceId}:
    get:
      tags: [Report]
      summary: 根据数据源ID获取报告
      description: 获取指定数据源的所有报告
      parameters:
        - name: dataSourceId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: 成功返回报告列表
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/StructuredReport'

  /reports/table:
    get:
      tags: [Report]
      summary: 根据表获取报告
      description: 获取指定表的最新报告
      parameters:
        - name: dataSourceId
          in: query
          required: true
          schema:
            type: string
        - name: schemaName
          in: query
          required: false
          schema:
            type: string
        - name: tableName
          in: query
          required: true
          schema:
            type: string
      responses:
        '200':
          description: 成功返回报告
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/StructuredReport'
        '404': { $ref: '#/components/responses/NotFound' }

  /reports:
    get:
      tags: [Report]
      summary: 获取所有报告（分页）
      parameters:
        - name: page
          in: query
          required: false
          schema:
            type: integer
            default: 0
            minimum: 0
        - name: size
          in: query
          required: false
          schema:
            type: integer
            default: 10
            minimum: 1
      responses:
        '200':
          description: 成功返回报告分页数据
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/StructuredReport'
                  totalElements:
                    type: integer
                  totalPages:
                    type: integer
                  size:
                    type: integer
                  number:
                    type: integer

  /reports/range:
    get:
      tags: [Report]
      summary: 根据日期范围获取报告
      parameters:
        - name: startDate
          in: query
          required: true
          schema:
            type: string
            format: date-time
        - name: endDate
          in: query
          required: true
          schema:
            type: string
            format: date-time
      responses:
        '200':
          description: 成功返回报告列表
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/StructuredReport'

  /reports/large:
    get:
      tags: [Report]
      summary: 获取大型报告
      description: 获取超过指定大小阈值的报告
      parameters:
        - name: sizeThreshold
          in: query
          required: false
          schema:
            type: integer
            default: 1048576
            minimum: 1
          description: 大小阈值（字节）
      responses:
        '200':
          description: 成功返回大型报告列表
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/StructuredReport'

  /reports/cleanup:
    delete:
      tags: [Report]
      summary: 清理旧报告
      description: 删除旧报告以释放存储空间
      parameters:
        - name: daysOld
          in: query
          required: false
          schema:
            type: integer
            default: 90
            minimum: 1
          description: 天数阈值
      responses:
        '200':
          description: 清理完成
          content:
            application/json:
              schema:
                type: object
                properties:
                  deletedCount:
                    type: integer
                  freedSpace:
                    type: integer

  /reports/statistics:
    get:
      tags: [Report]
      summary: 获取报告统计信息
      responses:
        '200':
          description: 成功返回统计信息
          content:
            application/json:
              schema:
                type: object
                properties:
                  totalReports:
                    type: integer
                  totalSize:
                    type: integer
                  averageSize:
                    type: number

  /reports/export/{reportId}:
    get:
      tags: [Report]
      summary: 导出报告
      description: 以各种格式导出指定报告
      parameters:
        - name: reportId
          in: path
          required: true
          schema:
            type: string
        - name: format
          in: query
          required: false
          schema:
            type: string
            enum: [json, csv, excel]
            default: json
      responses:
        '200':
          description: 报告导出成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  downloadUrl:
                    type: string
                  format:
                    type: string
        '404': { $ref: '#/components/responses/NotFound' }
        '400': { $ref: '#/components/responses/BadRequest' }

  /reports/health:
    get:
      tags: [Report]
      summary: 报告服务健康检查
      responses:
        '200':
          description: 服务健康
          content:
            application/json:
              schema:
                type: object
                properties:
                  status:
                    type: string
                    example: "healthy"
                  timestamp:
                    type: string
                    format: date-time


components:
  schemas:
    # --- Config & Request Schemas ---
    DataSourceConfig:
      type: object
      properties:
        id: { type: string, readOnly: true }
        name: { type: string, example: "季度销售报告.xlsx" }
        type: { type: string, enum: [MYSQL, POSTGRESQL, SQLITE, FILE] }
        properties:
          type: object
          description: "根据type动态变化的连接属性"
          example:
            # Example for POSTGRESQL
            host: "pg.dataprofiler.com"
            port: 5432
            username: "user"
            database: "sales_dw"
            schema: "public"
            # Example for FILE
            # originalFileName: "季度销售报告.xlsx"
            # internalFileId: "file-xyz-789"

    ProfilingTaskRequest:
      type: object
      required: [datasources]
      properties:
        datasources:
          type: object
          description: "Key为dataSourceId, Value为Schema和表的映射。空数组表示剖析该Schema下所有表。"
          example:
            "ds-pg-01":
              schemas:
                "public": ["orders", "customers"]
                "marketing": []
            "ds-file-01": {} # 表示剖析该文件源下的所有Sheets

    ReportSummaryRequest:
      type: object
      required: [dataSourceIds]
      properties:
        dataSourceIds:
          type: array
          items: { type: string }
          example: ["ds-pg-01", "ds-file-01"]

    DetailedReportRequest:
      type: object
      required: [datasources]
      properties:
        datasources:
          # Re-using the same structure as ProfilingTaskRequest for consistency
          $ref: '#/components/schemas/ProfilingTaskRequest/properties/datasources'
        pagination:
          type: object
          properties:
            page: { type: integer, default: 1 }
            pageSize: { type: integer, default: 10 }
    
    # --- Response Schemas ---
    ProfilingTask:
      type: object
      properties:
        taskId: { type: string }
        status: { type: string, enum: [PENDING, RUNNING, SUCCESS, FAILED] }
        createdAt: { type: string, format: date-time }
        updatedAt: { type: string, format: date-time }
        datasources: { type: object }
        progress: { type: number, format: float }
        errorMessage: { type: string, nullable: true }

    TaskStatusResponse:
      type: object
      properties:
        taskId: { type: string }
        status: { type: string, enum: [PENDING, RUNNING, SUCCESS, FAILED] }
        progress: { type: number, format: float }
        startTime: { type: string, format: date-time }
        endTime: { type: string, format: date-time, nullable: true }
        errorMessage: { type: string, nullable: true }
        completedDataSources: { type: integer }
        totalDataSources: { type: integer }

    ReportSummaryDto:
      type: object
      properties:
        dataSourceId: { type: string }
        dataSourceName: { type: string }
        dataSourceType: { type: string }
        schemas:
          type: array
          items:
            type: object
            properties:
              schemaName: { type: string, nullable: true }
              tables:
                type: array
                items:
                  type: object
                  properties:
                    tableName: { type: string }
                    rowCount: { type: integer }
                    columnCount: { type: integer }
                    comment: { type: string, nullable: true }

    StructuredReport:
      type: object
      properties:
        id: { type: string }
        taskId: { type: string }
        dataSourceId: { type: string }
        schemaName: { type: string, nullable: true }
        tableName: { type: string }
        createdAt: { type: string, format: date-time }
        reportData: { type: object }
        reportSize: { type: integer }
        status: { type: string, enum: [COMPLETED, FAILED] }

    StructuredReportDto:
      type: object
      properties:
        dataSourceId: { type: string }
        dataSourceType: { type: string, enum: [MYSQL, POSTGRESQL, SQLITE, FILE] }
        database: { type: object, properties: { name: { type: string } } }
        tables:
          type: array
          items:
            type: object
            properties:
              name: { type: string }
              schemaName: { type: string, nullable: true }
              rowCount: { type: integer }
              columns:
                $ref: '#/components/schemas/FormattedArray'
              sampleRows:
                $ref: '#/components/schemas/FormattedArray'
    
    FormattedArray:
      description: "一个可以表示为标准格式或紧凑格式的数组结构"
      oneOf:
        - $ref: '#/components/schemas/StandardFormat'
        - $ref: '#/components/schemas/CompactFormat'

    StandardFormat:
      type: array
      items:
        type: object
        additionalProperties: true
      example:
        - id: 1
          name: "Alice"
        - id: 2
          name: "Bob"

    CompactFormat:
      type: object
      properties:
        headers:
          type: array
          items: { type: string }
        rows:
          type: array
          items:
            type: array
            items: {}
      example:
        headers: ["id", "name"]
        rows:
          - [1, "Alice"]
          - [2, "Bob"]

    ApiError:
      type: object
      properties:
        statusCode: { type: integer }
        message: { type: string }
        timestamp: { type: string, format: date-time }
        path: { type: string }

  responses:
    BadRequest:
      description: 无效的请求参数
      content:
        application/json:
          schema: { $ref: '#/components/schemas/ApiError' }
    
    NotFound:
      description: 资源未找到
      content:
        application/json:
          schema: { $ref: '#/components/schemas/ApiError' }
    
    InternalServerError:
      description: 内部服务器错误
      content:
        application/json:
          schema: { $ref: '#/components/schemas/ApiError' }

```

-----

#### **3. TODO - 待实现功能**

以下接口和功能已在文档中定义但尚未实现，需要后续开发：

##### **3.1 数据源管理扩展功能**
- **TODO**: 实现数据源分页查询接口 (`GET /datasources/page`)
- **TODO**: 实现数据源批量操作接口 (`POST /datasources/batch`)
- **TODO**: 实现数据源配置验证接口 (`POST /datasources/validate`)
- **TODO**: 实现数据源使用历史查询接口 (`GET /datasources/{id}/history`)

##### **3.2 文件管理扩展功能**
- **TODO**: 实现文件批量上传接口 (`POST /files/batch-upload`)
- **TODO**: 实现文件删除接口 (`DELETE /files/{fileId}`)
- **TODO**: 实现文件下载接口 (`GET /files/{fileId}/download`)
- **TODO**: 实现文件预览接口 (`GET /files/{fileId}/preview`)
- **TODO**: 实现文件格式转换接口 (`POST /files/{fileId}/convert`)

##### **3.3 剖析任务扩展功能**
- **TODO**: 实现任务取消接口 (`POST /profiling/task/{taskId}/cancel`)
- **TODO**: 实现任务重试接口 (`POST /profiling/task/{taskId}/retry`)
- **TODO**: 实现任务日志查询接口 (`GET /profiling/task/{taskId}/logs`)
- **TODO**: 实现批量任务状态查询接口 (`POST /profiling/tasks/status`)
- **TODO**: 实现任务调度配置接口 (`POST /profiling/schedules`)

##### **3.4 报告管理扩展功能**
- **TODO**: 实现报告比较接口 (`POST /reports/compare`)
- **TODO**: 实现报告模板管理接口 (`GET/POST/PUT/DELETE /reports/templates`)
- **TODO**: 实现报告订阅接口 (`POST /reports/subscriptions`)
- **TODO**: 实现报告分享接口 (`POST /reports/{reportId}/share`)
- **TODO**: 实现报告版本管理接口 (`GET /reports/{reportId}/versions`)

##### **3.5 系统管理功能**
- **TODO**: 实现用户认证和授权模块
- **TODO**: 实现系统配置管理接口 (`GET/PUT /system/config`)
- **TODO**: 实现系统监控接口 (`GET /system/metrics`)
- **TODO**: 实现审计日志接口 (`GET /system/audit-logs`)
- **TODO**: 实现数据备份和恢复接口 (`POST /system/backup`, `POST /system/restore`)

##### **3.6 性能优化功能**
- **TODO**: 实现缓存管理接口 (`GET/DELETE /cache`)
- **TODO**: 实现异步任务队列监控接口 (`GET /queues/status`)
- **TODO**: 实现数据库连接池监控接口 (`GET /datasources/pool-status`)
- **TODO**: 实现系统性能分析接口 (`GET /system/performance`)

##### **3.7 集成和扩展功能**
- **TODO**: 实现Webhook通知接口 (`POST /webhooks`)
- **TODO**: 实现第三方系统集成接口 (`POST /integrations`)
- **TODO**: 实现插件管理接口 (`GET/POST/PUT/DELETE /plugins`)
- **TODO**: 实现API限流和配额管理接口 (`GET/PUT /api/limits`)

-----

#### **4. 实现状态总结**

**已实现接口数量**: 25个
**待实现接口数量**: 30+个
**实现完成度**: 约45%

**核心功能实现状态**:
- ✅ 数据源基础CRUD操作
- ✅ 文件上传和基础管理
- ✅ 剖析任务创建和状态查询
- ✅ 报告生成和基础查询
- ✅ 健康检查和基础统计
- ❌ 高级查询和过滤功能
- ❌ 批量操作和事务处理
- ❌ 用户权限和安全控制
- ❌ 系统监控和性能优化
- ❌ 第三方集成和扩展功能