<template>
  <div class="datasource-list">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">Data Sources</h2>
        <p class="page-description">Manage your database connections and data sources</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="$router.push('/datasources/create')">
          <el-icon><Plus /></el-icon>
          Add Data Source
        </el-button>
      </div>
    </div>
    
    <!-- Filters -->
    <div class="card">
      <div class="filter-section">
        <div class="filter-row">
          <div class="filter-item">
            <el-input
              v-model="searchQuery"
              placeholder="Search data sources..."
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
              v-model="selectedType"
              placeholder="Filter by type"
              clearable
              @change="handleTypeFilter"
            >
              <el-option label="All Types" value="" />
              <el-option label="MySQL" value="mysql" />
              <el-option label="PostgreSQL" value="postgresql" />
              <el-option label="Oracle" value="oracle" />
              <el-option label="SQL Server" value="sqlserver" />
              <el-option label="MongoDB" value="mongodb" />
              <el-option label="Redis" value="redis" />
            </el-select>
          </div>
          
          <div class="filter-item">
            <el-button @click="refreshList">
              <el-icon><Refresh /></el-icon>
              Refresh
            </el-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Data Source Cards -->
    <div class="datasource-grid" v-loading="loading">
      <div
        v-for="datasource in filteredDataSources"
        :key="datasource.id"
        class="datasource-card"
      >
        <div class="card-header">
          <div class="datasource-info">
            <div class="datasource-icon" :style="{ background: getTypeGradient(datasource.type) }">
              <el-icon color="#fff" size="28">
                <component :is="getTypeIcon(datasource.type)" />
              </el-icon>
            </div>
            <div class="datasource-details">
              <div class="datasource-title-row">
                <h3 class="datasource-name">{{ datasource.name }}</h3>
                <el-tag 
                  :type="getStatusType(datasource.id)" 
                  size="small" 
                  class="connection-tag"
                >
                  <el-icon><CircleCheck v-if="getStatusType(datasource.id) === 'success'" /><Warning v-else /></el-icon>
                  {{ getConnectionStatus(datasource.id) }}
                </el-tag>
              </div>
              <div class="datasource-meta">
                <span class="datasource-type">{{ formatType(datasource.type) }}</span>
                <span class="datasource-id">ID: {{ datasource.sourceId }}</span>
              </div>
            </div>
          </div>
          
          <div class="card-actions">
            <el-dropdown @command="handleAction">
              <el-button type="text">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="{ action: 'test', sourceId: datasource.sourceId }">
                    <el-icon><Connection /></el-icon>
                    Test Connection
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'edit', id: datasource.id }">
                    <el-icon><Edit /></el-icon>
                    Edit
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'delete', id: datasource.id }" divided>
                    <el-icon><Delete /></el-icon>
                    Delete
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
        
        <div class="card-content">
          <div class="connection-info">
            <div class="info-item">
              <span class="label">Host:</span>
              <span class="value">{{ datasource.host }}:{{ datasource.port }}</span>
            </div>
            <div class="info-item">
              <span class="label">Database:</span>
              <span class="value">{{ datasource.database || 'N/A' }}</span>
            </div>
            <div class="info-item">
              <span class="label">Username:</span>
              <span class="value">{{ datasource.username }}</span>
            </div>
          </div>
          
          <div class="connection-status">
            <el-tag
              :type="getStatusType(datasource.id)"
              size="small"
            >
              {{ getConnectionStatus(datasource.id) }}
            </el-tag>
          </div>
        </div>
        
        <div class="card-footer">
          <div class="footer-info">
            <span class="created-time">
              Created: {{ formatDate(datasource.createdAt, 'date') }}
            </span>
          </div>
          
          <div class="footer-actions">
            <el-button
              type="primary"
              size="small"
              @click="createTask(datasource.id)"
            >
              <el-icon><Operation /></el-icon>
              Create Task
            </el-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Empty State -->
    <div v-if="!loading && filteredDataSources.length === 0" class="empty-state">
      <el-icon class="empty-state-icon"><Coin /></el-icon>
      <div class="empty-state-text">
        <h3>No data sources found</h3>
        <p>{{ searchQuery ? 'Try adjusting your search criteria' : 'Get started by adding your first data source' }}</p>
      </div>
      <el-button
        v-if="!searchQuery"
        type="primary"
        @click="$router.push('/datasources/create')"
      >
        <el-icon><Plus /></el-icon>
        Add Data Source
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Plus,
  Search,
  Refresh,
  MoreFilled,
  Connection,
  Edit,
  Delete,
  Operation,
  Coin,
  CircleCheck,
  Warning,
  Files,
  Monitor,
  Box,
  Grid
} from '@element-plus/icons-vue'
import { useDataSourceStore } from '@/stores/datasource'
import { formatDate, debounce } from '@/utils'
import type { DataSourceConfig } from '@/types'

const router = useRouter()
const dataSourceStore = useDataSourceStore()

const loading = ref(false)
const searchQuery = ref('')
const selectedType = ref('')

const filteredDataSources = computed(() => {
  let sources = dataSourceStore.dataSources
  
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    sources = sources.filter(ds => 
      ds.name.toLowerCase().includes(query) ||
      ds.host.toLowerCase().includes(query) ||
      ds.database?.toLowerCase().includes(query)
    )
  }
  
  if (selectedType.value) {
    sources = sources.filter(ds => ds.type === selectedType.value)
  }
  
  return sources
})

const getTypeIcon = (type: string) => {
  const icons: Record<string, any> = {
    mysql: Coin,
    postgresql: Coin,
    oracle: Coin,
    sqlserver: Monitor,
    mongodb: Files,
    redis: Box
  }
  return icons[type] || Grid
}

const getTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    mysql: '#00758f',
    postgresql: '#336791',
    oracle: '#f80000',
    sqlserver: '#cc2927',
    mongodb: '#4db33d',
    redis: '#dc382d'
  }
  return colors[type] || '#409eff'
}

const getTypeGradient = (type: string) => {
  const gradients: Record<string, string> = {
    mysql: 'linear-gradient(135deg, #00758f 0%, #0099cc 100%)',
    postgresql: 'linear-gradient(135deg, #336791 0%, #4a90e2 100%)',
    oracle: 'linear-gradient(135deg, #f80000 0%, #ff4444 100%)',
    sqlserver: 'linear-gradient(135deg, #cc2927 0%, #e74c3c 100%)',
    mongodb: 'linear-gradient(135deg, #4db33d 0%, #27ae60 100%)',
    redis: 'linear-gradient(135deg, #dc382d 0%, #e74c3c 100%)'
  }
  return gradients[type] || 'linear-gradient(135deg, #409eff 0%, #66b3ff 100%)'
}

const formatType = (type: string) => {
  const typeNames: Record<string, string> = {
    mysql: 'MySQL',
    postgresql: 'PostgreSQL',
    oracle: 'Oracle',
    sqlserver: 'SQL Server',
    mongodb: 'MongoDB',
    redis: 'Redis'
  }
  return typeNames[type] || type.toUpperCase()
}

const getConnectionStatus = (id: string) => {
  const testResult = dataSourceStore.testResults[id]
  if (!testResult) return 'Unknown'
  return testResult.success ? 'Connected' : 'Failed'
}

const getStatusType = (id: string) => {
  const testResult = dataSourceStore.testResults[id]
  if (!testResult) return 'info'
  return testResult.success ? 'success' : 'danger'
}

const handleSearch = debounce(() => {
  // Search is handled by computed property
}, 300)

const handleTypeFilter = () => {
  // Filter is handled by computed property
}

const refreshList = async () => {
  loading.value = true
  try {
    await dataSourceStore.fetchDataSources()
  } finally {
    loading.value = false
  }
}

const handleAction = async (command: { action: string; id?: string; sourceId?: string }) => {
  const { action, id, sourceId } = command
  
  switch (action) {
    case 'test':
      if (sourceId) {
        await testConnection(sourceId)
      }
      break
    case 'edit':
      if (id) {
        router.push(`/datasources/${id}/edit`)
      }
      break
    case 'delete':
      if (id) {
        await deleteDataSource(id)
      }
      break
  }
}

const testConnection = async (sourceId: string) => {
  try {
    await dataSourceStore.testConnection(sourceId)
    const result = dataSourceStore.testResults[sourceId]
    if (result?.success) {
      ElMessage.success('Connection test successful')
    } else {
      ElMessage.error(`Connection test failed: ${result?.message || 'Unknown error'}`)
    }
  } catch (error) {
    ElMessage.error('Failed to test connection')
  }
}

const deleteDataSource = async (id: string) => {
  try {
    const datasource = dataSourceStore.dataSources.find(ds => ds.id === id)
    if (!datasource) return
    
    await ElMessageBox.confirm(
      `Are you sure you want to delete "${datasource.name}"? This action cannot be undone.`,
      'Confirm Delete',
      {
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    await dataSourceStore.deleteDataSource(id)
    ElMessage.success('Data source deleted successfully')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Failed to delete data source')
    }
  }
}

const createTask = (datasourceId: string) => {
  router.push({
    path: '/tasks/create',
    query: { datasourceId }
  })
}

onMounted(async () => {
  try {
    await refreshList()
  } catch (error) {
    console.error('Failed to load data source list:', error)
    ElMessage.error('Failed to load data source list')
  }
})
</script>

<style scoped>
.datasource-list {
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

.filter-section {
  padding: 0;
}

.filter-row {
  display: flex;
  gap: 16px;
  align-items: center;
}

.filter-item {
  min-width: 200px;
}

.filter-item:last-child {
  min-width: auto;
}

.datasource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.datasource-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
}

.datasource-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 20px 0;
}

.datasource-info {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex: 1;
}

.datasource-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  position: relative;
  overflow: hidden;
}

.datasource-icon::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
}

.datasource-details {
  flex: 1;
  min-width: 0;
}

.datasource-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  gap: 12px;
}

.datasource-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.connection-tag {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.datasource-meta {
  display: flex;
  gap: 16px;
  align-items: center;
}

.datasource-type {
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.datasource-id {
  color: #909399;
  font-size: 12px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
}

.card-content {
  padding: 16px 20px;
}

.connection-info {
  margin-bottom: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.label {
  color: #909399;
  font-weight: 500;
}

.value {
  color: #303133;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

.connection-status {
  display: flex;
  justify-content: flex-end;
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

.created-time {
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
  
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-item {
    min-width: auto;
  }
  
  .datasource-grid {
    grid-template-columns: 1fr;
  }
  
  .card-footer {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
}
</style>