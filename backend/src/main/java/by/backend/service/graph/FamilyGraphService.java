package by.backend.service.graph;

import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.RelationshipType;
import by.backend.repository.RelationshipRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance family graph service
 * Preprocesses graph structure for O(1) relationship queries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FamilyGraphService {
    
    private final RelationshipRepository relationshipRepository;
    
    // Adjacency list representation - O(1) neighbor lookup
    private final Map<Long, Set<PersonEdge>> adjacencyList = new ConcurrentHashMap<>();
    
    // Precomputed shortest paths for common relationship patterns
    private final Map<String, List<Relationship>> shortestPaths = new ConcurrentHashMap<>();
    
    // Generation levels for faster depth calculations
    private final Map<Long, Integer> personGenerations = new ConcurrentHashMap<>();
    
    // Family clusters for optimized search scoping
    private final Map<Long, Set<Long>> familyClusters = new ConcurrentHashMap<>();
    
    // Graph metadata
    private volatile long lastGraphUpdate = 0;
    private final AtomicLong graphVersion = new AtomicLong(0);
    private volatile boolean isGraphBuilding = false;
    
    @PostConstruct
    public void initializeGraph() {
        try {
            log.info("Initializing family graph...");
            buildGraph();
        } catch (Exception e) {
            log.warn("Failed to initialize graph during startup: {}. Graph will be built on first use.", e.getMessage());
        }
    }
    
    /**
     * Build complete graph structure
     * Time Complexity: O(V + E) for graph, O(V^2) for preprocessing
     */
    public synchronized void buildGraph() {
        if (isGraphBuilding) {
            log.warn("Graph is already being built, skipping duplicate request");
            return;
        }
        
        isGraphBuilding = true;
        long startTime = System.currentTimeMillis();
        log.info("Starting graph construction...");
        
        try {
            // Clear existing data
            adjacencyList.clear();
            shortestPaths.clear();
            personGenerations.clear();
            familyClusters.clear();
            
            // Build adjacency list
            buildAdjacencyList();
            
            // Calculate generations
            calculateGenerations();
            
            // Identify family clusters
            identifyFamilyClusters();
            
            // Precompute common relationship paths
            precomputeCommonPaths();
            
            lastGraphUpdate = System.currentTimeMillis();
            graphVersion.incrementAndGet();
            
            log.info("Graph construction completed: {} nodes, {} edges, time: {}ms, version: {}",
                    adjacencyList.size(), 
                    adjacencyList.values().stream().mapToInt(Set::size).sum(),
                    System.currentTimeMillis() - startTime,
                    graphVersion.get());
            
        } finally {
            isGraphBuilding = false;
        }
    }
    
    /**
     * Get neighbors of a person - O(1) lookup
     */
    public Set<PersonEdge> getNeighbors(Long personId) {
        ensureGraphIsBuilt();
        return adjacencyList.getOrDefault(personId, Collections.emptySet());
    }
    
    /**
     * Check if two persons are connected - O(1) lookup after preprocessing
     */
    public boolean areConnected(Long person1Id, Long person2Id) {
        Set<PersonEdge> neighbors = getNeighbors(person1Id);
        return neighbors.stream().anyMatch(edge -> edge.person().getId().equals(person2Id));
    }
    
    /**
     * Get generation difference - O(1) lookup
     */
    public int getGenerationDifference(Long person1Id, Long person2Id) {
        Integer gen1 = personGenerations.get(person1Id);
        Integer gen2 = personGenerations.get(person2Id);
        
        if (gen1 == null || gen2 == null) {
            return Integer.MAX_VALUE; // Unknown generation
        }
        
        return Math.abs(gen1 - gen2);
    }
    
    /**
     * Check if persons are in same family cluster - O(1) lookup
     */
    public boolean areSameFamily(Long person1Id, Long person2Id) {
        Set<Long> cluster1 = familyClusters.get(person1Id);
        return cluster1 != null && cluster1.contains(person2Id);
    }
    
    /**
     * Get precomputed shortest path - O(1) lookup for common patterns
     */
    public Optional<List<Relationship>> getPrecomputedPath(Long person1Id, Long person2Id) {
        String pathKey = createPathKey(person1Id, person2Id);
        List<Relationship> path = shortestPaths.get(pathKey);
        return Optional.ofNullable(path);
    }
    
    /**
     * Get all persons in generation level
     */
    public Set<Long> getPersonsInGeneration(int generation) {
        return personGenerations.entrySet().stream()
                .filter(entry -> entry.getValue().equals(generation))
                .map(Map.Entry::getKey)
                .collect(HashSet::new, Set::add, Set::addAll);
    }
    
    /**
     * Get family cluster size for optimization decisions
     */
    public int getFamilyClusterSize(Long personId) {
        Set<Long> cluster = familyClusters.get(personId);
        return cluster != null ? cluster.size() : 1;
    }
    
    // Scheduled maintenance
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void scheduleGraphMaintenance() {
        if (System.currentTimeMillis() - lastGraphUpdate > 600000) { // 10 minutes
            log.info("Scheduled graph refresh triggered");
            CompletableFuture.runAsync(this::buildGraph);
        }
    }
    
    // Private helper methods
    
    private void ensureGraphIsBuilt() {
        if (adjacencyList.isEmpty() && !isGraphBuilding) {
            log.info("Graph is empty, building on first use...");
            try {
                buildGraph();
            } catch (Exception e) {
                log.error("Failed to build graph on demand: {}", e.getMessage(), e);
            }
        }
    }
    
    private void buildAdjacencyList() {
        List<Relationship> allRelationships = relationshipRepository.findAllWithPersons();
        
        for (Relationship rel : allRelationships) {
            if (!rel.isActive()) continue;
            
            Long person1Id = rel.getPerson1().getId();
            Long person2Id = rel.getPerson2().getId();
            
            // Bidirectional edges
            adjacencyList.computeIfAbsent(person1Id, k -> ConcurrentHashMap.newKeySet())
                    .add(new PersonEdge(rel.getPerson2(), rel, calculateEdgeWeight(rel)));
            
            adjacencyList.computeIfAbsent(person2Id, k -> ConcurrentHashMap.newKeySet())
                    .add(new PersonEdge(rel.getPerson1(), rel, calculateEdgeWeight(rel)));
        }
        
        log.debug("Built adjacency list with {} nodes", adjacencyList.size());
    }
    
    private void calculateGenerations() {
        Set<Long> rootPersons = findRootPersons();
        Queue<PersonGeneration> queue = new LinkedList<>();
        Set<Long> visited = new HashSet<>();
        
        initializeRootGenerations(rootPersons, queue, visited);
        processGenerationQueue(queue, visited);
        
        log.debug("Calculated generations for {} persons", personGenerations.size());
    }
    
    private void initializeRootGenerations(Set<Long> rootPersons, Queue<PersonGeneration> queue, Set<Long> visited) {
        for (Long rootId : rootPersons) {
            queue.offer(new PersonGeneration(rootId, 0));
            personGenerations.put(rootId, 0);
            visited.add(rootId);
        }
    }
    
    private void processGenerationQueue(Queue<PersonGeneration> queue, Set<Long> visited) {
        while (!queue.isEmpty()) {
            PersonGeneration current = queue.poll();
            processChildrenOfPerson(current, visited, queue);
        }
    }
    
    private void processChildrenOfPerson(PersonGeneration current, Set<Long> visited, Queue<PersonGeneration> queue) {
        Set<PersonEdge> neighbors = getNeighbors(current.personId());
        
        for (PersonEdge edge : neighbors) {
            if (isParentChildRelationship(edge, current.personId())) {
                addChildToGeneration(edge.person().getId(), current.generation() + 1, visited, queue);
            }
        }
    }
    
    private boolean isParentChildRelationship(PersonEdge edge, Long currentPersonId) {
        return edge.relationship().getType() == RelationshipType.PARENT_CHILD 
               && edge.relationship().getPerson1().getId().equals(currentPersonId);
    }
    
    private void addChildToGeneration(Long childId, int generation, Set<Long> visited, Queue<PersonGeneration> queue) {
        if (!visited.contains(childId)) {
            personGenerations.put(childId, generation);
            queue.offer(new PersonGeneration(childId, generation));
            visited.add(childId);
        }
    }
    
    private Set<Long> findRootPersons() {
        Set<Long> allPersons = adjacencyList.keySet();
        Set<Long> personsWithParents = new HashSet<>();
        
        // Find persons who have parents
        for (Set<PersonEdge> edges : adjacencyList.values()) {
            for (PersonEdge edge : edges) {
                if (edge.relationship().getType() == RelationshipType.PARENT_CHILD) {
                    // Person2 is child in PARENT_CHILD relationship
                    personsWithParents.add(edge.relationship().getPerson2().getId());
                }
            }
        }
        
        // Root persons = all persons - persons with parents
        Set<Long> roots = new HashSet<>(allPersons);
        roots.removeAll(personsWithParents);
        
        log.debug("Found {} root persons", roots.size());
        return roots;
    }
    
    private void identifyFamilyClusters() {
        Set<Long> visited = new HashSet<>();
        int clusterId = 0;
        
        for (Long personId : adjacencyList.keySet()) {
            if (!visited.contains(personId)) {
                Set<Long> cluster = findConnectedComponent(personId, visited);
                assignClusterToMembers(cluster);
                clusterId++;
            }
        }
        
        log.debug("Identified {} family clusters", clusterId);
    }
    
    private Set<Long> findConnectedComponent(Long startPersonId, Set<Long> visited) {
        Set<Long> cluster = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        
        queue.offer(startPersonId);
        visited.add(startPersonId);
        
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            cluster.add(current);
            processNeighborsForClustering(current, visited, queue);
        }
        
        return cluster;
    }
    
    private void processNeighborsForClustering(Long current, Set<Long> visited, Queue<Long> queue) {
        Set<PersonEdge> neighbors = getNeighbors(current);
        
        for (PersonEdge edge : neighbors) {
            Long neighborId = edge.person().getId();
            if (!visited.contains(neighborId)) {
                visited.add(neighborId);
                queue.offer(neighborId);
            }
        }
    }
    
    private void assignClusterToMembers(Set<Long> cluster) {
        for (Long memberId : cluster) {
            familyClusters.put(memberId, cluster);
        }
    }
    
    private void precomputeCommonPaths() {
        // Precompute paths for small family clusters (< 50 members)
        familyClusters.values().stream()
                .filter(cluster -> cluster.size() < 50)
                .distinct()
                .forEach(this::precomputeClusterPaths);
    }
    
    private void precomputeClusterPaths(Set<Long> cluster) {
        List<Long> members = new ArrayList<>(cluster);
        
        // Floyd-Warshall for all-pairs shortest paths within cluster
        for (int i = 0; i < members.size(); i++) {
            for (int j = i + 1; j < members.size(); j++) {
                Long person1Id = members.get(i);
                Long person2Id = members.get(j);
                
                List<Relationship> path = computeShortestPath(person1Id, person2Id);
                if (path != null && path.size() <= 3) { // Only cache short paths
                    shortestPaths.put(createPathKey(person1Id, person2Id), path);
                }
            }
        }
    }
    
    private List<Relationship> computeShortestPath(Long startId, Long endId) {
        // Simple BFS for shortest path
        Queue<PathNode> queue = new LinkedList<>();
        Set<Long> visited = new HashSet<>();
        
        queue.offer(new PathNode(startId, null, null));
        visited.add(startId);
        
        while (!queue.isEmpty()) {
            PathNode current = queue.poll();
            
            if (current.personId().equals(endId)) {
                return reconstructPath(current);
            }
            
            Set<PersonEdge> neighbors = getNeighbors(current.personId());
            for (PersonEdge edge : neighbors) {
                Long neighborId = edge.person().getId();
                if (!visited.contains(neighborId)) {
                    visited.add(neighborId);
                    queue.offer(new PathNode(neighborId, current, edge.relationship()));
                }
            }
        }
        
        return null; // No path found
    }
    
    private List<Relationship> reconstructPath(PathNode endNode) {
        List<Relationship> path = new ArrayList<>();
        PathNode current = endNode;
        
        while (current.parent() != null) {
            path.add(0, current.relationship());
            current = current.parent();
        }
        
        return path;
    }
    
    private double calculateEdgeWeight(Relationship rel) {
        return switch (rel.getType()) {
            case PARENT_CHILD -> 1.0;
            case SPOUSE -> 1.1;
            case SIBLING -> 1.2;
            default -> 2.0;
        };
    }
    
    private String createPathKey(Long person1Id, Long person2Id) {
        long small = Math.min(person1Id, person2Id);
        long large = Math.max(person1Id, person2Id);
        return small + ":" + large;
    }
    
    // Record classes for data structures
    public record PersonEdge(Person person, Relationship relationship, double weight) {}
    
    private record PersonGeneration(Long personId, int generation) {}
    
    private record PathNode(Long personId, PathNode parent, Relationship relationship) {}
} 