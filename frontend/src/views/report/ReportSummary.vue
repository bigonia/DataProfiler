<template>
  <div class="report-summary">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <el-button text @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          Back
        </el-button>
        <div class="title-section">
          <h2 class="page-title">{{ report?.taskName || 'Report Summary' }}</h2>
          <p class="page-description">Executive summary of data profiling results</p>
        </div>
      </div>
      
      <div class="header-right">
        <el-button @click="exportSummary" :loading="exporting">
          <el-icon><Download /></el-icon>
          Export Summary
        </el-button>
        <el-button type="primary" @click="viewFullReport">
          <el-icon><View /></el-icon>
          View Full Report
        </el-button>
      </div>
    </div>
    
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>
    
    <div v-else-if="report" class="summary-content">
      <!-- Overview Cards -->
      <div class="overview-section">
        <div class="overview-card main-card">
          <div class="card-header">
            <h3 class="card-title">
              <el-icon><DataAnalysis /></el-icon>
              Data Overview
            </h3>
            <el-tag :type="getQualityType(report.qualityScore)" size="large">
              Quality Score: {{ report.qualityScore }}%
            </el-tag>
          </div>
          
          <div class="overview-stats">
            <div class="stat-group">
              <div class="stat-item">
                <div class="stat-value">{{ report.tableCount }}</div>
                <div class="stat-label">Tables Analyzed</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ report.columnCount }}</div>
                <div class="stat-label">Total Columns</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ formatNumber(report.totalRows) }}</div>
                <div class="stat-label">Total Rows</div>
              </div>
            </div>
            
            <div class="quality-section">
              <div class="quality-header">
                <span>Overall Data Quality</span>
                <span class="quality-score" :class="getQualityScoreClass(report.qualityScore)">
                  {{ report.qualityScore }}%
                </span>
              </div>
              <el-progress
                :percentage="report.qualityScore"
                :status="getQualityStatus(report.qualityScore)"
                :stroke-width="8"
              />
            </div>
          </div>
        </div>
      </div>
      
      <!-- Key Insights -->
      <div class="insights-section">
        <div class="section-header">
          <h3 class="section-title">
            <el-icon><InfoFilled /></el-icon>
            Key Insights
          </h3>
        </div>
        
        <div class="insights-grid">
          <div class="insight-card" v-for="insight in keyInsights" :key="insight.type">
            <div class="insight-icon" :class="getInsightIconClass(insight.type)">
              <el-icon><component :is="getInsightIcon(insight.type)" /></el-icon>
            </div>
            <div class="insight-content">
              <h4 class="insight-title">{{ insight.title }}</h4>
              <p class="insight-description">{{ insight.description }}</p>
              <div class="insight-value">{{ insight.value }}</div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Quality Issues -->
      <div v-if="report.issues && report.issues.length > 0" class="issues-section">
        <div class="section-header">
          <h3 class="section-title">
            <el-icon><Warning /></el-icon>
            Quality Issues Summary
          </h3>
          <el-tag type="danger" size="small">
            {{ report.issues.length }} Issues Found
          </el-tag>
        </div>
        
        <div class="issues-summary">
          <div class="severity-breakdown">
            <div class="severity-item" v-for="severity in severityBreakdown" :key="severity.level">
              <div class="severity-count" :class="getSeverityClass(severity.level)">
                {{ severity.count }}
              </div>
              <div class="severity-label">{{ severity.level }} Priority</div>
            </div>
          </div>
          
          <div class="top-issues">
            <h4 class="issues-subtitle">Top Issues</h4>
            <div class="issue-list">
              <div
                v-for="issue in report.issues.slice(0, 5)"
                :key="issue.id"
                class="issue-item"
              >
                <div class="issue-info">
                  <el-tag :type="getIssueType(issue.severity)" size="small">
                    {{ issue.severity.toUpperCase() }}
                  </el-tag>
                  <span class="issue-type">{{ issue.type }}</span>
                </div>
                <div class="issue-details">
                  <span class="issue-table">{{ issue.tableName }}</span>
                  <span v-if="issue.columnName" class="issue-column">{{ issue.columnName }}</span>
                </div>
                <div class="issue-count">{{ issue.count }} occurrences</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Table Summary -->
      <div class="tables-section">
        <div class="section-header">
          <h3 class="section-title">
            <el-icon><Grid /></el-icon>
            Table Summary
          </h3>
        </div>
        
        <div class="tables-grid">
          <div
            v-for="table in tableSummary"
            :key="table.name"
            class="table-card"
          >
            <div class="table-header">
              <h4 class="table-name">{{ table.name }}</h4>
              <el-tag :type="getQualityType(table.qualityScore)" size="small">
                {{ table.qualityScore }}%
              </el-tag>
            </div>
            
            <div class="table-stats">
              <div class="table-stat">
                <span class="stat-label">Columns:</span>
                <span class="stat-value">{{ table.columnCount }}</span>
              </div>
              <div class="table-stat">
                <span class="stat-label">Rows:</span>
                <span class="stat-value">{{ formatNumber(table.rowCount) }}</span>
              </div>
              <div class="table-stat">
                <span class="stat-label">Issues:</span>
                <span class="stat-value" :class="{ 'has-issues': table.issueCount > 0 }">
                  {{ table.issueCount }}
                </span>
              </div>
            </div>
            
            <div class="table-quality">
              <el-progress
                :percentage="table.qualityScore"
                :status="getQualityStatus(table.qualityScore)"
                :stroke-width="4"
                :show-text="false"
              />
            </div>
          </div>
        </div>
      </div>
      
      <!-- Data Types Distribution -->
      <div class="distribution-section">
        <div class="section-header">
          <h3 class="section-title">
            <el-icon><PieChart /></el-icon>
            Data Types Distribution
          </h3>
        </div>
        
        <div class="distribution-content">
          <div class="chart-container">
            <div ref="dataTypesChart" class="chart"></div>
          </div>
          
          <div class="distribution-stats">
            <div class="type-list">
              <div
                v-for="type in dataTypesDistribution"
                :key="type.name"
                class="type-item"
              >
                <div class="type-color" :style="{ backgroundColor: type.color }"></div>
                <div class="type-info">
                  <span class="type-name">{{ type.name }}</span>
                  <span class="type-count">{{ type.count }} columns ({{ type.percentage }}%)</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Recommendations -->
      <div class="recommendations-section">
        <div class="section-header">
          <h3 class="section-title">
            <el-icon><Guide /></el-icon>
            Recommendations
          </h3>
        </div>
        
        <div class="recommendations-list">
          <div
            v-for="recommendation in recommendations"
            :key="recommendation.id"
            class="recommendation-item"
          >
            <div class="recommendation-icon" :class="getRecommendationClass(recommendation.priority)">
              <el-icon><component :is="getRecommendationIcon(recommendation.type)" /></el-icon>
            </div>
            <div class="recommendation-content">
              <h4 class="recommendation-title">{{ recommendation.title }}</h4>
              <p class="recommendation-description">{{ recommendation.description }}</p>
              <div class="recommendation-meta">
                <el-tag :type="getRecommendationTagType(recommendation.priority)" size="small">
                  {{ recommendation.priority }} Priority
                </el-tag>
                <span class="recommendation-impact">Impact: {{ recommendation.impact }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else class="error-state">
      <el-result
        icon="error"
        title="Report Not Found"
        sub-title="The requested report could not be found or you don't have permission to view it."
      >
        <template #extra>
          <el-button type="primary" @click="$router.push('/reports')">
            Back to Reports
          </el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Download,
  View,
  DataAnalysis,
  InfoFilled,
  Warning,
  Grid,
  PieChart,
  Guide,
  TrendCharts,
  DocumentChecked,
  Tools,
  Setting
} from '@element-plus/icons-vue'
import { useReportStore } from '@/stores/report'
import { formatNumber } from '@/utils'
import * as echarts from 'echarts'
import type { ProfilingReport, QualityIssue } from '@/types'

const route = useRoute()
const router = useRouter()
const reportStore = useReportStore()

const loading = ref(false)
const exporting = ref(false)
const report = ref<ProfilingReport | null>(null)
const dataTypesChart = ref<HTMLElement>()
let chartInstance: any = null // Track chart instance for cleanup

const reportId = computed(() => parseInt(route.params.id as string))

const keyInsights = computed(() => {
  if (!report.value) return []
  
  return [
    {
      type: 'completeness',
      title: 'Data Completeness',
      description: 'Overall completeness of your dataset',
      value: '94.2%',
      icon: 'DocumentChecked'
    },
    {
      type: 'uniqueness',
      title: 'Data Uniqueness',
      description: 'Percentage of unique records',
      value: '98.7%',
      icon: 'TrendCharts'
    },
    {
      type: 'consistency',
      title: 'Data Consistency',
      description: 'Format and pattern consistency',
      value: '91.5%',
      icon: 'Setting'
    },
    {
      type: 'accuracy',
      title: 'Data Accuracy',
      description: 'Estimated data accuracy score',
      value: '96.1%',
      icon: 'Tools'
    }
  ]
})

const severityBreakdown = computed(() => {
  if (!report.value?.issues) return []
  
  const breakdown = { high: 0, medium: 0, low: 0 }
  report.value.issues.forEach(issue => {
    breakdown[issue.severity as keyof typeof breakdown]++
  })
  
  return [
    { level: 'High', count: breakdown.high },
    { level: 'Medium', count: breakdown.medium },
    { level: 'Low', count: breakdown.low }
  ]
})

const tableSummary = computed(() => {
  if (!report.value) return []
  
  // Mock table summary data
  return [
    {
      name: 'users',
      columnCount: 12,
      rowCount: 15420,
      qualityScore: 96,
      issueCount: 2
    },
    {
      name: 'orders',
      columnCount: 8,
      rowCount: 89234,
      qualityScore: 92,
      issueCount: 5
    },
    {
      name: 'products',
      columnCount: 15,
      rowCount: 2341,
      qualityScore: 88,
      issueCount: 8
    }
  ]
})

const dataTypesDistribution = computed(() => {
  return [
    { name: 'VARCHAR/TEXT', count: 45, percentage: 42, color: '#409eff' },
    { name: 'INTEGER', count: 28, percentage: 26, color: '#67c23a' },
    { name: 'DATETIME', count: 15, percentage: 14, color: '#e6a23c' },
    { name: 'DECIMAL', count: 12, percentage: 11, color: '#f56c6c' },
    { name: 'BOOLEAN', count: 8, percentage: 7, color: '#909399' }
  ]
})

const recommendations = computed(() => {
  return [
    {
      id: 1,
      type: 'quality',
      priority: 'high',
      title: 'Address Missing Email Values',
      description: 'Found 15 null values in the email column of users table. Consider implementing validation or default values.',
      impact: 'Data Quality'
    },
    {
      id: 2,
      type: 'performance',
      priority: 'medium',
      title: 'Optimize Large Text Fields',
      description: 'Several text columns contain very long values that might impact query performance.',
      impact: 'Performance'
    },
    {
      id: 3,
      type: 'consistency',
      priority: 'medium',
      title: 'Standardize Date Formats',
      description: 'Multiple date formats detected across different tables. Consider standardizing to ISO format.',
      impact: 'Consistency'
    },
    {
      id: 4,
      type: 'security',
      priority: 'low',
      title: 'Review Sensitive Data Exposure',
      description: 'Some columns may contain sensitive information. Consider implementing data masking.',
      impact: 'Security'
    }
  ]
})

const getQualityType = (score: number) => {
  if (score >= 90) return 'success'
  if (score >= 75) return 'primary'
  if (score >= 60) return 'warning'
  return 'danger'
}

const getQualityStatus = (score: number) => {
  if (score >= 90) return 'success'
  if (score >= 75) return undefined
  if (score >= 60) return 'warning'
  return 'exception'
}

const getQualityScoreClass = (score: number) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 75) return 'score-good'
  if (score >= 60) return 'score-fair'
  return 'score-poor'
}

const getInsightIcon = (type: string) => {
  const icons: Record<string, any> = {
    completeness: DocumentChecked,
    uniqueness: TrendCharts,
    consistency: Setting,
    accuracy: Tools
  }
  return icons[type] || DocumentChecked
}

const getInsightIconClass = (type: string) => {
  const classes: Record<string, string> = {
    completeness: 'insight-completeness',
    uniqueness: 'insight-uniqueness',
    consistency: 'insight-consistency',
    accuracy: 'insight-accuracy'
  }
  return classes[type] || 'insight-default'
}

const getSeverityClass = (level: string) => {
  const classes: Record<string, string> = {
    High: 'severity-high',
    Medium: 'severity-medium',
    Low: 'severity-low'
  }
  return classes[level] || 'severity-low'
}

const getIssueType = (severity: string) => {
  switch (severity) {
    case 'high':
      return 'danger'
    case 'medium':
      return 'warning'
    case 'low':
      return 'info'
    default:
      return 'info'
  }
}

const getRecommendationIcon = (type: string) => {
  const icons: Record<string, any> = {
    quality: DocumentChecked,
    performance: TrendCharts,
    consistency: Setting,
    security: Tools
  }
  return icons[type] || DocumentChecked
}

const getRecommendationClass = (priority: string) => {
  const classes: Record<string, string> = {
    high: 'rec-high',
    medium: 'rec-medium',
    low: 'rec-low'
  }
  return classes[priority] || 'rec-low'
}

const getRecommendationTagType = (priority: string) => {
  switch (priority) {
    case 'high':
      return 'danger'
    case 'medium':
      return 'warning'
    case 'low':
      return 'info'
    default:
      return 'info'
  }
}

const loadReport = async () => {
  loading.value = true
  try {
    // In a real implementation, you would fetch the report from the API
    const existingReport = reportStore.reports.find(r => r.id === reportId.value)
    if (existingReport) {
      report.value = existingReport
    } else {
      // Mock report data
      report.value = {
        id: reportId.value,
        taskId: 1,
        taskName: 'Sample Profiling Report',
        dataSourceId: 1,
        tableCount: 3,
        columnCount: 35,
        totalRows: 107995,
        qualityScore: 94,
        issues: [
          {
            id: 1,
            type: 'Missing Values',
            severity: 'high',
            tableName: 'users',
            columnName: 'email',
            count: 15,
            description: 'Null values found in email column'
          },
          {
            id: 2,
            type: 'Data Format',
            severity: 'medium',
            tableName: 'orders',
            columnName: 'order_date',
            count: 8,
            description: 'Inconsistent date formats'
          }
        ],
        createdAt: new Date().toISOString(),
        duration: 1800
      }
    }
  } catch (error) {
    ElMessage.error('Failed to load report')
  } finally {
    loading.value = false
  }
}

const initDataTypesChart = () => {
  // Enhanced DOM element validation
  if (!dataTypesChart.value || dataTypesChart.value.offsetWidth === 0) {
    console.warn('Data types chart element not ready or not visible')
    return
  }
  
  try {
    chartInstance = echarts.init(dataTypesChart.value)
    const chart = chartInstance
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: dataTypesDistribution.value.map(item => item.name)
    },
    series: [
      {
        name: 'Data Types',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
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
        data: dataTypesDistribution.value.map(item => ({
          value: item.count,
          name: item.name,
          itemStyle: {
            color: item.color
          }
        }))
      }
    ]
  }
  
    chart.setOption(option)
    
    // Make chart responsive with error handling
    const resizeHandler = () => {
      try {
        if (chart && !chart.isDisposed()) {
          chart.resize()
        }
      } catch (error) {
        console.error('Chart resize failed:', error)
      }
    }
    
    window.addEventListener('resize', resizeHandler)
    
    // Store resize handler for cleanup
    chart._resizeHandler = resizeHandler
    
  } catch (error) {
    console.error('Failed to initialize data types chart:', error)
  }
}

const exportSummary = async () => {
  exporting.value = true
  try {
    // In a real implementation, you would call the export API
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('Summary exported successfully')
  } catch (error) {
    ElMessage.error('Failed to export summary')
  } finally {
    exporting.value = false
  }
}

const viewFullReport = () => {
  router.push(`/reports/${reportId.value}`)
}

onMounted(async () => {
  await loadReport()
  await nextTick()
  initDataTypesChart()
})

onUnmounted(() => {
  // Clean up chart instance and event listeners
  if (chartInstance && !chartInstance.isDisposed()) {
    if (chartInstance._resizeHandler) {
      window.removeEventListener('resize', chartInstance._resizeHandler)
    }
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.report-summary {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.title-section {
  margin-left: 8px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.page-description {
  color: #606266;
  font-size: 14px;
  margin: 0;
}

.header-right {
  display: flex;
  gap: 12px;
}

.loading-container {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.overview-section {
  margin-bottom: 24px;
}

.overview-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.overview-stats {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  align-items: center;
}

.stat-group {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  color: #606266;
  font-size: 14px;
}

.quality-section {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.quality-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.quality-score {
  font-size: 18px;
  font-weight: 600;
}

.score-excellent { color: #67c23a; }
.score-good { color: #409eff; }
.score-fair { color: #e6a23c; }
.score-poor { color: #f56c6c; }

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.insights-section,
.issues-section,
.tables-section,
.distribution-section,
.recommendations-section {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 24px;
  margin-bottom: 24px;
}

.insights-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.insight-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.insight-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.insight-completeness { background: #67c23a; }
.insight-uniqueness { background: #409eff; }
.insight-consistency { background: #e6a23c; }
.insight-accuracy { background: #f56c6c; }

.insight-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.insight-description {
  font-size: 12px;
  color: #606266;
  margin: 0 0 8px 0;
}

.insight-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.issues-summary {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 24px;
}

.severity-breakdown {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.severity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.severity-count {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #fff;
}

.severity-high { background: #f56c6c; }
.severity-medium { background: #e6a23c; }
.severity-low { background: #909399; }

.severity-label {
  font-size: 14px;
  color: #303133;
}

.issues-subtitle {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.issue-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.issue-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  border-left: 3px solid #f56c6c;
}

.issue-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.issue-type {
  font-weight: 500;
  color: #303133;
}

.issue-details {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.issue-table {
  font-size: 14px;
  color: #303133;
}

.issue-column {
  font-size: 12px;
  color: #606266;
}

.issue-count {
  font-size: 12px;
  color: #909399;
}

.tables-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.table-card {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.table-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.table-stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.table-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.table-stat .stat-label {
  font-size: 12px;
  color: #606266;
}

.table-stat .stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.table-stat .stat-value.has-issues {
  color: #f56c6c;
}

.distribution-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: center;
}

.chart-container {
  height: 300px;
}

.chart {
  width: 100%;
  height: 100%;
}

.type-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.type-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.type-color {
  width: 16px;
  height: 16px;
  border-radius: 2px;
}

.type-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.type-name {
  font-weight: 500;
  color: #303133;
}

.type-count {
  font-size: 12px;
  color: #606266;
}

.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recommendation-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.recommendation-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
  flex-shrink: 0;
}

.rec-high { background: #f56c6c; }
.rec-medium { background: #e6a23c; }
.rec-low { background: #909399; }

.recommendation-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.recommendation-description {
  color: #606266;
  margin: 0 0 12px 0;
  line-height: 1.5;
}

.recommendation-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.recommendation-impact {
  font-size: 12px;
  color: #909399;
}

.error-state {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 40px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .header-right {
    align-self: stretch;
  }
  
  .overview-stats {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .stat-group {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .insights-grid {
    grid-template-columns: 1fr;
  }
  
  .insight-card {
    flex-direction: column;
    text-align: center;
  }
  
  .issues-summary {
    grid-template-columns: 1fr;
  }
  
  .tables-grid {
    grid-template-columns: 1fr;
  }
  
  .distribution-content {
    grid-template-columns: 1fr;
  }
  
  .recommendation-item {
    flex-direction: column;
    text-align: center;
  }
}
</style>