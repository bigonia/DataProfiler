<template>
  <div class="page-header">
    <div class="page-header-content">
      <!-- Back Button -->
      <div v-if="showBack" class="page-header-back">
        <el-button 
          type="text" 
          @click="handleBack"
          class="back-button"
        >
          <el-icon><ArrowLeft /></el-icon>
          Back
        </el-button>
      </div>
      
      <!-- Title Section -->
      <div class="page-header-main">
        <div class="page-header-title">
          <h1>{{ title }}</h1>
          <el-tag v-if="status" :type="getStatusType(status)" size="small">
            {{ status }}
          </el-tag>
        </div>
        <div v-if="subtitle" class="page-header-subtitle">
          {{ subtitle }}
        </div>
        
        <!-- Breadcrumb -->
        <div v-if="breadcrumb && breadcrumb.length > 0" class="page-header-breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item 
              v-for="item in breadcrumb" 
              :key="item.path"
              :to="item.path ? { path: item.path } : undefined"
            >
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
      </div>
      
      <!-- Actions -->
      <div v-if="$slots.actions" class="page-header-actions">
        <slot name="actions"></slot>
      </div>
    </div>
    
    <!-- Extra Content -->
    <div v-if="$slots.extra" class="page-header-extra">
      <slot name="extra"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import type { BreadcrumbItem } from '../types'

interface Props {
  title: string
  subtitle?: string
  status?: string
  showBack?: boolean
  breadcrumb?: BreadcrumbItem[]
}

const props = withDefaults(defineProps<Props>(), {
  showBack: false
})

const emit = defineEmits<{
  back: []
}>()

const router = useRouter()

const handleBack = () => {
  emit('back')
  if (!props.showBack) return
  
  // Default back behavior
  if (window.history.length > 1) {
    router.go(-1)
  } else {
    router.push('/dashboard')
  }
}

const getStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    'running': 'warning',
    'completed': 'success',
    'failed': 'danger',
    'pending': 'info',
    'cancelled': 'info'
  }
  return statusMap[status.toLowerCase()] || 'info'
}
</script>

<style scoped>
.page-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 24px;
}

.page-header-content {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 24px;
  min-height: 80px;
}

.page-header-back {
  flex-shrink: 0;
  padding-top: 4px;
}

.back-button {
  padding: 8px 12px;
  font-size: 14px;
  color: #606266;
  transition: all 0.3s;
}

.back-button:hover {
  color: #409eff;
  background: #ecf5ff;
}

.page-header-main {
  flex: 1;
  min-width: 0;
}

.page-header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.page-header-title h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.page-header-subtitle {
  font-size: 14px;
  color: #909399;
  line-height: 1.5;
  margin-bottom: 12px;
}

.page-header-breadcrumb {
  margin-top: 8px;
}

.page-header-actions {
  flex-shrink: 0;
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding-top: 4px;
}

.page-header-extra {
  padding: 0 24px 24px;
  border-top: 1px solid #f5f7fa;
  margin-top: -1px;
}

@media (max-width: 768px) {
  .page-header-content {
    flex-direction: column;
    gap: 12px;
    padding: 16px;
  }
  
  .page-header-title h1 {
    font-size: 20px;
  }
  
  .page-header-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .page-header-extra {
    padding: 0 16px 16px;
  }
}
</style>