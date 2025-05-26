import React, { useMemo, useCallback, useEffect, useState, ComponentType } from 'react';
import {
  ReactFlow,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  ReactFlowProvider,
  NodeTypes,
  BackgroundVariant,
  NodeProps,
  Node,
  Edge,
  Connection,
  useReactFlow,
  Viewport,
} from '@xyflow/react';
import { Box, Paper, Typography } from '@mui/material';
import AccountTreeIcon from '@mui/icons-material/AccountTree';

import { useFamilyTree } from '../../context/FamilyTreeContext';
import { useLanguage } from '../../context/LanguageContext';
import { useThemeContext } from '../../context/ThemeContext';
import { transformToFlowData } from '../../utils/familyTreeUtils';
import { getLayoutedElements } from '../../utils/layoutUtils';
import PersonNode from './PersonNode';
import LoadingIndicator from '../ui/LoadingIndicator';
import ErrorMessage from '../ui/ErrorMessage';
import EmptyState from '../ui/EmptyState';
import { Person, PersonNodeData } from '../../types/Person';

import '@xyflow/react/dist/style.css';
import dagre from 'dagre';

const nodeTypes: NodeTypes = {
  personNode: PersonNode as any,
};

const nodeWidth = 130;
const nodeHeight = 150;

const FamilyTreeReactFlow: React.FC = () => {
  const { t } = useLanguage();
  const { mode } = useThemeContext();
  const { treeData, loading, error, selectedPerson } = useFamilyTree();

  const [layoutedNodes, setLayoutedNodes] = useState<Node[]>([]);
  const [layoutedEdges, setLayoutedEdges] = useState<Edge[]>([]);

  useEffect(() => {
    if (treeData) {
      const { nodes: initialNodes, edges: initialEdges } = transformToFlowData(treeData);
      
      const { nodes: positionedNodes, edges: positionedEdges } = getLayoutedElements(
        initialNodes,
        initialEdges,
        {
          direction: 'TB',
          nodeWidth: nodeWidth,
          nodeHeight: nodeHeight,
          spacing: 70,
        }
      );
      
      setLayoutedNodes(positionedNodes);
      setLayoutedEdges(positionedEdges);
    } else {
      setLayoutedNodes([]);
      setLayoutedEdges([]);
    }
  }, [treeData]);

  const [nodes, setNodes, onNodesChange] = useNodesState(layoutedNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(layoutedEdges);

  useEffect(() => {
    setNodes(layoutedNodes);
    setEdges(layoutedEdges);
  }, [layoutedNodes, layoutedEdges, setNodes, setEdges]);

  const onConnect = useCallback(
    (params: any) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  if (loading && !treeData && selectedPerson) {
    return <LoadingIndicator />;
  }
  if (error && !treeData) {
    return <ErrorMessage message={error} />;
  }
  if (!loading && !error && !treeData && selectedPerson) {
    return <EmptyState message={t('selectPersonToShowTree')} />;
  }
  if (loading || layoutedNodes.length === 0 && selectedPerson) {
    return <LoadingIndicator />;
  }
  if (selectedPerson && layoutedNodes.length === 0 && !loading && !error) {
    return <ErrorMessage message={t('errorGeneratingTreeLayout')} />;
  }

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
        <Background variant={BackgroundVariant['Dots']} gap={12} size={1} />
      </ReactFlow>
    </Box>
  );
};

const ProvidedFamilyTreeReactFlow: React.FC = () => (
  <ReactFlowProvider>
    <FamilyTreeReactFlow />
  </ReactFlowProvider>
);

export default ProvidedFamilyTreeReactFlow; 