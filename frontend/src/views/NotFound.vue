<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <div class="error-icon">
        <el-icon size="120" color="#909399">
          <WarningFilled />
        </el-icon>
      </div>
      
      <h1 class="error-title">404</h1>
      <p class="error-message">Page Not Found</p>
      <p class="error-description">
        The page you are looking for might have been removed, had its name changed, or is temporarily unavailable.
      </p>
      
      <div class="error-actions">
        <el-button type="primary" @click="goHome">
          <el-icon><House /></el-icon>
          Back to Home
        </el-button>
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          Go Back
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { House, ArrowLeft, WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()

const goHome = () => {
  router.push('/dashboard')
}

const goBack = () => {
  router.go(-1)
}
</script>

<style scoped>
.not-found-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.not-found-content {
  text-align: center;
  background: white;
  padding: 60px 40px;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  max-width: 500px;
  width: 100%;
}

.error-icon {
  margin-bottom: 30px;
}

.error-title {
  font-size: 72px;
  font-weight: bold;
  color: #303133;
  margin: 0 0 20px 0;
  line-height: 1;
}

.error-message {
  font-size: 24px;
  color: #606266;
  margin: 0 0 15px 0;
  font-weight: 500;
}

.error-description {
  font-size: 16px;
  color: #909399;
  line-height: 1.6;
  margin: 0 0 40px 0;
}

.error-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  flex-wrap: wrap;
}

.error-actions .el-button {
  padding: 12px 24px;
  font-size: 16px;
}

@media (max-width: 768px) {
  .not-found-content {
    padding: 40px 20px;
  }
  
  .error-title {
    font-size: 48px;
  }
  
  .error-message {
    font-size: 20px;
  }
  
  .error-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .error-actions .el-button {
    width: 200px;
  }
}
</style>