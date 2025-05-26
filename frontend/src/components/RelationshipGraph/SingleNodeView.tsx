import React from 'react';
import { Box } from '@mui/material';
import { NodeProps, Position } from 'reactflow';
import CustomNode from './CustomNode'; // CustomNode aynı dizinde veya doğru yolda olmalı

interface SingleNodeViewProps {
  node: NodeProps['data']; // CustomNode'un beklediği data tipi
  width?: string;
  height?: string;
}

const SingleNodeView: React.FC<SingleNodeViewProps> = ({
  node,
  width = "100%",
  height = "100%",
}) => {
  // CustomNode'a geçilecek NodeProps benzeri bir obje oluşturalım
  // CustomNode'un beklentilerine göre bu props'ları ayarlamanız gerekebilir.
  const nodePropsForCustomNode: NodeProps = {
    id: node.id || 'single-node', // Eğer node.id yoksa varsayılan bir id
    data: node, // Gelen node verisi doğrudan data olarak geçiliyor
    type: 'custom',
    selected: false,
    isConnectable: false,
    xPos: 0, // Merkezi konum için x
    yPos: 0, // Merkezi konum için y
    zIndex: 1,
    dragging: false,
    targetPosition: Position.Left,
    sourcePosition: Position.Right,
    // CustomNode'un beklediği diğer zorunlu olmayan propslar varsa buraya eklenebilir
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