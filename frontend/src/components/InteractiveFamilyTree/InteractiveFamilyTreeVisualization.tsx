import React, { useState, useCallback, useMemo } from 'react';
import { Handle, Position, Node, Edge, ReactFlow, Background, Controls, MiniMap } from 'reactflow';
import { Person } from '../../types/Person';
import { RelationshipType } from '../../types/relationship';

interface FamilyMember extends Node {
  id: string;
  data: {
    person: Person;
    isHighlighted?: boolean;
    isSelected?: boolean;
    relationshipToSelected?: string;
  };
}

interface FamilyTreeVisualizationProps {
  familyData: Person[];
  relationships: Array<{
    person1Id: number;
    person2Id: number;
    type: RelationshipType;
  }>;
  onPersonSelect?: (person: Person) => void;
  selectedPersonId?: number;
  highlightedRelationship?: {
    person1Id: number;
    person2Id: number;
  };
}

const PersonNode = ({ data }: { data: FamilyMember['data'] }) => {
  const { person, isHighlighted, isSelected, relationshipToSelected } = data;
  
  return (
    <div 
      className={`
        relative p-4 border-2 rounded-lg bg-white shadow-md transition-all duration-300
        ${isSelected ? 'border-blue-500 bg-blue-50' : 'border-gray-200'}
        ${isHighlighted ? 'border-yellow-400 bg-yellow-50 shadow-lg scale-105' : ''}
        hover:shadow-lg hover:scale-102 cursor-pointer
      `}
    >
      <Handle type="target" position={Position.Top} />
      <Handle type="source" position={Position.Bottom} />
      
      <div className="text-center">
        <div className="w-12 h-12 mx-auto mb-2 rounded-full bg-gradient-to-b from-blue-400 to-blue-600 flex items-center justify-center">
          <span className="text-white font-bold text-lg">
            {person.firstName.charAt(0)}
          </span>
        </div>
        
        <h3 className="font-semibold text-gray-800">
          {person.firstName} {person.lastName}
        </h3>
        
        <div className="text-xs text-gray-500 mt-1">
          {person.birthDate && `${new Date(person.birthDate).getFullYear()}`}
          {person.birthDate && person.deathDate && ' - '}
          {person.deathDate && `${new Date(person.deathDate).getFullYear()}`}
        </div>
        
        {relationshipToSelected && (
          <div className="mt-2 px-2 py-1 bg-green-100 text-green-800 text-xs rounded">
            {relationshipToSelected}
          </div>
        )}
      </div>
    </div>
  );
};

const nodeTypes = {
  person: PersonNode,
};

export const InteractiveFamilyTreeVisualization: React.FC<FamilyTreeVisualizationProps> = ({
  familyData,
  relationships,
  onPersonSelect,
  selectedPersonId,
  highlightedRelationship
}) => {
  const [selectedNode, setSelectedNode] = useState<string | null>(null);

  // Nodes ve edges'i memo ile optimize ediyoruz
  const { nodes, edges } = useMemo(() => {
    const nodeMap = new Map<number, FamilyMember>();
    const edgeList: Edge[] = [];

    // Nodes oluştur
    familyData.forEach((person, index) => {
      const isSelected = person.id === selectedPersonId;
      const isHighlighted = highlightedRelationship && 
        (person.id === highlightedRelationship.person1Id || 
         person.id === highlightedRelationship.person2Id);

      nodeMap.set(person.id, {
        id: person.id.toString(),
        type: 'person',
        position: {
          x: (index % 5) * 200,
          y: Math.floor(index / 5) * 150
        },
        data: {
          person,
          isSelected,
          isHighlighted,
          relationshipToSelected: isSelected ? 'Seçili' : undefined
        }
      });
    });

    // Edges oluştur
    relationships.forEach((rel) => {
      const sourceNode = nodeMap.get(rel.person1Id);
      const targetNode = nodeMap.get(rel.person2Id);
      
      if (sourceNode && targetNode) {
        edgeList.push({
          id: `${rel.person1Id}-${rel.person2Id}`,
          source: rel.person1Id.toString(),
          target: rel.person2Id.toString(),
          type: 'smoothstep',
          animated: highlightedRelationship && 
            rel.person1Id === highlightedRelationship.person1Id && 
            rel.person2Id === highlightedRelationship.person2Id,
          style: {
            stroke: getEdgeColor(rel.type),
            strokeWidth: highlightedRelationship && 
              rel.person1Id === highlightedRelationship.person1Id && 
              rel.person2Id === highlightedRelationship.person2Id ? 3 : 2
          },
          label: getRelationshipLabel(rel.type)
        });
      }
    });

    return { 
      nodes: Array.from(nodeMap.values()), 
      edges: edgeList 
    };
  }, [familyData, relationships, selectedPersonId, highlightedRelationship]);

  const onNodeClick = useCallback((event: any, node: Node) => {
    const person = familyData.find(p => p.id.toString() === node.id);
    if (person && onPersonSelect) {
      onPersonSelect(person);
    }
    setSelectedNode(node.id);
  }, [familyData, onPersonSelect]);

  return (
    <div className="w-full h-96 border rounded-lg shadow-inner bg-gray-50">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodeClick={onNodeClick}
        nodeTypes={nodeTypes}
        fitView
        attributionPosition="bottom-left"
      >
        <Background gap={12} />
        <Controls />
        <MiniMap 
          nodeColor="#3B82F6"
          nodeStrokeWidth={3}
          pannable
          zoomable
        />
      </ReactFlow>
    </div>
  );
};

function getEdgeColor(relationshipType: RelationshipType): string {
  switch (relationshipType) {
    case RelationshipType.PARENT_CHILD:
      return '#059669'; // Green
    case RelationshipType.SPOUSE:
      return '#DC2626'; // Red
    case RelationshipType.SIBLING:
      return '#7C3AED'; // Purple
    default:
      return '#6B7280'; // Gray
  }
}

function getRelationshipLabel(relationshipType: RelationshipType): string {
  switch (relationshipType) {
    case RelationshipType.PARENT_CHILD:
      return 'Ebeveyn-Çocuk';
    case RelationshipType.SPOUSE:
      return 'Eş';
    case RelationshipType.SIBLING:
      return 'Kardeş';
    default:
      return '';
  }
} 