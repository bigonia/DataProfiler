<template>
  <div class="report-detail-viewer">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" type="text" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          Back
        </el-button>
        <div class="header-info">
          <h1 class="page-title">Detailed Report</h1>
          <div class="header-meta">
            <el-tag v-if="taskId" type="info" size="small">
              <el-icon><Operation /></el-icon>
              Task: {{ taskId }}
            </el-tag>
            <span class="meta-text">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(new Date()) }}
            </span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <el-dropdown @command="handleExport">
          <el-button type="primary">
            <el-icon><Download /></el-icon>
            Export
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="pdf">
                <el-icon><Document /></el-icon>
                Export as PDF
              </el-dropdown-item>
              <el-dropdown-item command="excel">
                <el-icon><Grid /></el-icon>
                Export as Excel
              </el-dropdown-item>
              <el-dropdown-item command="json">
                <el-icon><DocumentCopy /></el-icon>
                Export as JSON
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- Report Content -->
    <div v-else-if="reports && reports.length > 0" class="report-content">
      <!-- Data Source Tabs -->
      <el-tabs v-model="activeDataSource" @tab-change="handleDataSourceChange">
        <el-tab-pane 
          v-for="report in reports" 
          :key="report.dataSourceId"
          :label="report.dataSourceId"
          :name="report.dataSourceId"
        >
          <div class="data-source-content">
            <!-- Data Source Info -->
            <div class="data-source-info">
              <h3>{{ report.database?.name || report.dataSourceId }}</h3>
              <p class="data-source-type">Type: {{ report.dataSourceType }}</p>
              <p class="generated-at">Generated: {{ formatDate(report.generatedAt) }}</p>
            </div>

            <!-- Tables List -->
            <div class="tables-section">
              <div class="section-header">
                <h4>Tables ({{ report.tables?.length || 0 }})</h4>
                <el-input
                  v-model="tableSearch"
                  placeholder="Search tables..."
                  class="search-input"
                  clearable
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
              </div>

              <div class="tables-grid">
                <div 
                  v-for="table in filteredTables" 
                  :key="`${table.schemaName}.${table.name}`"
                  class="table-card"
                  @click="selectTable(table)"
                >
                  <div class="table-header">
                    <h5>{{ table.name }}</h5>
                    <el-tag size="small">{{ table.schemaName }}</el-tag>
                  </div>
                  <div class="table-stats">
                    <div class="stat">
                      <span class="stat-label">Rows:</span>
                      <span class="stat-value">{{ formatNumber(table.rowCount) }}</span>
                    </div>
                    <div class="stat">
                      <span class="stat-label">Columns:</span>
                      <span class="stat-value">{{ table.columns?.length || 0 }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <el-empty description="No detailed reports found" />
    </div>

    <!-- Table Detail Dialog -->
    <el-dialog 
      v-model="tableDialogVisible" 
      :title="selectedTable?.name || 'Table Details'"
      width="80%"
      class="table-dialog"
    >
      <div v-if="selectedTable" class="table-detail">
        <el-tabs v-model="tableDialogTab">
          <el-tab-pane label="Columns" name="columns">
            <div class="columns-table">
              <el-table :data="selectedTable.columns" stripe>
                <el-table-column prop="name" label="Column Name" width="200" />
                <el-table-column prop="type" label="Type" width="120" />
                <el-table-column label="Primary Key" width="100">
                  <template #default="{ row }">
                    <el-icon v-if="row.isPrimaryKey" color="#67c23a"><Check /></el-icon>
                  </template>
                </el-table-column>
                <el-table-column label="Null Rate" width="100">
                  <template #default="{ row }">
                    {{ (row.metrics?.nullRate * 100).toFixed(1) }}%
                  </template>
                </el-table-column>
                <el-table-column label="Distinct Rate" width="120">
                  <template #default="{ row }">
                    {{ (row.metrics?.distinctRate * 100).toFixed(1) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="comment" label="Comment" show-overflow-tooltip />
              </el-table>
            </div>
          </el-tab-pane>
          <el-tab-pane label="Sample Data" name="sample">
            <div class="sample-data">
              <el-table :data="selectedTable.sampleRows?.rows || []" stripe>
                <el-table-column 
                  v-for="(header, index) in selectedTable.sampleRows?.headers || []"
                  :key="index"
                  :prop="index.toString()"
                  :label="header"
                  show-overflow-tooltip
                >
                  <template #default="{ row }">
                    {{ row[index] }}
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useReportStore } from '@/stores/report'
import { formatDate, formatNumber } from '@/utils'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Download,
  ArrowDown,
  Calendar,
  Document,
  Grid,
  DocumentCopy,
  Search,
  Operation,
  Check
} from '@element-plus/icons-vue'
import type { StructuredReportDto } from '@/types'

const route = useRoute()
const router = useRouter()
const reportStore = useReportStore()

const loading = ref(false)
const reports = ref<StructuredReportDto[]>([])
const activeDataSource = ref('')
const tableSearch = ref('')
const tableDialogVisible = ref(false)
const selectedTable = ref(null)
const tableDialogTab = ref('columns')
const taskId = ref('')

// Computed properties
const currentReport = computed(() => {
  return reports.value.find(r => r.dataSourceId === activeDataSource.value)
})

const filteredTables = computed(() => {
  if (!currentReport.value?.tables) return []
  
  let tables = currentReport.value.tables
  
  // Filter by search
  if (tableSearch.value) {
    tables = tables.filter(table => 
      table.name.toLowerCase().includes(tableSearch.value.toLowerCase()) ||
      table.schemaName.toLowerCase().includes(tableSearch.value.toLowerCase())
    )
  }
  
  return tables
})

// Methods
const loadDetailedReports = async () => {
  const taskIdParam = route.query.taskId as string
  if (!taskIdParam) {
    ElMessage.error('Task ID is required')
    return
  }

  taskId.value = taskIdParam
  loading.value = true
  
  try {
    // Get detailed reports by taskId
    const response = await reportStore.getDetailedByTaskId({
      taskId: taskIdParam,
      page: 0,
      pageSize: 100,
      format: 'standard'
    })
    
    if (response && response.content && response.content.length > 0) {
      reports.value = response.content
      // Set first data source as active
      if (reports.value.length > 0) {
        activeDataSource.value = reports.value[0].dataSourceId
      }
    } else {
      ElMessage.warning('No detailed reports found for this task')
    }
  } catch (error) {
    console.error('Failed to load detailed reports:', error)
    ElMessage.error('Failed to load detailed reports')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.go(-1)
}

const handleExport = async (format: string) => {
  try {
    // TODO: Implement export functionality
    ElMessage.success(`Export as ${format.toUpperCase()} - Coming soon!`)
  } catch (error) {
    console.error('Export failed:', error)
    ElMessage.error('Failed to export report')
  }
}

const handleDataSourceChange = (dataSourceId: string) => {
  activeDataSource.value = dataSourceId
  tableSearch.value = ''
}

const selectTable = (table: any) => {
  selectedTable.value = table
  tableDialogVisible.value = true
}

onMounted(async () => {
  try {
    await loadDetailedReports()
  } catch (error) {
    console.error('Failed to initialize detailed report viewer:', error)
    ElMessage.error('Failed to initialize detailed report viewer')
  }
})
</script>

<style scoped>
.report-detail-viewer {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.back-btn {
  padding: 8px;
  margin-top: 4px;
}

.header-info {
  flex: 1;
}

.page-title {
  margin: 0 0 12px 0;
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-text {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.loading-container {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.report-content {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.data-source-content {
  padding: 24px;
}

.data-source-info {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.data-source-info h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.data-source-type,
.generated-at {
  margin: 4px 0;
  color: #909399;
  font-size: 14px;
}

.tables-section {
  margin-top: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h4 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.search-input {
  width: 300px;
}

.tables-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.table-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.table-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.table-header h5 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.table-stats {
  display: flex;
  gap: 16px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.empty-state {
  background: white;
  padding: 48px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  text-align: center;
}

.table-dialog {
  .table-detail {
    max-height: 60vh;
    overflow-y: auto;
  }
}

.columns-table,
.sample-data {
  max-height: 400px;
  overflow-y: auto;
}
</style>