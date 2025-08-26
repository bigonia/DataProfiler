<template>
  <div id="app">
    <div class="app-container">
      <!-- Sidebar -->
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="sidebar-header">
          <div class="logo">
            <el-icon v-if="!sidebarCollapsed" size="24" color="#409eff">
              <DataAnalysis />
            </el-icon>
            <span v-if="!sidebarCollapsed" class="logo-text">数据剖析平台</span>
          </div>
        </div>
        
        <nav class="sidebar-nav">
          <el-menu
            :default-active="$route.path"
            :collapse="sidebarCollapsed"
            :unique-opened="true"
            router
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409eff"
          >
            <el-menu-item index="/dashboard">
              <el-icon><DataBoard /></el-icon>
              <template #title>仪表板</template>
            </el-menu-item>
            
            <el-sub-menu index="datasources">
              <template #title>
                <el-icon><Coin /></el-icon>
                <span>数据源</span>
              </template>
              <el-menu-item index="/datasources">数据源列表</el-menu-item>
            <el-menu-item index="/datasources/create">创建数据源</el-menu-item>
            </el-sub-menu>
            
            <el-menu-item index="/files">
              <el-icon><Document /></el-icon>
              <template #title>文件管理</template>
            </el-menu-item>
            
            <el-sub-menu index="tasks">
              <template #title>
                <el-icon><Operation /></el-icon>
                <span>数据剖析任务</span>
              </template>
              <el-menu-item index="/tasks">任务列表</el-menu-item>
            <el-menu-item index="/tasks/create">创建任务</el-menu-item>
            </el-sub-menu>
            
            <el-sub-menu index="reports">
              <template #title>
                <el-icon><DataBoard /></el-icon>
                <span>报告</span>
              </template>
              <el-menu-item index="/reports">所有报告</el-menu-item>
            <el-menu-item index="/reports/summary">摘要报告</el-menu-item>
            <el-menu-item index="/reports/details">详细报告</el-menu-item>
            </el-sub-menu>
            
            <el-menu-item index="/ai-analysis">
              <el-icon><ChatDotRound /></el-icon>
              <template #title>AI分析</template>
            </el-menu-item>
          </el-menu>
        </nav>
      </aside>
      
      <!-- Main Content -->
      <div class="main-content">
        <!-- Top Navigation -->
        <header class="navbar">
          <div class="navbar-left">
            <el-button
              type="text"
              @click="toggleSidebar"
              class="sidebar-toggle"
            >
              <el-icon size="20">
                <Expand v-if="sidebarCollapsed" />
                <Fold v-else />
              </el-icon>
            </el-button>
            
            <el-breadcrumb separator="/" class="breadcrumb">
              <el-breadcrumb-item
                v-for="item in breadcrumbs"
                :key="item.path"
                :to="item.path"
              >
                {{ item.title }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          
          <div class="navbar-right">
            <el-button type="text" @click="refreshPage">
              <el-icon><Refresh /></el-icon>
            </el-button>
            

            
            <el-dropdown>
              <el-button type="text">
                <el-icon><User /></el-icon>
                <span>管理员</span>
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>个人资料</el-dropdown-item>
              <el-dropdown-item>设置</el-dropdown-item>
              <el-dropdown-item divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </header>
        
        <!-- Page Content -->
        <main class="content-wrapper">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'


import {
  DataBoard,
  Coin,
  Document,
  Operation,
  DataAnalysis,
  Expand,
  Fold,
  Refresh,
  User,
  ArrowDown,
  ChatDotRound
} from '@element-plus/icons-vue'

const route = useRoute()

const sidebarCollapsed = ref(false)

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const refreshPage = () => {
  try {
    // Check if window is available (client-side only)
    if (typeof window !== 'undefined' && window.location) {
      window.location.reload()
    }
  } catch (error) {
    console.error('Failed to refresh page:', error)
  }
}

// Generate breadcrumbs based on current route
const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  const breadcrumbItems = matched.map(item => ({
    path: item.path,
    title: item.meta?.title || ''
  }))
  
  // Add home breadcrumb if not already present
  if (breadcrumbItems.length > 0 && breadcrumbItems[0].path !== '/dashboard') {
    breadcrumbItems.unshift({
      path: '/dashboard',
      title: 'Dashboard'
    })
  }
  
  return breadcrumbItems
})

// Watch route changes to update document title
watch(
  () => route.meta.title,
  (title) => {
    try {
      // Check if document is available (client-side only)
      if (title && typeof document !== 'undefined') {
        document.title = `${title} - Data Profiler`
      }
    } catch (error) {
      console.error('Failed to update document title:', error)
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #434a50;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.sidebar-nav {
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sidebar-toggle {
  padding: 8px;
  color: #606266;
}

.sidebar-toggle:hover {
  color: #409eff;
}

.breadcrumb {
  font-size: 14px;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
}

:deep(.el-sub-menu .el-sub-menu__title) {
  height: 48px;
  line-height: 48px;
}

:deep(.el-menu--collapse) {
  width: 64px;
}

:deep(.el-menu--collapse .el-menu-item) {
  padding: 0 20px;
}

:deep(.el-menu--collapse .el-sub-menu) {
  padding: 0 20px;
}
</style>