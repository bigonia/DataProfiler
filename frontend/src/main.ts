import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import router from './router'
import App from './App.vue'
import './style.css'
import { errorHandler } from './utils/errorHandler'


const app = createApp(App)
const pinia = createPinia()

// Enable Vue devtools in development
if (import.meta.env.DEV) {
  app.config.devtools = true
  app.config.debug = true
}

// Setup Vue error handler
app.config.errorHandler = (error: Error, instance: any, info: string) => {
  console.error('Vue error:', error, info)
  errorHandler.handleGeneralError(error, `Vue: ${info}`)
}

// Setup global error handlers
window.addEventListener('unhandledrejection', (event) => {
  console.error('Unhandled promise rejection:', event.reason)
  errorHandler.handleGeneralError(
    new Error(event.reason?.message || 'Unhandled promise rejection'),
    'unhandledrejection'
  )
  event.preventDefault()
})

window.addEventListener('error', (event) => {
  console.error('Global error:', event.error)
  errorHandler.handleGeneralError(
    event.error || new Error(event.message),
    `${event.filename}:${event.lineno}:${event.colno}`
  )
})

// Register Element Plus icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)

app.use(ElementPlus)

app.mount('#app')