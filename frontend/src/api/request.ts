import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { errorHandler } from '@/utils/errorHandler'

// Create axios instance
const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
request.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    // Add auth token if needed
    // const token = localStorage.getItem('token')
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`
    // }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// Response interceptor
request.interceptors.response.use(
  (response: AxiosResponse) => {
    // Handle different response formats
    if (response.status === 200 || response.status === 201) {
      // Always return response.data, but handle null/undefined cases
      if (response.data !== null && response.data !== undefined) {
        return response.data
      } else {
        // Return appropriate default values based on expected response type
        return null
      }
    }
    
    return response.data
  },
  (error) => {
    // Use centralized error handler
    const appError = errorHandler.handleAxiosError(error)
    
    // Handle specific error types that need special treatment
    if (appError.code === 'UNAUTHORIZED') {
      // Redirect to login if needed
      // router.push('/login')
    }
    
    return Promise.reject(appError)
  }
)

export default request