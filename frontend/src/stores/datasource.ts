import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { dataSourceApi } from '@/api'
import type { DataSourceConfig, DataSourceTestResult } from '@/types'
import { ElMessage } from 'element-plus'

export const useDataSourceStore = defineStore('datasource', () => {
  // State
  const dataSources = ref<DataSourceConfig[]>([])
  const currentDataSource = ref<DataSourceConfig | null>(null)
  const loading = ref(false)
  const testResults = ref<Record<string, DataSourceTestResult>>({})

  // Getters
  const dataSourceCount = computed(() => dataSources.value.length)
  const dataSourcesByType = computed(() => {
    const grouped: Record<string, DataSourceConfig[]> = {}
    dataSources.value.forEach(ds => {
      if (!grouped[ds.type]) {
        grouped[ds.type] = []
      }
      grouped[ds.type].push(ds)
    })
    return grouped
  })

  // Actions
  const fetchDataSources = async () => {
    try {
      loading.value = true
      const response = await dataSourceApi.list()
      
      // Handle different response formats with defensive checks
      if (Array.isArray(response)) {
        dataSources.value = response
      } else if (response && typeof response === 'object' && Array.isArray(response.content)) {
        dataSources.value = response.content
      } else {
        dataSources.value = []
      }
    } catch (error) {
      console.error('Failed to fetch data sources:', error)
      ElMessage.error('Failed to load data sources')
      dataSources.value = []
    } finally {
      loading.value = false
    }
  }

  const getDataSourceById = async (id: string) => {
    try {
      loading.value = true
      const response = await dataSourceApi.getById(id)
      currentDataSource.value = response
      return response
    } catch (error) {
      console.error('Failed to fetch data source:', error)
      ElMessage.error('Failed to load data source')
      throw error
    } finally {
      loading.value = false
    }
  }

  const createDataSource = async (data: DataSourceConfig) => {
    try {
      loading.value = true
      const response = await dataSourceApi.create(data)
      dataSources.value.push(response)
      ElMessage.success('Data source created successfully')
      return response
    } catch (error) {
      console.error('Failed to create data source:', error)
      ElMessage.error('Failed to create data source')
      throw error
    } finally {
      loading.value = false
    }
  }

  const updateDataSource = async (id: string, data: DataSourceConfig) => {
    try {
      loading.value = true
      const response = await dataSourceApi.update(id, data)
      const index = dataSources.value.findIndex(ds => ds.id === id)
      if (index !== -1) {
        dataSources.value[index] = response
      }
      if (currentDataSource.value?.id === id) {
        currentDataSource.value = response
      }
      ElMessage.success('Data source updated successfully')
      return response
    } catch (error) {
      console.error('Failed to update data source:', error)
      ElMessage.error('Failed to update data source')
      throw error
    } finally {
      loading.value = false
    }
  }

  const deleteDataSource = async (id: string) => {
    try {
      loading.value = true
      await dataSourceApi.delete(id)
      dataSources.value = dataSources.value.filter(ds => ds.id !== id)
      if (currentDataSource.value?.id === id) {
        currentDataSource.value = null
      }
      ElMessage.success('Data source deleted successfully')
    } catch (error) {
      console.error('Failed to delete data source:', error)
      ElMessage.error('Failed to delete data source')
      throw error
    } finally {
      loading.value = false
    }
  }

  const testConnection = async (sourceId: string) => {
    try {
      loading.value = true
      const result = await dataSourceApi.test(sourceId)
      testResults.value[sourceId] = result
      
      if (result.success) {
        ElMessage.success(`Connection test successful: ${result.message}`)
      } else {
        ElMessage.error(`Connection test failed: ${result.message}`)
      }
      
      return result
    } catch (error) {
      console.error('Connection test failed:', error)
      ElMessage.error('Connection test failed')
      throw error
    } finally {
      loading.value = false
    }
  }

  const searchDataSources = async (query: string) => {
    try {
      loading.value = true
      const response = await dataSourceApi.search(query)
      return response
    } catch (error) {
      console.error('Failed to search data sources:', error)
      ElMessage.error('Search failed')
      throw error
    } finally {
      loading.value = false
    }
  }

  const getDataSourcesByType = async (type: string) => {
    try {
      loading.value = true
      const response = await dataSourceApi.getByType(type)
      return response
    } catch (error) {
      console.error('Failed to fetch data sources by type:', error)
      ElMessage.error('Failed to load data sources')
      throw error
    } finally {
      loading.value = false
    }
  }

  const clearCurrentDataSource = () => {
    currentDataSource.value = null
  }

  const clearTestResults = () => {
    testResults.value = {}
  }

  return {
    // State
    dataSources,
    currentDataSource,
    loading,
    testResults,
    
    // Getters
    dataSourceCount,
    dataSourcesByType,
    
    // Actions
    fetchDataSources,
    getDataSourceById,
    createDataSource,
    updateDataSource,
    deleteDataSource,
    testConnection,
    searchDataSources,
    getDataSourcesByType,
    clearCurrentDataSource,
    clearTestResults
  }
})