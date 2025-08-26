<template>
  <div class="profiling-reports">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">Profiling Reports</h2>
        <p class="page-description">Query and analyze profiling reports by task ID</p>
      </div>
      <div class="header-right">
        <el-button @click="refreshReports" :loading="loading">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
      </div>
    </div>

    <!-- Query Form -->
    <div class="card query-form">
      <div class="form-header">
        <h3>Query Parameters</h3>
      </div>
      <el-form :model="queryForm" label-width="120px" class="query-form-content">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Task ID" required>
              <el-input
                v-model="queryForm.taskId"
                placeholder="Enter task ID"
                clearable
                @keyup.enter="handleQuery"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Report Type">
              <el-radio-group v-model="queryForm.reportType">
                <el-radio-button label="summary">Summary</el-radio-button>
                <el-radio-button label="detailed">Detailed</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        
        <!-- Detailed Report Filters -->
        <div v-if="queryForm.reportType === 'detailed'" class="detailed-filters">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="Format">
                <el-select v-model="queryForm.format" placeholder="Select format">
                  <el-option label="Standard" value="standard" />
                  <el-option label="Compact" value="compact" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="Page Size">
                <el-select v-model="queryForm.pageSize" placeholder="Select page size">
                  <el-option label="10" :value="10" />
                  <el-option label="20" :value="20" />
                  <el-option label="50" :value="50" />
                  <el-option label="100" :value="100" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item>
                <el-button type="primary" @click="handleQuery" :loading="loading">
                  <el-icon><Search /></el-icon>
                  Query Reports
                </el-button>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
        
        <!-- Summary Report Filters -->
        <div v-else class="summary-filters">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="Page Size">
                <el-select v-model="queryForm.pageSize" placeholder="Select page size">
                  <el-option label="10" :value="10" />
                  <el-option label="20" :value="20" />
                  <el-option label="50" :value="50" />
                  <el-option label="100" :value="100" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item>
                <el-button type="primary" @click="handleQuery" :loading="loading">
                  <el-icon><Search /></el-icon>
                  Query Reports
                </el-button>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>
    </div>

    <!-- Results Section -->
    <div v-if="hasResults" class="results-section">
      <!-- Summary Reports -->
      <div v-if="queryForm.reportType === 'summary' && summaryReports" class="summary-results">
        <div class="section-header">
          <h3>Summary Reports</h3>
          <div class="pagination-info">
            Showing {{ summaryReports.numberOfElements }} of {{ summaryReports.totalElements }} results
          </div>
        </div>
        
        <div class="summary-cards">
          <div
            v-for="(report, index) in summaryReports.content"
            :key="index"
            class="summary-card"
          >
            <div class="card-header">
              <div class="data-source-info">
                <h4>{{ report.dataSourceName }}</h4>
                <span class="data-source-type">{{ report.dataSourceType }}</span>
              </div>
              <div class="card-actions">
                <el-button type="success" size="small" @click="viewSummaryReport(queryForm.taskId)">
                  <el-icon><View /></el-icon>
                  View Summary
                </el-button>
                <el-button type="primary" size="small" @click="viewDetailedReport(queryForm.taskId)">
                  <el-icon><DataBoard /></el-icon>
                  View Reports
                </el-button>
              </div>
            </div>
            
            <div class="tables-section">
              <h5>Tables ({{ report.tables.length }})</h5>
              <div class="tables-grid">
                <div
                  v-for="table in report.tables"
                  :key="table.name"
                  class="table-item"
                  @click="showTableDetails(table)"
                >
                  <div class="table-header">
                    <span class="table-name">{{ table.name }}</span>
                    <span class="schema-name">{{ table.schemaName }}</span>
                  </div>
                  <div class="table-stats">
                    <span class="stat">{{ formatNumber(table.rowCount) }} rows</span>
                    <span class="stat">{{ table.columnCount }} columns</span>
                  </div>
                  <div v-if="table.comment" class="table-comment">
                    {{ table.comment }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Summary Pagination -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="queryForm.pageSize"
            :total="summaryReports.totalElements"
            layout="prev, pager, next, jumper, total"
            @current-change="handlePageChange"
          />
        </div>
      </div>

      <!-- Detailed Reports -->
      <div v-if="queryForm.reportType === 'detailed' && detailedReports" class="detailed-results">
        <div class="section-header">
          <h3>Detailed Reports</h3>
          <div class="pagination-info">
            Showing {{ detailedReports.numberOfElements }} of {{ detailedReports.totalElements }} results
          </div>
        </div>
        
        <div class="detailed-cards">
          <div
            v-for="(report, index) in detailedReports.content"
            :key="index"
            class="detailed-card"
          >
            <div class="card-header">
              <div class="report-info">
                <h4>{{ report.database.name }}</h4>
                <div class="report-meta">
                  <span class="data-source-type">{{ report.dataSourceType }}</span>
                  <span class="generated-time">{{ formatDate(report.generatedAt) }}</span>
                </div>
              </div>
              <div class="card-actions">
                <el-button type="success" size="small" @click="viewSummaryReport(queryForm.taskId)">
                  <el-icon><View /></el-icon>
                  View Summary
                </el-button>
                <el-button type="primary" size="small" @click="viewDetailedReport(queryForm.taskId)">
                  <el-icon><DataBoard /></el-icon>
                  View Reports
                </el-button>
              </div>
            </div>
            
            <div class="tables-section">
              <h5>Tables Analysis ({{ report.tables.length }})</h5>
              <div class="tables-list">
                <div
                  v-for="table in report.tables"
                  :key="table.name"
                  class="table-analysis"
                >
                  <div class="table-header">
                    <span class="table-name">{{ table.name }}</span>
                    <span class="schema-name">{{ table.schemaName }}</span>
                    <span class="row-count">{{ formatNumber(table.rowCount) }} rows</span>
                  </div>
                  
                  <div class="columns-preview">
                    <h6>Columns ({{ table.columns.length }})</h6>
                    <div class="columns-grid">
                      <div
                        v-for="column in table.columns.slice(0, 6)"
                        :key="column.name"
                        class="column-item"
                      >
                        <span class="column-name">{{ column.name }}</span>
                        <span class="column-type">{{ column.type }}</span>
                        <span v-if="column.isPrimaryKey" class="primary-key">PK</span>
                      </div>
                      <div v-if="table.columns.length > 6" class="more-columns">
                        +{{ table.columns.length - 6 }} more
                      </div>
                    </div>
                  </div>
                  
                  <div v-if="table.sampleRows && table.sampleRows.rows.length > 0" class="sample-data">
                    <h6>Sample Data</h6>
                    <el-table
                      :data="table.sampleRows.rows.slice(0, 3)"
                      size="small"
                      class="sample-table"
                    >
                      <el-table-column
                        v-for="(header, headerIndex) in table.sampleRows.headers"
                        :key="headerIndex"
                        :prop="headerIndex.toString()"
                        :label="header"
                        min-width="100"
                        show-overflow-tooltip
                      >
                        <template #default="{ row }">
                          {{ row[headerIndex] }}
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Detailed Pagination -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="queryForm.pageSize"
            :total="detailedReports.totalElements"
            layout="prev, pager, next, jumper, total"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && !hasResults && queryExecuted" class="empty-state">
      <el-icon class="empty-state-icon"><Document /></el-icon>
      <div class="empty-state-text">
        <h3>No reports found</h3>
        <p>No reports found for the specified task ID. Please check the task ID and try again.</p>
      </div>
    </div>

    <!-- Table Details Dialog -->
    <el-dialog
      v-model="tableDetailsVisible"
      :title="selectedTable?.name || 'Table Details'"
      width="80%"
      class="table-details-dialog"
    >
      <div v-if="selectedTable" class="table-details">
        <div class="table-info">
          <div class="info-row">
            <span class="label">Schema:</span>
            <span class="value">{{ selectedTable.schemaName }}</span>
          </div>
          <div class="info-row">
            <span class="label">Rows:</span>
            <span class="value">{{ formatNumber(selectedTable.rowCount) }}</span>
          </div>
          <div class="info-row">
            <span class="label">Columns:</span>
            <span class="value">{{ selectedTable.columnCount }}</span>
          </div>
          <div v-if="selectedTable.comment" class="info-row">
            <span class="label">Comment:</span>
            <span class="value">{{ selectedTable.comment }}</span>
          </div>
        </div>
        
        <div class="columns-section">
          <h4>Columns</h4>
          <div class="columns-list">
            <div
              v-for="columnName in selectedTable.columnNames"
              :key="columnName"
              class="column-tag"
            >
              {{ columnName }}
            </div>
          </div>
        </div>
        
        <div v-if="selectedTable.sampleRows && selectedTable.sampleRows.rows.length > 0" class="sample-section">
          <h4>Sample Data</h4>
          <el-table
            :data="selectedTable.sampleRows.rows"
            class="sample-table"
            max-height="300"
          >
            <el-table-column
              v-for="(header, index) in selectedTable.sampleRows.headers"
              :key="index"
              :prop="index.toString()"
              :label="header"
              min-width="120"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                {{ row[index] }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  Search,
  Document,
  View,
  DataBoard
} from '@element-plus/icons-vue'
import { reportApi } from '@/api/report'
import { formatDate, formatNumber } from '@/utils'
import type {
  ReportSummaryDto,
  StructuredReportDto,
  SimplePaginationResponse
} from '@/types'

// Form data
const queryForm = ref({
  taskId: '',
  reportType: 'summary' as 'summary' | 'detailed',
  format: 'standard' as 'standard' | 'compact',
  pageSize: 20
})

// State
const loading = ref(false)
const queryExecuted = ref(false)
const currentPage = ref(1)
const summaryReports = ref<SimplePaginationResponse<ReportSummaryDto> | null>(null)
const detailedReports = ref<SimplePaginationResponse<StructuredReportDto> | null>(null)
const tableDetailsVisible = ref(false)
const selectedTable = ref<any>(null)

// Router
const router = useRouter()
const route = useRoute()

// Computed
const hasResults = computed(() => {
  return (summaryReports.value && summaryReports.value.content.length > 0) ||
         (detailedReports.value && detailedReports.value.content.length > 0)
})

// Methods
const handleQuery = async () => {
  if (!queryForm.value.taskId.trim()) {
    ElMessage.warning('Please enter a task ID')
    return
  }
  
  loading.value = true
  queryExecuted.value = true
  currentPage.value = 1
  
  try {
    if (queryForm.value.reportType === 'summary') {
      await querySummaryReports()
    } else {
      await queryDetailedReports()
    }
  } catch (error) {
    console.error('Query failed:', error)
    ElMessage.error('Failed to query reports')
  } finally {
    loading.value = false
  }
}

const querySummaryReports = async () => {
  try {
    const response = await reportApi.getSummaryByTaskId({
      taskId: queryForm.value.taskId,
      page: currentPage.value - 1,
      pageSize: queryForm.value.pageSize
    })
    summaryReports.value = response
    detailedReports.value = null
  } catch (error) {
    throw error
  }
}

const queryDetailedReports = async () => {
  try {
    const response = await reportApi.getDetailedByTaskId({
      taskId: queryForm.value.taskId,
      page: currentPage.value - 1,
      pageSize: queryForm.value.pageSize,
      format: queryForm.value.format
    })
    detailedReports.value = response
    summaryReports.value = null
  } catch (error) {
    throw error
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  handleQuery()
}

const refreshReports = () => {
  if (queryForm.value.taskId.trim()) {
    handleQuery()
  }
}

const showTableDetails = (table: any) => {
  selectedTable.value = table
  tableDetailsVisible.value = true
}

const viewSummaryReport = (taskId: string) => {
  router.push(`/reports/view?taskId=${taskId}`)
}

const viewDetailedReport = (taskId: string) => {
  router.push(`/reports/detailed?taskId=${taskId}`)
}

const viewFullReport = (reportId: string) => {
  router.push(`/reports/${reportId}/view`)
}

onMounted(() => {
  // Get taskId from URL query parameter
  const taskIdFromQuery = route.query.taskId as string
  if (taskIdFromQuery) {
    queryForm.value.taskId = taskIdFromQuery
    // Auto-execute query if taskId is provided
    handleQuery()
  }
})
</script>

<style scoped>
.profiling-reports {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  flex: 1;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.page-description {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.header-right {
  display: flex;
  gap: 12px;
}

.card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.query-form {
  padding: 24px;
}

.form-header {
  margin-bottom: 20px;
}

.form-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.query-form-content {
  margin-top: 20px;
}

.detailed-filters,
.summary-filters {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.results-section {
  margin-top: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.pagination-info {
  font-size: 14px;
  color: #909399;
}

/* Summary Reports Styles */
.summary-cards {
  display: grid;
  gap: 16px;
}

.summary-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.summary-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.summary-card .card-actions {
  flex-shrink: 0;
}

.data-source-info h4 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.data-source-type {
  display: inline-block;
  padding: 2px 8px;
  background: #f0f9ff;
  color: #0369a1;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.tables-section h5 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.tables-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.table-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.table-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.table-name {
  font-weight: 600;
  color: #303133;
}

.schema-name {
  font-size: 12px;
  color: #909399;
}

.table-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #606266;
}

.table-comment {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  font-style: italic;
}

/* Detailed Reports Styles */
.detailed-cards {
  display: grid;
  gap: 20px;
}

.detailed-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.detailed-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.detailed-card .card-actions {
  flex-shrink: 0;
}

.report-info h4 {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.report-meta {
  display: flex;
  gap: 16px;
  align-items: center;
}

.generated-time {
  font-size: 12px;
  color: #909399;
}

.table-analysis {
  margin-bottom: 20px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.table-analysis .table-header {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 12px;
}

.table-analysis .table-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.row-count {
  font-size: 12px;
  color: #606266;
}

.columns-preview h6,
.sample-data h6 {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.columns-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 8px;
}

.column-item {
  display: flex;
  flex-direction: column;
  padding: 8px;
  background: #f8f9fa;
  border-radius: 4px;
  font-size: 12px;
}

.column-name {
  font-weight: 600;
  color: #303133;
}

.column-type {
  color: #606266;
  margin-top: 2px;
}

.primary-key {
  background: #fef0e6;
  color: #e6a23c;
  padding: 1px 4px;
  border-radius: 2px;
  font-size: 10px;
  margin-top: 2px;
  align-self: flex-start;
}

.more-columns {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  background: #f0f2f5;
  border-radius: 4px;
  font-size: 12px;
  color: #909399;
}

.sample-data {
  margin-top: 16px;
}

.sample-table {
  margin-top: 8px;
}

/* Pagination */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.empty-state-icon {
  font-size: 64px;
  color: #c0c4cc;
  margin-bottom: 16px;
}

.empty-state-text h3 {
  font-size: 18px;
  color: #303133;
  margin: 0 0 8px 0;
}

.empty-state-text p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* Table Details Dialog */
.table-details {
  padding: 16px;
}

.table-info {
  margin-bottom: 24px;
}

.info-row {
  display: flex;
  margin-bottom: 8px;
}

.info-row .label {
  width: 100px;
  font-weight: 600;
  color: #303133;
}

.info-row .value {
  color: #606266;
}

.columns-section {
  margin-bottom: 24px;
}

.columns-section h4,
.sample-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.columns-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.column-tag {
  padding: 4px 8px;
  background: #f0f9ff;
  color: #0369a1;
  border-radius: 4px;
  font-size: 12px;
}

/* Responsive Design */
@media (max-width: 768px) {
  .profiling-reports {
    padding: 16px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .tables-grid {
    grid-template-columns: 1fr;
  }
  
  .columns-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  }
  
  .table-analysis .table-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>