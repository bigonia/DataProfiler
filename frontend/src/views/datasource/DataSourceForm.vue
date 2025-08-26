<template>
  <div class="datasource-form">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <el-button type="text" @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          Back
        </el-button>
        <div class="header-info">
          <h2 class="page-title">{{ isEdit ? 'Edit Data Source' : 'Create Data Source' }}</h2>
          <p class="page-description">
            {{ isEdit ? 'Update your data source configuration' : 'Configure a new database connection' }}
          </p>
        </div>
      </div>
    </div>
    
    <!-- Form -->
    <div class="form-container">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        label-position="left"
        @submit.prevent="handleSubmit"
      >
        <!-- Basic Information -->
        <div class="form-section">
          <h3 class="form-section-title">Basic Information</h3>
          
          <el-form-item label="Name" prop="name">
            <el-input
              v-model="formData.name"
              placeholder="Enter a descriptive name for this data source"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>
          
          <el-form-item label="Type" prop="type">
            <el-select
              v-model="formData.type"
              placeholder="Select database type"
              @change="handleTypeChange"
              style="width: 100%"
            >
              <el-option
                v-for="type in databaseTypes"
                :key="type.value"
                :label="type.label"
                :value="type.value"
              >
                <div class="type-option">
                  <el-icon :color="type.color">
                    <Coin />
                  </el-icon>
                  <span>{{ type.label }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="Description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="3"
              placeholder="Optional description for this data source"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </div>
        
        <!-- Connection Settings -->
        <div class="form-section">
          <h3 class="form-section-title">Connection Settings</h3>
          
          <div class="connection-row">
            <el-form-item label="Host" prop="host" class="host-field">
              <el-input
                v-model="formData.host"
                placeholder="Database server hostname or IP"
              />
            </el-form-item>
            
            <el-form-item label="Port" prop="port" class="port-field">
              <el-input-number
                v-model="formData.port"
                :min="1"
                :max="65535"
                placeholder="Port"
                style="width: 100%"
              />
            </el-form-item>
          </div>
          
          <el-form-item
            v-if="showDatabaseField"
            label="Database"
            prop="database"
          >
            <el-input
              v-model="formData.database"
              placeholder="Database name (optional for some database types)"
            />
          </el-form-item>
          
          <div class="credentials-row">
            <el-form-item label="Username" prop="username" class="username-field">
              <el-input
                v-model="formData.username"
                placeholder="Database username"
                autocomplete="username"
              />
            </el-form-item>
            
            <el-form-item label="Password" prop="password" class="password-field">
              <el-input
                v-model="formData.password"
                type="password"
                placeholder="Database password"
                show-password
                autocomplete="new-password"
              />
            </el-form-item>
          </div>
        </div>
        
        <!-- Advanced Settings -->
        <div class="form-section">
          <h3 class="form-section-title">
            <span>Advanced Settings</span>
            <el-button type="text" @click="showAdvanced = !showAdvanced">
              <el-icon>
                <ArrowDown v-if="!showAdvanced" />
                <ArrowUp v-else />
              </el-icon>
              {{ showAdvanced ? 'Hide' : 'Show' }}
            </el-button>
          </h3>
          
          <div v-show="showAdvanced" class="advanced-settings">
            <el-form-item label="Connection URL">
              <el-input
                v-model="formData.connectionUrl"
                placeholder="Custom connection URL (optional)"
                type="textarea"
                :rows="2"
              />
              <div class="field-help">
                If provided, this will override the individual connection parameters above
              </div>
            </el-form-item>
            
            <el-form-item label="Extra Parameters">
              <el-input
                v-model="formData.extraParams"
                placeholder="Additional connection parameters (JSON format)"
                type="textarea"
                :rows="3"
              />
              <div class="field-help">
                JSON object with additional connection parameters, e.g., {"ssl": true, "timeout": 30}
              </div>
            </el-form-item>
          </div>
        </div>
        
        <!-- Form Actions -->
        <div class="form-actions">
          <el-button @click="$router.back()">Cancel</el-button>
          <el-button
            type="info"
            :loading="testing"
            @click="testConnection"
          >
            <el-icon><Connection /></el-icon>
            Test Connection
          </el-button>
          <el-button
            type="primary"
            :loading="submitting"
            @click="handleSubmit"
          >
            <el-icon><Check /></el-icon>
            {{ isEdit ? 'Update' : 'Create' }}
          </el-button>
        </div>
      </el-form>
    </div>
    
    <!-- Test Result Dialog -->
    <el-dialog
      v-model="showTestResult"
      title="Connection Test Result"
      width="500px"
    >
      <div class="test-result">
        <div class="result-status">
          <el-icon
            :color="testResult?.success ? '#67c23a' : '#f56c6c'"
            size="32"
          >
            <CircleCheck v-if="testResult?.success" />
            <CircleClose v-else />
          </el-icon>
          <h3 :class="testResult?.success ? 'success' : 'error'">
            {{ testResult?.success ? 'Connection Successful' : 'Connection Failed' }}
          </h3>
        </div>
        
        <div v-if="testResult?.message" class="result-message">
          <p>{{ testResult.message }}</p>
        </div>
        
        <div v-if="testResult?.details" class="result-details">
          <h4>Details:</h4>
          <pre>{{ testResult.details }}</pre>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showTestResult = false">Close</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  ArrowLeft,
  ArrowDown,
  ArrowUp,
  Coin,
  Connection,
  Check,
  CircleCheck,
  CircleClose
} from '@element-plus/icons-vue'
import { useDataSourceStore } from '@/stores/datasource'
import type { DataSourceConfig } from '@/types'

const route = useRoute()
const router = useRouter()
const dataSourceStore = useDataSourceStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const testing = ref(false)
const showAdvanced = ref(false)
const showTestResult = ref(false)

const isEdit = computed(() => !!route.params.id)
const datasourceId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id) : undefined
})

const databaseTypes = [
  { label: 'MySQL', value: 'mysql', color: '#00758f', defaultPort: 3306 },
  { label: 'PostgreSQL', value: 'postgresql', color: '#336791', defaultPort: 5432 },
  { label: 'Oracle', value: 'oracle', color: '#f80000', defaultPort: 1521 },
  { label: 'SQL Server', value: 'sqlserver', color: '#cc2927', defaultPort: 1433 },
  { label: 'MongoDB', value: 'mongodb', color: '#4db33d', defaultPort: 27017 },
  { label: 'Redis', value: 'redis', color: '#dc382d', defaultPort: 6379 }
]

const formData = reactive<Partial<DataSourceConfig>>({
  name: '',
  type: '',
  host: '',
  port: undefined,
  database: '',
  username: '',
  password: '',
  description: '',
  connectionUrl: '',
  extraParams: ''
})

const testResult = ref<{
  success: boolean
  message?: string
  details?: string
} | null>(null)

const showDatabaseField = computed(() => {
  return !['redis'].includes(formData.type || '')
})

const formRules: FormRules = {
  name: [
    { required: true, message: 'Please enter a name', trigger: 'blur' },
    { min: 2, max: 100, message: 'Name should be 2-100 characters', trigger: 'blur' }
  ],
  type: [
    { required: true, message: 'Please select a database type', trigger: 'change' }
  ],
  host: [
    { required: true, message: 'Please enter the host', trigger: 'blur' }
  ],
  port: [
    { required: true, message: 'Please enter the port', trigger: 'blur' },
    { type: 'number', min: 1, max: 65535, message: 'Port must be between 1-65535', trigger: 'blur' }
  ],
  username: [
    { required: true, message: 'Please enter the username', trigger: 'blur' }
  ],
  password: [
    { required: true, message: 'Please enter the password', trigger: 'blur' }
  ]
}

const handleTypeChange = (type: string) => {
  const dbType = databaseTypes.find(t => t.value === type)
  if (dbType && !formData.port) {
    formData.port = dbType.defaultPort
  }
}

const testConnection = async () => {
  if (!formRef.value) return
  
  try {
    const valid = await formRef.value.validate()
    if (!valid) {
      ElMessage.warning('Please fill in all required fields before testing')
      return
    }
  } catch {
    ElMessage.warning('Please fill in all required fields before testing')
    return
  }
  
  testing.value = true
  try {
    // Create a temporary datasource for testing
    const tempDatasource: DataSourceConfig = {
      id: 0, // Temporary ID
      name: formData.name!,
      type: formData.type!,
      host: formData.host!,
      port: formData.port!,
      database: formData.database,
      username: formData.username!,
      password: formData.password!,
      description: formData.description,
      connectionUrl: formData.connectionUrl,
      extraParams: formData.extraParams,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
    
    // Test connection using the store method
    await dataSourceStore.testConnection(0, tempDatasource)
    const result = dataSourceStore.testResults[0]
    
    testResult.value = {
      success: result?.success || false,
      message: result?.message,
      details: result?.details
    }
    
    showTestResult.value = true
  } catch (error) {
    testResult.value = {
      success: false,
      message: 'Connection test failed',
      details: error instanceof Error ? error.message : 'Unknown error'
    }
    showTestResult.value = true
  } finally {
    testing.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    const valid = await formRef.value.validate()
    if (!valid) return
  } catch {
    return
  }
  
  submitting.value = true
  try {
    const datasourceData: DataSourceConfig = {
      id: datasourceId.value?.toString(),
      name: formData.name!,
      type: formData.type!,
      properties: {
        host: formData.host || '',
        port: formData.port?.toString() || '',
        database: formData.database || '',
        username: formData.username || '',
        password: formData.password || '',
        description: formData.description || '',
        connectionUrl: formData.connectionUrl || '',
        extraParams: formData.extraParams || ''
      },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
    
    if (isEdit.value && datasourceId.value) {
      await dataSourceStore.updateDataSource(datasourceId.value, datasourceData)
      ElMessage.success('Data source updated successfully')
    } else {
      await dataSourceStore.createDataSource(datasourceData)
      ElMessage.success('Data source created successfully')
    }
    
    router.push('/datasources')
  } catch (error) {
    ElMessage.error(isEdit.value ? 'Failed to update data source' : 'Failed to create data source')
  } finally {
    submitting.value = false
  }
}

const loadDataSource = async () => {
  if (!isEdit.value || !datasourceId.value) return
  
  try {
    const datasource = await dataSourceStore.getDataSourceById(datasourceId.value)
    if (datasource) {
      Object.assign(formData, datasource)
    } else {
      ElMessage.error('Data source not found')
      router.push('/datasources')
    }
  } catch (error) {
    ElMessage.error('Failed to load data source')
    router.push('/datasources')
  }
}

onMounted(() => {
  if (isEdit.value) {
    loadDataSource()
  }
})
</script>

<style scoped>
.datasource-form {
  padding: 0;
}

.page-header {
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.header-info {
  flex: 1;
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

.form-container {
  max-width: 800px;
  margin: 0 auto;
}

.form-section {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 24px;
  margin-bottom: 20px;
}

.form-section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
}

.connection-row,
.credentials-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.credentials-row {
  grid-template-columns: 1fr 1fr;
}

.type-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.field-help {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

.advanced-settings {
  border-top: 1px solid #ebeef5;
  padding-top: 20px;
  margin-top: 20px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  position: sticky;
  bottom: 20px;
}

.test-result {
  text-align: center;
}

.result-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.result-status h3 {
  margin: 0;
  font-size: 18px;
}

.result-status h3.success {
  color: #67c23a;
}

.result-status h3.error {
  color: #f56c6c;
}

.result-message {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  text-align: left;
}

.result-details {
  text-align: left;
}

.result-details h4 {
  margin-bottom: 8px;
  color: #303133;
}

.result-details pre {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.4;
  overflow-x: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}

@media (max-width: 768px) {
  .header-left {
    flex-direction: column;
    gap: 12px;
  }
  
  .connection-row,
  .credentials-row {
    grid-template-columns: 1fr;
    gap: 0;
  }
  
  .form-actions {
    flex-direction: column;
    position: static;
  }
}
</style>