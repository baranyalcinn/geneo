import React, { useCallback, useEffect, useState } from 'react';
import {
  ReactFlow,
  Background,
  Controls,
  useNodesState,
  useEdgesState,
  addEdge,
  Node,
  Edge,
  Connection,
} from '@xyflow/react';
import { Box } from '@mui/material';
import '@xyflow/react/dist/style.css';

import { PersonNode } from './PersonNode';
import { useFamilyTree } from '../../context/FamilyTreeContext';
import { useLanguage } from '../../context/LanguageContext';
import { useThemeContext } from '../../context/ThemeContext';
import { PersonNodeData } from '../../types/Person';

// Custom node types
const nodeTypes = {
  person: PersonNode,
};

interface FamilyTreeReactFlowProps {
  onPersonClick?: (personId: string) => void;
}

export const FamilyTreeReactFlow: React.FC<FamilyTreeReactFlowProps> = ({
  onPersonClick
}) => {
  const { t } = useLanguage();
  const { mode } = useThemeContext();
  const { allPersons } = useFamilyTree();

  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);

  useEffect(() => {
    if (allPersons && allPersons.length > 0) {
      // Simple layout for now - convert persons to nodes
      const newNodes: Node[] = allPersons.map((person, index) => ({
        id: person.id.toString(),
        type: 'person',
        position: { x: (index % 3) * 200, y: Math.floor(index / 3) * 200 },
        data: { person } as PersonNodeData,
      }));
      
      setNodes(newNodes);
      setEdges([]);
    }
  }, [allPersons, setNodes, setEdges]);

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  return (
    <Box sx={{ 
      flexGrow: 1, 
      width: '100%', 
      height: '85vh',
      minHeight: '600px', 
      overflow: 'hidden',
      backgroundColor: mode === 'dark' ? 'rgba(25, 30, 45, 0.8)' : 'rgba(230, 240, 255, 0.4)',
      borderRadius: 2,
      backdropFilter: 'blur(5px)',
      border: mode === 'dark' ? '1px solid rgba(80, 80, 80, 0.3)' : '1px solid rgba(200, 220, 240, 0.3)',
    }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        nodeTypes={nodeTypes}
        fitView
      >
        <Controls />
        <Background gap={12} size={1} />
      </ReactFlow>
    </Box>
  );
};

export default FamilyTreeReactFlow; 