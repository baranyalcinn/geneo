import React, { useMemo, useEffect, useState } from "react";
import {
  ReactFlow,
  Controls,
  Background,
  MiniMap,
  ReactFlowProvider,
  Node,
  Edge,
  useReactFlow,
  BackgroundVariant,
  useNodesState,
  useEdgesState,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css"; // React Flow stilleri
import {
  Box,
  useTheme,
  alpha,
} from "@mui/material";
import { transformDataToFlow } from "../utils/graphUtils";
import CustomNode from "./RelationshipGraph/CustomNode";
import { useRelationshipGraphLayout } from "../hooks/useRelationshipGraphLayout";
import GraphLoadingIndicator from "./RelationshipGraph/GraphLoadingIndicator";
import GraphEmptyState from "./RelationshipGraph/GraphEmptyState";
import GraphNodeErrorState from "./RelationshipGraph/GraphNodeErrorState";
import SingleNodeView from "./RelationshipGraph/SingleNodeView";
import { RelationshipStep } from "../types/game";

interface RelationshipGraphProps {
  path?: RelationshipStep[];
  height?: string;
  width?: string;
  layoutDirection?: 'TB' | 'LR' | 'BT' | 'RL'; // Layout yönü seçeneği
}

// Bileşen dışında tipleri tanımla - CustomNode import edildiği için nodeTypes'ı güncelle
export const nodeTypes: any = { custom: CustomNode };
export const edgeTypes = {};

// Flow bileşeni - ReactFlow Provider içerisinde
const FlowComponent: React.FC<{
  nodes: Node[];
  edges: Edge[];
  onNodesChange: (changes: any) => void;
  onEdgesChange: (changes: any) => void;
}> = ({ nodes, edges, onNodesChange, onEdgesChange }) => {
  const theme = useTheme();
  const { fitView } = useReactFlow();

  useEffect(() => {
    if (nodes.length > 0) {
      const timeoutId = setTimeout(() => {
        fitView({ padding: 0.2, duration: 300 });
      }, 50);
      return () => clearTimeout(timeoutId);
    }
  }, [nodes, fitView]);

  return (
    <div style={{ width: "100%", height: "100%" }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ padding: 0.25, duration: 450 }}
        style={{
          width: "100%",
          height: "100%",
          background:
            theme.palette.mode === "dark"
              ? alpha(theme.palette.grey[900], 0.9)
              : alpha(theme.palette.grey[50], 0.9),
        }}
        proOptions={{ hideAttribution: true }}
        zoomOnScroll={true}
        panOnScroll={true}
        minZoom={0.5}
        maxZoom={1.5}
      >
      <Controls
        style={{
          left: 15,
          bottom: 15,
          boxShadow: `0 3px 10px ${alpha(theme.palette.common.black, 0.15)}`,
          border: `1px solid ${alpha(theme.palette.divider, 0.2)}`,
          borderRadius: `${Number(theme.shape.borderRadius) * 1.5}px`,
          padding: theme.spacing(0.5),
          gap: theme.spacing(0.5),
        }}
      />
      <Background
        variant={
          theme.palette.mode === "dark"
            ? BackgroundVariant.Dots
            : BackgroundVariant.Lines
        }
        gap={20}
        size={1}
        color={
          theme.palette.mode === "dark"
            ? alpha(theme.palette.primary.main, 0.15)
            : alpha(theme.palette.primary.main, 0.08)
        }
      />
      <MiniMap
        nodeStrokeWidth={3}
        nodeStrokeColor={(n) => {
          if (n.data?.isSource) return theme.palette.success.main;
          if (n.data?.isTarget) return theme.palette.secondary.main;
          return theme.palette.primary.main;
        }}
        nodeColor={(n) => {
          if (n.data?.isSource) return alpha(theme.palette.success.light, 0.7);
          if (n.data?.isTarget)
            return alpha(theme.palette.secondary.light, 0.7);
          return alpha(theme.palette.primary.light, 0.7);
        }}
        nodeBorderRadius={Number(theme.shape.borderRadius) * 0.5}
        style={{
          bottom: 15,
          right: 15,
          height: 100,
          width: 150,
          backgroundColor: alpha(theme.palette.background.paper, 0.9),
          border: `1px solid ${alpha(theme.palette.divider, 0.3)}`,
          borderRadius: `${Number(theme.shape.borderRadius) * 1.5}px`,
          boxShadow: `0 3px 12px ${alpha(theme.palette.common.black, 0.18)}`,
        }}
      />
    </ReactFlow>
    </div>
  );
};

// Ana bileşen
const RelationshipGraph: React.FC<RelationshipGraphProps> = ({
  path,
  height = "600px",
  width = "100%",
  layoutDirection,
}) => {
  // React Flow için minimum boyutları garanti etmek
  const containerHeight = height === "100%" ? "100%" : (parseInt(height as string) < 300 ? "300px" : height);
  const containerWidth = width === "100%" ? "100%" : (parseInt(width as string) < 300 ? "300px" : width);
  const theme = useTheme();
  const [isLoading, setIsLoading] = useState(true);
  const themeMode = theme.palette.mode;

  const themeColorsForFlow = useMemo(
    () => ({
      edgeBaseColor: "#FF0000",
      edgeLabelColor: "#FFFFFF",
      edgeLabelBg: "#FF0000",
      themeMode,
    }),
    [themeMode],
  );

  const { nodes: initialNodes, edges: initialEdges } = useMemo(
    () => transformDataToFlow(path, themeColorsForFlow),
    [path, themeColorsForFlow],
  );

  const layoutedNodes = useRelationshipGraphLayout({
    nodes: initialNodes,
    edges: initialEdges,
    direction: layoutDirection,
  });

  const [nodes, setNodes, onNodesChange] = useNodesState(layoutedNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  useEffect(() => {
    setNodes(layoutedNodes);
  }, [layoutedNodes, setNodes]);

  useEffect(() => {
    setEdges(initialEdges);
  }, [initialEdges, setEdges]);

  useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 50);
    return () => clearTimeout(timer);
  }, [path]);

  if (isLoading) {
    return <GraphLoadingIndicator width={containerWidth} height={containerHeight} />;
  }

  if (!path || path.length === 0) {
    return <GraphEmptyState width={containerWidth} height={containerHeight} />;
  }

  if (initialNodes.length === 0 && path && path.length > 0) {
    return <GraphNodeErrorState width={containerWidth} height={containerHeight} />;
  }
  
  return (
    <Box
      sx={{
        width: containerWidth,
        height: containerHeight,
        minHeight: "350px",
        minWidth: "300px",
        border: `1px solid ${alpha(theme.palette.primary.main, 0.2)}`,
        borderRadius: `${Number(theme.shape.borderRadius) * 1.5}px`,
        overflow: "hidden",
        position: "relative",
        display: "flex",
        boxShadow: `0 5px 15px ${alpha(theme.palette.common.black, 0.08)}`,
        background:
          theme.palette.mode === "dark"
            ? alpha(theme.palette.grey[900], 0.5)
            : alpha(theme.palette.grey[50], 0.5),
        backdropFilter: "blur(8px)",
        "& .react-flow__node": {
          background: "transparent",
          border: "none",
          width: "auto",
          height: "auto",
        },
        "& .react-flow__handle": {
          width: 12,
          height: 12,
          background: theme.palette.primary.main,
          border: `2px solid ${theme.palette.background.paper}`,
          zIndex: 5,
        },
        "& .react-flow__edge-path": {
          strokeWidth: 2,
          strokeLinecap: "butt",
          strokeLinejoin: "miter",
        },
        "& .react-flow__edge-text": {
          fontWeight: "bold",
          fontSize: "0.85rem",
          filter: "none",
          textShadow: "none",
        },
        "& .react-flow__edge-textbg": {
          borderRadius: 0,
          padding: "2px 4px",
          boxShadow: "none",
        },
        "& .react-flow__attribution": {
          background: "transparent",
          color: theme.palette.text.secondary,
        },
        "& .react-flow__pane": {
          width: "100%",
          height: "100%",
        },
      }}
    >
      <ReactFlowProvider>
        <FlowComponent 
          nodes={nodes} 
          edges={edges} 
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
        />
      </ReactFlowProvider>
    </Box>
  );
};

export default RelationshipGraph;
