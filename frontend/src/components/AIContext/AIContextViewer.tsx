import React, { useState, useEffect } from 'react';
import {
  Card,
  Button,
  Input,
  Space,
  Typography,
  Alert,
  Spin,
  Divider,
  Tag,
  Tooltip,
  Modal,
  InputNumber,
  Row,
  Col,
  Statistic,
} from 'antd';
import {
  FileTextOutlined,
  CopyOutlined,
  DownloadOutlined,
  InfoCircleOutlined,
  SettingOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import { useAIContext } from '../../hooks/useAIContext';
import './AIContextViewer.css';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

export interface AIContextViewerProps {
  taskId?: string;
  onContextGenerated?: (context: string) => void;
  showTaskIdInput?: boolean;
  defaultMaxLength?: number;
}

/**
 * AI Context Viewer Component
 * Provides interface for generating and viewing LLM-optimized context from profiling reports
 */
export const AIContextViewer: React.FC<AIContextViewerProps> = ({
  taskId: propTaskId,
  onContextGenerated,
  showTaskIdInput = true,
  defaultMaxLength = 8000,
}) => {
  const [inputTaskId, setInputTaskId] = useState(propTaskId || '');
  const [customMaxLength, setCustomMaxLength] = useState<number>(defaultMaxLength);
  const [showSettings, setShowSettings] = useState(false);
  const [useCustomLength, setUseCustomLength] = useState(false);

  const {
    loading,
    context,
    contextLength,
    metadata,
    error,
    buildContext,
    buildOptimizedContext,
    checkContext,
    getMetadata,
    clearContext,
    clearError,
  } = useAIContext();

  const currentTaskId = propTaskId || inputTaskId;

  // Load metadata when task ID changes
  useEffect(() => {
    if (currentTaskId) {
      getMetadata(currentTaskId);
    }
  }, [currentTaskId, getMetadata]);

  // Notify parent when context is generated
  useEffect(() => {
    if (context && onContextGenerated) {
      onContextGenerated(context);
    }
  }, [context, onContextGenerated]);

  const handleBuildContext = async () => {
    if (!currentTaskId) {
      return;
    }

    if (useCustomLength) {
      await buildContext(currentTaskId, customMaxLength);
    } else {
      await buildOptimizedContext(currentTaskId);
    }
  };

  const handleCopyContext = () => {
    if (context) {
      navigator.clipboard.writeText(context);
      // You might want to show a success message here
    }
  };

  const handleDownloadContext = () => {
    if (context && currentTaskId) {
      const blob = new Blob([context], { type: 'text/markdown' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `ai-context-${currentTaskId}.md`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }
  };

  const handleClearContext = () => {
    clearContext();
  };

  const renderMetadataInfo = () => {
    if (!metadata) return null;

    return (
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Statistic
            title="Availability"
            value={metadata.available ? 'Available' : 'Not Available'}
            prefix={
              metadata.available ? (
                <CheckCircleOutlined style={{ color: '#52c41a' }} />
              ) : (
                <ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />
              )
            }
          />
        </Col>
        <Col span={6}>
          <Statistic
            title="Estimated Length"
            value={metadata.estimatedLength || 0}
            suffix="chars"
          />
        </Col>
        <Col span={6}>
          <Statistic
            title="Recommended Max"
            value={metadata.maxRecommendedLength || 0}
            suffix="chars"
          />
        </Col>
        <Col span={6}>
          <Statistic
            title="Compression"
            value={metadata.compressionNeeded ? 'Needed' : 'Not Needed'}
            prefix={
              metadata.compressionNeeded ? (
                <InfoCircleOutlined style={{ color: '#faad14' }} />
              ) : (
                <CheckCircleOutlined style={{ color: '#52c41a' }} />
              )
            }
          />
        </Col>
      </Row>
    );
  };

  const renderContextStats = () => {
    if (!context) return null;

    const lines = context.split('\n').length;
    const words = context.split(/\s+/).length;

    return (
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Statistic title="Characters" value={contextLength} />
        </Col>
        <Col span={8}>
          <Statistic title="Lines" value={lines} />
        </Col>
        <Col span={8}>
          <Statistic title="Words" value={words} />
        </Col>
      </Row>
    );
  };

  return (
    <div className="ai-context-viewer">
      <Card
        title={
          <Space>
            <FileTextOutlined />
            <span>AI Context Generator</span>
          </Space>
        }
        extra={
          <Space>
            <Tooltip title="Settings">
              <Button
                icon={<SettingOutlined />}
                onClick={() => setShowSettings(true)}
                type="text"
              />
            </Tooltip>
          </Space>
        }
      >
        {/* Task ID Input */}
        {showTaskIdInput && (
          <div style={{ marginBottom: 16 }}>
            <Text strong>Task ID:</Text>
            <Input
              value={inputTaskId}
              onChange={(e) => setInputTaskId(e.target.value)}
              placeholder="Enter profiling task ID"
              style={{ marginTop: 8 }}
            />
          </div>
        )}

        {/* Error Display */}
        {error && (
          <Alert
            message="Error"
            description={error}
            type="error"
            closable
            onClose={clearError}
            style={{ marginBottom: 16 }}
          />
        )}

        {/* Metadata Info */}
        {renderMetadataInfo()}

        {/* Action Buttons */}
        <Space style={{ marginBottom: 16 }}>
          <Button
            type="primary"
            icon={<FileTextOutlined />}
            onClick={handleBuildContext}
            loading={loading}
            disabled={!currentTaskId || (metadata && !metadata.available)}
          >
            Generate Context
          </Button>
          
          {context && (
            <>
              <Button
                icon={<CopyOutlined />}
                onClick={handleCopyContext}
              >
                Copy
              </Button>
              <Button
                icon={<DownloadOutlined />}
                onClick={handleDownloadContext}
              >
                Download
              </Button>
              <Button
                onClick={handleClearContext}
              >
                Clear
              </Button>
            </>
          )}
        </Space>

        {/* Context Display */}
        {loading && (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Spin size="large" />
            <div style={{ marginTop: 16 }}>
              <Text type="secondary">Generating AI context...</Text>
            </div>
          </div>
        )}

        {context && !loading && (
          <>
            <Divider />
            <Title level={4}>Generated Context</Title>
            
            {/* Context Statistics */}
            {renderContextStats()}
            
            {/* Context Content */}
            <TextArea
              value={context}
              readOnly
              rows={20}
              style={{ fontFamily: 'monospace', fontSize: '12px' }}
              placeholder="AI context will appear here..."
            />
          </>
        )}
      </Card>

      {/* Settings Modal */}
      <Modal
        title="Context Generation Settings"
        open={showSettings}
        onOk={() => setShowSettings(false)}
        onCancel={() => setShowSettings(false)}
        width={500}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <div>
            <Text strong>Length Control:</Text>
            <div style={{ marginTop: 8 }}>
              <Space direction="vertical">
                <label>
                  <input
                    type="radio"
                    checked={!useCustomLength}
                    onChange={() => setUseCustomLength(false)}
                  />
                  <span style={{ marginLeft: 8 }}>Auto-optimize (Recommended)</span>
                </label>
                <label>
                  <input
                    type="radio"
                    checked={useCustomLength}
                    onChange={() => setUseCustomLength(true)}
                  />
                  <span style={{ marginLeft: 8 }}>Custom max length</span>
                </label>
              </Space>
            </div>
          </div>

          {useCustomLength && (
            <div>
              <Text strong>Max Length (characters):</Text>
              <InputNumber
                value={customMaxLength}
                onChange={(value) => setCustomMaxLength(value || defaultMaxLength)}
                min={1000}
                max={50000}
                step={1000}
                style={{ width: '100%', marginTop: 8 }}
              />
            </div>
          )}

          <Alert
            message="Tip"
            description="Auto-optimize mode automatically adjusts context length based on the report size and LLM token limits for optimal performance."
            type="info"
            showIcon
          />
        </Space>
      </Modal>
    </div>
  );
};

export default AIContextViewer;