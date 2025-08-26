import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { reportApi } from '@/api'
import type {
  ReportSummaryRequest,
  ReportSummaryDto,
  DetailedReportRequest,
  TaskSummaryRequest,
  TaskReportRequest,
  StructuredReportDto,
  StructuredReport,
  PageResponse
} from '@/types'
import { ElMessage } from 'element-plus'

export const useReportStore = defineStore('report', () => {
  // State
  const summaryReports = ref<ReportSummaryDto[]>([])
  const detailedReports = ref<PageResponse<StructuredReportDto> | null>(null)
  const allReports = ref<PageResponse<StructuredReport> | null>(null)
  const currentReport = ref<StructuredReport | null>(null)
  const reportStatistics = ref<{
    totalReports: number
    totalSize: number
    averageSize: number
  } | null>(null)
  const loading = ref(false)
  const exportLoading = ref(false)

  // Getters
  const summaryReportCount = computed(() => summaryReports.value.length)
  const detailedReportCount = computed(() => detailedReports.value?.totalElements || 0)
  const allReportCount = computed(() => allReports.value?.totalElements || 0)
  
  // Add reports as a computed property for backward compatibility
  const reports = computed(() => allReports.value?.content || [])
  
  const reportsByDataSource = computed(() => {
    const grouped: Record<string, ReportSummaryDto[]> = {}
    summaryReports.value.forEach(report => {
      if (!grouped[report.dataSourceId]) {
        grouped[report.dataSourceId] = []
      }
      grouped[report.dataSourceId].push(report)
    })
    return grouped
  })

  // Actions
  const fetchSummaryReports = async (request: ReportSummaryRequest) => {
    try {
      loading.value = true
      const response = await reportApi.getSummary(request)
      summaryReports.value = response
      return response
    } catch (error) {
      console.error('Failed to fetch summary reports:', error)
      ElMessage.error('Failed to load summary reports')
      throw error
    } finally {
      loading.value = false
    }
  }

  const fetchDetailedReports = async (request: DetailedReportRequest) => {
    try {
      loading.value = true
      const response = await reportApi.getDetailed(request)
      detailedReports.value = response
      return response
    } catch (error) {
      console.error('Failed to fetch detailed reports:', error)
      ElMessage.error('Failed to load detailed reports')
      throw error
    } finally {
      loading.value = false
    }
  }

  const fetchAllReports = async (params?: { page?: number; size?: number }) => {
    try {
      loading.value = true
      const response = await reportApi.getAll(params)
      allReports.value = response
      return response
    } catch (error) {
      console.error('Failed to fetch all reports:', error)
      ElMessage.error('Failed to load reports')
      throw error
    } finally {
      loading.value = false
    }
  }

  const fetchReportsByTaskId = async (taskId: string) => {
    try {
      loading.value = true
      const response = await reportApi.getByTaskId(taskId)
      return response
    } catch (error) {
      console.error('Failed to fetch reports by task ID:', error)
      ElMessage.error('Failed to load task reports')
      throw error
    } finally {
      loading.value = false
    }
  }

  const getSummaryByTaskId = async (params: {
    taskId: string
    page?: number
    pageSize?: number
  }) => {
    try {
      loading.value = true
      const response = await reportApi.getSummaryByTaskId(params)
      return response
    } catch (error) {
      console.error('Failed to fetch summary reports by task ID:', error)
      ElMessage.error('Failed to load task summary reports')
      throw error
    } finally {
      loading.value = false
    }
  }

  const getDetailedByTaskId = async (params: {
    taskId: string
    page?: number
    size?: number
  }) => {
    try {
      loading.value = true
      const response = await reportApi.getDetailedByTaskId(params)
      return response
    } catch (error) {
      console.error('Failed to get detailed report by task ID:', error)
      ElMessage.error('Failed to load detailed report')
      throw error
    } finally {
      loading.value = false
    }
  }

  const fetchReportsByDataSourceId = async (dataSourceId: string) => {
    try {
      loading.value = true
      const response = await reportApi.getByDataSourceId(dataSourceId)
      return response
    } catch (error) {
      console.error('Failed to fetch reports by data source ID:', error)
      ElMessage.error('Failed to load data source reports')
      throw error
    } finally {
      loading.value = false
    }
  }

  const fetchReportByTable = async (params: {
    dataSourceId: string
    schemaName?: string
    tableName: string
  }) => {
    try {
      loading.value = true
      const response = await reportApi.getByTable(params)
      currentReport.value = response
      return response
    } catch (error) {
      console.error('Failed to fetch report by table:', error)
      ElMessage.error('Failed to load table report')
      throw error
    } finally {
      loading.value = false
    }
  }

  const getReport = async (reportId: string) => {
    try {
      loading.value = true
      const response = await reportApi.getById(reportId)
      currentReport.value = response
      return response
    } catch (error) {
      console.error('Failed to fetch report:', error)
      ElMessage.error('Failed to load report')
      throw error
    } finally {
      loading.value = false
    }
  }

  const fetchReportsByDateRange = async (startDate: string, endDate: string) => {
    try {
      loading.value = true
      const response = await reportApi.getByDateRange(startDate, endDate)
      return response
    } catch (error) {
      console.error('Failed to fetch reports by date range:', error)
      ElMessage.error('Failed to load reports')
      throw error
    } finally {
      loading.value = false
    }
  }

  const fetchLargeReports = async (sizeThreshold?: number) => {
    try {
      loading.value = true
      const response = await reportApi.getLargeReports(sizeThreshold)
      return response
    } catch (error) {
      console.error('Failed to fetch large reports:', error)
      ElMessage.error('Failed to load large reports')
      throw error
    } finally {
      loading.value = false
    }
  }

  const fetchReportStatistics = async () => {
    try {
      loading.value = true
      const response = await reportApi.getStatistics()
      
      // Handle different response formats with defensive checks
      if (response && typeof response === 'object') {
        reportStatistics.value = {
          totalReports: typeof response.totalReports === 'number' ? response.totalReports : 0,
          totalSize: typeof response.totalSize === 'number' ? response.totalSize : 0,
          averageSize: typeof response.averageSize === 'number' ? response.averageSize : 0
        }
      } else {
        reportStatistics.value = {
          totalReports: 0,
          totalSize: 0,
          averageSize: 0
        }
      }
      
      return response
    } catch (error) {
      console.error('Failed to fetch report statistics:', error)
      ElMessage.error('Failed to load statistics')
      reportStatistics.value = {
        totalReports: 0,
        totalSize: 0,
        averageSize: 0
      }
      throw error
    } finally {
      loading.value = false
    }
  }

  const exportReport = async (reportId: string, format: 'json' | 'csv' | 'excel' = 'json') => {
    try {
      exportLoading.value = true
      const response = await reportApi.export(reportId, format)
      
      // Create download link
      const link = document.createElement('a')
      link.href = response.downloadUrl
      link.download = `report_${reportId}.${format}`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
      ElMessage.success('Report exported successfully')
      return response
    } catch (error) {
      console.error('Failed to export report:', error)
      ElMessage.error('Failed to export report')
      throw error
    } finally {
      exportLoading.value = false
    }
  }

  const checkReportHealth = async () => {
    try {
      const response = await reportApi.healthCheck()
      return response
    } catch (error) {
      console.error('Report service health check failed:', error)
      throw error
    }
  }

  // Task-based report methods
  const fetchTaskSummary = async (request: TaskSummaryRequest) => {
    try {
      loading.value = true
      const response = await reportApi.getTaskSummary(request)
      return response
    } catch (error) {
      console.error('Failed to fetch task summary:', error)
      ElMessage.error('Failed to load task summary')
      throw error
    } finally {
      loading.value = false
    }
  }

  const fetchTaskDetailedReports = async (request: TaskReportRequest) => {
    try {
      loading.value = true
      const response = await reportApi.getTaskDetailedReports(request)
      return response
    } catch (error) {
      console.error('Failed to fetch task detailed reports:', error)
      ElMessage.error('Failed to load task detailed reports')
      throw error
    } finally {
      loading.value = false
    }
  }

  const setCurrentReport = (report: StructuredReport | null) => {
    currentReport.value = report
  }

  const clearSummaryReports = () => {
    summaryReports.value = []
  }

  const clearDetailedReports = () => {
    detailedReports.value = null
  }

  const clearAllReports = () => {
    allReports.value = null
  }

  const clearCurrentReport = () => {
    currentReport.value = null
  }

  const clearAll = () => {
    summaryReports.value = []
    detailedReports.value = null
    allReports.value = null
    currentReport.value = null
    reportStatistics.value = null
  }

  // Add fetchReports as an alias for fetchAllReports for backward compatibility
  const fetchReports = fetchAllReports

  return {
    // State
    summaryReports,
    detailedReports,
    allReports,
    currentReport,
    reportStatistics,
    loading,
    exportLoading,
    
    // Getters
    summaryReportCount,
    detailedReportCount,
    allReportCount,
    reports, // Add the reports computed property
    reportsByDataSource,
    
    // Actions
    fetchSummaryReports,
    fetchDetailedReports,
    fetchAllReports,
    fetchReports, // Add the alias method
    fetchReportsByTaskId,
    getSummaryByTaskId,
    getDetailedByTaskId,
    fetchReportsByDataSourceId,
    fetchReportByTable,
    getReport,
    fetchReportsByDateRange,
    fetchLargeReports,
    fetchReportStatistics,
    exportReport,
    checkReportHealth,
    fetchTaskSummary,
    fetchTaskDetailedReports,
    setCurrentReport,
    clearSummaryReports,
    clearDetailedReports,
    clearAllReports,
    clearCurrentReport,
    clearAll
  }
})