import { ElMessage, ElNotification } from 'element-plus'
import type { AxiosError } from 'axios'

// Error types enum
export enum ErrorType {
  NETWORK = 'NETWORK',
  VALIDATION = 'VALIDATION',
  AUTHENTICATION = 'AUTHENTICATION',
  AUTHORIZATION = 'AUTHORIZATION',
  NOT_FOUND = 'NOT_FOUND',
  SERVER = 'SERVER',
  BUSINESS = 'BUSINESS',
  UNKNOWN = 'UNKNOWN'
}

// Error severity levels
export enum ErrorSeverity {
  LOW = 'low',
  MEDIUM = 'medium',
  HIGH = 'high',
  CRITICAL = 'critical'
}

// Error interface
export interface AppError {
  type: ErrorType
  severity: ErrorSeverity
  message: string
  code?: string
  details?: any
  timestamp: Date
  url?: string
  userId?: string
}

// Error handler class
export class ErrorHandler {
  private static instance: ErrorHandler
  private errorLog: AppError[] = []
  private maxLogSize = 100

  private constructor() {}

  public static getInstance(): ErrorHandler {
    if (!ErrorHandler.instance) {
      ErrorHandler.instance = new ErrorHandler()
    }
    return ErrorHandler.instance
  }

  // Handle axios errors
  public handleAxiosError(error: AxiosError): AppError {
    const appError: AppError = {
      type: ErrorType.UNKNOWN,
      severity: ErrorSeverity.MEDIUM,
      message: 'An error occurred',
      timestamp: new Date(),
      url: error.config?.url
    }

    if (error.response) {
      // Server responded with error status
      const { status, data } = error.response
      
      switch (status) {
        case 400:
          appError.type = ErrorType.VALIDATION
          appError.severity = ErrorSeverity.LOW
          appError.message = this.extractErrorMessage(data) || 'Invalid request data'
          appError.code = 'BAD_REQUEST'
          break
          
        case 401:
          appError.type = ErrorType.AUTHENTICATION
          appError.severity = ErrorSeverity.HIGH
          appError.message = 'Authentication required'
          appError.code = 'UNAUTHORIZED'
          break
          
        case 403:
          appError.type = ErrorType.AUTHORIZATION
          appError.severity = ErrorSeverity.HIGH
          appError.message = 'Access denied'
          appError.code = 'FORBIDDEN'
          break
          
        case 404:
          appError.type = ErrorType.NOT_FOUND
          appError.severity = ErrorSeverity.LOW
          appError.message = 'Resource not found'
          appError.code = 'NOT_FOUND'
          break
          
        case 422:
          appError.type = ErrorType.BUSINESS
          appError.severity = ErrorSeverity.MEDIUM
          appError.message = this.extractErrorMessage(data) || 'Business logic error'
          appError.code = 'UNPROCESSABLE_ENTITY'
          break
          
        case 500:
        case 502:
        case 503:
        case 504:
          appError.type = ErrorType.SERVER
          appError.severity = ErrorSeverity.CRITICAL
          appError.message = 'Server error occurred'
          appError.code = `SERVER_ERROR_${status}`
          break
          
        default:
          appError.message = this.extractErrorMessage(data) || `HTTP Error ${status}`
          appError.code = `HTTP_${status}`
      }
      
      appError.details = data
    } else if (error.request) {
      // Network error
      appError.type = ErrorType.NETWORK
      appError.severity = ErrorSeverity.HIGH
      appError.message = 'Network connection failed'
      appError.code = 'NETWORK_ERROR'
    } else {
      // Request setup error
      appError.message = error.message || 'Request configuration error'
      appError.code = 'REQUEST_ERROR'
    }

    this.logError(appError)
    this.showErrorNotification(appError)
    
    return appError
  }

  // Handle general JavaScript errors
  public handleGeneralError(error: Error, context?: string): AppError {
    const appError: AppError = {
      type: ErrorType.UNKNOWN,
      severity: ErrorSeverity.MEDIUM,
      message: error.message || 'An unexpected error occurred',
      timestamp: new Date(),
      details: {
        stack: error.stack,
        context
      }
    }

    this.logError(appError)
    this.showErrorNotification(appError)
    
    return appError
  }

  // Handle DOM operation errors
  public handleDOMError(error: Error, element?: string): AppError {
    const appError: AppError = {
      type: ErrorType.UNKNOWN,
      severity: ErrorSeverity.LOW,
      message: `DOM operation failed${element ? ` on ${element}` : ''}`,
      timestamp: new Date(),
      code: 'DOM_ERROR',
      details: {
        originalError: error.message,
        element,
        stack: error.stack
      }
    }

    this.logError(appError)
    // Don't show notification for DOM errors, just log them
    console.warn('DOM Error:', appError)
    
    return appError
  }

  // Extract error message from response data
  private extractErrorMessage(data: any): string | null {
    if (typeof data === 'string') {
      return data
    }
    
    if (data && typeof data === 'object') {
      // Try different common error message fields
      return data.message || data.error || data.msg || data.detail || null
    }
    
    return null
  }

  // Show error notification to user
  private showErrorNotification(error: AppError): void {
    const config = {
      title: this.getErrorTitle(error.type),
      message: error.message,
      type: this.getNotificationType(error.severity),
      duration: this.getNotificationDuration(error.severity),
      showClose: true
    }

    switch (error.severity) {
      case ErrorSeverity.CRITICAL:
      case ErrorSeverity.HIGH:
        ElNotification.error(config)
        break
      case ErrorSeverity.MEDIUM:
        ElMessage.error(error.message)
        break
      case ErrorSeverity.LOW:
        ElMessage.warning(error.message)
        break
    }
  }

  // Get error title based on type
  private getErrorTitle(type: ErrorType): string {
    const titles = {
      [ErrorType.NETWORK]: 'Network Error',
      [ErrorType.VALIDATION]: 'Validation Error',
      [ErrorType.AUTHENTICATION]: 'Authentication Error',
      [ErrorType.AUTHORIZATION]: 'Authorization Error',
      [ErrorType.NOT_FOUND]: 'Not Found',
      [ErrorType.SERVER]: 'Server Error',
      [ErrorType.BUSINESS]: 'Business Error',
      [ErrorType.UNKNOWN]: 'Error'
    }
    return titles[type] || 'Error'
  }

  // Get notification type based on severity
  private getNotificationType(severity: ErrorSeverity): 'success' | 'warning' | 'info' | 'error' {
    switch (severity) {
      case ErrorSeverity.CRITICAL:
      case ErrorSeverity.HIGH:
        return 'error'
      case ErrorSeverity.MEDIUM:
        return 'warning'
      case ErrorSeverity.LOW:
        return 'info'
      default:
        return 'error'
    }
  }

  // Get notification duration based on severity
  private getNotificationDuration(severity: ErrorSeverity): number {
    switch (severity) {
      case ErrorSeverity.CRITICAL:
        return 0 // Don't auto-close critical errors
      case ErrorSeverity.HIGH:
        return 8000
      case ErrorSeverity.MEDIUM:
        return 5000
      case ErrorSeverity.LOW:
        return 3000
      default:
        return 5000
    }
  }

  // Log error to internal log
  private logError(error: AppError): void {
    this.errorLog.unshift(error)
    
    // Keep log size manageable
    if (this.errorLog.length > this.maxLogSize) {
      this.errorLog = this.errorLog.slice(0, this.maxLogSize)
    }

    // Log to console for development
    if (process.env.NODE_ENV === 'development') {
      console.error('App Error:', error)
    }

    // TODO: Send to logging service in production
    // this.sendToLoggingService(error)
  }

  // Get error log
  public getErrorLog(): AppError[] {
    return [...this.errorLog]
  }

  // Clear error log
  public clearErrorLog(): void {
    this.errorLog = []
  }

  // Get error statistics
  public getErrorStats(): { [key in ErrorType]: number } {
    const stats = Object.values(ErrorType).reduce((acc, type) => {
      acc[type] = 0
      return acc
    }, {} as { [key in ErrorType]: number })

    this.errorLog.forEach(error => {
      stats[error.type]++
    })

    return stats
  }
}

// Global error handler instance
export const errorHandler = ErrorHandler.getInstance()

// Global error event handlers
export const setupGlobalErrorHandlers = (): void => {
  // Handle unhandled promise rejections
  window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled promise rejection:', event.reason)
    errorHandler.handleGeneralError(
      new Error(event.reason?.message || 'Unhandled promise rejection'),
      'unhandledrejection'
    )
    event.preventDefault()
  })

  // Handle general JavaScript errors
  window.addEventListener('error', (event) => {
    console.error('Global error:', event.error)
    errorHandler.handleGeneralError(
      event.error || new Error(event.message),
      `${event.filename}:${event.lineno}:${event.colno}`
    )
  })

  // Handle Vue errors (if using Vue 3)
  if (typeof window !== 'undefined' && (window as any).app) {
    (window as any).app.config.errorHandler = (error: Error, instance: any, info: string) => {
      console.error('Vue error:', error, info)
      errorHandler.handleGeneralError(error, `Vue: ${info}`)
    }
  }
}

// Utility functions for common error scenarios
export const handleAsyncError = async <T>(
  asyncFn: () => Promise<T>,
  context?: string
): Promise<T | null> => {
  try {
    return await asyncFn()
  } catch (error) {
    if (error instanceof Error) {
      errorHandler.handleGeneralError(error, context)
    } else {
      errorHandler.handleGeneralError(new Error(String(error)), context)
    }
    return null
  }
}

export const handleDOMOperation = <T>(
  domFn: () => T,
  element?: string
): T | null => {
  try {
    return domFn()
  } catch (error) {
    if (error instanceof Error) {
      errorHandler.handleDOMError(error, element)
    } else {
      errorHandler.handleDOMError(new Error(String(error)), element)
    }
    return null
  }
}