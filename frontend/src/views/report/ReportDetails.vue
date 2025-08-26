<template>
  <div class="report-details">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" type="text" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          Back
        </el-button>
        <div class="header-info">
          <h1 class="page-title">{{ report?.taskName || 'Report Details' }}</h1>
          <div class="header-meta">
            <el-tag v-if="report?.dataSource" type="info" size="small">
              {{ report.dataSource }}
            </el-tag>
            <span class="meta-text">Generated {{ formatDate(report?.createdAt) }}</span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <el-button @click="exportReport" type="primary">
          <el-icon><Download /></el-icon>
          Export Report
        </el-button>
        <el-button @click="refreshReport">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- Report Content -->
    <div v-else-if="report" class="report-content">
      <!-- Overview Section -->
      <div class="section">
        <h2 class="section-title">
          <el-icon><DataAnalysis /></el-icon>
          Data Overview
        </h2>
        <div class="overview-grid">
          <div class="overview-card">
            <div class="card-icon quality">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ report.qualityScore }}%</div>
              <div class="card-label">Quality Score</div>
            </div>
          </div>
          <div class="overview-card">
            <div class="card-icon tables">
              <el-icon><Grid /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ formatNumber(report.totalTables) }}</div>
              <div class="card-label">Tables Analyzed</div>
            </div>
          </div>
          <div class="overview-card">
            <div class="card-icon columns">
              <el-icon><Menu /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ formatNumber(report.totalColumns) }}</div>
              <div class="card-label">Total Columns</div>
            </div>
          </div>
          <div class="overview-card">
            <div class="card-icon rows">
              <el-icon><Document /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ formatNumber(report.totalRows) }}</div>
              <div class="card-label">Total Rows</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tables Section -->
      <div class="section">
        <h2 class="section-title">
          <el-icon><Grid /></el-icon>
          Table Analysis
        </h2>
        <div class="table-analysis">
          <el-table :data="report.tables" stripe>
            <el-table-column prop="name" label="Table Name" min-width="150">
              <template #default="{ row }">
                <div class="table-name">
                  <el-icon><Grid /></el-icon>
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="columns" label="Columns" width="100" align="center" />
            <el-table-column prop="rows" label="Rows" width="120" align="center">
              <template #default="{ row }">
                {{ formatNumber(row.rows) }}
              </template>
            </el-table-column>
            <el-table-column prop="qualityScore" label="Quality Score" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="getQualityScoreType(row.qualityScore)" size="small">
                  {{ row.qualityScore }}%
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="issues" label="Issues" width="100" align="center">
              <template #default="{ row }">
                <el-badge :value="row.issues" :type="row.issues > 0 ? 'danger' : 'success'">
                  <el-icon><Warning /></el-icon>
                </el-badge>
              </template>
            </el-table-column>
            <el-table-column label="Actions" width="120" align="center">
              <template #default="{ row }">
                <el-button @click="viewTableDetails(row)" type="text" size="small">
                  <el-icon><View /></el-icon>
                  Details
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- Data Quality Issues -->
      <div class="section">
        <h2 class="section-title">
          <el-icon><Warning /></el-icon>
          Data Quality Issues
        </h2>
        <div class="issues-container">
          <div v-if="report.issues && report.issues.length > 0" class="issues-list">
            <div v-for="issue in report.issues" :key="issue.id" class="issue-item">
              <div class="issue-header">
                <div class="issue-severity" :class="issue.severity">
                  <el-icon><Warning /></el-icon>
                  {{ issue.severity.toUpperCase() }}
                </div>
                <div class="issue-table">{{ issue.tableName }}.{{ issue.columnName }}</div>
              </div>
              <div class="issue-content">
                <h4 class="issue-title">{{ issue.title }}</h4>
                <p class="issue-description">{{ issue.description }}</p>
                <div class="issue-stats">
                  <span class="stat-item">
                    <strong>Affected Rows:</strong> {{ formatNumber(issue.affectedRows) }}
                  </span>
                  <span class="stat-item">
                    <strong>Impact:</strong> {{ issue.impact }}%
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="no-issues">
            <el-icon size="48" color="#67c23a"><SuccessFilled /></el-icon>
            <p>No data quality issues found!</p>
          </div>
        </div>
      </div>

      <!-- Data Types Distribution -->
      <div class="section">
        <h2 class="section-title">
          <el-icon><PieChart /></el-icon>
          Data Types Distribution
        </h2>
        <div class="chart-container">
          <div class="chart-wrapper">
            <v-chart ref="dataTypesChart" :option="dataTypesChartOption" autoresize />
          </div>
          <div class="chart-legend">
            <div v-for="item in report.dataTypes" :key="item.type" class="legend-item">
              <div class="legend-color" :style="{ backgroundColor: item.color }"></div>
              <span class="legend-label">{{ item.type }}</span>
              <span class="legend-value">{{ item.count }} ({{ item.percentage }}%)</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Recommendations -->
      <div class="section">
        <h2 class="section-title">
          <el-icon><InfoFilled /></el-icon>
          Recommendations
        </h2>
        <div class="recommendations">
          <div v-for="(recommendation, index) in report.recommendations" :key="index" class="recommendation-item">
            <div class="recommendation-priority" :class="recommendation.priority">
              {{ recommendation.priority.toUpperCase() }}
            </div>
            <div class="recommendation-content">
              <h4>{{ recommendation.title }}</h4>
              <p>{{ recommendation.description }}</p>
              <div v-if="recommendation.actions" class="recommendation-actions">
                <strong>Suggested Actions:</strong>
                <ul>
                  <li v-for="action in recommendation.actions" :key="action">{{ action }}</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Error State -->
    <div v-else class="error-state">
      <el-result icon="error" title="Failed to Load Report" sub-title="Please try again later">
        <template #extra>
          <el-button @click="loadReport" type="primary">Retry</el-button>
        </template>
      </el-result>
    </div>

    <!-- Table Details Dialog -->
    <el-dialog v-model="tableDetailsVisible" title="Table Details" width="80%" top="5vh">
      <div v-if="selectedTable" class="table-details">
        <div class="table-info">
          <h3>{{ selectedTable.name }}</h3>
          <div class="table-stats">
            <div class="stat"><strong>Columns:</strong> {{ selectedTable.columns }}</div>
            <div class="stat"><strong>Rows:</strong> {{ formatNumber(selectedTable.rows) }}</div>
            <div class="stat"><strong>Quality Score:</strong> {{ selectedTable.qualityScore }}%</div>
          </div>
        </div>
        
        <el-tabs v-model="activeTab">
          <el-tab-pane label="Column Details" name="columns">
            <el-table :data="selectedTable.columnDetails" stripe>
              <el-table-column prop="name" label="Column Name" />
              <el-table-column prop="type" label="Data Type" />
              <el-table-column prop="nullCount" label="Null Count" />
              <el-table-column prop="uniqueCount" label="Unique Values" />
              <el-table-column prop="completeness" label="Completeness">
                <template #default="{ row }">
                  {{ row.completeness }}%
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="Issues" name="issues">
            <div v-if="selectedTable.tableIssues && selectedTable.tableIssues.length > 0">
              <div v-for="issue in selectedTable.tableIssues" :key="issue.id" class="table-issue">
                <div class="issue-severity" :class="issue.severity">
                  {{ issue.severity.toUpperCase() }}
                </div>
                <div class="issue-details">
                  <h4>{{ issue.title }}</h4>
                  <p>{{ issue.description }}</p>
                </div>
              </div>
            </div>
            <div v-else class="no-table-issues">
              <p>No issues found for this table.</p>
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
  Refresh,
  DataAnalysis,
  TrendCharts,
  Grid,
  Menu,
  Document,
  Warning,
  View,
  SuccessFilled,
  PieChart,
  InfoFilled
} from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart as EChartsPie } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

// Register ECharts components
use([
  CanvasRenderer,
  EChartsPie,
  TitleComponent,
  TooltipComponent,
  LegendComponent
])

const route = useRoute()
const router = useRouter()
const reportStore = useReportStore()

const loading = ref(false)
const report = ref(null)
const tableDetailsVisible = ref(false)
const selectedTable = ref(null)
const activeTab = ref('columns')

// Chart options
const dataTypesChartOption = computed(() => {
  if (!report.value?.dataTypes) return {}
  
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    series: [
      {
        name: 'Data Types',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '18',
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: report.value.dataTypes.map(item => ({
          value: item.count,
          name: item.type,
          itemStyle: {
            color: item.color
          }
        }))
      }
    ]
  }
})

const loadReport = async () => {
  const reportId = route.query.id as string
  if (!reportId) {
    ElMessage.error('Report ID is required')
    return
  }

  loading.value = true
  try {
    report.value = await reportStore.getReportDetails(reportId)
  } catch (error) {
    console.error('Failed to load report:', error)
    ElMessage.error('Failed to load report details')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.go(-1)
}

const refreshReport = () => {
  loadReport()
}

const exportReport = async () => {
  try {
    await reportStore.exportReport(route.query.id as string, 'detailed')
    ElMessage.success('Report exported successfully')
  } catch (error) {
    console.error('Export failed:', error)
    ElMessage.error('Failed to export report')
  }
}

const getQualityScoreType = (score: number) => {
  if (score >= 90) return 'success'
  if (score >= 70) return 'warning'
  return 'danger'
}

const viewTableDetails = (table: any) => {
  selectedTable.value = table
  tableDetailsVisible.value = true
}

onMounted(async () => {
  try {
    await loadReport()
  } catch (error) {
    console.error('Failed to initialize report details:', error)
    ElMessage.error('Failed to initialize report details')
  }
})
</script>

<style scoped>
.report-details {
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
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
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
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-text {
  color: #909399;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.loading-container {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.report-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 20px 24px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
  background-color: #fafafa;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
  padding: 24px;
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.overview-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 8px rgba(64, 158, 255, 0.1);
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.card-icon.quality { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.card-icon.tables { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.card-icon.columns { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.card-icon.rows { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }

.card-content {
  flex: 1;
}

.card-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1;
  margin-bottom: 4px;
}

.card-label {
  font-size: 14px;
  color: #909399;
}

.table-analysis {
  padding: 24px;
}

.table-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.issues-container {
  padding: 24px;
}

.issues-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.issue-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.issue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

.issue-severity {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.issue-severity.high {
  background-color: #fef0f0;
  color: #f56c6c;
}

.issue-severity.medium {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.issue-severity.low {
  background-color: #f0f9ff;
  color: #409eff;
}

.issue-table {
  font-size: 14px;
  color: #909399;
  font-family: 'Courier New', monospace;
}

.issue-content {
  padding: 16px;
}

.issue-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.issue-description {
  margin: 0 0 12px 0;
  color: #606266;
  line-height: 1.5;
}

.issue-stats {
  display: flex;
  gap: 24px;
  font-size: 14px;
}

.stat-item {
  color: #909399;
}

.no-issues {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.chart-container {
  display: flex;
  padding: 24px;
  gap: 24px;
}

.chart-wrapper {
  flex: 1;
  height: 300px;
}

.chart-legend {
  min-width: 200px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-label {
  flex: 1;
  color: #606266;
}

.legend-value {
  color: #909399;
  font-weight: 500;
}

.recommendations {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recommendation-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.recommendation-priority {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  height: fit-content;
}

.recommendation-priority.high {
  background-color: #fef0f0;
  color: #f56c6c;
}

.recommendation-priority.medium {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.recommendation-priority.low {
  background-color: #f0f9ff;
  color: #409eff;
}

.recommendation-content {
  flex: 1;
}

.recommendation-content h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
}

.recommendation-content p {
  margin: 0 0 12px 0;
  color: #606266;
  line-height: 1.5;
}

.recommendation-actions ul {
  margin: 8px 0 0 0;
  padding-left: 20px;
}

.recommendation-actions li {
  margin-bottom: 4px;
  color: #606266;
}

.error-state {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.table-details {
  max-height: 70vh;
  overflow-y: auto;
}

.table-info {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.table-info h3 {
  margin: 0 0 12px 0;
  color: #303133;
}

.table-stats {
  display: flex;
  gap: 24px;
}

.stat {
  font-size: 14px;
  color: #606266;
}

.table-issue {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 12px;
}

.issue-details h4 {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: #303133;
}

.issue-details p {
  margin: 0;
  font-size: 13px;
  color: #606266;
}

.no-table-issues {
  text-align: center;
  padding: 40px;
  color: #909399;
}

@media (max-width: 768px) {
  .report-details {
    padding: 16px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .header-left {
    flex-direction: column;
    gap: 12px;
  }
  
  .overview-grid {
    grid-template-columns: 1fr;
  }
  
  .chart-container {
    flex-direction: column;
  }
  
  .chart-wrapper {
    height: 250px;
  }
  
  .issue-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .issue-stats {
    flex-direction: column;
    gap: 8px;
  }
  
  .recommendation-item {
    flex-direction: column;
    gap: 12px;
  }
  
  .table-stats {
    flex-direction: column;
    gap: 8px;
  }
}
</style>