<template>
  <div class="file-list">
    <!-- Page Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">File Management</h1>
        <p class="page-description">Upload, manage and convert your data files</p>
      </div>
      <el-upload
        ref="uploadRef"
        :action="uploadAction"
        :headers="uploadHeaders"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
        :show-file-list="false"
        multiple
      >
        <el-button type="primary">
          <el-icon><Upload /></el-icon>
          Upload Files
        </el-button>
      </el-upload>
    </div>

    <!-- Statistics Cards -->
    <div class="stats-row">
      <div class="stat-card">
        <el-icon class="stat-icon" color="#409EFF"><Files /></el-icon>
        <div class="stat-content">
          <div class="stat-number">{{ fileStore.files.length }}</div>
          <div class="stat-label">Total Files</div>
        </div>
      </div>
      <div class="stat-card">
        <el-icon class="stat-icon" color="#67C23A"><Document /></el-icon>
        <div class="stat-content">
          <div class="stat-number">{{ convertedFilesCount }}</div>
          <div class="stat-label">Converted Files</div>
        </div>
      </div>
      <div class="stat-card">
        <el-icon class="stat-icon" color="#E6A23C"><FolderOpened /></el-icon>
        <div class="stat-content">
          <div class="stat-number">{{ totalFileSize }}</div>
          <div class="stat-label">Total Size</div>
        </div>
      </div>
    </div>

    <!-- Search and Filter Bar -->
    <div class="table-header">
      <div class="search-filters">
        <el-input
          v-model="searchQuery"
          placeholder="Search files..."
          class="search-input"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select
          v-model="selectedType"
          placeholder="File Type"
          clearable
          class="filter-item"
        >
          <el-option label="All Types" value="" />
          <el-option label="Excel" value="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" />
          <el-option label="CSV" value="text/csv" />
          <el-option label="PDF" value="application/pdf" />
          <el-option label="Image" value="image" />
        </el-select>
        
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="To"
          start-placeholder="Start date"
          end-placeholder="End date"
          class="filter-item"
        />
      </div>
      
      <div class="table-actions">
        <el-button @click="refreshList" :loading="loading">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
        <el-button
          v-if="selectedFiles.length > 0"
          type="danger"
          @click="deleteSelectedFiles"
        >
          <el-icon><Delete /></el-icon>
          Delete Selected ({{ selectedFiles.length }})
        </el-button>
      </div>
    </div>

    <!-- File Table -->
    <el-table
      :data="paginatedFiles"
      v-loading="loading"
      @selection-change="handleSelectionChange"
      class="file-table"
    >
      <el-table-column type="selection" width="55" />
      
      <el-table-column label="File Name" min-width="200">
        <template #default="{ row }">
          <div class="file-info">
            <el-icon class="file-icon" :color="getMimeTypeIcon(row.mimeType).color">
              <component :is="getMimeTypeIcon(row.mimeType).icon" />
            </el-icon>
            <div class="file-details">
              <div class="file-name">{{ row.originalFilename || row.filename }}</div>
              <div class="file-path">{{ row.filePath }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      
      <el-table-column label="Size" width="100">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      
      <el-table-column label="Type" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ getFileTypeLabel(row.mimeType) }}</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="Conversion Status" width="140">
        <template #default="{ row }">
          <el-tag 
            :type="row.converted ? 'success' : 'info'"
            size="small"
          >
            {{ row.converted ? 'Converted' : 'Not Converted' }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="Upload Time" width="160">
        <template #default="{ row }">
          {{ formatDate(row.uploadedAt) }}
        </template>
      </el-table-column>
      
      <el-table-column label="Actions" width="200" fixed="right">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button
              v-if="!row.converted"
              type="primary"
              size="small"
              @click="convertFile(row)"
              :loading="convertingFiles.has(row.id)"
            >
              Convert
            </el-button>
            <el-button type="text" size="small" @click="downloadFile(row)">
              <el-icon><Download /></el-icon>
            </el-button>
            <el-button type="text" size="small" @click="viewFileDetails(row)">
              <el-icon><View /></el-icon>
            </el-button>
            <el-button
              type="text"
              size="small"
              @click="deleteFile(row.id)"
              class="delete-btn"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="filteredFiles.length"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- File Details Dialog -->
    <el-dialog
      v-model="showFileDetails"
      title="File Details"
      width="600px"
    >
      <div v-if="selectedFile" class="file-details-content">
        <div class="detail-row">
          <span class="detail-label">Filename:</span>
          <span class="detail-value">{{ selectedFile.filename }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">Original Filename:</span>
          <span class="detail-value">{{ selectedFile.originalFilename }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">File Size:</span>
          <span class="detail-value">{{ formatFileSize(selectedFile.fileSize) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">MIME Type:</span>
          <span class="detail-value">{{ selectedFile.mimeType }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">File Path:</span>
          <span class="detail-value">{{ selectedFile.filePath }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">Upload Time:</span>
          <span class="detail-value">{{ formatDate(selectedFile.uploadedAt) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">Converted:</span>
          <span class="detail-value">
            <el-tag :type="selectedFile.converted ? 'success' : 'info'" size="small">
              {{ selectedFile.converted ? 'Yes' : 'No' }}
            </el-tag>
          </span>
        </div>
        <div v-if="selectedFile.converted && selectedFile.convertedAt" class="detail-row">
          <span class="detail-label">Converted At:</span>
          <span class="detail-value">{{ formatDate(selectedFile.convertedAt) }}</span>
        </div>
        <div v-if="selectedFile.description" class="detail-row">
          <span class="detail-label">Description:</span>
          <span class="detail-value">{{ selectedFile.description }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showFileDetails = false">Close</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadInstance } from 'element-plus'
import {
  Upload,
  Document,
  FolderOpened,
  Search,
  Refresh,
  Close,
  Delete,
  Download,
  View,
  Picture,
  VideoPlay,
  Microphone,
  Files
} from '@element-plus/icons-vue'
import { useFileStore } from '@/stores/file'
import { formatFileSize, formatDate, debounce, getMimeTypeIcon } from '@/utils'
import type { FileMetadata } from '@/types'

const fileStore = useFileStore()
const uploadRef = ref<UploadInstance>()

const loading = ref(false)
const searchQuery = ref('')
const selectedType = ref('')
const dateRange = ref<[Date, Date] | null>(null)
const selectedFiles = ref<FileMetadata[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const showFileDetails = ref(false)
const selectedFile = ref<FileMetadata | null>(null)
const convertingFiles = ref(new Set<number>())

const uploadAction = '/api/files/upload'
const uploadHeaders = {
  // Add authorization headers if needed
}

const filteredFiles = computed(() => {
  let files = fileStore.files
  
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    files = files.filter(file => 
      (file.originalFilename || file.filename).toLowerCase().includes(query) ||
      file.description?.toLowerCase().includes(query)
    )
  }
  
  if (selectedType.value) {
    files = files.filter(file => {
      if (selectedType.value === 'image') {
        return file.mimeType.startsWith('image/')
      }
      return file.mimeType === selectedType.value
    })
  }
  
  if (dateRange.value && dateRange.value.length === 2) {
    const [startDate, endDate] = dateRange.value
    files = files.filter(file => {
      const uploadDate = new Date(file.uploadedAt)
      return uploadDate >= startDate && uploadDate <= endDate
    })
  }
  
  return files
})

const paginatedFiles = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredFiles.value.slice(start, end)
})

const convertedFilesCount = computed(() => {
  return fileStore.files.filter(file => file.converted).length
})

const totalFileSize = computed(() => {
  const total = fileStore.files.reduce((sum, file) => sum + file.fileSize, 0)
  return formatFileSize(total)
})

const getFileTypeLabel = (mimeType: string) => {
  if (mimeType.includes('spreadsheet') || mimeType.includes('excel')) return 'Excel'
  if (mimeType.includes('csv')) return 'CSV'
  if (mimeType.includes('pdf')) return 'PDF'
  if (mimeType.startsWith('image/')) return 'Image'
  if (mimeType.startsWith('video/')) return 'Video'
  if (mimeType.startsWith('audio/')) return 'Audio'
  return 'Other'
}

const refreshList = async () => {
  loading.value = true
  try {
    await fileStore.fetchFiles()
  } catch (error) {
    console.error('Failed to refresh file list:', error)
    ElMessage.error('Failed to refresh file list')
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (selection: FileMetadata[]) => {
  selectedFiles.value = selection
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
}

const beforeUpload = (file: File) => {
  const isValidSize = file.size / 1024 / 1024 < 100 // 100MB limit
  if (!isValidSize) {
    ElMessage.error('File size cannot exceed 100MB!')
    return false
  }
  return true
}

const handleUploadSuccess = (response: any, file: File) => {
  if (response.success) {
    ElMessage.success('File uploaded successfully')
    refreshList()
  } else {
    ElMessage.error(response.message || 'Upload failed')
  }
}

const handleUploadError = (error: any) => {
  console.error('Upload error:', error)
  ElMessage.error('Upload failed')
}

const downloadFile = async (file: FileMetadata) => {
  try {
    const response = await fetch(`/api/files/download/${file.id}`)
    if (!response.ok) {
      throw new Error('Download failed')
    }
    
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = file.originalFilename || file.filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('Download failed:', error)
    ElMessage.error('Download failed')
  }
}

const viewFileDetails = (file: FileMetadata) => {
  selectedFile.value = file
  showFileDetails.value = true
}

const deleteFile = async (fileId: number) => {
  try {
    await ElMessageBox.confirm(
      'This will permanently delete the file. Continue?',
      'Warning',
      {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning',
      }
    )
    
    const response = await fetch(`/api/files/${fileId}`, {
      method: 'DELETE'
    })
    
    const result = await response.json()
    
    if (response.ok && result.success) {
      ElMessage.success('File deleted successfully')
      await refreshList()
    } else {
      ElMessage.error(result.message || 'Failed to delete file')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
      ElMessage.error('Failed to delete file')
    }
  }
}

const deleteSelectedFiles = async () => {
  if (selectedFiles.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(
      `This will permanently delete ${selectedFiles.value.length} files. Continue?`,
      'Warning',
      {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning',
      }
    )
    
    const deletePromises = selectedFiles.value.map(file => 
      fetch(`/api/files/${file.id}`, { method: 'DELETE' })
    )
    
    await Promise.all(deletePromises)
    ElMessage.success(`${selectedFiles.value.length} files deleted successfully`)
    selectedFiles.value = []
    await refreshList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Batch delete failed:', error)
      ElMessage.error('Failed to delete files')
    }
  }
}

const convertFile = async (file: FileMetadata) => {
  try {
    const dataSourceName = await ElMessageBox.prompt(
      `Enter a name for the data source created from "${file.originalFilename || file.filename}":`,
      'Convert File to Data Source',
      {
        confirmButtonText: 'Convert',
        cancelButtonText: 'Cancel',
        inputPattern: /^.{1,50}$/,
        inputErrorMessage: 'Data source name must be 1-50 characters long'
      }
    )
    
    if (!dataSourceName.value) {
      ElMessage.warning('Data source name is required')
      return
    }
    
    convertingFiles.value.add(file.id)
    
    const response = await fetch(`/api/files/convert/${file.id}?dataSourceName=${encodeURIComponent(dataSourceName.value)}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    const result = await response.json()
    
    if (response.ok && result.success) {
      ElMessage.success('File conversion has been started and is running in the background. Please refresh the page later to check the conversion status.')
      // Note: Do not update file status immediately as conversion is now asynchronous
      // The file status will be updated when the background conversion completes
    } else {
      ElMessage.error(result.message || 'Failed to start file conversion')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('File conversion failed:', error)
      ElMessage.error('Failed to convert file')
    }
  } finally {
    convertingFiles.value.delete(file.id)
  }
}

onMounted(async () => {
  try {
    await refreshList()
  } catch (error) {
    console.error('Failed to load file list:', error)
    ElMessage.error('Failed to load file list')
  }
})
</script>

<style scoped>
.file-list {
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

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  font-size: 32px;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
}

.search-filters {
  display: flex;
  gap: 12px;
  flex: 1;
}

.search-input {
  width: 300px;
}

.filter-item {
  width: 150px;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.file-table {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-path {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.action-buttons {
  display: flex;
  gap: 4px;
  align-items: center;
}

.delete-btn {
  color: #f56c6c;
}

.delete-btn:hover {
  color: #f56c6c;
  background-color: #fef0f0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.file-details-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-label {
  font-weight: 500;
  color: #303133;
  min-width: 120px;
  flex-shrink: 0;
}

.detail-value {
  color: #606266;
  flex: 1;
  word-break: break-all;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  
  .search-filters {
    flex-direction: column;
  }
  
  .search-input {
    width: 100%;
  }
  
  .filter-item {
    min-width: auto;
  }
  
  .table-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
}
</style>