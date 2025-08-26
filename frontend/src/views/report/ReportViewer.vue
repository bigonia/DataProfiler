<template>
  <div class="report-viewer">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" type="text" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          Back
        </el-button>
        <div class="header-info">
          <h1 class="page-title">{{ report?.taskName || 'Report Viewer' }}</h1>
          <div class="header-meta">
            <el-tag v-if="report?.dataSource" type="info" size="small">
              <el-icon><Coin /></el-icon>
              {{ report.dataSource }}
            </el-tag>
            <span class="meta-text">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(report?.createdAt) }}
            </span>
            <span class="meta-text">
              <el-icon><Clock /></el-icon>
              Duration: {{ formatDuration(report?.duration) }}
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
        <el-button @click="viewSummary">
          <el-icon><DataAnalysis /></el-icon>
          Summary
        </el-button>
        <el-button @click="viewDetails">
          <el-icon><View /></el-icon>
          Details
        </el-button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- Report Content -->
    <div v-else-if="report" class="report-content">
      <!-- Quick Stats -->
      <div class="quick-stats">
        <div class="stat-card quality">
          <div class="stat-icon">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ report.qualityScore }}%</div>
            <div class="stat-label">Overall Quality</div>
            <div class="stat-trend" :class="getQualityTrend(report.qualityScore)">
              <el-icon><ArrowUp v-if="report.qualityScore >= 80" /><ArrowDown v-else /></el-icon>
              {{ getQualityStatus(report.qualityScore) }}
            </div>
          </div>
        </div>
        
        <div class="stat-card tables">
          <div class="stat-icon">
            <el-icon><Grid /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatNumber(report.totalTables) }}</div>
            <div class="stat-label">Tables Analyzed</div>
            <div class="stat-detail">{{ report.completedTables }}/{{ report.totalTables }} completed</div>
          </div>
        </div>
        
        <div class="stat-card issues">
          <div class="stat-icon">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ report.totalIssues || 0 }}</div>
            <div class="stat-label">Issues Found</div>
            <div class="stat-detail">
              {{ report.criticalIssues || 0 }} critical, {{ report.warningIssues || 0 }} warnings
            </div>
          </div>
        </div>
        
        <div class="stat-card data">
          <div class="stat-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatNumber(report.totalRows) }}</div>
            <div class="stat-label">Total Records</div>
            <div class="stat-detail">{{ formatNumber(report.totalColumns) }} columns</div>
          </div>
        </div>
      </div>

      <!-- Navigation Tabs -->
      <div class="report-tabs">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="Overview" name="overview">
            <div class="tab-content">
              <!-- Data Quality Overview -->
              <div class="overview-section">
                <h3 class="section-title">
                  <el-icon><DataAnalysis /></el-icon>
                  Data Quality Overview
                </h3>
                <div class="quality-metrics">
                  <div class="metric-item">
                    <div class="metric-label">Completeness</div>
                    <el-progress :percentage="report.completeness" :color="getProgressColor(report.completeness)" />
                    <div class="metric-value">{{ report.completeness }}%</div>
                  </div>
                  <div class="metric-item">
                    <div class="metric-label">Uniqueness</div>
                    <el-progress :percentage="report.uniqueness" :color="getProgressColor(report.uniqueness)" />
                    <div class="metric-value">{{ report.uniqueness }}%</div>
                  </div>
                  <div class="metric-item">
                    <div class="metric-label">Consistency</div>
                    <el-progress :percentage="report.consistency" :color="getProgressColor(report.consistency)" />
                    <div class="metric-value">{{ report.consistency }}%</div>
                  </div>
                  <div class="metric-item">
                    <div class="metric-label">Accuracy</div>
                    <el-progress :percentage="report.accuracy" :color="getProgressColor(report.accuracy)" />
                    <div class="metric-value">{{ report.accuracy }}%</div>
                  </div>
                </div>
              </div>

              <!-- Data Distribution Chart -->
              <div class="overview-section">
                <h3 class="section-title">
                  <el-icon><PieChart /></el-icon>
                  Data Distribution
                </h3>
                <div class="chart-container">
                  <v-chart ref="distributionChart" :option="distributionChartOption" autoresize />
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="Tables" name="tables">
            <div class="tab-content">
              <div class="table-filters">
                <el-input
                  v-model="tableSearch"
                  placeholder="Search tables..."
                  prefix-icon="Search"
                  clearable
                  style="width: 300px;"
                />
                <el-select v-model="tableFilter" placeholder="Filter by quality" clearable style="width: 200px;">
                  <el-option label="High Quality (>90%)" value="high" />
                  <el-option label="Medium Quality (70-90%)" value="medium" />
                  <el-option label="Low Quality (<70%)" value="low" />
                </el-select>
              </div>
              
              <div class="tables-grid">
                <div v-for="table in filteredTables" :key="table.name" class="table-card" @click="selectTable(table)">
                  <div class="table-header">
                    <div class="table-name">
                      <el-icon><Grid /></el-icon>
                      {{ table.name }}
                    </div>
                    <el-tag :type="getQualityScoreType(table.qualityScore)" size="small">
                      {{ table.qualityScore }}%
                    </el-tag>
                  </div>
                  <div class="table-stats">
                    <div class="stat">
                      <span class="stat-label">Columns:</span>
                      <span class="stat-value">{{ table.columns }}</span>
                    </div>
                    <div class="stat">
                      <span class="stat-label">Rows:</span>
                      <span class="stat-value">{{ formatNumber(table.rows) }}</span>
                    </div>
                    <div class="stat">
                      <span class="stat-label">Issues:</span>
                      <span class="stat-value" :class="{ 'has-issues': table.issues > 0 }">
                        {{ table.issues }}
                      </span>
                    </div>
                  </div>
                  <div v-if="table.issues > 0" class="table-issues">
                    <el-icon><Warning /></el-icon>
                    {{ table.issues }} issue{{ table.issues > 1 ? 's' : '' }} found
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="Issues" name="issues">
            <div class="tab-content">
              <div class="issues-filters">
                <el-select v-model="issueFilter" placeholder="Filter by severity" clearable style="width: 200px;">
                  <el-option label="Critical" value="critical" />
                  <el-option label="High" value="high" />
                  <el-option label="Medium" value="medium" />
                  <el-option label="Low" value="low" />
                </el-select>
                <el-input
                  v-model="issueSearch"
                  placeholder="Search issues..."
                  prefix-icon="Search"
                  clearable
                  style="width: 300px;"
                />
              </div>
              
              <div class="issues-list">
                <div v-for="issue in filteredIssues" :key="issue.id" class="issue-card">
                  <div class="issue-header">
                    <div class="issue-severity" :class="issue.severity">
                      <el-icon><Warning /></el-icon>
                      {{ issue.severity.toUpperCase() }}
                    </div>
                    <div class="issue-location">
                      {{ issue.tableName }}.{{ issue.columnName }}
                    </div>
                  </div>
                  <div class="issue-content">
                    <h4 class="issue-title">{{ issue.title }}</h4>
                    <p class="issue-description">{{ issue.description }}</p>
                    <div class="issue-metrics">
                      <div class="metric">
                        <span class="metric-label">Affected Rows:</span>
                        <span class="metric-value">{{ formatNumber(issue.affectedRows) }}</span>
                      </div>
                      <div class="metric">
                        <span class="metric-label">Impact:</span>
                        <span class="metric-value">{{ issue.impact }}%</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="Recommendations" name="recommendations">
            <div class="tab-content">
              <div class="recommendations-list">
                <div v-for="(recommendation, index) in report.recommendations" :key="index" class="recommendation-card">
                  <div class="recommendation-header">
                    <div class="recommendation-priority" :class="recommendation.priority">
                      <el-icon><Star /></el-icon>
                      {{ recommendation.priority.toUpperCase() }}
                    </div>
                    <div class="recommendation-category">
                      {{ recommendation.category }}
                    </div>
                  </div>
                  <div class="recommendation-content">
                    <h4 class="recommendation-title">{{ recommendation.title }}</h4>
                    <p class="recommendation-description">{{ recommendation.description }}</p>
                    <div v-if="recommendation.actions" class="recommendation-actions">
                      <h5>Suggested Actions:</h5>
                      <ul>
                        <li v-for="action in recommendation.actions" :key="action">
                          {{ action }}
                        </li>
                      </ul>
                    </div>
                    <div v-if="recommendation.impact" class="recommendation-impact">
                      <strong>Expected Impact:</strong> {{ recommendation.impact }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
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
    <el-dialog v-model="tableDialogVisible" :title="selectedTable?.name" width="90%" top="5vh">
      <div v-if="selectedTable" class="table-details-dialog">
        <el-tabs v-model="tableDialogTab">
          <el-tab-pane label="Column Analysis" name="columns">
            <el-table :data="selectedTable.columnDetails" stripe max-height="400">
              <el-table-column prop="name" label="Column Name" min-width="150" />
              <el-table-column prop="type" label="Data Type" width="120" />
              <el-table-column prop="nullCount" label="Null Count" width="100" align="center" />
              <el-table-column prop="uniqueCount" label="Unique Values" width="120" align="center" />
              <el-table-column prop="completeness" label="Completeness" width="120" align="center">
                <template #default="{ row }">
                  <el-progress :percentage="row.completeness" :show-text="false" :stroke-width="6" />
                  <span class="progress-text">{{ row.completeness }}%</span>
                </template>
              </el-table-column>
              <el-table-column prop="distinctness" label="Distinctness" width="120" align="center">
                <template #default="{ row }">
                  <el-progress :percentage="row.distinctness" :show-text="false" :stroke-width="6" />
                  <span class="progress-text">{{ row.distinctness }}%</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="Data Sample" name="sample">
            <el-table :data="selectedTable.sampleData" stripe max-height="400">
              <el-table-column
                v-for="column in selectedTable.columns"
                :key="column"
                :prop="column"
                :label="column"
                min-width="120"
                show-overflow-tooltip
              />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="Issues" name="tableIssues">
            <div v-if="selectedTable.tableIssues && selectedTable.tableIssues.length > 0" class="table-issues-list">
              <div v-for="issue in selectedTable.tableIssues" :key="issue.id" class="table-issue-item">
                <div class="issue-severity" :class="issue.severity">
                  {{ issue.severity.toUpperCase() }}
                </div>
                <div class="issue-details">
                  <h5>{{ issue.title }}</h5>
                  <p>{{ issue.description }}</p>
                  <div class="issue-stats">
                    <span><strong>Column:</strong> {{ issue.columnName }}</span>
                    <span><strong>Affected Rows:</strong> {{ formatNumber(issue.affectedRows) }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="no-table-issues">
              <el-icon size="48" color="#67c23a"><SuccessFilled /></el-icon>
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
import { formatDate, formatNumber, formatDuration } from '@/utils'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Download,
  ArrowDown,
  Coin,
  Calendar,
  Clock,
  Document,
  Grid,
  DocumentCopy,
  DataAnalysis,
  View,
  TrendCharts,
  Warning,
  ArrowUp,
  PieChart,
  Search,
  Star,
  SuccessFilled
} from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart as EChartsPie, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

// Register ECharts components
use([
  CanvasRenderer,
  EChartsPie,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const route = useRoute()
const router = useRouter()
const reportStore = useReportStore()

const loading = ref(false)
const report = ref(null)
const activeTab = ref('overview')
const tableSearch = ref('')
const tableFilter = ref('')
const issueSearch = ref('')
const issueFilter = ref('')
const tableDialogVisible = ref(false)
const selectedTable = ref(null)
const tableDialogTab = ref('columns')

// Computed properties
const filteredTables = computed(() => {
  if (!report.value?.tables) return []
  
  let tables = report.value.tables
  
  // Filter by search
  if (tableSearch.value) {
    tables = tables.filter(table => 
      table.name.toLowerCase().includes(tableSearch.value.toLowerCase())
    )
  }
  
  // Filter by quality
  if (tableFilter.value) {
    tables = tables.filter(table => {
      const score = table.qualityScore
      switch (tableFilter.value) {
        case 'high': return score > 90
        case 'medium': return score >= 70 && score <= 90
        case 'low': return score < 70
        default: return true
      }
    })
  }
  
  return tables
})

const filteredIssues = computed(() => {
  if (!report.value?.issues) return []
  
  let issues = report.value.issues
  
  // Filter by severity
  if (issueFilter.value) {
    issues = issues.filter(issue => issue.severity === issueFilter.value)
  }
  
  // Filter by search
  if (issueSearch.value) {
    issues = issues.filter(issue => 
      issue.title.toLowerCase().includes(issueSearch.value.toLowerCase()) ||
      issue.description.toLowerCase().includes(issueSearch.value.toLowerCase())
    )
  }
  
  return issues
})

const distributionChartOption = computed(() => {
  if (!report.value?.dataTypes) return {}
  
  return {
    title: {
      text: 'Data Types Distribution',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    series: [
      {
        name: 'Data Types',
        type: 'pie',
        radius: ['40%', '70%'],
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

// Methods
const loadReport = async () => {
  const taskId = route.query.taskId as string
  if (!taskId) {
    ElMessage.error('Task ID is required')
    return
  }

  loading.value = true
  try {
    // Get summary reports by taskId
    const response = await reportStore.getSummaryByTaskId({ taskId })
    if (response && response.content && response.content.length > 0) {
      report.value = response.content[0] // Use the first report for this taskId
    } else {
      throw new Error('No report found for this task')
    }
  } catch (error) {
    console.error('Failed to load report:', error)
    ElMessage.error('Failed to load report')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.go(-1)
}

const handleExport = async (format: string) => {
  try {
    await reportStore.exportReport(route.params.id as string, format)
    ElMessage.success(`Report exported as ${format.toUpperCase()}`)
  } catch (error) {
    console.error('Export failed:', error)
    ElMessage.error('Failed to export report')
  }
}

const viewSummary = () => {
  router.push({ name: 'ReportSummary', query: { id: route.params.id } })
}

const viewDetails = () => {
  router.push({ name: 'ReportDetails', query: { id: route.params.id } })
}

const handleTabChange = (tabName: string) => {
  activeTab.value = tabName
}

const selectTable = (table: any) => {
  selectedTable.value = table
  tableDialogVisible.value = true
}

const getQualityScoreType = (score: number) => {
  if (score >= 90) return 'success'
  if (score >= 70) return 'warning'
  return 'danger'
}

const getQualityTrend = (score: number) => {
  return score >= 80 ? 'positive' : 'negative'
}

const getQualityStatus = (score: number) => {
  if (score >= 90) return 'Excellent'
  if (score >= 80) return 'Good'
  if (score >= 70) return 'Fair'
  return 'Poor'
}

const getProgressColor = (percentage: number) => {
  if (percentage >= 90) return '#67c23a'
  if (percentage >= 70) return '#e6a23c'
  return '#f56c6c'
}

onMounted(async () => {
  try {
    await loadReport()
  } catch (error) {
    console.error('Failed to initialize report viewer:', error)
    ElMessage.error('Failed to initialize report viewer')
  }
})
</script>

<style scoped>
.report-viewer {
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
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.quick-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.stat-card.quality .stat-icon { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.stat-card.tables .stat-icon { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.stat-card.issues .stat-icon { background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%); }
.stat-card.data .stat-icon { background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%); }

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-detail {
  font-size: 12px;
  color: #c0c4cc;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
}

.stat-trend.positive { color: #67c23a; }
.stat-trend.negative { color: #f56c6c; }

.report-tabs {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.tab-content {
  padding: 24px;
}

.overview-section {
  margin-bottom: 32px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.quality-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.metric-item {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.metric-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-top: 8px;
}

.chart-container {
  height: 400px;
}

.table-filters {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.tables-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.table-card {
  background: #f8f9fa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.table-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 8px rgba(64, 158, 255, 0.1);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.table-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
}

.table-stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
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

.stat-value.has-issues {
  color: #f56c6c;
}

.table-issues {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #e6a23c;
  background: #fdf6ec;
  padding: 4px 8px;
  border-radius: 4px;
}

.issues-filters {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.issues-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.issue-card {
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

.issue-severity.critical {
  background-color: #fef0f0;
  color: #f56c6c;
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

.issue-location {
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

.issue-metrics {
  display: flex;
  gap: 24px;
}

.metric {
  font-size: 14px;
}

.metric-label {
  color: #909399;
}

.metric-value {
  color: #303133;
  font-weight: 500;
}

.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recommendation-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.recommendation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

.recommendation-priority {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
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

.recommendation-category {
  font-size: 14px;
  color: #909399;
}

.recommendation-content {
  padding: 16px;
}

.recommendation-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.recommendation-description {
  margin: 0 0 12px 0;
  color: #606266;
  line-height: 1.5;
}

.recommendation-actions h5 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #303133;
}

.recommendation-actions ul {
  margin: 0;
  padding-left: 20px;
}

.recommendation-actions li {
  margin-bottom: 4px;
  color: #606266;
}

.recommendation-impact {
  margin-top: 12px;
  font-size: 14px;
  color: #606266;
}

.error-state {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.table-details-dialog {
  max-height: 70vh;
  overflow-y: auto;
}

.progress-text {
  margin-left: 8px;
  font-size: 12px;
  color: #606266;
}

.table-issues-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.table-issue-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.issue-details h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: #303133;
}

.issue-details p {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #606266;
}

.issue-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.no-table-issues {
  text-align: center;
  padding: 40px;
  color: #909399;
}

@media (max-width: 768px) {
  .report-viewer {
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
  
  .header-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .quick-stats {
    grid-template-columns: 1fr;
  }
  
  .quality-metrics {
    grid-template-columns: 1fr;
  }
  
  .tables-grid {
    grid-template-columns: 1fr;
  }
  
  .table-filters,
  .issues-filters {
    flex-direction: column;
    align-items: stretch;
  }
  
  .table-filters .el-input,
  .table-filters .el-select,
  .issues-filters .el-input,
  .issues-filters .el-select {
    width: 100% !important;
  }
  
  .issue-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .issue-metrics {
    flex-direction: column;
    gap: 8px;
  }
  
  .recommendation-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>