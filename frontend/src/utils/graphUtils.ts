import {
    Node,
    Edge,
    Position,
    MarkerType
} from "reactflow";
import {
    GRAPH_NODE_WIDTH,
    GRAPH_NODE_HEIGHT,
    EDGE_TYPE_DEFAULT,
    RELATIONSHIP_COLOR_SPOUSE,
    RELATIONSHIP_COLOR_PARENT_CHILD
} from "../config/graphConfig";
import { RelationshipStep } from "../types/game";

// RelationshipStep arayüzü buraya kopyalanabilir veya 
// types klasöründen import edilebilir. Şimdilik kopyalayalım.
/*
interface RelationshipStep {
    personId: number;
    personName: string;
    personGender?: "Erkek" | "Kadın" | string;
    personBirthYear?: number;
    personDeathYear?: number;
    relationshipToNextPerson?: string;
    sourcePerson: boolean;
    targetPerson: boolean;
}
*/

// Tema renkleri için arayüz
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
    // Diğer ebeveyn/çocuk ilişkileri eklenebilir: dede, torun, kardeş vb.
    // if (lowerRel.includes('kardeş')) return '#some_other_color'; 
    return defaultColor;
};

/**
 * Backend'den gelen ilişki yolu verisini React Flow'un anlayacağı
 * nodes ve edges dizisine dönüştürür.
 * Pozisyonlama bu fonksiyonda yapılmaz, layout hook'una bırakılır.
 */
export const transformDataToFlow = (
    path: RelationshipStep[] | undefined,
    themeColors: GraphThemeColors,
): { nodes: Node[]; edges: Edge[] } => {
    if (!path || path.length === 0) {
        return { nodes: [], edges: [] };
    }

    const nodes: Node[] = [];
    const edges: Edge[] = [];

    path.forEach((step, index) => {
        const nodeId = step.personId.toString();
        nodes.push({
            id: nodeId,
            type: 'custom', // Özel düğüm tipi
            position: { x: 0, y: 0 }, // Linter hatasını gidermek için varsayılan pozisyon
            data: {
                name: step.personName,
                gender: step.personGender,
                birthYear: step.personBirthYear,
                deathYear: step.personDeathYear,
                isSource: step.sourcePerson,
                isTarget: step.targetPerson,
                // Dagre'nin kullanması için düğüm boyutları
                width: GRAPH_NODE_WIDTH,
                height: GRAPH_NODE_HEIGHT,
            },
            // sourcePosition ve targetPosition dagre tarafından otomatik yönetilebilir
            // veya belirli portlar tanımlanırsa kullanılabilir.
            // Şimdilik kaldırabilir veya yorum satırı yapabiliriz.
            // sourcePosition: Position.Right, 
            // targetPosition: Position.Left,
        });

        if (index < path.length - 1) {
            const nextStep = path[index + 1];
            const nextNodeId = nextStep.personId.toString();
            const relationshipType = step.relationshipToNextPerson;
            const edgeColor = getEdgeColorByRelationship(relationshipType, themeColors.edgeBaseColor);

            edges.push({
                id: `e${nodeId}-${nextNodeId}`,
                source: nodeId,
                target: nextNodeId,
                type: EDGE_TYPE_DEFAULT,
                markerEnd: {
                    type: MarkerType.ArrowClosed,
                    width: 20,
                    height: 20,
                    color: edgeColor,
                },
                style: {
                    strokeWidth: 2.5,
                    stroke: edgeColor,
                },
                label: relationshipType,
                labelStyle: {
                    fill: '#fff',
                    fontWeight: 'bold',
                    fontSize: 14,
                },
                labelBgPadding: [8, 4],
                labelBgBorderRadius: 4,
                labelBgStyle: {
                    fill: edgeColor,
                    fillOpacity: 1,
                },
                data: {
                    relationshipType: relationshipType,
                }
            });
        }
    });

    return { nodes, edges };
}; 