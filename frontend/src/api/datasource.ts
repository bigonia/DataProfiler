import request from './request'
import type { DataSourceConfig, DataSourceTestResult } from '@/types'

export const dataSourceApi = {
  // Get all data sources
  list: () => {
    return request.get<DataSourceConfig[]>('/datasources')
  },

  // Get data source by ID
  getById: (id: string) => {
    return request.get<DataSourceConfig>(`/datasources/${id}`)
  },

  // Create new data source
  create: (data: DataSourceConfig) => {
    // Convert dataSourceType to uppercase to match backend enum
    if (data.type) {
      data.type = data.type.toUpperCase()
    }
    return request.post<DataSourceConfig>('/datasources', data)
  },

  // Update data source
  update: (id: string, data: DataSourceConfig) => {
    return request.put<DataSourceConfig>(`/datasources/${id}`, data)
  },

  // Delete data source
  delete: (id: string) => {
    return request.delete(`/datasources/${id}`)
  },

  // Test data source connection
  test: (sourceId: string) => {
    return request.post<DataSourceTestResult>(`/datasources/${sourceId}/test`)
  },

  // Get data sources by type
  getByType: (type: string) => {
    return request.get<DataSourceConfig[]>(`/datasources/type/${type}`)
  },

  // Search data sources
  search: (query: string) => {
    return request.get<DataSourceConfig[]>(`/datasources/search?q=${encodeURIComponent(query)}`)
  },

  // Get schemas for a data source
  getSchemas: (sourceId: string) => {
    return request.get<string[]>(`/datasources/${sourceId}/schemas`)
  },

  // Get tables for a data source and schema
  getTables: (sourceId: string, schema: string) => {
    return request.get<string[]>(`/datasources/${sourceId}/tables?schema=${encodeURIComponent(schema)}`)
  },

  // Get complete datasource info (schemas and tables)
  getDatasourceInfo: (sourceId: string) => {
    return request.get(`/datasources/${sourceId}/info`)
  },

  // Refresh datasource cache
  refreshCache: (sourceId: string) => {
    return request.post(`/datasources/${sourceId}/refresh-cache`)
  }
}