export { AIContextViewer } from './AIContextViewer';
export type { AIContextViewerProps } from './AIContextViewer';

// Re-export related types and hooks for convenience
export { useAIContext } from '../../hooks/useAIContext';
export type { UseAIContextReturn } from '../../hooks/useAIContext';

export {
  aiContextApi,
  type AIContextResponse,
  type AIContextCheckResponse,
  type AIContextMetadataResponse,
} from '../../api/aiContext';