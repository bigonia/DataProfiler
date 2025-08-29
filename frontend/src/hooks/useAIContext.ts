import { useState, useCallback } from 'react';
import { aiContextApi, AIContextResponse, AIContextCheckResponse, AIContextMetadataResponse } from '../api/aiContext';
import { message } from 'antd';

export interface UseAIContextReturn {
  // State
  loading: boolean;
  context: string | null;
  contextLength: number;
  metadata: AIContextMetadataResponse | null;
  error: string | null;

  // Actions
  buildContext: (taskId: string, maxLength?: number) => Promise<void>;
  buildOptimizedContext: (taskId: string) => Promise<void>;
  checkContext: (taskId: string) => Promise<boolean>;
  getMetadata: (taskId: string) => Promise<void>;
  clearContext: () => void;
  clearError: () => void;
}

/**
 * Custom hook for AI Context operations
 * Provides state management and actions for building LLM context from profiling reports
 */
export const useAIContext = (): UseAIContextReturn => {
  const [loading, setLoading] = useState(false);
  const [context, setContext] = useState<string | null>(null);
  const [contextLength, setContextLength] = useState(0);
  const [metadata, setMetadata] = useState<AIContextMetadataResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  /**
   * Build AI context for the given task ID
   */
  const buildContext = useCallback(async (taskId: string, maxLength?: number) => {
    if (!taskId) {
      setError('Task ID is required');
      message.error('Task ID is required');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await aiContextApi.buildContext(taskId, maxLength);
      
      if (response.success && response.context) {
        setContext(response.context);
        setContextLength(response.contextLength || 0);
        message.success(`AI context built successfully (${response.contextLength} characters)`);
      } else {
        throw new Error(response.error || 'Failed to build AI context');
      }
    } catch (err: any) {
      const errorMessage = err.message || 'Failed to build AI context';
      setError(errorMessage);
      message.error(errorMessage);
      console.error('Build context error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Build optimized AI context with automatic length adjustment
   */
  const buildOptimizedContext = useCallback(async (taskId: string) => {
    if (!taskId) {
      setError('Task ID is required');
      message.error('Task ID is required');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await aiContextApi.buildOptimizedContext(taskId);
      
      if (response.success && response.context) {
        setContext(response.context);
        setContextLength(response.contextLength || 0);
        message.success(`Optimized AI context built successfully (${response.contextLength} characters)`);
      } else {
        throw new Error(response.error || 'Failed to build optimized AI context');
      }
    } catch (err: any) {
      const errorMessage = err.message || 'Failed to build optimized AI context';
      setError(errorMessage);
      message.error(errorMessage);
      console.error('Build optimized context error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Check if AI context can be built for the given task ID
   */
  const checkContext = useCallback(async (taskId: string): Promise<boolean> => {
    if (!taskId) {
      setError('Task ID is required');
      return false;
    }

    setError(null);

    try {
      const response = await aiContextApi.checkContext(taskId);
      
      if (response.success) {
        return response.canBuildContext || false;
      } else {
        throw new Error(response.error || 'Failed to check AI context availability');
      }
    } catch (err: any) {
      const errorMessage = err.message || 'Failed to check AI context availability';
      setError(errorMessage);
      console.error('Check context error:', err);
      return false;
    }
  }, []);

  /**
   * Get AI context metadata
   */
  const getMetadata = useCallback(async (taskId: string) => {
    if (!taskId) {
      setError('Task ID is required');
      return;
    }

    setError(null);

    try {
      const response = await aiContextApi.getContextMetadata(taskId);
      
      if (response.success) {
        setMetadata(response);
      } else {
        throw new Error(response.error || 'Failed to get AI context metadata');
      }
    } catch (err: any) {
      const errorMessage = err.message || 'Failed to get AI context metadata';
      setError(errorMessage);
      console.error('Get metadata error:', err);
    }
  }, []);

  /**
   * Clear current context
   */
  const clearContext = useCallback(() => {
    setContext(null);
    setContextLength(0);
    setMetadata(null);
    setError(null);
  }, []);

  /**
   * Clear error state
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    // State
    loading,
    context,
    contextLength,
    metadata,
    error,

    // Actions
    buildContext,
    buildOptimizedContext,
    checkContext,
    getMetadata,
    clearContext,
    clearError,
  };
};

export default useAIContext;