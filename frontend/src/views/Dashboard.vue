<template>
  <div class="dashboard">
    <!-- Statistics Cards -->
    <div class="stats-grid">
      <div class="stat-card" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
        <div class="stat-card-icon">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="stat-card-value">{{ dataSourceCount }}</div>
        <div class="stat-card-label">Data Sources</div>
      </div>
      
      <div class="stat-card" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
        <div class="stat-card-icon">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-card-value">{{ fileCount }}</div>
        <div class="stat-card-label">Files</div>
      </div>
      
      <div class="stat-card" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
        <div class="stat-card-icon">
          <el-icon><Operation /></el-icon>
        </div>
        <div class="stat-card-value">{{ taskCount }}</div>
        <div class="stat-card-label">Active Tasks</div>
      </div>
      
      <div class="stat-card" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
        <div class="stat-card-icon">
          <el-icon><DataBoard /></el-icon>
        </div>
        <div class="stat-card-value">{{ reportCount }}</div>
        <div class="stat-card-label">Reports</div>
      </div>
    </div>
    
    <!-- Charts Row -->
    <div class="charts-row">
      <div class="chart-card">
        <div class="card-header">
          <h3 class="card-title">Task Status Distribution</h3>
        </div>
        <div class="chart-container" ref="taskStatusChart"></div>
      </div>
      
      <div class="chart-card">
        <div class="card-header">
          <h3 class="card-title">Data Source Types</h3>
        </div>
        <div class="chart-container" ref="dataSourceChart"></div>
      </div>
    </div>
    
    <!-- Recent Activities -->
    <div class="activity-section">
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">Recent Activities</h3>
          <el-button type="text" @click="refreshActivities">
            <el-icon><Refresh /></el-icon>
            Refresh
          </el-button>
        </div>
        
        <div class="activity-list">
          <div
            v-for="activity in recentActivities"
            :key="activity.id"
            class="activity-item"
          >
            <div class="activity-icon">
              <el-icon :color="getActivityColor(activity.type)">
                <component :is="getActivityIcon(activity.type)" />
              </el-icon>
            </div>
            <div class="activity-content">
              <div class="activity-title">{{ activity.title }}</div>
              <div class="activity-description">{{ activity.description }}</div>
              <div class="activity-time">{{ formatDate(activity.timestamp, 'datetime') }}</div>
            </div>
            <div class="activity-status">
              <el-tag
                :type="getStatusType(activity.status)"
                size="small"
              >
                {{ activity.status }}
              </el-tag>
            </div>
          </div>
        </div>
        
        <div v-if="recentActivities.length === 0" class="empty-state">
          <el-icon class="empty-state-icon"><DocumentRemove /></el-icon>
          <div class="empty-state-text">No recent activities</div>
        </div>
      </div>
    </div>
    
    <!-- Quick Actions -->
    <div class="quick-actions">
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">Quick Actions</h3>
        </div>
        
        <div class="action-grid">
          <el-button
            type="primary"
            size="large"
            @click="$router.push('/datasources/create')"
          >
            <el-icon><Plus /></el-icon>
            Add Data Source
          </el-button>
          
          <el-button
            type="success"
            size="large"
            @click="$router.push('/tasks/create')"
          >
            <el-icon><Operation /></el-icon>
            Create Task
          </el-button>
          
          <el-button
            type="info"
            size="large"
            @click="$router.push('/files')"
          >
            <el-icon><Upload /></el-icon>
            Upload File
          </el-button>
          
          <el-button
            type="warning"
            size="large"
            @click="$router.push('/reports')"
          >
            <el-icon><View /></el-icon>
            View Reports
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  Coin,
  Document,
  Operation,
  DataBoard,
  Refresh,
  DocumentRemove,
  Plus,
  Upload,
  View,
  CircleCheck,
  Warning,
  CircleClose
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { formatDate } from '@/utils'
import { 
  getDashboardStats, 
  getTaskStatusDistribution, 
  getDataSourceTypesDistribution, 
  getRecentActivities,
  type DashboardStats,
  type StatusCount,
  type TypeCount,
  type Activity
} from '@/api/dashboard'

// Loading states
const loading = ref(false)
const chartsLoading = ref(false)

const taskStatusChart = ref<HTMLElement>()
const dataSourceChart = ref<HTMLElement>()

// Statistics
const dataSourceCount = ref(0)
const fileCount = ref(0)
const taskCount = ref(0)
const reportCount = ref(0)

// Dashboard data
const recentActivities = ref<Activity[]>([])
const taskStatusData = ref<StatusCount[]>([])
const dataSourceTypesData = ref<TypeCount[]>([])

const getActivityIcon = (type: string) => {
  switch (type) {
    case 'task':
      return Operation
    case 'datasource':
      return Coin
    case 'file':
      return Document
    case 'report':
      return DataBoard
    default:
      return CircleCheck
  }
}

const getActivityColor = (type: string) => {
  switch (type) {
    case 'task':
      return '#409eff'
    case 'datasource':
      return '#67c23a'
    case 'file':
      return '#e6a23c'
    case 'report':
      return '#f56c6c'
    default:
      return '#909399'
  }
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'completed':
    case 'active':
      return 'success'
    case 'failed':
    case 'error':
      return 'danger'
    case 'running':
    case 'pending':
      return 'warning'
    default:
      return 'info'
  }
}

const refreshActivities = async () => {
  try {
    loading.value = true
    const activities = await getRecentActivities(10)
    recentActivities.value = activities
    ElMessage.success('Activities refreshed successfully')
  } catch (error) {
    console.error('Failed to refresh activities:', error)
    ElMessage.error('Failed to refresh activities')
  } finally {
    loading.value = false
  }
}

const initCharts = () => {
  nextTick(() => {
    try {
      // Task Status Chart with real data
      if (taskStatusChart.value && taskStatusChart.value.offsetWidth > 0) {
        try {
          const taskChart = echarts.init(taskStatusChart.value)
          const taskOption = {
            tooltip: {
              trigger: 'item'
            },
            legend: {
              orient: 'vertical',
              left: 'left'
            },
            series: [
              {
                name: 'Task Status',
                type: 'pie',
                radius: '50%',
                data: taskStatusData.value.map(item => ({
                  value: item.value,
                  name: item.name
                })),
                emphasis: {
                  itemStyle: {
                    shadowBlur: 10,
                    shadowOffsetX: 0,
                    shadowColor: 'rgba(0, 0, 0, 0.5)'
                  }
                }
              }
            ]
          }
          taskChart.setOption(taskOption)
        } catch (error) {
          console.error('Failed to initialize task status chart:', error)
        }
      }
      
      // Data Source Chart with real data
      if (dataSourceChart.value && dataSourceChart.value.offsetWidth > 0) {
        try {
          const dsChart = echarts.init(dataSourceChart.value)
          const dsOption = {
            tooltip: {
              trigger: 'axis',
              axisPointer: {
                type: 'shadow'
              }
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              data: dataSourceTypesData.value.map(item => item.name)
            },
            yAxis: {
              type: 'value'
            },
            series: [
              {
                name: 'Count',
                type: 'bar',
                data: dataSourceTypesData.value.map(item => item.value),
                itemStyle: {
                  color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    { offset: 0, color: '#83bff6' },
                    { offset: 0.5, color: '#188df0' },
                    { offset: 1, color: '#188df0' }
                  ])
                }
              }
            ]
          }
          dsChart.setOption(dsOption)
        } catch (error) {
          console.error('Failed to initialize data source chart:', error)
        }
      }
    } catch (error) {
      console.error('Failed to initialize charts:', error)
    }
  })
}

const loadStatistics = async () => {
  try {
    loading.value = true
    
    // Load comprehensive dashboard statistics
    const dashboardStats = await getDashboardStats()
    
    // Update statistics counts
    dataSourceCount.value = dashboardStats.dataSourceCount
    fileCount.value = dashboardStats.fileCount
    taskCount.value = dashboardStats.taskCount
    reportCount.value = dashboardStats.reportCount
    
    // Update chart data
    taskStatusData.value = dashboardStats.taskStatusDistribution
    dataSourceTypesData.value = dashboardStats.dataSourceTypesDistribution
    
    // Update recent activities
    recentActivities.value = dashboardStats.recentActivities
    
    // Initialize charts with new data
    chartsLoading.value = true
    await nextTick()
    initCharts()
    chartsLoading.value = false
    
  } catch (error) {
    console.error('Failed to load dashboard statistics:', error)
    ElMessage.error('Failed to load dashboard data')
    
    // Set fallback values
    dataSourceCount.value = 0
    fileCount.value = 0
    taskCount.value = 0
    reportCount.value = 0
    taskStatusData.value = []
    dataSourceTypesData.value = []
    recentActivities.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStatistics()
  initCharts()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.activity-section {
  margin-bottom: 20px;
}

.activity-list {
  max-height: 400px;
  overflow-y: auto;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.activity-description {
  color: #606266;
  font-size: 14px;
  margin-bottom: 4px;
}

.activity-time {
  color: #909399;
  font-size: 12px;
}

.activity-status {
  flex-shrink: 0;
  margin-left: 16px;
}

.quick-actions {
  margin-bottom: 20px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.action-grid .el-button {
  height: 60px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

@media (max-width: 768px) {
  .charts-row {
    grid-template-columns: 1fr;
  }
  
  .action-grid {
    grid-template-columns: 1fr;
  }
}
</style>