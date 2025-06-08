import React from 'react';
import { Box } from '@mui/material';
import { NodeProps, Position } from '@xyflow/react';
import CustomNode from './CustomNode';

interface SingleNodeViewProps {
  node: any;
  width?: string;
  height?: string;
}

const SingleNodeView: React.FC<SingleNodeViewProps> = ({
  node,
  width = "100%",
  height = "100%",
}) => {
  // CustomNode'a geçilecek NodeProps benzeri bir obje oluşturalım
  const nodePropsForCustomNode: any = {
    id: node.id || 'single-node',
    data: node.data || node,
    type: 'custom',
    selected: false,
    isConnectable: false,
    zIndex: 1,
    dragging: false,
    targetPosition: Position.Left,
    sourcePosition: Position.Right,
    width: node.width,
    height: node.height,
    dragHandle: node.dragHandle,
    parentId: node.parentId,
    deletable: false,
    focusable: node.focusable,
    selectable: node.selectable,
    measured: node.measured || { width: 0, height: 0 },
    resizing: false,
  };

  return (
    <Box sx={{ 
      width: width, 
      height: height, 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center' 
    }}>
      <Box sx={{ width: 180 }}> {/* CustomNode'un yaklaşık genişliği */} 
        <CustomNode {...nodePropsForCustomNode} />
      </Box>
    </Box>
  );
};

export default SingleNodeView; 