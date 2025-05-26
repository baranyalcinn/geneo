import { Node, Edge } from '@xyflow/react';
import { Person } from '../types/Person';

// Interface to hold both nodes and edges
interface FlowData {
  nodes: Node[];
  edges: Edge[];
}

// Recursive function to traverse the tree and generate nodes/edges
const processPerson = (
  person: Person,
  processedIds: Set<string>,
  nodes: Node[],
  edges: Edge[]
) => {
  // Avoid processing the same person multiple times (in case of complex relationships/duplicates)
  if (!person || processedIds.has(person.id.toString())) {
    return;
  }
  processedIds.add(person.id.toString());

  // Create node for the current person
  nodes.push({
    id: person.id.toString(),
    type: 'personNode',
    data: { person },
    position: { x: 0, y: 0 },
  });

  // Create node for the spouse if exists
  if (person.spouse && !processedIds.has(person.spouse.id.toString())) {
    processedIds.add(person.spouse.id.toString());
    nodes.push({
      id: person.spouse.id.toString(),
      type: 'personNode',
      data: { person: person.spouse },
      position: { x: 0, y: 0 },
    });
    
    // Optional: Add an edge between spouses (can be styled differently)
    // edges.push({
    //   id: `edge-spouse-${person.id.toString()}-${person.spouse.id.toString()}`,
    //   source: person.id.toString(),
    //   target: person.spouse.id.toString(),
    //   type: 'smoothstep', 
    // });
  }

  // Process children
  if (person.children && person.children.length > 0) {
    person.children.forEach((child) => {
      if (child) {
        edges.push({
          id: `edge-${person.id.toString()}-${child.id.toString()}`,
          source: person.id.toString(),
          target: child.id.toString(),
          type: 'smoothstep',
        });

        // Optional: Edge from the spouse to the child
        // if (person.spouse) {
        //   edges.push({
        //     id: `edge-${person.spouse.id.toString()}-${child.id.toString()}`,
        //     source: person.spouse.id.toString(),
        //     target: child.id.toString(),
        //     type: 'smoothstep',
        //   });
        // }
        
        processPerson(child, processedIds, nodes, edges);
      }
    });
  }
};

/**
 * Transforms hierarchical Person data into flat lists of nodes and edges
 * suitable for React Flow.
 * 
 * @param rootPerson The starting person of the family tree section to display.
 * @returns An object containing arrays of nodes and edges.
 */
export const transformToFlowData = (rootPerson: Person | null): FlowData => {
  if (!rootPerson) {
    return { nodes: [], edges: [] };
  }

  const nodes: Node[] = [];
  const edges: Edge[] = [];
  const processedIds = new Set<string>();

  processPerson(rootPerson, processedIds, nodes, edges);

  // Note: This function only generates nodes and edges. 
  // Actual layout (positioning) needs to be handled separately,
  // typically using a layouting library like Dagre or ELK integrated with React Flow.

  return { nodes, edges };
}; 