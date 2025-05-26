import { Node, Edge, Position } from '@xyflow/react';
import dagre from 'dagre';

// Define layout options
interface LayoutOptions {
  direction: 'TB' | 'LR'; // Top-Bottom or Left-Right
  nodeWidth: number;
  nodeHeight: number;
  spacing?: number; // Optional spacing between nodes
}

const defaultOptions: Required<Omit<LayoutOptions, 'direction'>> = {
  nodeWidth: 150, // Default width, adjust based on PersonNode
  nodeHeight: 150, // Default height, adjust based on PersonNode
  spacing: 50, // Default spacing
};

export const getLayoutedElements = (
  nodes: Node[], 
  edges: Edge[], 
  options: LayoutOptions
): { nodes: Node[]; edges: Edge[] } => {
  
  const mergedOptions = { ...defaultOptions, ...options };
  const { direction, nodeWidth, nodeHeight, spacing } = mergedOptions;

  const g = new dagre.graphlib.Graph().setDefaultEdgeLabel(() => ({}));
  g.setGraph({ 
    rankdir: direction,
    nodesep: spacing, // Vertical separation
    ranksep: spacing, // Horizontal separation
    marginx: 20,
    marginy: 20,
  });

  // Add nodes to the graph
  nodes.forEach((node) => {
    g.setNode(node.id, { 
      label: node.data?.label || node.id, // Use label or id for debugging
      width: nodeWidth, 
      height: nodeHeight 
    });
  });

  // Add edges to the graph
  edges.forEach((edge) => {
    g.setEdge(edge.source, edge.target);
  });

  // Calculate the layout
  dagre.layout(g);

  // Update node positions based on the layout
  const layoutedNodes = nodes.map((node): Node => {
    const nodeWithPosition = g.node(node.id);
    const isHorizontal = direction === 'LR';

    // We need to shift the node position (anchor=center) 
    // to the top-left corner expected by React Flow.
    const position = {
      x: nodeWithPosition.x - nodeWidth / 2,
      y: nodeWithPosition.y - nodeHeight / 2,
    };

    return {
      ...node,
      position,
      targetPosition: isHorizontal ? Position.Left : Position.Top,
      sourcePosition: isHorizontal ? Position.Right : Position.Bottom,
    };
  });

  return { nodes: layoutedNodes, edges };
}; 