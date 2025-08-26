### **智能数据剖析与处理平台 - API设计文档 (基于OpenAPI v3)**

#### **1. 概述**

本文档定义了"智能数据剖析与处理平台"的RESTful API。所有API遵循OpenAPI v3.0规范，基于实际后端控制器实现，为前后端分离开发提供准确的接口规范。

通过此规范，您可以：

  * **生成交互式API文档 (Swagger UI)**：方便前后端开发人员在线浏览、测试API。
  * **生成客户端与服务端代码**: 为多种语言自动生成调用API的客户端SDK或服务端代码存根，提升开发效率。

本文档基于实际控制器代码生成，确保API规范与实现保持一致。

-----

#### **2. OpenAPI 核心定义 (oas-spec.yaml)**

```yaml
openapi: 3.0.3
info:
  title: 智能数据剖析与处理平台 API
  description: |-
    用于管理数据源、发起统一的数据剖析任务（支持单源与多源）、并以分层和可压缩载荷的方式获取结构化分析报告的API。
    核心流程：
    1.  配置一个或多个数据源 (`POST /api/datasources`)。
    2.  发起剖析任务 (`POST /api/profiling/profiling-tasks`)，获取任务ID。
    3.  根据任务ID轮询任务状态 (`GET /api/profiling/task-status/{id}`)。
    4.  任务成功后，通过分层查询接口获取报告:
        -   获取摘要报告 (`GET /api/reports/summary`) 以了解概况。
        -   获取详细报告 (`POST /api/reports/detailed`) 进行深度分析。
  version: 1.0.0

servers:
  - url: http://localhost:8080/api
    description: 本地开发服务器
  - url: https://api.dataprofiler.com
    description: 生产环境服务器

tags:
  - name: Data Source Management
    description: 数据源管理相关接口
  - name: File Management
    description: 文件管理相关接口
  - name: Profiling Tasks
    description: 数据剖析任务相关接口
  - name: Report Management
    description: 分析报告获取相关接口

paths:
  # --- DataSource Endpoints ---
  /datasources:
    post:
      tags: [Data Source Management]
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
        '400':
          description: 无效输入或数据源名称已存在
        '500':
          description: 内部服务器错误
    get:
      tags: [Data Source Management]
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

  /datasources/{sourceId}:
    get:
      tags: [Data Source Management]
      summary: 根据sourceId获取数据源
      parameters:
        - name: sourceId
          in: path
          required: true
          schema:
            type: string
          description: 数据源唯一标识符
      responses:
        '200':
          description: 成功返回数据源
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DataSourceConfig'
        '404':
          description: 数据源未找到
    put:
      tags: [Data Source Management]
      summary: 更新数据源配置
      parameters:
        - name: sourceId
          in: path
          required: true
          schema:
            type: string
          description: 数据源唯一标识符
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
        '400':
          description: 无效输入或名称冲突
        '404':
          description: 数据源未找到

  /datasources/{id}:
    delete:
      tags: [Data Source Management]
      summary: 删除数据源（软删除）
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
          description: 数据源数据库ID
      responses:
        '204':
          description: 数据源删除成功
        '404':
          description: 数据源未找到

  /datasources/{sourceId}/test:
    post:
      tags: [Data Source Management]
      summary: 测试数据源连接
      parameters:
        - name: sourceId
          in: path
          required: true
          schema:
            type: string
          description: 数据源唯一标识符
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
                  durationMs:
                    type: integer
                    format: int64
                  timestamp:
                    type: string
                    format: date-time
        '404':
          description: 数据源未找到

  /datasources/test:
    post:
      tags: [Data Source Management]
      summary: 测试数据源连接（使用配置）
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
                  durationMs:
                    type: integer
                    format: int64
                  timestamp:
                    type: string
                    format: date-time

  /datasources/type/{type}:
    get:
      tags: [Data Source Management]
      summary: 根据类型获取数据源
      parameters:
        - name: type
          in: path
          required: true
          schema:
            type: string
            enum: [MYSQL, POSTGRESQL, SQLITE, FILE]
          description: 数据源类型
      responses:
        '200':
          description: 成功返回指定类型的数据源
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/DataSourceConfig'

  /datasources/{sourceId}/schemas:
    get:
      tags: [Data Source Management]
      summary: 获取数据源的所有模式
      description: 检索指定数据源的所有模式（数据库/命名空间）
      parameters:
        - name: sourceId
          in: path
          required: true
          schema:
            type: string
          description: 数据源唯一标识符
      responses:
        '200':
          description: 模式检索成功
          content:
            application/json:
              schema:
                type: array
                items:
                  type: string
        '404':
          description: 数据源未找到

  /datasources/{sourceId}/tables:
    get:
      tags: [Data Source Management]
      summary: 获取数据源和模式的所有表
      description: 检索指定数据源和模式的所有表
      parameters:
        - name: sourceId
          in: path
          required: true
          schema:
            type: string
          description: 数据源唯一标识符
        - name: schema
          in: query
          required: true
          schema:
            type: string
          description: 模式名称
      responses:
        '200':
          description: 表检索成功
          content:
            application/json:
              schema:
                type: array
                items:
                  type: string
        '404':
          description: 数据源未找到

  /datasources/{sourceId}/info:
    get:
      tags: [Data Source Management]
      summary: 获取完整的数据源信息
      description: 检索完整的数据源信息，包括所有模式及其表
      parameters:
        - name: sourceId
          in: path
          required: true
          schema:
            type: string
          description: 数据源唯一标识符
      responses:
        '200':
          description: 数据源信息检索成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DataSourceInfoDto'
        '404':
          description: 数据源未找到
        '500':
          description: 内部服务器错误

  /datasources/{sourceId}/refresh-cache:
    post:
      tags: [Data Source Management]
      summary: 刷新数据源缓存
      description: 刷新缓存的数据源信息，包括模式和表
      parameters:
        - name: sourceId
          in: path
          required: true
          schema:
            type: string
          description: 数据源唯一标识符
      responses:
        '200':
          description: 缓存刷新成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  success:
                    type: boolean
                  message:
                    type: string
                  timestamp:
                    type: string
                    format: date-time
        '404':
          description: 数据源未找到
  # --- File Endpoints ---
  /files/upload:
    post:
      tags: [File Management]
      summary: 上传文件并创建文件数据源
      description: 上传一个文件（CSV, Excel等），并自动创建一个基于文件的数据源。
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
                  description: 要上传的文件
      responses:
        '201':
          description: 文件上传成功，数据源创建成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DataSourceConfig'
        '400':
          description: 无效文件或上传失败
          content:
            application/json:
              schema:
                type: string
        '500':
          description: 内部服务器错误
          content:
            application/json:
              schema:
                type: string

  /files/search:
    get:
      tags: [File Management]
      summary: 搜索文件
      description: 根据文件名搜索文件
      parameters:
        - name: searchTerm
          in: query
          required: true
          schema:
            type: string
          description: 搜索关键词
          example: report
      responses:
        '200':
          description: 搜索完成
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/FileMetadata'
        '500':
          description: 内部服务器错误
          content:
            application/json:
              schema:
                type: string

  /files/{id}:
    get:
      tags: [File Management]
      summary: 根据ID获取文件
      description: 根据ID获取文件元数据
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
          description: 文件ID
          example: 1
      responses:
        '200':
          description: 文件获取成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FileMetadata'
        '404':
          description: 文件未找到
    delete:
      tags: [File Management]
      summary: 删除文件
      description: 根据ID删除文件及其元数据
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
          description: 文件ID
          example: 1
      responses:
        '204':
          description: 文件删除成功
        '404':
          description: 文件未找到
        '500':
          description: 内部服务器错误

  /files/statistics:
    get:
      tags: [File Management]
      summary: 获取文件统计信息
      description: 获取文件上传统计信息
      responses:
        '200':
          description: 统计信息获取成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  totalFiles:
                    type: integer
                  totalSize:
                    type: integer
                    format: int64
                  averageSize:
                    type: number
        '500':
          description: 内部服务器错误

  /files/list:
    get:
      tags: [File Management]
      summary: 获取文件列表
      description: 获取分页的已上传文件列表
      parameters:
        - name: page
          in: query
          required: false
          schema:
            type: integer
            default: 0
          description: 页码（从0开始）
          example: 0
        - name: size
          in: query
          required: false
          schema:
            type: integer
            default: 10
          description: 页面大小
          example: 10
      responses:
        '200':
          description: 文件列表获取成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/FileMetadata'
                  totalElements:
                    type: integer
                  totalPages:
                    type: integer
                  size:
                    type: integer
                  number:
                    type: integer
        '500':
          description: 内部服务器错误
          content:
            application/json:
              schema:
                type: string
  # --- Profiling Endpoints ---
  /profiling/profiling-tasks:
    post:
      tags: [Profiling Tasks]
      summary: 启动一个新的数据剖析任务
      description: 创建并启动一个或多个数据源的数据剖析任务。任务异步运行。
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProfilingTaskRequest'
      responses:
        '201':
          description: 剖析任务创建成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProfilingTask'
        '400':
          description: 无效的请求参数
          content:
            application/json:
              schema:
                type: string
        '500':
          description: 内部服务器错误
          content:
            application/json:
              schema:
                type: string

  /profiling/profiling-tasks/{taskId}:
    get:
      tags: [Profiling Tasks]
      summary: 获取剖析任务详情
      description: 根据任务ID获取剖析任务的详细信息
      parameters:
        - name: taskId
          in: path
          required: true
          schema:
            type: string
          description: 剖析任务的唯一标识符
          example: task-123
      responses:
        '200':
          description: 成功获取任务详情
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProfilingTask'
        '404':
          description: 任务未找到
          content:
            application/json:
              schema:
                type: string
        '500':
          description: 内部服务器错误
          content:
            application/json:
              schema:
                type: string
    delete:
      tags: [Profiling Tasks]
      summary: 删除分析任务
      description: 根据任务ID删除指定的分析任务
      parameters:
        - name: taskId
          in: path
          required: true
          schema:
            type: string
            description: 任务唯一标识
      responses:
        '204':
          description: 任务删除成功
        '404':
          description: 任务未找到
          content:
            application/json:
              schema:
                type: string
        '400':
          description: 无效的任务ID
          content:
            application/json:
              schema:
                type: string

  /profiling/profiling-tasks/list:
    get:
      tags: [Profiling Tasks]
      summary: 获取剖析任务列表
      description: 获取所有剖析任务的列表，支持分页和过滤
      parameters:
        - name: page
          in: query
          required: false
          schema:
            type: integer
            default: 0
          description: 页码（从0开始）
          example: 0
        - name: size
          in: query
          required: false
          schema:
            type: integer
            default: 10
          description: 每页任务数量
          example: 10
        - name: status
          in: query
          required: false
          schema:
            type: string
            enum: [PENDING, RUNNING, SUCCESS, FAILED]
          description: 根据任务状态过滤
          example: SUCCESS
      responses:
        '200':
          description: 成功获取任务列表
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/ProfilingTask'
                  totalElements:
                    type: integer
                  totalPages:
                    type: integer
                  size:
                    type: integer
                  number:
                    type: integer
        '500':
          description: 内部服务器错误
          content:
            application/json:
              schema:
                type: string

  /profiling/task-status/{taskId}:
    get:
      tags: [Profiling Tasks]
      summary: 获取剖析任务状态
      description: 检索剖析任务的当前状态和进度信息
      parameters:
        - name: taskId
          in: path
          required: true
          schema:
            type: string
            description: 任务唯一标识
      responses:
        '200':
          description: 任务状态获取成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TaskStatusResponse'
        '404':
          description: 任务未找到
          content:
            application/json:
              schema:
                type: string
        '400':
          description: 无效的任务ID
          content:
            application/json:
              schema:
                type: string
  # --- Report Endpoints ---
  /reports/summary:
    get:
      tags: [Report Management]
      summary: 获取报告摘要列表
      description: 获取轻量级的报告摘要信息，支持基于任务的分页。
      parameters:
        - name: page
          in: query
          required: false
          schema:
            type: integer
            default: 0
          description: 页码（从0开始）
          example: 0
        - name: size
          in: query
          required: false
          schema:
            type: integer
            default: 10
          description: 每页报告数量
          example: 10
        - name: taskId
          in: query
          required: false
          schema:
            type: string
          description: 根据任务ID过滤报告
          example: task-123
      responses:
        '200':
          description: 成功获取报告摘要列表
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/ReportSummary'
                  totalElements:
                    type: integer
                  totalPages:
                    type: integer
                  size:
                    type: integer
                  number:
                    type: integer
        '500':
          description: 内部服务器错误
          content:
            application/json:
              schema:
                type: string

  /reports/detailed:
    post:
      tags: [Report Management]
      summary: 获取详细报告
      description: 根据过滤条件获取详细的剖析报告内容，支持高级过滤和分页。
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                taskIds:
                  type: array
                  items:
                    type: string
                  description: 任务ID列表
                  example: ["task-123", "task-456"]
                dataSourceIds:
                  type: array
                  items:
                    type: integer
                  description: 数据源ID列表
                  example: [1, 2, 3]
                startDate:
                  type: string
                  format: date-time
                  description: 开始日期
                  example: "2024-01-01T00:00:00Z"
                endDate:
                  type: string
                  format: date-time
                  description: 结束日期
                  example: "2024-12-31T23:59:59Z"
                page:
                  type: integer
                  default: 0
                  description: 页码（从0开始）
                  example: 0
                size:
                  type: integer
                  default: 10
                  description: 每页报告数量
                  example: 10
      responses:
        '200':
          description: 成功获取详细报告
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/ProfilingReport'
                  totalElements:
                    type: integer
                  totalPages:
                    type: integer
                  size:
                    type: integer
                  number:
                    type: integer
        '400':
          description: 无效的请求参数
          content:
            application/json:
              schema:
                type: string
        '500':
          description: 内部服务器错误
          content:
            application/json:
              schema:
                type: string

  /reports/infolist:
    get:
      tags: [Report Management]
      summary: 获取报告基本信息列表
      description: 获取所有报告的基本信息，包括任务元数据和统计信息。
      responses:
        '200':
          description: 成功获取报告信息列表
          content:
            application/json:
              schema:
                type: array
                items:
                  type: object
                  properties:
                    taskId:
                      type: string
                      description: 任务ID
                    taskName:
                      type: string
                      description: 任务名称
                    dataSourceName:
                      type: string
                      description: 数据源名称
                    createdAt:
                      type: string
                      format: date-time
                      description: 创建时间
                    status:
                      type: string
                      description: 任务状态
                    recordCount:
                      type: integer
                      description: 记录数量
                    columnCount:
                      type: integer
                      description: 列数量
        '500':
          description: 内部服务器错误
          content:
            application/json:
              schema:
                type: string

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
        id: { type: integer, format: int64, readOnly: true }
        name: { type: string, example: "季度销售报告.xlsx" }
        type: { type: string, enum: [MYSQL, POSTGRESQL, SQLITE, FILE] }
        host: { type: string, nullable: true }
        port: { type: integer, nullable: true }
        username: { type: string, nullable: true }
        password: { type: string, nullable: true }
        database: { type: string, nullable: true }
        schema: { type: string, nullable: true }
        filePath: { type: string, nullable: true }
        createdAt: { type: string, format: date-time }
        updatedAt: { type: string, format: date-time }
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
        name: { type: string, nullable: true }
        description: { type: string, nullable: true }
        status: { type: string, enum: [PENDING, RUNNING, SUCCESS, FAILED] }
        createdAt: { type: string, format: date-time }
        updatedAt: { type: string, format: date-time }
        datasources:
          type: object
          description: "Key为dataSourceId, Value为Schema和表的映射。空数组表示剖析该Schema下所有表。"
        progress: { type: number }
        errorMessage: { type: string, nullable: true }

    TaskStatusResponse:
      type: object
      properties:
        taskId: { type: string }
        name: { type: string, nullable: true }
        description: { type: string, nullable: true }
        status: { type: string, enum: [PENDING, RUNNING, SUCCESS, FAILED] }
        info: { type: string, nullable: true }
        createdAt: { type: string, format: date-time }
        completedAt: { type: string, format: date-time, nullable: true }
        totalDataSources: { type: integer }
        processedDataSources: { type: integer }

    ReportSummary:
      type: object
      properties:
        taskId: { type: string }
        taskName: { type: string }
        dataSourceName: { type: string }
        createdAt: { type: string, format: date-time }
        status: { type: string }
        recordCount: { type: integer }
        columnCount: { type: integer }

    ProfilingReport:
      type: object
      properties:
        taskId: { type: string }
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

    FileMetadata:
      type: object
      properties:
        id: { type: integer, format: int64 }
        filename: { type: string }
        originalFilename: { type: string }
        size: { type: integer, format: int64 }
        uploadTime: { type: string, format: date-time }
        contentType: { type: string }
        filePath: { type: string }
        dataSourceId: { type: integer, format: int64 }

    TaskStatus:
      type: object
      properties:
        taskId: { type: string }
        status: { type: string, enum: [PENDING, RUNNING, COMPLETED, FAILED] }
        progress: { type: number, minimum: 0, maximum: 100 }
        message: { type: string }
        startTime: { type: string, format: date-time }
        endTime: { type: string, format: date-time, nullable: true }
        errorDetails: { type: string, nullable: true }

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
- ✅ 已实现根据类型获取数据源接口 (`GET /datasources/type/{type}`)
- ✅ 已实现数据源搜索接口 (`GET /datasources/search`)

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
- ✅ 已实现删除分析任务接口 (`DELETE /profiling/profiling-tasks/{taskId}`)

##### **3.4 报告管理扩展功能**
- **TODO**: 实现报告比较接口 (`POST /reports/compare`)
- **TODO**: 实现报告模板管理接口 (`GET/POST/PUT/DELETE /reports/templates`)
- **TODO**: 实现报告订阅接口 (`POST /reports/subscriptions`)
- **TODO**: 实现报告分享接口 (`POST /reports/{reportId}/share`)
- **TODO**: 实现报告版本管理接口 (`GET /reports/{reportId}/versions`)
- ✅ 已实现根据任务ID获取报告摘要接口 (`GET /api/reports/{taskId}/summary`)
- ✅ 已实现根据任务ID获取详细报告接口 (`POST /api/reports/{taskId}/detailed`)

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

**已实现接口数量**: 28个
**待实现接口数量**: 27+个
**实现完成度**: 约51%

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