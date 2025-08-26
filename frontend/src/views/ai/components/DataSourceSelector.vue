<template>
  <div class="data-source-selector">
    <el-select
      :model-value="modelValue"
      placeholder="请选择数据源"
      :loading="loading"
      :disabled="loading || dataSources.length === 0"
      clearable
      filterable
      class="selector"
      @update:model-value="handleChange"
    >
      <el-option
        v-for="dataSource in dataSources"
        :key="dataSource.id"
        :label="dataSource.name"
        :value="dataSource.id"
      >
        <div class="option-content">
          <div class="option-main">
            <span class="option-name">{{ dataSource.name }}</span>
            <el-tag
              :type="getDataSourceTypeTag(dataSource.type)"
              size="small"
              class="option-tag"
            >
              {{ getDataSourceTypeLabel(dataSource.type) }}
            </el-tag>
          </div>
          <div class="option-description" v-if="dataSource.description">
            {{ dataSource.description }}
          </div>
        </div>
      </el-option>
    </el-select>
    
    <!-- Data Source Info -->
    <div v-if="selectedDataSource" class="data-source-info">
      <div class="info-item">
        <span class="info-label">类型:</span>
        <el-tag :type="getDataSourceTypeTag(selectedDataSource.type)" size="small">
          {{ getDataSourceTypeLabel(selectedDataSource.type) }}
        </el-tag>
      </div>
      <div class="info-item" v-if="selectedDataSource.host">
        <span class="info-label">主机:</span>
        <span class="info-value">{{ selectedDataSource.host }}:{{ selectedDataSource.port }}</span>
      </div>
      <div class="info-item" v-if="selectedDataSource.database">
        <span class="info-label">数据库:</span>
        <span class="info-value">{{ selectedDataSource.database }}</span>
      </div>
      <div class="info-item" v-if="selectedDataSource.description">
        <span class="info-label">描述:</span>
        <span class="info-value">{{ selectedDataSource.description }}</span>
      </div>
    </div>
    
    <!-- Empty State -->
    <div v-if="!loading && dataSources.length === 0" class="empty-state">
      <el-empty
        description="暂无可用数据源"
        :image-size="60"
      >
        <template #description>
          <span class="empty-description">请先配置数据源</span>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DataSource } from '@/types'

// Props
interface Props {
  modelValue: string | null
  loading?: boolean
  dataSources: DataSource[]
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

// Emits
interface Emits {
  (e: 'update:modelValue', value: string | null): void
  (e: 'change', value: string | null): void
}

const emit = defineEmits<Emits>()

// Computed
const selectedDataSource = computed(() => {
  if (!props.modelValue) return null
  return props.dataSources.find(ds => ds.id === props.modelValue) || null
})

// Methods
const handleChange = (value: string | null) => {
  emit('update:modelValue', value)
  emit('change', value)
}

const getDataSourceTypeLabel = (type: string): string => {
  const typeMap: Record<string, string> = {
    'mysql': 'MySQL',
    'postgresql': 'PostgreSQL',
    'oracle': 'Oracle',
    'sqlserver': 'SQL Server',
    'mongodb': 'MongoDB',
    'redis': 'Redis',
    'elasticsearch': 'Elasticsearch',
    'clickhouse': 'ClickHouse'
  }
  return typeMap[type.toLowerCase()] || type.toUpperCase()
}

const getDataSourceTypeTag = (type: string): string => {
  const tagMap: Record<string, string> = {
    'mysql': 'primary',
    'postgresql': 'success',
    'oracle': 'warning',
    'sqlserver': 'info',
    'mongodb': 'success',
    'redis': 'danger',
    'elasticsearch': 'warning',
    'clickhouse': 'info'
  }
  return tagMap[type.toLowerCase()] || 'info'
}
</script>

<style scoped>
.data-source-selector {
  width: 100%;
}

.selector {
  width: 100%;
}

.option-content {
  width: 100%;
}

.option-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.option-name {
  font-weight: 500;
  color: #303133;
}

.option-tag {
  margin-left: 8px;
}

.option-description {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.data-source-info {
  margin-top: 12px;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  min-width: 50px;
  margin-right: 8px;
}

.info-value {
  font-size: 13px;
  color: #303133;
  word-break: break-all;
}

.empty-state {
  margin-top: 20px;
  text-align: center;
}

.empty-description {
  color: #909399;
  font-size: 14px;
}

/* Responsive */
@media (max-width: 768px) {
  .option-main {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .option-tag {
    margin-left: 0;
  }
  
  .info-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .info-label {
    min-width: auto;
    margin-right: 0;
  }
}
</style>