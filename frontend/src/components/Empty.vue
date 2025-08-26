<template>
  <div class="empty-container">
    <div class="empty-content">
      <div class="empty-icon">
        <el-icon :size="iconSize" :color="iconColor">
          <component :is="icon" />
        </el-icon>
      </div>
      <div class="empty-title">{{ title }}</div>
      <div v-if="description" class="empty-description">{{ description }}</div>
      <div v-if="$slots.action" class="empty-actions">
        <slot name="action"></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DocumentRemove, FolderRemove, DataBoard, Warning } from '@element-plus/icons-vue'

interface Props {
  icon?: string
  iconSize?: number
  iconColor?: string
  title?: string
  description?: string
}

withDefaults(defineProps<Props>(), {
  icon: 'DocumentRemove',
  iconSize: 64,
  iconColor: '#c0c4cc',
  title: 'No Data',
  description: ''
})
</script>

<style scoped>
.empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  min-height: 200px;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 400px;
}

.empty-icon {
  margin-bottom: 20px;
  opacity: 0.6;
}

.empty-title {
  font-size: 18px;
  font-weight: 500;
  color: #909399;
  margin-bottom: 12px;
  line-height: 1.4;
}

.empty-description {
  font-size: 14px;
  color: #c0c4cc;
  line-height: 1.6;
  margin-bottom: 24px;
}

.empty-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

@media (max-width: 768px) {
  .empty-container {
    padding: 40px 20px;
  }
  
  .empty-title {
    font-size: 16px;
  }
  
  .empty-description {
    font-size: 13px;
  }
  
  .empty-actions {
    flex-direction: column;
    align-items: center;
  }
}
</style>