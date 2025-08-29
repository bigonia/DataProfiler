<template>
  <div class="task-create">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <el-button text @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          Back
        </el-button>
        <h2 class="page-title">Create Profiling Task</h2>
        <p class="page-description">Configure a new data profiling task</p>
      </div>
    </div>
    
    <div class="form-container">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="140px"
        size="large"
      >
        <!-- Basic Information -->
        <div class="form-section">
          <h3 class="section-title">Basic Information</h3>
          
          <el-form-item label="Task Name" prop="taskName">
            <el-input
              v-model="form.taskName"
              placeholder="Enter task name"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>
          
          <el-form-item label="Description" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="Enter task description (optional)"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </div>
        
        <!-- Data Source Selection -->
        <div class="form-section">
          <h3 class="section-title">Data Source</h3>
          
          <el-form-item label="Data Source" prop="dataSourceIds">
            <el-select
              v-model="form.dataSourceIds"
              placeholder="Select data sources"
              style="width: 100%"
              multiple
              collapse-tags
              collapse-tags-tooltip
              @change="handleDataSourceChange"
            >
              <el-option
                v-for="ds in availableDataSources"
                :key="ds.sourceId"
                :label="ds.name"
                :value="ds.sourceId"
              >
                <div class="datasource-option">
                  <div class="option-info">
                    <span class="option-name">{{ ds.name }}</span>
                    <span class="option-type">{{ ds.type }}</span>
                  </div>
                  <el-tag
                    :type="ds.status === 'connected' ? 'success' : 'warning'"
                    size="small"
                  >
                    {{ ds.status }}
                  </el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          
          <div v-if="selectedDataSources.length > 0" class="datasource-info">
            <div v-for="ds in selectedDataSources" :key="ds.sourceId" class="info-card">
              <div class="info-header">
                <el-icon><Coin /></el-icon>
                <span>{{ ds.name }}</span>
              </div>
              <div class="info-details">
                <div class="detail-item">
                  <span class="label">Type:</span>
                  <span class="value">{{ ds.type }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">Host:</span>
                  <span class="value">{{ ds.host }}:{{ ds.port }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">Database:</span>
                  <span class="value">{{ ds.database }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Advanced Configuration -->
        <div class="form-section">
          <el-collapse v-model="activeCollapse">
            <el-collapse-item title="Advanced Configuration" name="advanced">
              <div class="advanced-config">
                <el-form-item label="Field Max Length" prop="fieldMaxLength">
                  <el-input-number
                    v-model="form.fieldMaxLength"
                    :min="32"
                    :max="1024"
                    :step="32"
                    placeholder="128"
                    style="width: 200px"
                  />
                  <div class="field-hint">Maximum length for field content display (default: 128)</div>
                </el-form-item>
                
                <el-form-item label="Sample Data Limit" prop="sampleDataLimit">
                  <el-input-number
                    v-model="form.sampleDataLimit"
                    :min="5"
                    :max="100"
                    :step="5"
                    placeholder="10"
                    style="width: 200px"
                  />
                  <div class="field-hint">Maximum number of sample data rows (default: 10)</div>
                </el-form-item>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
        
        <!-- Schema & Table Selection -->
        <el-card v-if="form.dataSourceIds.length > 0" class="datasource-selection">
          <template #header>
            <div class="card-header">
              <span>Schema & Table Selection</span>
              <el-tag type="info">{{ selectedTableCount }} tables selected</el-tag>
            </div>
          </template>

          <!-- Selection Mode -->
          <el-form-item label="Selection Mode">
            <el-radio-group v-model="tableSelectionMode" @change="handleSelectionModeChange">
              <el-radio value="all">All Tables</el-radio>
              <el-radio value="specific">Specific Tables</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- Data Source Configurations -->
          <div v-if="tableSelectionMode === 'specific'" class="datasource-configs">
            <div v-for="dataSource in selectedDataSources" :key="dataSource.sourceId" class="datasource-config">
              <div class="datasource-header">
                <h4>{{ dataSource.name }} ({{ dataSource.type }})</h4>
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="loadSchemasForDataSource(dataSource.sourceId)"
                  :loading="loadingSchemas[dataSource.sourceId]"
                >
                  Load Schemas
                </el-button>
              </div>

              <div class="schema-list">
                <div v-if="form.dataSourceConfigs[dataSource.sourceId]?.schemas && Object.keys(form.dataSourceConfigs[dataSource.sourceId].schemas).length > 0" class="schema-items">
                  <div v-for="(tables, schemaName) in form.dataSourceConfigs[dataSource.sourceId].schemas" :key="`${dataSource.sourceId}-${schemaName}`" class="schema-item">
                    <div class="schema-header">
                      <el-checkbox 
                        :model-value="tables.length > 0"
                        @change="toggleSchemaSelection(dataSource.sourceId, schemaName)"
                      >
                        {{ schemaName }}
                        <span class="table-count">({{ getOriginalTableCount(dataSource.sourceId, schemaName) }} tables, {{ tables.length }} selected)</span>
                      </el-checkbox>
                      <el-button 
                        type="text" 
                        size="small"
                        @click="toggleSchemaExpansion(dataSource.sourceId, schemaName)"
                      >
                        <el-icon>
                          <ArrowUp v-if="isSchemaExpanded(dataSource.sourceId, schemaName)" />
                          <ArrowDown v-else />
                        </el-icon>
                        {{ isSchemaExpanded(dataSource.sourceId, schemaName) ? 'Collapse' : 'Expand' }}
                      </el-button>
                    </div>
                    
                    <!-- Expanded table list -->
                    <div v-if="isSchemaExpanded(dataSource.sourceId, schemaName)" class="table-list">
                      <div class="table-list-header">
                        <el-checkbox 
                          :model-value="areAllTablesSelected(dataSource.sourceId, schemaName)"
                          :indeterminate="isSomeTablesSelected(dataSource.sourceId, schemaName)"
                          @change="toggleAllTablesInSchema(dataSource.sourceId, schemaName)"
                        >
                          Select All Tables
                        </el-checkbox>
                      </div>
                      <div class="table-grid">
                        <div 
                          v-for="tableName in getOriginalTables(dataSource.sourceId, schemaName)" 
                          :key="`${dataSource.sourceId}-${schemaName}-${tableName}`" 
                          class="table-checkbox"
                        >
                          <el-checkbox 
                            :model-value="isTableSelected(dataSource.sourceId, schemaName, tableName)"
                            @change="toggleTableSelection(dataSource.sourceId, schemaName, tableName)"
                          >
                            <div class="table-info">
                              <span class="table-name">{{ tableName }}</span>
                            </div>
                          </el-checkbox>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-else class="empty-state">
                  <el-empty description="No schemas loaded. Click 'Load Schemas' to get available schemas." />
                </div>
              </div>
            </div>
          </div>

          <!-- All Tables Mode Info -->
          <div v-else class="all-tables-info">
            <el-alert
              title="All Tables Mode"
              description="All available tables from selected data sources will be included in the profiling task."
              type="info"
              show-icon
              :closable="false"
            />
          </div>
        </el-card>

        <!-- Submit Actions -->
        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitting" size="large">
            Create Task
          </el-button>
          <el-button @click="resetForm" size="large">Reset</el-button>
          <el-button @click="$router.push('/tasks')" size="large">Cancel</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  ArrowLeft,
  ArrowUp,
  ArrowDown,
  Coin,
  Refresh,
  Loading,
  Warning
} from '@element-plus/icons-vue'
import { useTaskStore } from '@/stores/task'
import { useDataSourceStore } from '@/stores/datasource'
import { formatNumber } from '@/utils'
import type { DataSource, TableInfo, DataSourceInfo } from '@/types'
import { dataSourceApi } from '@/api'

const router = useRouter()
const taskStore = useTaskStore()
const dataSourceStore = useDataSourceStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const loadingTables = ref<Record<string, boolean>>({})
const loadingSchemas = ref<Record<string, boolean>>({})
const tableSelectionMode = ref('all')
const selectAllTables = ref(false)
const availableTables = ref<TableInfo[]>([])
const activeCollapse = ref<string[]>([])
const expandedSchemas = ref<Record<string, boolean>>({})

const form = ref({
  taskName: '',
  description: '',
  dataSourceIds: [] as string[],
  dataSourceConfigs: {} as Record<string, {
    schemas: Record<string, string[]>
    originalSchemas?: Record<string, string[]>
  }>,
  fieldMaxLength: 128,
  sampleDataLimit: 10
})

const formRules = {
  taskName: [
    { required: true, message: 'Please enter task name', trigger: 'blur' },
    { min: 2, max: 50, message: 'Task name should be 2-50 characters', trigger: 'blur' }
  ],
  dataSourceIds: [
    { required: true, message: 'Please select at least one data source', trigger: 'change' }
  ],
  fieldMaxLength: [
    { type: 'number', min: 32, max: 1024, message: 'Field max length must be between 32 and 1024', trigger: 'blur' }
  ],
  sampleDataLimit: [
    { type: 'number', min: 5, max: 100, message: 'Sample data limit must be between 5 and 100', trigger: 'blur' }
  ]
}

const availableDataSources = computed(() => {
  // Remove status filter to show all data sources
  return dataSourceStore.dataSources
})

const selectedDataSources = computed(() => {
  return dataSourceStore.dataSources.filter(ds => 
    form.value.dataSourceIds.includes(ds.sourceId)
  )
})

const selectedTableCount = computed(() => {
  let totalTables = 0
  Object.values(form.value.dataSourceConfigs).forEach(config => {
    Object.values(config.schemas).forEach(tables => {
      totalTables += tables.length
    })
  })
  return totalTables
})

const isIndeterminate = computed(() => {
  const count = selectedTableCount.value
  return count > 0 && count < availableTables.value.length
})

const disabledDate = (time: Date) => {
  return time.getTime() < Date.now() - 24 * 60 * 60 * 1000
}

const handleDataSourceChange = () => {
  // Reset configurations for removed data sources
  const newConfigs: Record<string, { schemas: Record<string, string[]> }> = {}
  form.value.dataSourceIds.forEach(sourceId => {
    if (form.value.dataSourceConfigs[sourceId]) {
      newConfigs[sourceId] = form.value.dataSourceConfigs[sourceId]
    } else {
      newConfigs[sourceId] = { schemas: {} }
    }
  })
  form.value.dataSourceConfigs = newConfigs
  
  // Load schemas for newly selected data sources
  form.value.dataSourceIds.forEach(sourceId => {
    if (!form.value.dataSourceConfigs[sourceId] || Object.keys(form.value.dataSourceConfigs[sourceId].schemas).length === 0) {
      loadSchemasForDataSource(sourceId)
    }
  })
}

const handleSelectionModeChange = () => {
  if (tableSelectionMode.value === 'all') {
    // Clear all specific selections
    Object.keys(form.value.dataSourceConfigs).forEach(sourceId => {
      Object.keys(form.value.dataSourceConfigs[sourceId].schemas).forEach(schema => {
        form.value.dataSourceConfigs[sourceId].schemas[schema] = []
      })
    })
  }
}

const loadSchemasForDataSource = async (sourceId: string) => {
  try {
    loadingSchemas.value[sourceId] = true
    const datasourceInfo: DataSourceInfo = await dataSourceApi.getDatasourceInfo(sourceId)
    
    // Initialize schema configuration for this data source
    if (!form.value.dataSourceConfigs[sourceId]) {
      form.value.dataSourceConfigs[sourceId] = { schemas: {} }
    }
    
    // Use the complete schema and table information from the unified API
    form.value.dataSourceConfigs[sourceId].schemas = datasourceInfo.schemas
    // Save original schemas for toggle functionality
    form.value.dataSourceConfigs[sourceId].originalSchemas = JSON.parse(JSON.stringify(datasourceInfo.schemas))
    
    ElMessage.success(`Loaded ${datasourceInfo.schemaCount} schemas with ${datasourceInfo.totalTableCount} tables total`)
  } catch (error) {
    console.error(`Failed to load datasource info for ${sourceId}:`, error)
    ElMessage.error(`Failed to load datasource information`)
  } finally {
    loadingSchemas.value[sourceId] = false
  }
}

const loadTablesForSchema = async (sourceId: string, schema: string) => {
  // Tables are already loaded with the datasource info, just return them
  const config = form.value.dataSourceConfigs[sourceId]
  if (config && config.schemas[schema]) {
    return config.schemas[schema]
  }
  return []
}

const toggleSchemaSelection = (sourceId: string, schema: string) => {
  if (!form.value.dataSourceConfigs[sourceId]) return
  
  const config = form.value.dataSourceConfigs[sourceId]
  const currentTables = config.schemas[schema] || []
  const isSelected = currentTables.length > 0
  
  if (isSelected) {
    // Deselect all tables in this schema
    config.schemas[schema] = []
  } else {
    // Select all tables in this schema from original data
    const originalTables = config.originalSchemas?.[schema] || []
    config.schemas[schema] = [...originalTables]
  }
}

// Schema expansion methods
const toggleSchemaExpansion = (sourceId: string, schema: string) => {
  const key = `${sourceId}-${schema}`
  expandedSchemas.value[key] = !expandedSchemas.value[key]
}

const isSchemaExpanded = (sourceId: string, schema: string) => {
  const key = `${sourceId}-${schema}`
  return expandedSchemas.value[key] || false
}

// Table selection methods
const getOriginalTables = (sourceId: string, schema: string) => {
  const config = form.value.dataSourceConfigs[sourceId]
  return config?.originalSchemas?.[schema] || []
}

const getOriginalTableCount = (sourceId: string, schema: string) => {
  return getOriginalTables(sourceId, schema).length
}

const isTableSelected = (sourceId: string, schema: string, tableName: string) => {
  const config = form.value.dataSourceConfigs[sourceId]
  const selectedTables = config?.schemas[schema] || []
  return selectedTables.includes(tableName)
}

const toggleTableSelection = (sourceId: string, schema: string, tableName: string) => {
  if (!form.value.dataSourceConfigs[sourceId]) return
  
  const config = form.value.dataSourceConfigs[sourceId]
  const selectedTables = config.schemas[schema] || []
  const isSelected = selectedTables.includes(tableName)
  
  if (isSelected) {
    // Remove table from selection
    config.schemas[schema] = selectedTables.filter(t => t !== tableName)
  } else {
    // Add table to selection
    config.schemas[schema] = [...selectedTables, tableName]
  }
}

const areAllTablesSelected = (sourceId: string, schema: string) => {
  const originalTables = getOriginalTables(sourceId, schema)
  const selectedTables = form.value.dataSourceConfigs[sourceId]?.schemas[schema] || []
  return originalTables.length > 0 && selectedTables.length === originalTables.length
}

const isSomeTablesSelected = (sourceId: string, schema: string) => {
  const originalTables = getOriginalTables(sourceId, schema)
  const selectedTables = form.value.dataSourceConfigs[sourceId]?.schemas[schema] || []
  return selectedTables.length > 0 && selectedTables.length < originalTables.length
}

const toggleAllTablesInSchema = (sourceId: string, schema: string) => {
  if (!form.value.dataSourceConfigs[sourceId]) return
  
  const config = form.value.dataSourceConfigs[sourceId]
  const originalTables = getOriginalTables(sourceId, schema)
  const allSelected = areAllTablesSelected(sourceId, schema)
  
  if (allSelected) {
    // Deselect all tables
    config.schemas[schema] = []
  } else {
    // Select all tables
    config.schemas[schema] = [...originalTables]
  }
}

const loadTables = async () => {
  if (!form.value.dataSourceId) return
  
  loadingTables.value = true
  try {
    // In a real implementation, you would call an API to get table information
    // For now, we'll simulate loading tables
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // Mock table data
    availableTables.value = [
      { name: 'users', rowCount: 15420 },
      { name: 'orders', rowCount: 89234 },
      { name: 'products', rowCount: 2341 },
      { name: 'categories', rowCount: 45 },
      { name: 'reviews', rowCount: 12890 },
      { name: 'inventory', rowCount: 5678 }
    ]
  } catch (error) {
    ElMessage.error('Failed to load tables')
    availableTables.value = []
  } finally {
    loadingTables.value = false
  }
}

const handleSelectAll = (checked: boolean) => {
  if (checked) {
    form.value.tableNames = availableTables.value.map(table => table.name)
  } else {
    form.value.tableNames = []
  }
}

const handleTableSelection = () => {
  const count = selectedTableCount.value
  selectAllTables.value = count === availableTables.value.length
}

const submitForm = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    submitting.value = true
    
    // Build data source configurations according to ProfilingTaskRequest format
    const datasources: Record<string, { schemas: Record<string, string[]> }> = {}
    
    if (tableSelectionMode.value === 'all') {
      // For "all tables" mode, we need to get all schemas and tables for each data source
      for (const sourceId of form.value.dataSourceIds) {
        datasources[sourceId] = { schemas: {} }
        
        try {
          const datasourceInfo: DataSourceInfo = await dataSourceApi.getDatasourceInfo(sourceId)
          datasources[sourceId].schemas = datasourceInfo.schemas
        } catch (error) {
          console.error(`Failed to load datasource info for ${sourceId}:`, error)
          // Continue with empty configuration for this data source
          datasources[sourceId] = { schemas: {} }
        }
      }
    } else {
      // For "specific tables" mode, use the selected configurations
      for (const sourceId of form.value.dataSourceIds) {
        const config = form.value.dataSourceConfigs[sourceId]
        if (config) {
          datasources[sourceId] = { schemas: config.schemas }
        } else {
          datasources[sourceId] = { schemas: {} }
        }
      }
    }
    
    const taskData = {
      datasources: datasources,
      fieldMaxLength: form.value.fieldMaxLength,
      sampleDataLimit: form.value.sampleDataLimit
    }
    
    await taskStore.createTask(taskData)
    
    ElMessage.success('Task created successfully')
    
    router.push('/tasks')
  } catch (error) {
    if (error !== 'validation failed') {
      ElMessage.error('Failed to create task')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    await dataSourceStore.fetchDataSources()
  } catch (error) {
    console.error('Failed to load data sources:', error)
    ElMessage.error('Failed to load data sources')
  }
})

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  
  // Reset form data
  form.value = {
    taskName: '',
    description: '',
    dataSourceIds: [],
    dataSourceConfigs: {},
    fieldMaxLength: 128,
    sampleDataLimit: 10
  }
  
  // Reset selection mode
  tableSelectionMode.value = 'all'
  
  // Clear loading states
  loadingSchemas.value = {}
  loadingTables.value = {}
  
  ElMessage.success('Form has been reset')
}
</script>

<style scoped>
.task-create {
  padding: 0;
}

.page-header {
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
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

.form-container {
  max-width: 800px;
}

.form-section {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 24px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.datasource-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.option-info {
  display: flex;
  flex-direction: column;
}

.option-name {
  font-weight: 500;
  color: #303133;
}

.option-type {
  font-size: 12px;
  color: #909399;
}

.datasource-info {
  margin-top: 16px;
}

.info-card {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
}

.info-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 12px;
}

.info-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 8px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.label {
  color: #909399;
}

.value {
  color: #303133;
  font-weight: 500;
}

.table-selection {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
}

.datasource-config {
  border-bottom: 1px solid #ebeef5;
}

.datasource-config:last-child {
  border-bottom: none;
}

.datasource-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.datasource-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.schema-list {
  padding: 16px;
}

.schema-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.schema-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.schema-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

.table-count {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.table-list {
  padding: 16px;
  background: #fff;
}

.table-list-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.loading-state,
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #909399;
}

.checkbox-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 12px;
}

.table-checkbox {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px;
  transition: all 0.2s;
}

.table-checkbox:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.table-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.table-name {
  font-weight: 500;
  color: #303133;
}

.table-rows {
  font-size: 12px;
  color: #909399;
}

.advanced-config {
  padding: 16px 0;
}

.advanced-config .el-form-item {
  margin-bottom: 24px;
}

.field-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

.el-collapse {
  border: none;
}

.el-collapse-item__header {
  background: #f8f9fa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px 16px;
  font-weight: 500;
  color: #303133;
}

.el-collapse-item__wrap {
  border: none;
  background: #fff;
}

.el-collapse-item__content {
  padding: 16px 0 0 0;
  border: 1px solid #e4e7ed;
  border-top: none;
  border-radius: 0 0 6px 6px;
  background: #fafbfc;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

@media (max-width: 768px) {
  .form-container {
    max-width: none;
  }
  
  .selection-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .info-details {
    grid-template-columns: 1fr;
  }
  
  .checkbox-grid {
    grid-template-columns: 1fr;
  }
  
  .form-actions {
    flex-direction: column;
  }
}
</style>