<template>
  <div class="file-upload">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <el-button text @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          Back
        </el-button>
        <h2 class="page-title">Upload Files</h2>
        <p class="page-description">Upload data files for processing and analysis</p>
      </div>
    </div>
    
    <!-- Upload Area -->
    <div class="upload-container">
      <el-upload
        ref="uploadRef"
        class="upload-dragger"
        drag
        :action="uploadAction"
        :headers="uploadHeaders"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :on-progress="handleUploadProgress"
        :on-change="handleFileChange"
        :before-upload="beforeUpload"
        :file-list="fileList"
        multiple
        :auto-upload="false"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          Drop files here or <em>click to upload</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            Supported formats: CSV, Excel, JSON, XML, TXT (max 100MB per file)
          </div>
        </template>
      </el-upload>
      
      <!-- Upload Actions -->
      <div v-if="fileList.length > 0" class="upload-actions">
        <el-button @click="clearFiles">Clear All</el-button>
        <el-button 
          type="primary" 
          @click="submitUpload"
          :loading="uploading"
          :disabled="fileList.length === 0"
        >
          Upload {{ fileList.length }} File{{ fileList.length > 1 ? 's' : '' }}
        </el-button>
      </div>
    </div>
    
    <!-- Upload Progress -->
    <div v-if="uploading" class="progress-container">
      <div class="card">
        <div class="progress-header">
          <h4>Uploading Files</h4>
          <span class="progress-status">{{ uploadedCount }}/{{ totalFiles }} completed</span>
        </div>
        <el-progress
          :percentage="overallProgress"
          :status="overallProgress === 100 ? 'success' : undefined"
        />
        <div class="file-progress-list">
          <div 
            v-for="(file, index) in uploadingFiles" 
            :key="index"
            class="file-progress-item"
          >
            <div class="file-info">
              <el-icon><Document /></el-icon>
              <span class="file-name">{{ file.name }}</span>
            </div>
            <div class="file-status">
              <el-progress
                :percentage="file.progress"
                :status="file.status"
                :show-text="false"
                :stroke-width="4"
              />
              <span class="status-text">{{ getStatusText(file.status, file.progress) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Upload Guidelines -->
    <div class="guidelines-container">
      <div class="card">
        <h3>Upload Guidelines</h3>
        <div class="guidelines-content">
          <div class="guideline-section">
            <h4><el-icon><Files /></el-icon> Supported File Types</h4>
            <ul>
              <li><strong>CSV:</strong> Comma-separated values (.csv)</li>
              <li><strong>Excel:</strong> Excel workbooks (.xlsx, .xls)</li>
              <li><strong>JSON:</strong> JavaScript Object Notation (.json)</li>
              <li><strong>XML:</strong> Extensible Markup Language (.xml)</li>
              <li><strong>Text:</strong> Plain text files (.txt)</li>
            </ul>
          </div>
          
          <div class="guideline-section">
            <h4><el-icon><Warning /></el-icon> File Requirements</h4>
            <ul>
              <li>Maximum file size: 100MB per file</li>
              <li>Files should contain structured data</li>
              <li>CSV files should have headers in the first row</li>
              <li>Excel files will use the first sheet by default</li>
              <li>JSON files should contain arrays or objects</li>
            </ul>
          </div>
          
          <div class="guideline-section">
            <h4><el-icon><InfoFilled /></el-icon> Best Practices</h4>
            <ul>
              <li>Use descriptive file names</li>
              <li>Ensure data quality before uploading</li>
              <li>Remove sensitive information if not needed</li>
              <li>Consider file size for better performance</li>
              <li>Upload related files together</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadInstance, UploadFile, UploadFiles } from 'element-plus'
import {
  ArrowLeft,
  UploadFilled,
  Document,
  Files,
  Warning,
  InfoFilled
} from '@element-plus/icons-vue'
import { useFileStore } from '@/stores/file'

interface UploadingFile {
  name: string
  progress: number
  status: 'uploading' | 'success' | 'exception'
}

const router = useRouter()
const fileStore = useFileStore()
const uploadRef = ref<UploadInstance>()

const fileList = ref<UploadFile[]>([])
const uploading = ref(false)
const uploadingFiles = ref<UploadingFile[]>([])
const uploadedCount = ref(0)

const uploadAction = '/api/files/upload'
const uploadHeaders = {
  // Add authorization headers if needed
}

const totalFiles = computed(() => fileList.value.length)
const overallProgress = computed(() => {
  if (totalFiles.value === 0) return 0
  return Math.round((uploadedCount.value / totalFiles.value) * 100)
})

const beforeUpload = (file: File) => {
  const maxSize = 100 * 1024 * 1024 // 100MB
  if (file.size > maxSize) {
    ElMessage.error(`File ${file.name} exceeds 100MB limit`)
    return false
  }
  
  const allowedTypes = [
    'text/csv',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-excel',
    'application/json',
    'application/xml',
    'text/xml',
    'text/plain'
  ]
  
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error(`File type ${file.type} is not supported`)
    return false
  }
  
  return true
}

const handleFileChange = (file: UploadFile, files: UploadFiles) => {
  fileList.value = files
}

const handleUploadSuccess = (response: any, file: UploadFile) => {
  const uploadingFile = uploadingFiles.value.find(f => f.name === file.name)
  if (uploadingFile) {
    uploadingFile.status = 'success'
    uploadingFile.progress = 100
  }
  uploadedCount.value++
  
  if (uploadedCount.value === totalFiles.value) {
    ElMessage.success(`All ${totalFiles.value} files uploaded successfully`)
    setTimeout(() => {
      router.push('/files')
    }, 1500)
  }
}

const handleUploadError = (error: any, file: UploadFile) => {
  const uploadingFile = uploadingFiles.value.find(f => f.name === file.name)
  if (uploadingFile) {
    uploadingFile.status = 'exception'
  }
  ElMessage.error(`Failed to upload ${file.name}`)
}

const handleUploadProgress = (event: any, file: UploadFile) => {
  const uploadingFile = uploadingFiles.value.find(f => f.name === file.name)
  if (uploadingFile) {
    uploadingFile.progress = Math.round(event.percent)
  }
}

const submitUpload = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('Please select files to upload')
    return
  }
  
  uploading.value = true
  uploadedCount.value = 0
  
  // Initialize uploading files tracking
  uploadingFiles.value = fileList.value.map(file => ({
    name: file.name,
    progress: 0,
    status: 'uploading' as const
  }))
  
  // Start upload
  uploadRef.value?.submit()
}

const clearFiles = async () => {
  if (uploading.value) {
    try {
      await ElMessageBox.confirm(
        'Upload is in progress. Are you sure you want to cancel and clear all files?',
        'Confirm Clear',
        {
          confirmButtonText: 'Clear',
          cancelButtonText: 'Keep Uploading',
          type: 'warning'
        }
      )
      uploadRef.value?.abort()
      uploading.value = false
    } catch {
      return
    }
  }
  
  uploadRef.value?.clearFiles()
  fileList.value = []
  uploadingFiles.value = []
  uploadedCount.value = 0
}

const getStatusText = (status: string, progress: number) => {
  switch (status) {
    case 'success':
      return 'Completed'
    case 'exception':
      return 'Failed'
    case 'uploading':
      return `${progress}%`
    default:
      return 'Waiting'
  }
}
</script>

<style scoped>
.file-upload {
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

.upload-container {
  margin-bottom: 30px;
}

.upload-dragger {
  width: 100%;
}

.upload-dragger :deep(.el-upload-dragger) {
  width: 100%;
  height: 200px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.upload-dragger :deep(.el-upload-dragger:hover) {
  border-color: #409eff;
  background: #f0f9ff;
}

.upload-dragger :deep(.el-icon--upload) {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 16px;
}

.upload-dragger :deep(.el-upload__text) {
  color: #606266;
  font-size: 16px;
  margin-bottom: 8px;
}

.upload-dragger :deep(.el-upload__text em) {
  color: #409eff;
  font-style: normal;
}

.upload-dragger :deep(.el-upload__tip) {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}

.upload-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.progress-container {
  margin-bottom: 30px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.progress-header h4 {
  margin: 0;
  color: #303133;
}

.progress-status {
  color: #606266;
  font-size: 14px;
}

.file-progress-list {
  margin-top: 20px;
}

.file-progress-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.file-progress-item:last-child {
  border-bottom: none;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.file-name {
  color: #303133;
  font-size: 14px;
  truncate: true;
}

.file-status {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 120px;
}

.status-text {
  font-size: 12px;
  color: #606266;
  min-width: 60px;
  text-align: right;
}

.guidelines-container {
  margin-top: 30px;
}

.guidelines-container h3 {
  margin-bottom: 20px;
  color: #303133;
}

.guidelines-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
}

.guideline-section h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
  margin-bottom: 12px;
  font-size: 16px;
}

.guideline-section ul {
  margin: 0;
  padding-left: 20px;
  color: #606266;
}

.guideline-section li {
  margin-bottom: 8px;
  line-height: 1.5;
}

.guideline-section li strong {
  color: #303133;
}

@media (max-width: 768px) {
  .upload-actions {
    flex-direction: column;
  }
  
  .file-progress-item {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  
  .file-status {
    min-width: auto;
  }
  
  .guidelines-content {
    grid-template-columns: 1fr;
  }
}
</style>