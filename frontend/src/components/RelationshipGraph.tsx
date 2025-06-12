import React, { useEffect, useState } from 'react';
import {
  ReactFlow,
  useNodesState,
  useEdgesState,
  Controls,
  Background,
  MiniMap,
  Node,
  Edge,
  Position,
  ReactFlowProvider,
  BackgroundVariant,
} from '@xyflow/react';
import { Box, useTheme, alpha } from '@mui/material';
import dagre from 'dagre';
// RelationshipStep tipini yerel olarak tanımlayalım
interface RelationshipStep {
  personId: number | string;
  personName: string;
  personGender?: string;
  personBirthYear?: number;
  personDeathYear?: number;
  relationshipToNextPerson?: string;
  sourcePerson?: boolean;
  targetPerson?: boolean;
}

// Graph utils - gerekli sabitler ve fonksiyonlar
const GRAPH_NODE_WIDTH = 200;
const GRAPH_NODE_HEIGHT = 130;
const EDGE_TYPE_DEFAULT = 'smoothstep';
const RELATIONSHIP_COLOR_SPOUSE = '#4CAF50';
const RELATIONSHIP_COLOR_PARENT_CHILD = '#2196F3';

interface GraphThemeColors {
  edgeBaseColor: string;
  edgeLabelColor: string;
  edgeLabelBg: string;
}

const getEdgeColorByRelationship = (relationshipType: string | undefined, defaultColor: string): string => {
  if (!relationshipType) return defaultColor;
  const lowerRel = relationshipType.toLowerCase();
  if (lowerRel.includes('eş') || lowerRel.includes('koca') || lowerRel.includes('karı')) {
    return RELATIONSHIP_COLOR_SPOUSE;
  }
  if (lowerRel.includes('baba') || lowerRel.includes('anne') || lowerRel.includes('çocuk') || lowerRel.includes('oğul') || lowerRel.includes('kız')) {
    return RELATIONSHIP_COLOR_PARENT_CHILD;
  }
  return defaultColor;
};

const transformDataToFlow = (
  path: RelationshipStep[] | undefined,
  themeColors: GraphThemeColors,
): { nodes: Node[]; edges: Edge[] } => {
  if (!path || path.length === 0) {
    return { nodes: [], edges: [] };
  }

  const nodes: Node[] = [];
  const edges: Edge[] = [];
  const existingNodeIds = new Set<string>();

  path.forEach((step, index) => {
    const nodeId = step.personId.toString();
    
    if (!existingNodeIds.has(nodeId)) {
      nodes.push({
        id: nodeId,
        type: 'custom',
        position: { x: 0, y: 0 },
        data: {
          name: step.personName,
          gender: step.personGender,
          birthYear: step.personBirthYear,
          deathYear: step.personDeathYear,
          isSource: step.sourcePerson,
          isTarget: step.targetPerson,
          width: GRAPH_NODE_WIDTH,
          height: GRAPH_NODE_HEIGHT,
        },
      });
      existingNodeIds.add(nodeId);
    }

    if (index < path.length - 1) {
      const nextStep = path[index + 1];
      const nextNodeId = nextStep.personId.toString();
      const relationshipType = step.relationshipToNextPerson;
      const edgeColor = getEdgeColorByRelationship(relationshipType, themeColors.edgeBaseColor);

      edges.push({
        id: `e${nodeId}-${nextNodeId}-${index}`,
        source: nodeId,
        target: nextNodeId,
        type: EDGE_TYPE_DEFAULT,
        style: {
          strokeWidth: 2.5,
          stroke: edgeColor,
        },
        label: relationshipType || "?",
        labelStyle: {
          fill: '#fff',
          fontWeight: 'bold',
          fontSize: 12,
          fontFamily: 'Inter, system-ui, sans-serif',
        },
        labelBgPadding: [10, 6],
        labelBgBorderRadius: 8,
        labelBgStyle: {
          fill: edgeColor,
          fillOpacity: 0.95,
          stroke: '#fff',
          strokeWidth: 1,
        },
        data: {
          relationshipType: relationshipType,
        }
      } as any);
    }
  });

  return { nodes, edges };
};
import CustomNode from './RelationshipGraph/CustomNode';
import GraphLoadingIndicator from './RelationshipGraph/GraphLoadingIndicator';
import GraphEmptyState from './RelationshipGraph/GraphEmptyState';
import GraphNodeErrorState from './RelationshipGraph/GraphNodeErrorState';
import SingleNodeView from './RelationshipGraph/SingleNodeView';
import 'reactflow/dist/style.css';

const nodeTypes = {
  custom: CustomNode,
};

const getLayoutedElements = (
  nodes: Node[],
  edges: Edge[],
  direction: 'TB' | 'LR' | 'BT' | 'RL'
) => {
  const dagreGraph = new dagre.graphlib.Graph();
  dagreGraph.setDefaultEdgeLabel(() => ({}));
  const nodeWidth = 172;
  const nodeHeight = 50; 
  dagreGraph.setGraph({ rankdir: direction, nodesep: 25, ranksep: 60 });

  nodes.forEach((node) => {
    dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight });
  });

  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target);
  });

  dagre.layout(dagreGraph);

  nodes.forEach((node) => {
    const nodeWithPosition = dagreGraph.node(node.id);
    node.targetPosition = direction === 'TB' ? Position.Top : Position.Left;
    node.sourcePosition = direction === 'TB' ? Position.Bottom : Position.Right;
    node.position = {
      x: nodeWithPosition.x - nodeWidth / 2,
      y: nodeWithPosition.y - nodeHeight / 2,
    };
  });

  return { nodes, edges };
};

const FlowComponent: React.FC<{
  nodes: Node[];
  edges: Edge[];
  onNodesChange: (changes: any) => void;
  onEdgesChange: (changes: any) => void;
}> = ({ nodes, edges, onNodesChange, onEdgesChange }) => {
  const theme = useTheme();
  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      nodeTypes={nodeTypes}
      fitView
      proOptions={{ hideAttribution: true }}
      style={{
        background: theme.palette.mode === 'dark' ? alpha(theme.palette.background.default, 0.8) : alpha(theme.palette.grey[50], 0.8)
      }}
    >
      <Controls />
      <Background
        variant={theme.palette.mode === 'dark' ? BackgroundVariant.Dots : BackgroundVariant.Lines}
        gap={15}
        size={1}
        color={alpha(theme.palette.primary.main, 0.1)}
      />
      <MiniMap nodeStrokeWidth={3} pannable zoomable />
    </ReactFlow>
  );
}

interface RelationshipGraphProps {
  path?: RelationshipStep[];
  height?: string;
  width?: string;
  layoutDirection?: 'TB' | 'LR' | 'BT' | 'RL';
}

const RelationshipGraph: React.FC<RelationshipGraphProps> = ({
  path,
  height = '400px',
  width = '100%',
  layoutDirection = 'TB',
}) => {
  const theme = useTheme();
  const [nodes, setNodes, onNodesChange] = useNodesState([] as any);
  const [edges, setEdges, onEdgesChange] = useEdgesState([] as any);
  const [status, setStatus] = useState<'loading' | 'error' | 'empty' | 'single' | 'ready'>('loading');

  useEffect(() => {
    setStatus('loading');
    console.log('RelationshipGraph - Gelen path:', path);
    if (!path || path.length === 0) {
      setNodes([]);
      setEdges([]);
      setStatus('empty');
      return;
    }

    try {
      const themeColors = {
        edgeBaseColor: theme.palette.divider,
        edgeLabelColor: theme.palette.text.secondary,
        edgeLabelBg: theme.palette.background.default,
      };

      const { nodes: initialNodes, edges: initialEdges } = transformDataToFlow(path, themeColors);
      
      if (initialNodes.length === 0) {
        setNodes([]);
        setEdges([]);
        setStatus('empty');
        return;
      }
      
      if (initialNodes.length === 1) {
        setNodes(initialNodes.map((n: Node) => ({...n, position: {x: 0, y: 0}})));
        setEdges([]);
        setStatus('single');
        return;
      }
      
      const { nodes: layoutedNodes, edges: layoutedEdges } = getLayoutedElements(
        initialNodes,
        initialEdges,
        layoutDirection
      );

      setNodes(layoutedNodes);
      setEdges(layoutedEdges);
      setStatus('ready');

    } catch (e) {
      console.error("Failed to build relationship graph:", e);
      setNodes([]);
      setEdges([]);
      setStatus('error');
    }
  }, [path, theme.palette.mode, layoutDirection, setNodes, setEdges]);

  const renderContent = () => {
    switch (status) {
      case 'loading':
        return <GraphLoadingIndicator width={width} height={height} />;
      case 'empty':
        return <GraphEmptyState width={width} height={height} messageBody="Görüntülenecek ilişki yolu bulunamadı." />;
      case 'error':
        return <GraphNodeErrorState width={width} height={height} />;
      case 'single':
        return <SingleNodeView node={nodes[0]} width={width} height={height} />;
      case 'ready':
        return (
          <ReactFlowProvider>
            <FlowComponent
              nodes={nodes}
              edges={edges}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
            />
          </ReactFlowProvider>
        );
      default:
        return <GraphEmptyState width={width} height={height} />;
    }
  };

  return (
    <Box
      sx={{
        height,
        width,
        minHeight: '300px',
        position: 'relative',
        borderRadius: 2,
        overflow: 'hidden',
        border: '1px solid',
        borderColor: theme.palette.divider,
        backgroundColor: theme.palette.background.paper,
      }}
    >
      {renderContent()}
    </Box>
  );
};

export default RelationshipGraph;
