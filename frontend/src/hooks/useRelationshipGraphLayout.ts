import { useMemo } from 'react';
import { Node, Edge } from '@xyflow/react';
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
    direction?: 'TB' | 'LR' | 'BT' | 'RL'; // Tüm yön seçeneklerini destekliyoruz
}

export const useRelationshipGraphLayout = ({
    nodes,
    edges,
    direction = GRAPH_LAYOUT_RANKDIR as 'TB' | 'LR' | 'BT' | 'RL' // Varsayılan yön config'den
}: UseRelationshipGraphLayoutProps): Node[] => {

    const layoutedNodes = useMemo(() => {
        if (!nodes || nodes.length === 0) {
            return [];
        }



        // Her seferinde yeni bir dagre graph instance'ı oluştur
        const dagreGraph = new dagre.graphlib.Graph();
        dagreGraph.setDefaultEdgeLabel(() => ({}));

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
            const nodeWidth = (node.data as any)?.width ?? GRAPH_NODE_WIDTH;
            const nodeHeight = (node.data as any)?.height ?? GRAPH_NODE_HEIGHT;
            dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight });
        });

        edges.forEach((edge) => {
            dagreGraph.setEdge(edge.source, edge.target);
        });

        dagre.layout(dagreGraph);

        const result = nodes.map((node: Node) => {
            const nodeWithPosition = dagreGraph.node(node.id);
            if (nodeWithPosition) {
                return {
                    ...node,
                    // Dagre düğümün merkezini verir, React Flow sol üst köşeyi bekler.
                    // Bu yüzden düğüm genişliğinin/yüksekliğinin yarısını çıkarıyoruz.
                    position: {
                        x: nodeWithPosition.x - ((node.data as any)?.width ?? GRAPH_NODE_WIDTH) / 2,
                        y: nodeWithPosition.y - ((node.data as any)?.height ?? GRAPH_NODE_HEIGHT) / 2,
                    },
                };
            }
            return node; // Pozisyon bulunamazsa orijinal düğümü döndür
        });



        return result;

    }, [nodes, edges, direction]);

    return layoutedNodes;
}; 