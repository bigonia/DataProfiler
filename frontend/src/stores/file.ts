import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { fileApi } from '@/api'
import type { FileMetadata, DataSourceConfig } from '@/types'
import { ElMessage } from 'element-plus'

export const useFileStore = defineStore('file', () => {
  // State
  const files = ref<FileMetadata[]>([])
  const currentFile = ref<FileMetadata | null>(null)
  const loading = ref(false)
  const uploadProgress = ref(0)
  const uploading = ref(false)
  const fileCount = ref(0)

  // Getters
  const localFileCount = computed(() => files.value.length)
  const totalFileSize = computed(() => 
    files.value.reduce((total, file) => total + file.fileSize, 0)
  )
  const filesByType = computed(() => {
    const grouped: Record<string, FileMetadata[]> = {}
    files.value.forEach(file => {
      const type = file.mimeType.split('/')[0] || 'unknown'
      if (!grouped[type]) {
        grouped[type] = []
      }
      grouped[type].push(file)
    })
    return grouped
  })

  // Actions
  const fetchFiles = async (page: number = 1, size: number = 20) => {
    try {
      loading.value = true
      const response = await fileApi.list({ page: page - 1, size }) // Backend uses 0-based pagination
      
      if (response && response.content) {
        files.value = response.content
        fileCount.value = response.totalElements
      } else {
        files.value = []
        fileCount.value = 0
      }
      
      return response
    } catch (error) {
      console.error('Failed to fetch files:', error)
      ElMessage.error('Failed to load files')
      files.value = []
      fileCount.value = 0
      throw error
    } finally {
      loading.value = false
    }
  }

  const searchFiles = async (filename: string, page: number = 1, size: number = 20) => {
    try {
      loading.value = true
      const response = await fileApi.search({ filename, page: page - 1, size })
      
      if (response && response.content) {
        files.value = response.content
        fileCount.value = response.totalElements
      } else {
        files.value = []
        fileCount.value = 0
      }
      
      return response
    } catch (error) {
      console.error('Failed to search files:', error)
      ElMessage.error('Failed to search files')
      files.value = []
      fileCount.value = 0
      throw error
    } finally {
      loading.value = false
    }
  }

  const getFileById = async (fileId: number) => {
    try {
      const response = await fileApi.getById(fileId)
      currentFile.value = response
      return response
    } catch (error) {
      console.error('Failed to get file by ID:', error)
      ElMessage.error('Failed to get file details')
      throw error
    }
  }

  const getFileStatistics = async () => {
    try {
      const response = await fileApi.getStatistics()
      return response
    } catch (error) {
      console.error('Failed to get file statistics:', error)
      ElMessage.error('Failed to get file statistics')
      throw error
    }
  }

  const uploadFile = async (file: File, onProgress?: (progress: number) => void) => {
    try {
      uploading.value = true
      uploadProgress.value = 0
      
      // Create a custom axios config for upload progress
      const response = await fileApi.upload(file)
      
      // Add the new data source to the files list if it's a file type
      if (response.type === 'FILE') {
        // Refresh the file list to get the latest files
        await fetchFiles()
      }
      
      uploadProgress.value = 100
      ElMessage.success(`File "${file.name}" uploaded successfully`)
      return response
    } catch (error) {
      console.error('Failed to upload file:', error)
      ElMessage.error(`Failed to upload file "${file.name}"`)
      throw error
    } finally {
      uploading.value = false
      uploadProgress.value = 0
    }
  }

  const uploadMultipleFiles = async (files: File[], onProgress?: (progress: number) => void) => {
    const results: DataSourceConfig[] = []
    const errors: { file: File; error: any }[] = []
    
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      try {
        const result = await uploadFile(file)
        results.push(result)
        
        if (onProgress) {
          onProgress(Math.round(((i + 1) / files.length) * 100))
        }
      } catch (error) {
        errors.push({ file, error })
      }
    }
    
    if (errors.length > 0) {
      const errorMessage = `${errors.length} file(s) failed to upload`
      ElMessage.warning(errorMessage)
    }
    
    if (results.length > 0) {
      ElMessage.success(`${results.length} file(s) uploaded successfully`)
    }
    
    return { results, errors }
  }

  const deleteFile = async (fileId: number) => {
    try {
      loading.value = true
      await fileApi.delete(fileId)
      
      // Remove from files list
      files.value = files.value.filter(file => file.id !== fileId)
      
      // Clear current file if it matches
      if (currentFile.value?.id === fileId) {
        currentFile.value = null
      }
      
      ElMessage.success('File deleted successfully')
    } catch (error) {
      console.error('Failed to delete file:', error)
      ElMessage.error('Failed to delete file')
      throw error
    } finally {
      loading.value = false
    }
  }

  const deleteMultipleFiles = async (fileIds: number[]) => {
    const results: number[] = []
    const errors: { fileId: number; error: any }[] = []
    
    for (const fileId of fileIds) {
      try {
        await fileApi.delete(fileId)
        results.push(fileId)
      } catch (error) {
        errors.push({ fileId, error })
      }
    }
    
    // Update files list
    files.value = files.value.filter(file => !results.includes(file.id))
    
    // Clear current file if it was deleted
    if (currentFile.value && results.includes(currentFile.value.id)) {
      currentFile.value = null
    }
    
    if (errors.length > 0) {
      ElMessage.warning(`${errors.length} file(s) failed to delete`)
    }
    
    if (results.length > 0) {
      ElMessage.success(`${results.length} file(s) deleted successfully`)
    }
    
    return { results, errors }
  }

  const setCurrentFile = (file: FileMetadata | null) => {
    currentFile.value = file
  }

  const addFile = (file: FileMetadata) => {
    const existingIndex = files.value.findIndex(f => f.id === file.id)
    if (existingIndex !== -1) {
      files.value[existingIndex] = file
    } else {
      files.value.unshift(file)
    }
  }

  const filterFiles = (query: string) => {
    if (!query.trim()) {
      return files.value
    }
    
    const lowerQuery = query.toLowerCase()
    return files.value.filter(file => 
      file.filename.toLowerCase().includes(lowerQuery) ||
      file.originalFilename.toLowerCase().includes(lowerQuery) ||
      file.mimeType.toLowerCase().includes(lowerQuery)
    )
  }

  const getFilesByType = (mimeType: string) => {
    return files.value.filter(file => file.mimeType === mimeType)
  }

  const getFilesByDateRange = (startDate: Date, endDate: Date) => {
    return files.value.filter(file => {
      const uploadDate = new Date(file.uploadedAt)
      return uploadDate >= startDate && uploadDate <= endDate
    })
  }

  const clearFiles = () => {
    files.value = []
    currentFile.value = null
  }

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes'
    
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  return {
    // State
    files,
    currentFile,
    loading,
    uploadProgress,
    uploading,
    fileCount,
    
    // Getters
    localFileCount,
    totalFileSize,
    filesByType,
    
    // Actions
    fetchFiles,
    searchFiles,
    getFileById,
    getFileStatistics,
    uploadFile,
    uploadMultipleFiles,
    deleteFile,
    deleteMultipleFiles,
    setCurrentFile,
    addFile,
    filterFiles,
    getFilesByType,
    getFilesByDateRange,
    clearFiles,
    formatFileSize
  }
})