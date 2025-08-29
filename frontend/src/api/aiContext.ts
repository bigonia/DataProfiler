import { request } from './request';

/**
 * AI Context API interfaces
 */
export interface AIContextResponse {
  success: boolean;
  taskId: string;
  context?: string;
  contextLength?: number;
  timestamp: number;
  error?: string;
}

export interface AIContextCheckResponse {
  success: boolean;
  taskId: string;
  canBuildContext?: boolean;
  estimatedLength?: number;
  timestamp: number;
  error?: string;
}

export interface AIContextMetadataResponse {
  success: boolean;
  taskId: string;
  available?: boolean;
  estimatedLength?: number;
  maxRecommendedLength?: number;
  compressionNeeded?: boolean;
  timestamp: number;
  error?: string;
}

/**
 * AI Context Service API
 */
export const aiContextApi = {
  /**
   * Build AI context for LLM based on task ID
   */
  buildContext: (taskId: string, maxLength?: number): Promise<AIContextResponse> => {
    const params = maxLength ? { maxLength } : {};
    return request.get(`/api/ai-context/build/${taskId}`, { params });
  },

  /**
   * Check if AI context can be built for the given task ID
   */
  checkContext: (taskId: string): Promise<AIContextCheckResponse> => {
    return request.get(`/api/ai-context/check/${taskId}`);
  },

  /**
   * Get AI context metadata without building full context
   */
  getContextMetadata: (taskId: string): Promise<AIContextMetadataResponse> => {
    return request.get(`/api/ai-context/metadata/${taskId}`);
  },

  /**
   * Build context with automatic length optimization
   */
  buildOptimizedContext: async (taskId: string): Promise<AIContextResponse> => {
    try {
      // First check metadata to determine optimal length
      const metadata = await aiContextApi.getContextMetadata(taskId);
      
      if (!metadata.success || !metadata.available) {
        throw new Error(metadata.error || 'Context not available for this task');
      }

      // Use recommended length if compression is needed
      const maxLength = metadata.compressionNeeded ? metadata.maxRecommendedLength : undefined;
      
      return await aiContextApi.buildContext(taskId, maxLength);
    } catch (error) {
      throw error;
    }
  }
};

export default aiContextApi;