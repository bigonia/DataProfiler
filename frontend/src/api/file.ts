import request from './request'
import type { FileMetadata, DataSourceConfig, PageResponse } from '@/types'

export const fileApi = {
  // Get file list with pagination
  list: (params?: {
    page?: number
    size?: number
  }) => {
    return request.get<PageResponse<FileMetadata>>('/files/list', { params })
  },

  // Search files by filename
  search: (params: {
    filename: string
    page?: number
    size?: number
  }) => {
    return request.get<PageResponse<FileMetadata>>('/files/search', { params })
  },

  // Get file by ID
  getById: (fileId: number) => {
    return request.get<FileMetadata>(`/files/${fileId}`)
  },

  // Upload file
  upload: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    
    return request.post<DataSourceConfig>('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // Delete file
  delete: (fileId: number) => {
    return request.delete(`/files/${fileId}`)
  },

  // Get file statistics
  getStatistics: () => {
    return request.get<{
      totalFiles: number
      totalSize: number
    }>('/files/statistics')
  }
}