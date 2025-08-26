import request from './request'
import type {
  ReportSummaryRequest,
  ReportSummaryDto,
  DetailedReportRequest,
  TaskSummaryRequest,
  TaskReportRequest,
  StructuredReportDto,
  StructuredReport,
  ReportInfo,
  PageResponse,
  SimplePaginationResponse
} from '@/types'

export const reportApi = {
  // Get summary reports
  getSummary: (data: ReportSummaryRequest) => {
    return request.post<ReportSummaryDto[]>('/reports/summary', data)
  },

  // Get detailed reports
  getDetailed: (data: DetailedReportRequest) => {
    return request.post<PageResponse<StructuredReportDto>>('/reports/detailed', data)
  },

  // Get summary reports by task ID (based on ReportController)
  getSummaryByTaskId: (params: {
    taskId: string
    page?: number
    pageSize?: number
  }) => {
    return request.get<SimplePaginationResponse<ReportSummaryDto>>('/reports/summary', { params })
  },

  // Get detailed reports by task ID (based on ReportController)
  getDetailedByTaskId: (data: {
    taskId: string
    page?: number
    pageSize?: number
    format?: 'standard' | 'compact'
    dataSourceIds?: string[]
    schemas?: Record<string, string[]>
  }) => {
    return request.post<SimplePaginationResponse<StructuredReportDto>>('/reports/detailed', data)
  },

  // Get reports by task ID
  getByTaskId: (taskId: string) => {
    return request.get<StructuredReport[]>(`/reports/task/${taskId}`)
  },

  // Task-based report APIs
  getTaskSummary: (data: TaskSummaryRequest) => {
    return request.post<ReportSummaryDto[]>('/reports/task/summary', data)
  },

  getTaskDetailedReports: (data: TaskReportRequest) => {
    return request.post<PageResponse<StructuredReportDto>>('/reports/task/detailed', data)
  },

  // Get reports by data source ID
  getByDataSourceId: (dataSourceId: string) => {
    return request.get<StructuredReport[]>(`/reports/datasource/${dataSourceId}`)
  },

  // Get report by table
  getByTable: (params: {
    dataSourceId: string
    schemaName?: string
    tableName: string
  }) => {
    return request.get<StructuredReport>('/reports/table', { params })
  },

  // Get all reports with pagination
  getAll: (params?: {
    page?: number
    size?: number
  }) => {
    return request.get<PageResponse<StructuredReport>>('/reports', { params })
  },

  // Get report by ID
  getById: (reportId: string) => {
    return request.get<StructuredReport>(`/reports/${reportId}`)
  },

  // Get report info list (for All Reports page)
  getInfoList: (params?: {
    page?: number
    size?: number
  }) => {
    return request.get<PageResponse<ReportInfo>>('/reports/infolist', { params })
  },

  // Get reports by date range
  getByDateRange: (startDate: string, endDate: string) => {
    return request.get<StructuredReport[]>('/reports/range', {
      params: { startDate, endDate }
    })
  },

  // Get large reports
  getLargeReports: (sizeThreshold?: number) => {
    return request.get<StructuredReport[]>('/reports/large', {
      params: { sizeThreshold }
    })
  },

  // Get report statistics
  getStatistics: () => {
    return request.get<{
      totalReports: number
      totalSize: number
      averageSize: number
    }>('/reports/statistics')
  },

  // Export report
  export: (reportId: string, format: 'json' | 'csv' | 'excel' = 'json') => {
    return request.get<{
      downloadUrl: string
      format: string
    }>(`/reports/export/${reportId}?format=${format}`)
  },

  // Health check
  healthCheck: () => {
    return request.get<{
      status: string
      timestamp: string
    }>('/reports/health')
  }
}