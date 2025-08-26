import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { errorHandler } from './errorHandler'

// Create axios instance
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
service.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// Response interceptor
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data, status } = response
    
    // Handle successful responses
    if (status >= 200 && status < 300) {
      return data
    }
    
    return response
  },
  (error) => {
    // Handle error responses using error handler
    const appError = errorHandler.handleAxiosError(error)
    
    // Handle specific error cases
    if (error.response?.status === 401) {
      // Unauthorized - redirect to login
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    
    return Promise.reject(appError)
  }
)

// Generic request function
export const request = <T = any>(config: AxiosRequestConfig): Promise<T> => {
  return service(config)
}

// Convenience methods
export const get = <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
  return request({ ...config, method: 'GET', url })
}

export const post = <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  return request({ ...config, method: 'POST', url, data })
}

export const put = <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  return request({ ...config, method: 'PUT', url, data })
}

export const del = <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
  return request({ ...config, method: 'DELETE', url })
}

export const patch = <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  return request({ ...config, method: 'PATCH', url, data })
}

export default service