import { useMemo } from 'react';
import { Node, Edge } from 'reactflow';
import dagre from 'dagre';
import {
    GRAPH_NODE_WIDTH,
    GRAPH_NODE_HEIGHT,
    GRAPH_LAYOUT_RANKDIR,
    GRAPH_LAYOUT_ALIGN,
    GRAPH_LAYOUT_NODESEP,
    GRAPH_LAYOUT_RANKSEP,
    GRAPH_LAYOUT_MARGIN_X,
    GRAPH_LAYOUT_MARGIN_Y
} from '../config/graphConfig';

interface UseRelationshipGraphLayoutProps {
    nodes: Node[];
    edges: Edge[];
    direction?: 'TB' | 'LR'; // Opsiyonel olarak yön belirtmek için
}

const dagreGraph = new dagre.graphlib.Graph();
dagreGraph.setDefaultEdgeLabel(() => ({}));

export const useRelationshipGraphLayout = ({
    nodes,
    edges,
    direction = GRAPH_LAYOUT_RANKDIR as 'TB' | 'LR' // Varsayılan yön config'den
}: UseRelationshipGraphLayoutProps): Node[] => {

    const layoutedNodes = useMemo(() => {
        if (!nodes || nodes.length === 0) {
            return [];
        }

        dagreGraph.setGraph({
            rankdir: direction,
            align: GRAPH_LAYOUT_ALIGN,
            nodesep: GRAPH_LAYOUT_NODESEP,
            ranksep: GRAPH_LAYOUT_RANKSEP,
            marginx: GRAPH_LAYOUT_MARGIN_X,
            marginy: GRAPH_LAYOUT_MARGIN_Y,
        });

        nodes.forEach((node) => {
            // graphUtils'ta node.data içine width ve height ekledik, onları kullanalım
            const nodeWidth = node.data?.width || GRAPH_NODE_WIDTH;
            const nodeHeight = node.data?.height || GRAPH_NODE_HEIGHT;
            dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight });
        });

        edges.forEach((edge) => {
            dagreGraph.setEdge(edge.source, edge.target);
        });

        dagre.layout(dagreGraph);

        return nodes.map((node: Node) => {
            const nodeWithPosition = dagreGraph.node(node.id);
            if (nodeWithPosition) {
                return {
                    ...node,
                    // Dagre düğümün merkezini verir, React Flow sol üst köşeyi bekler.
                    // Bu yüzden düğüm genişliğinin/yüksekliğinin yarısını çıkarıyoruz.
                    position: {
                        x: nodeWithPosition.x - (node.data?.width || GRAPH_NODE_WIDTH) / 2,
                        y: nodeWithPosition.y - (node.data?.height || GRAPH_NODE_HEIGHT) / 2,
                    },
                };
            }
            return node; // Pozisyon bulunamazsa orijinal düğümü döndür
        });

    }, [nodes, edges, direction]);

    return layoutedNodes;
}; 