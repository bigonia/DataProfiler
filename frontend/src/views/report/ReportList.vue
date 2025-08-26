<template>
  <div class="report-list">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">Profiling Reports</h2>
        <p class="page-description">View and analyze your data profiling results</p>
      </div>
      <div class="header-right">
        <el-button @click="refreshList" :loading="loading">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
      </div>
    </div>
    
    <!-- Statistics Cards -->
    <div class="stats-row">
      <div class="stat-card" style="border-left: 4px solid #409eff">
        <div class="stat-content">
          <div class="stat-value">{{ totalReports }}</div>
          <div class="stat-label">Total Reports</div>
        </div>
        <div class="stat-icon">
          <el-icon color="#409eff" size="24"><Document /></el-icon>
        </div>
      </div>
      
      <div class="stat-card" style="border-left: 4px solid #67c23a">
        <div class="stat-content">
          <div class="stat-value">{{ recentReports }}</div>
          <div class="stat-label">This Week</div>
        </div>
        <div class="stat-icon">
          <el-icon color="#67c23a" size="24"><Calendar /></el-icon>
        </div>
      </div>
      
      <div class="stat-card" style="border-left: 4px solid #e6a23c">
        <div class="stat-content">
          <div class="stat-value">{{ totalTables }}</div>
          <div class="stat-label">Tables Analyzed</div>
        </div>
        <div class="stat-icon">
          <el-icon color="#e6a23c" size="24"><Grid /></el-icon>
        </div>
      </div>
      
      <div class="stat-card" style="border-left: 4px solid #f56c6c">
        <div class="stat-content">
          <div class="stat-value">{{ qualityIssues }}</div>
          <div class="stat-label">Quality Issues</div>
        </div>
        <div class="stat-icon">
          <el-icon color="#f56c6c" size="24"><Warning /></el-icon>
        </div>
      </div>
    </div>
    
    <!-- Filters -->
    <div class="card">
      <div class="filter-section">
        <div class="filter-row">
          <div class="filter-item">
            <el-input
              v-model="searchQuery"
              placeholder="Search reports..."
              clearable
              @input="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          
          <div class="filter-item">
            <el-select
              v-model="selectedDataSource"
              placeholder="Filter by data source"
              clearable
              @change="handleDataSourceFilter"
            >
              <el-option label="All Data Sources" value="" />
              <el-option
                v-for="ds in availableDataSources"
                :key="ds.id"
                :label="ds.name"
                :value="ds.id"
              />
            </el-select>
          </div>
          
          <div class="filter-item">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="To"
              start-placeholder="Start date"
              end-placeholder="End date"
              @change="handleDateFilter"
            />
          </div>
          
          <div class="filter-item">
            <el-select
              v-model="sortBy"
              placeholder="Sort by"
              @change="handleSort"
            >
              <el-option label="Latest First" value="date-desc" />
              <el-option label="Oldest First" value="date-asc" />
              <el-option label="Name A-Z" value="name-asc" />
              <el-option label="Name Z-A" value="name-desc" />
            </el-select>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Report Cards -->
    <div class="report-grid" v-loading="loading">
      <div
        v-for="report in filteredReports"
        :key="report.id"
        class="report-card"
        @click="viewReport(report.id)"
      >
        <div class="card-header">
          <div class="report-info">
            <div class="report-title-row">
              <h3 class="report-title">{{ report.taskId }}</h3>
              <el-tag :type="getReportStatusType(report)" size="small" class="report-status">
                <el-icon><CircleCheck v-if="isReportComplete(report)" /><Clock v-else /></el-icon>
                {{ getReportStatusText(report) }}
              </el-tag>
            </div>
            <div class="datasource-info">
              <div class="datasource-icon">
                <el-icon :color="getDataSourceColor(report.dataSourceType)" size="18">
                  <component :is="getDataSourceIcon(report.dataSourceType)" />
                </el-icon>
              </div>
              <div class="datasource-details">
                <span class="datasource-name">{{ report.dataSourceName }}</span>
                <span class="datasource-type">{{ report.dataSourceType }}</span>
              </div>
            </div>
          </div>
          
          <div class="report-actions" @click.stop>
            <el-dropdown @command="handleReportAction">
              <el-button type="text" size="small">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="{ action: 'view', id: report.taskId }">
                    <el-icon><View /></el-icon>
                    View Report
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'summary', id: report.id }">
                    <el-icon><DataAnalysis /></el-icon>
                    View Summary
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'export', id: report.id }">
                    <el-icon><Download /></el-icon>
                    Export Report
                  </el-dropdown-item>
                  <el-dropdown-item
                    :command="{ action: 'delete', id: report.id }"
                    divided
                  >
                    <el-icon><Delete /></el-icon>
                    Delete Report
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
        
        <div class="card-content">
          <div class="report-stats">
            <div class="stat-item">
              <span class="stat-label">Tables:</span>
              <span class="stat-value">{{ report.totalTables || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Columns:</span>
              <span class="stat-value">{{ report.totalColumns || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Rows:</span>
              <span class="stat-value">{{ formatNumber(report.estimatedTotalRows || 0) }}</span>
            </div>
          </div>
          
          <div class="report-details">
            <div class="detail-item">
              <span class="detail-label">Generated:</span>
              <span class="detail-value">{{ formatDate(report.generatedAt) }}</span>
            </div>
            <div class="detail-item" v-if="report.formattedDataSize">
              <span class="detail-label">Data Size:</span>
              <span class="detail-value">{{ report.formattedDataSize }}</span>
            </div>
            <div class="detail-item" v-if="report.analysisTimeMinutes">
              <span class="detail-label">Analysis Time:</span>
              <span class="detail-value">{{ report.analysisTimeMinutes }} min</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Data Source:</span>
              <span class="detail-value">{{ report.dataSourceType }}</span>
            </div>
          </div>
        </div>
        
        <div class="card-footer">
          <div class="footer-info">
            <span class="created-date">
              {{ formatDate(report.generatedAt, 'datetime') }}
            </span>
            <span v-if="report.analysisTimeMinutes" class="duration">
              Analysis: {{ report.analysisTimeMinutes }} min
            </span>
          </div>
          
          <div class="footer-actions">
            <el-button type="primary" size="small" @click.stop="viewReport(report.taskId)">
              <el-icon><View /></el-icon>
              View Report
            </el-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Empty State -->
    <div v-if="!loading && filteredReports.length === 0" class="empty-state">
      <el-icon class="empty-state-icon"><Document /></el-icon>
      <div class="empty-state-text">
        <h3>No reports found</h3>
        <p>{{ searchQuery ? 'Try adjusting your search criteria' : 'No profiling reports have been generated yet' }}</p>
      </div>
      <el-button
        v-if="!searchQuery"
        type="primary"
        @click="$router.push('/tasks/create')"
      >
        <el-icon><Plus /></el-icon>
        Create Profiling Task
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Document,
  Calendar,
  Grid,
  Warning,
  Search,
  MoreFilled,
  View,
  DataAnalysis,
  Download,
  Delete,
  Plus,
  CircleCheck,
  Clock,
  Coin,
  Files,
  Connection,
  DocumentCopy,
  FolderOpened,
  Monitor
} from '@element-plus/icons-vue'
import { useReportStore } from '@/stores/report'
import { useDataSourceStore } from '@/stores/datasource'
import { formatDate, formatDuration, formatNumber, debounce } from '@/utils'
import { reportApi } from '@/api/report'
import type { ReportInfo } from '@/types'

const router = useRouter()
const route = useRoute()
const reportStore = useReportStore()
const dataSourceStore = useDataSourceStore()

const loading = ref(false)
const searchQuery = ref('')
const selectedDataSource = ref('')
const dateRange = ref<[Date, Date] | null>(null)
const sortBy = ref('date-desc')
const reportInfoList = ref<ReportInfo[]>([])

const filteredReports = computed(() => {
  // Ensure reportInfoList.value is an array before processing
  if (!reportInfoList.value || !Array.isArray(reportInfoList.value)) {
    return []
  }
  
  let reports = [...reportInfoList.value]
  
  // Filter by taskId from URL query parameter
  const taskIdFromQuery = route.query.taskId as string
  if (taskIdFromQuery) {
    reports = reports.filter(report => report.taskId === taskIdFromQuery)
  }
  
  // Search filter
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    reports = reports.filter(report => 
      report.taskId?.toLowerCase().includes(query) ||
      report.dataSourceName?.toLowerCase().includes(query)
    )
  }
  
  // Data source filter
  if (selectedDataSource.value) {
    reports = reports.filter(report => report.dataSourceId === selectedDataSource.value)
  }
  
  // Date range filter
  if (dateRange.value) {
    const [start, end] = dateRange.value
    reports = reports.filter(report => {
      const createdDate = new Date(report.generatedAt)
      return createdDate >= start && createdDate <= end
    })
  }
  
  // Sort
  reports.sort((a, b) => {
    switch (sortBy.value) {
      case 'date-desc':
        return new Date(b.generatedAt).getTime() - new Date(a.generatedAt).getTime()
      case 'date-asc':
        return new Date(a.generatedAt).getTime() - new Date(b.generatedAt).getTime()
      case 'name-asc':
        return a.taskId.localeCompare(b.taskId)
      case 'name-desc':
        return b.taskId.localeCompare(a.taskId)
      default:
        return 0
    }
  })
  
  return reports
})

const totalReports = computed(() => {
  return reportInfoList.value?.length || 0
})

const recentReports = computed(() => {
  if (!reportInfoList.value || !Array.isArray(reportInfoList.value)) {
    return 0
  }
  const oneWeekAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
  return reportInfoList.value.filter(report => 
    new Date(report.generatedAt) >= oneWeekAgo
  ).length
})

const totalTables = computed(() => {
  if (!reportInfoList.value || !Array.isArray(reportInfoList.value)) {
    return 0
  }
  return reportInfoList.value.reduce((sum, report) => sum + (report.totalTables || 0), 0)
})

const qualityIssues = computed(() => {
  // For now, return 0 as quality issues are not part of ReportInfo
  // This could be enhanced later if needed
  return 0
})

const availableDataSources = computed(() => {
  if (!reportInfoList.value || !Array.isArray(reportInfoList.value)) {
    return []
  }
  const usedDataSourceIds = [...new Set(reportInfoList.value.map(r => r.dataSourceId))]
  return dataSourceStore.dataSources?.filter(ds => usedDataSourceIds.includes(ds.sourceId)) || []
})

const getDataSourceName = (dataSourceId: string) => {
  const dataSource = dataSourceStore.dataSources.find(ds => ds.sourceId === dataSourceId)
  return dataSource?.name || `DataSource #${dataSourceId}`
}

// Get data source icon based on type
const getDataSourceIcon = (type: string) => {
  const iconMap: Record<string, string> = {
    'MYSQL': 'Coin',
    'POSTGRESQL': 'Connection', 
    'ORACLE': 'Monitor',
    'SQLSERVER': 'Connection',
    'CSV': 'DocumentCopy',
    'EXCEL': 'Files',
    'JSON': 'FolderOpened'
  }
  return iconMap[type] || 'Connection'
}

// Get data source color based on type
const getDataSourceColor = (type: string) => {
  const colorMap: Record<string, string> = {
    'MYSQL': '#f89820',
    'POSTGRESQL': '#336791',
    'ORACLE': '#f80000', 
    'SQLSERVER': '#cc2927',
    'CSV': '#67c23a',
    'EXCEL': '#217346',
    'JSON': '#409eff'
  }
  return colorMap[type] || '#909399'
}

// Check if report is complete
const isReportComplete = (report: any) => {
  return report.generatedAt && report.totalTables > 0
}

// Get report status type for el-tag
const getReportStatusType = (report: any) => {
  return isReportComplete(report) ? 'success' : 'warning'
}

// Get report status text
const getReportStatusText = (report: any) => {
  return isReportComplete(report) ? 'Completed' : 'Processing'
}

const handleSearch = debounce(() => {
  // Search is handled by computed property
}, 300)

const handleDataSourceFilter = () => {
  // Filter is handled by computed property
}

const handleDateFilter = () => {
  // Filter is handled by computed property
}

const handleSort = () => {
  // Sort is handled by computed property
}

const refreshList = async () => {
  loading.value = true
  try {
    const response = await reportApi.getInfoList()
    // Handle PageResponse structure - response is already the data due to axios interceptor
    if (response && response.content && Array.isArray(response.content)) {
      reportInfoList.value = response.content
    } else {
      reportInfoList.value = []
    }
  } catch (error) {
    console.error('Failed to fetch report info list:', error)
    ElMessage.error('Failed to load report list')
    // Set to empty array on error to prevent undefined issues
    reportInfoList.value = []
  } finally {
    loading.value = false
  }
}

const viewReport = (taskId: string) => {
  router.push(`/reports/view?taskId=${taskId}`)
}

const handleReportAction = async (command: { action: string; id: string }) => {
  const { action, id } = command
  
  switch (action) {
    case 'view':
      viewReport(id)
      break
    case 'summary':
      router.push(`/reports/${id}/summary`)
      break
    case 'export':
      await exportReport(id)
      break
    case 'delete':
      await deleteReport(id)
      break
  }
}

const exportReport = async (reportId: string) => {
  try {
    const report = reportInfoList.value.find(r => r.id === reportId)
    if (!report) return
    
    // In a real implementation, you would call the export API
    ElMessage.success('Report export started. You will be notified when it\'s ready.')
  } catch (error) {
    ElMessage.error('Failed to export report')
  }
}

const deleteReport = async (reportId: string) => {
  try {
    const report = reportInfoList.value.find(r => r.id === reportId)
    if (!report) return
    
    await ElMessageBox.confirm(
      `Are you sure you want to delete the report "${report.taskId}"? This action cannot be undone.`,
      'Confirm Delete',
      {
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    // For now, just remove from local list since we don't have delete API for ReportInfo
    if (reportInfoList.value && Array.isArray(reportInfoList.value)) {
      reportInfoList.value = reportInfoList.value.filter(r => r.id !== reportId)
    }
    ElMessage.success('Report deleted successfully')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Failed to delete report')
    }
  }
}

onMounted(async () => {
  try {
    await refreshList()
  } catch (error) {
    console.error('Failed to load report list:', error)
    ElMessage.error('Failed to load report list')
  }
  
  try {
    await dataSourceStore.fetchDataSources()
  } catch (error) {
    console.error('Failed to load data sources:', error)
    // Don't show error message for data sources as it's secondary data
  }
  
  // Handle taskId from URL query parameter
  const taskIdFromQuery = route.query.taskId as string
  if (taskIdFromQuery) {
    searchQuery.value = taskIdFromQuery
    ElMessage.info(`Showing reports for task: ${taskIdFromQuery}`)
  }
})
</script>

<style scoped>
.report-list {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.page-description {
  color: #606266;
  font-size: 14px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  color: #606266;
  font-size: 14px;
}

.filter-section {
  padding: 0;
}

.filter-row {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-item {
  min-width: 200px;
}

.filter-item:last-child {
  min-width: 150px;
}

.report-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.report-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
  border-left: 4px solid #409eff;
}

.report-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 20px 0;
}

.report-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.report-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  flex: 1;
}

.report-status {
  margin-left: 12px;
}

.datasource-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.datasource-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.datasource-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.datasource-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  line-height: 1.2;
}

.datasource-type {
  font-size: 12px;
  color: #606266;
  text-transform: uppercase;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.card-content {
  padding: 16px 20px;
}

.report-stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-item .stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-item .stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.quality-overview {
  margin-bottom: 16px;
}

.quality-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.quality-label {
  font-size: 14px;
  color: #606266;
}

.quality-score {
  font-size: 16px;
  font-weight: 600;
}

.score-excellent {
  color: #67c23a;
}

.score-good {
  color: #409eff;
}

.score-fair {
  color: #e6a23c;
}

.score-poor {
  color: #f56c6c;
}

.issues-preview {
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}

.issues-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #f56c6c;
}

.issues-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.issue-tag {
  margin: 0;
}

.more-issues {
  font-size: 12px;
  color: #909399;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px 20px;
  border-top: 1px solid #ebeef5;
  margin-top: 16px;
  padding-top: 16px;
}

.footer-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.created-date {
  color: #606266;
  font-size: 14px;
}

.duration {
  color: #909399;
  font-size: 12px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-state-icon {
  font-size: 64px;
  margin-bottom: 20px;
  color: #c0c4cc;
}

.empty-state-text h3 {
  font-size: 18px;
  color: #303133;
  margin-bottom: 8px;
}

.empty-state-text p {
  font-size: 14px;
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .stats-row {
    grid-template-columns: 1fr;
  }
  
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-item {
    min-width: auto;
  }
  
  .report-grid {
    grid-template-columns: 1fr;
  }
  
  .report-stats {
    flex-direction: column;
    gap: 8px;
  }
  
  .stat-item {
    flex-direction: row;
    justify-content: space-between;
  }
  
  .card-footer {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
}
</style>