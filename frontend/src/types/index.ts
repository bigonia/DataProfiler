// Data Source Types
export interface DataSourceConfig {
  id?: string
  sourceId?: string
  name: string
  type: 'MYSQL' | 'POSTGRESQL' | 'SQLITE' | 'FILE'
  properties: Record<string, any>
  createdAt?: string
  updatedAt?: string
}

export interface DataSourceTestResult {
  success: boolean
  message: string
  durationMs: number
  timestamp: string
}

export interface DataSourceInfo {
  sourceId: string
  name: string
  type: string
  schemas: Record<string, string[]>
  lastUpdated: string
  fromCache: boolean
  schemaCount: number
  totalTableCount: number
}

// File Types
export interface FileMetadata {
  id: number
  filename: string
  originalFilename: string
  filePath: string
  fileSize: number
  mimeType: string
  uploadedAt: string
  description?: string
  dataSourceId?: number
}

// Profiling Task Types
export interface ProfilingTaskRequest {
  datasources: Record<string, {
    schemas: Record<string, string[]>
  }>
  options?: {
    sampleSize?: number
    timeout?: number
  }
}

export interface ProfilingTask {
  id: number  // Database auto-increment ID
  taskId: string  // UUID task identifier
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  createdAt: string
  updatedAt?: string
  completedAt?: string
  datasources?: string[]
  progress?: number
  errorMessage?: string
  name?: string
  description?: string
  totalDataSources?: number
  processedDataSources?: number
  requestPayload?: string  // JSON string containing task configuration
  info?: string  // Task completion or status information
  allDataSourcesProcessed?: boolean  // Whether all data sources have been processed
}

export interface TaskStatusResponse {
  taskId: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  info?: string
  createdAt?: string
  completedAt?: string
  totalDataSources?: number
  processedDataSources?: number
  name?: string
  description?: string
  dataSources?: DataSourceInfo[]
}

export interface DataSourceInfo {
  id: number
  sourceId: string
  name: string
  type: 'MYSQL' | 'POSTGRESQL' | 'SQLITE' | 'FILE'
  active: boolean
  createdAt: string
}

// Report Types
export interface ReportSummaryRequest {
  dataSourceIds: string[]
}

export interface ReportSummaryDto {
  dataSourceId: string
  dataSourceName: string
  dataSourceType: string
  tables: {
    name: string
    schemaName: string
    rowCount: number
    columnCount: number
    comment?: string
    columnNames: string[]
    sampleRows: {
      headers: string[]
      rows: any[][]
    }
  }[]
}

export interface DetailedReportRequest {
  datasources: Record<string, Record<string, string[]>>
  pagination?: {
    page: number
    pageSize: number
  }
}

// Task-based Report Request Types
export interface TaskSummaryRequest {
  taskId: string
  dataSourceIds?: string[]
}

export interface TaskReportRequest {
  taskId: string
  dataSourceIds?: string[]
  page: number
  pageSize: number
  format?: 'standard' | 'compact'
  summaryOnly?: boolean
}

export interface ColumnMetrics {
  nullCount: number
  nullRate: number
  distinctCount: number
  distinctRate: number
  range?: {
    min: any
    max: any
  }
  avg?: number
  stddev?: number
}

export interface ColumnInfo {
  name: string
  type: string
  isPrimaryKey: boolean
  comment?: string
  metrics: ColumnMetrics
}

export interface TableInfo {
  name: string
  schemaName: string
  rowCount: number
  columns: ColumnInfo[]
  sampleRows: {
    headers: string[]
    rows: any[][]
  }
}

export interface StructuredReportDto {
  taskId: string
  dataSourceId: string
  dataSourceType: string
  database: {
    name: string
  }
  tables: TableInfo[]
  generatedAt: string
}

export interface StructuredReport {
  id: string
  taskId: string
  dataSourceId: string
  reportData: StructuredReportDto
  createdAt: string
  size: number
}

export interface ReportInfo {
  id: string
  taskId: string
  dataSourceId: string
  dataSourceName: string
  dataSourceType: string
  generatedAt: string
  profilingStartTime?: string
  profilingEndTime?: string
  totalTables?: number
  totalColumns?: number
  estimatedTotalRows?: number
  estimatedTotalSizeBytes?: number
  // Computed properties
  analysisTimeMinutes?: number
  formattedDataSize?: string
}

// Pagination Types
export interface PageRequest {
  page: number
  size: number
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface SimplePaginationResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  numberOfElements: number
}

// API Response Types
export interface ApiResponse<T = any> {
  success: boolean
  data: T
  message?: string
  code?: number
}

// UI Types
export interface MenuItem {
  path: string
  name: string
  icon: string
  children?: MenuItem[]
}

export interface BreadcrumbItem {
  title: string
  path?: string
}

// Form Types
export interface FormRule {
  required?: boolean
  message?: string
  trigger?: string | string[]
  validator?: (rule: any, value: any, callback: any) => void
}

export type FormRules = Record<string, FormRule[]>

// AI Analysis Types
export interface AnalysisRequest {
  question: string
  taskId: string
  userId: string
}

export interface AnalysisStreamResponse {
  type: 'status' | 'content' | 'error' | 'complete' | 'progress'
  event?: string // SSE event type (status, connected, started, progress, chunk, etc.)
  content?: string
  error?: string
  timestamp?: string
  nodeData?: WorkflowNodeData // Workflow node execution data
}

// Workflow Node Data from AI analysis
export interface WorkflowNodeData {
  node_id?: string
  node_type?: string
  title?: string
  index?: number
  predecessor_node_id?: string
  inputs?: Record<string, any>
  process_data?: Record<string, any>
  outputs?: Record<string, any>
  status?: string
  error?: string
  elapsed_time?: number
  execution_metadata?: {
    total_tokens?: number
    total_price?: string
    currency?: string
  }
  created_at?: number
  finished_at?: number
}

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: string
  isStreaming?: boolean
  hasError?: boolean
  type?: 'analysis' | 'suggestion' | 'warning' | 'error' | 'streaming' | 'progress'
  workflowNodes?: WorkflowNodeData[] // Store workflow node information
}

export interface ProfilingTaskForAI {
  taskId: string
  name: string
  completedAt: string
  dataSourceId: string
  dataSourceName: string
}

export interface AIServiceHealth {
  available: boolean
  timestamp: number
  status?: string
  error?: string
}

export interface AIServiceInfo {
  service: string
  version: string
  provider: string
  features: string[]
  supported_formats: string[]
  max_context_length: number
  timeout_minutes: number
}