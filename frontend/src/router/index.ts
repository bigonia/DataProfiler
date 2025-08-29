import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: {
      title: 'Dashboard',
      icon: 'Dashboard'
    }
  },
  {
    path: '/datasources',
    name: 'DataSources',
    component: () => import('@/views/datasource/DataSourceList.vue'),
    meta: {
      title: 'Data Sources',
      icon: 'Coin'
    }
  },
  {
    path: '/datasources/create',
    name: 'CreateDataSource',
    component: () => import('@/views/datasource/DataSourceForm.vue'),
    meta: {
      title: 'Create Data Source',
      icon: 'Plus'
    }
  },
  {
    path: '/datasources/:id/edit',
    name: 'EditDataSource',
    component: () => import('@/views/datasource/DataSourceForm.vue'),
    meta: {
      title: 'Edit Data Source',
      icon: 'Edit'
    }
  },
  {
    path: '/files',
    name: 'Files',
    component: () => import('@/views/file/FileList.vue'),
    meta: {
      title: 'File Management',
      icon: 'Document'
    }
  },
  {
    path: '/tasks',
    name: 'Tasks',
    component: () => import('@/views/task/TaskList.vue'),
    meta: {
      title: 'Profiling Tasks',
      icon: 'Operation'
    }
  },
  {
    path: '/tasks/create',
    name: 'CreateTask',
    component: () => import('@/views/task/TaskCreate.vue'),
    meta: {
      title: 'Create Task',
      icon: 'Plus'
    }
  },
  {
    path: '/tasks/:id/status',
    name: 'TaskStatus',
    component: () => import('@/views/task/TaskStatus.vue'),
    meta: {
      title: 'Task Status',
      icon: 'View'
    }
  },
  {
    path: '/reports',
    name: 'Reports',
    component: () => import('@/views/report/ReportList.vue'),
    meta: {
      title: 'Reports',
      icon: 'Document'
    }
  },
  {
    path: '/reports/profiling',
    name: 'ProfilingReports',
    component: () => import('@/views/report/ProfilingReports.vue'),
    meta: {
      title: 'Profiling Reports',
      icon: 'DataAnalysis'
    }
  },
  {
    path: '/reports/summary',
    name: 'ReportSummary',
    component: () => import('@/views/report/ReportSummary.vue'),
    meta: {
      title: 'Summary Reports',
      icon: 'DataAnalysis'
    }
  },
  {
    path: '/reports/details',
    name: 'ReportDetails',
    component: () => import('@/views/report/ReportDetails.vue'),
    meta: {
      title: 'Detailed Reports',
      icon: 'DataBoard'
    }
  },
  {
    path: '/reports/:id/view',
    name: 'ViewReport',
    component: () => import('@/views/report/ReportViewer.vue'),
    meta: {
      title: 'View Report Summary',
      icon: 'View'
    }
  },
  {
    path: '/reports/view',
    name: 'ViewReportByTaskId',
    component: () => import('@/views/report/ReportViewer.vue'),
    meta: {
      title: 'View Report Summary',
      icon: 'View'
    }
  },
  {
    path: '/reports/:id/detailed',
    name: 'ViewDetailedReport',
    component: () => import('@/views/report/ReportDetailViewer.vue'),
    meta: {
      title: 'View Detailed Report',
      icon: 'DataBoard'
    }
  },
  {
    path: '/reports/detailed',
    name: 'ViewDetailedReportByTaskId',
    component: () => import('@/views/report/ReportDetailViewer.vue'),
    meta: {
      title: 'View Detailed Report',
      icon: 'DataBoard'
    }
  },
  {
    path: '/ai-analysis',
    name: 'AIAnalysis',
    component: () => import('@/views/ai/AIAnalysisView.vue'),
    meta: {
      title: 'AI Analysis',
      icon: 'ChatDotRound'
    }
  },
  {
    path: '/test-markdown',
    name: 'TestMarkdown',
    component: () => import('@/views/ai/test-markdown.vue'),
    meta: {
      title: 'Markdown Test',
      icon: 'Document'
    }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: {
      title: 'Page Not Found'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guards
router.beforeEach((to, from, next) => {
  // Set page title
  if (to.meta?.title) {
    document.title = `${to.meta.title} - Data Profiler`
  }
  
  next()
})

export default router