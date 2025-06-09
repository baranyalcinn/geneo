package by.backend.service.pathfinding;

import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.RelationshipType;
import by.backend.repository.RelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gelişmiş pathfinding algoritmaları ile ilişki bulma performansını optimize eder
 * TODO: Cache service entegrasyonu sonrası tam aktif edilecek
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PathFinderService {
    
    private final RelationshipRepository relationshipRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    
    // Graf yapısı cache'i - memory'de tutulan adjacency list
    private final Map<Long, Set<PersonNode>> adjacencyList = new ConcurrentHashMap<>();
    private volatile long lastGraphUpdate = 0;
    private static final long GRAPH_CACHE_TTL_MS = 5 * 60 * 1000; // 5 dakika
    
    /**
     * Enhanced Bidirectional BFS - Meet in the middle algorithm
     * Time Complexity: O(b^(d/2)) instead of O(b^d) - exponential improvement!
     */
    public Optional<List<Relationship>> findShortestPathBidirectional(Person start, Person end, int maxDepth) {
        
        if (start.getId().equals(end.getId())) {
            return Optional.of(Collections.emptyList());
        }
        
        warmupGraphCache();
        
        // Priority queues for smarter expansion (shorter paths first)
        PriorityQueue<PathNode> forwardQueue = new PriorityQueue<>(
            Comparator.comparingInt(node -> node.depth));
        PriorityQueue<PathNode> backwardQueue = new PriorityQueue<>(
            Comparator.comparingInt(node -> node.depth));
            
        Map<Long, PathNode> forwardVisited = new HashMap<>();
        Map<Long, PathNode> backwardVisited = new HashMap<>();
        
        PathNode startNode = new PathNode(start, null, null, 0);
        PathNode endNode = new PathNode(end, null, null, 0);
        
        forwardQueue.offer(startNode);
        backwardQueue.offer(endNode);
        forwardVisited.put(start.getId(), startNode);
        backwardVisited.put(end.getId(), endNode);
        
        int iterationCount = 0;
        final int MAX_ITERATIONS = 1000; // Prevent infinite loops
        
        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty() && iterationCount < MAX_ITERATIONS) {
            iterationCount++;
            
            // Check for intersection after each expansion
            for (Long personId : forwardVisited.keySet()) {
                if (backwardVisited.containsKey(personId)) {
                    PathNode forwardNode = forwardVisited.get(personId);
                    PathNode backwardNode = backwardVisited.get(personId);
                    List<Relationship> path = reconstructBidirectionalPath(forwardNode, backwardNode);
                    log.debug("Bidirectional path found in {} iterations, length: {}", iterationCount, path.size());
                    return Optional.of(path);
                }
            }
            
            // Expand smaller frontier first for balanced search
            boolean expandForward = forwardQueue.size() <= backwardQueue.size();
            
            if (expandForward && !forwardQueue.isEmpty()) {
                PathNode current = forwardQueue.poll();
                if (current.depth < maxDepth / 2) {
                    expandNodeOptimized(current, forwardQueue, forwardVisited, true, maxDepth / 2);
                }
            } else if (!backwardQueue.isEmpty()) {
                PathNode current = backwardQueue.poll();
                if (current.depth < maxDepth / 2) {
                    expandNodeOptimized(current, backwardQueue, backwardVisited, false, maxDepth / 2);
                }
            }
            
            // Early termination if queues get too large (memory protection)
            if (forwardQueue.size() > 5000 || backwardQueue.size() > 5000) {
                log.warn("Bidirectional search terminated due to large frontier size");
                break;
            }
        }
        
        log.debug("Bidirectional search completed without finding path. Iterations: {}", iterationCount);
        return Optional.empty();
    }
    
    /**
     * A* algoritması ile hedefli arama
     * Heuristic kullanarak daha akıllı pathfinding
     */
    public Optional<List<Relationship>> findPathWithAStar(Person start, Person end, int maxDepth) {
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(
                Comparator.comparingDouble(node -> node.fCost));
        Map<Long, AStarNode> allNodes = new HashMap<>();
        Set<Long> closedSet = new HashSet<>();
        
        AStarNode startNode = new AStarNode(start, null, null, 0, 
                calculateHeuristic(start, end));
        openSet.offer(startNode);
        allNodes.put(start.getId(), startNode);
        
        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            
            if (current.person.getId().equals(end.getId())) {
                List<Relationship> path = reconstructAStarPath(current);
                return Optional.of(path);
            }
            
            closedSet.add(current.person.getId());
            
            if (current.gCost >= maxDepth) continue;
            
            for (Relationship rel : getRelationships(current.person)) {
                Person neighbor = getOtherPerson(rel, current.person);
                if (closedSet.contains(neighbor.getId())) continue;
                
                double tentativeGCost = current.gCost + getRelationshipWeight(rel);
                AStarNode neighborNode = allNodes.get(neighbor.getId());
                
                if (neighborNode == null) {
                    neighborNode = new AStarNode(neighbor, current, rel, tentativeGCost,
                            calculateHeuristic(neighbor, end));
                    allNodes.put(neighbor.getId(), neighborNode);
                    openSet.offer(neighborNode);
                } else if (tentativeGCost < neighborNode.gCost) {
                    neighborNode.parent = current;
                    neighborNode.relationshipFromParent = rel;
                    neighborNode.gCost = tentativeGCost;
                    neighborNode.fCost = tentativeGCost + neighborNode.hCost;
                    // PriorityQueue'yu yeniden düzenle
                    openSet.remove(neighborNode);
                    openSet.offer(neighborNode);
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Paralel çoklu yol arama
     * Farklı stratejilerle eş zamanlı arama
     */
    public CompletableFuture<Optional<List<Relationship>>> findPathAsync(
            Person start, Person end, int maxDepth) {
        
        List<CompletableFuture<Optional<List<Relationship>>>> futures = Arrays.asList(
                // Bidirectional BFS
                CompletableFuture.supplyAsync(() -> 
                        findShortestPathBidirectional(start, end, maxDepth), executorService),
                
                // A* search
                CompletableFuture.supplyAsync(() -> 
                        findPathWithAStar(start, end, maxDepth), executorService),
                
                // Specialized family path search
                CompletableFuture.supplyAsync(() -> 
                        findFamilyPath(start, end, maxDepth), executorService)
        );
        
        // İlk dönen sonucu al
        return CompletableFuture.anyOf(futures.toArray(new CompletableFuture<?>[0]))
                .thenApply(result -> {
                    @SuppressWarnings("unchecked")
                    Optional<List<Relationship>> typedResult = (Optional<List<Relationship>>) result;
                    return typedResult;
                });
    }
    
    /**
     * Aile ağacına özel path finding
     * Aile ilişkilerinin özelliklerini kullanarak optimize eder
     */
    public Optional<List<Relationship>> findFamilyPath(Person start, Person end, int maxDepth) {
        // Önce common ancestors ara
        Set<Person> startAncestors = findAncestors(start, maxDepth / 2);
        Set<Person> endAncestors = findAncestors(end, maxDepth / 2);
        
        // Ortak ancestor bul
        Set<Person> commonAncestors = new HashSet<>(startAncestors);
        commonAncestors.retainAll(endAncestors);
        
        if (!commonAncestors.isEmpty()) {
            // En yakın ortak ancestor'u bul
            Person closestAncestor = findClosestCommonAncestor(start, end, commonAncestors);
            if (closestAncestor != null) {
                List<Relationship> pathToAncestor = findPathToAncestor(start, closestAncestor);
                List<Relationship> pathFromAncestor = findPathFromAncestor(closestAncestor, end);
                
                if (pathToAncestor != null && pathFromAncestor != null) {
                    List<Relationship> fullPath = new ArrayList<>(pathToAncestor);
                    fullPath.addAll(pathFromAncestor);
                    return Optional.of(fullPath);
                }
            }
        }
        
        // Spouse connections ara
        return findThroughSpouseConnections(start, end, maxDepth);
    }
    
    /**
     * Graf yapısını memory'de tut - hızlı erişim için
     */
    public void warmupGraphCache() {
        if (System.currentTimeMillis() - lastGraphUpdate < GRAPH_CACHE_TTL_MS) {
            return; // Cache hala geçerli
        }
        
        log.info("Graf cache'i yenileniyor...");
        long startTime = System.currentTimeMillis();
        
        Map<Long, Set<PersonNode>> newAdjacencyList = new ConcurrentHashMap<>();
        
        // Basit repository çağrısı kullan - tüm aktif ilişkileri getir
        List<Relationship> allRelationships = relationshipRepository.findAllWithPersons()
                .stream()
                .filter(r -> r.isActive())
                .toList();
        
        for (Relationship rel : allRelationships) {
            Long person1Id = rel.getPerson1().getId();
            Long person2Id = rel.getPerson2().getId();
            
            newAdjacencyList.computeIfAbsent(person1Id, k -> ConcurrentHashMap.newKeySet())
                    .add(new PersonNode(rel.getPerson2(), rel));
            newAdjacencyList.computeIfAbsent(person2Id, k -> ConcurrentHashMap.newKeySet())
                    .add(new PersonNode(rel.getPerson1(), rel));
        }
        
        this.adjacencyList.clear();
        this.adjacencyList.putAll(newAdjacencyList);
        this.lastGraphUpdate = System.currentTimeMillis();
        
        log.info("Graf cache'i güncellendi: {} kişi, {} ilişki, süre: {}ms",
                adjacencyList.size(), allRelationships.size(), 
                System.currentTimeMillis() - startTime);
    }
    
    /**
     * İlişki ağırlığı hesapla - A* için
     */
    private double getRelationshipWeight(Relationship rel) {
        // İlişki tipine göre ağırlık
        return switch (rel.getType()) {
            case PARENT_CHILD -> 1.0;
            case SPOUSE -> 1.2;
            case SIBLING -> 1.1;
            default -> 1.5;
        };
    }
    
    /**
     * Heuristic hesapla - A* için
     */
    private double calculateHeuristic(Person current, Person target) {
        // Basit heuristic: yaş farkı ve isim benzerliği
        double ageDiff = Math.abs(getPersonAge(current) - getPersonAge(target));
        double nameScore = calculateNameSimilarity(current, target);
        
        return (ageDiff / 50.0) + (1.0 - nameScore);
    }
    
    // Yardımcı metodlar
    
    /**
     * Optimized node expansion with pruning and heuristics
     */
    private void expandNodeOptimized(PathNode current, PriorityQueue<PathNode> queue, 
                                   Map<Long, PathNode> visited, boolean forward, int maxDepth) {
        List<Relationship> relationships = getRelationships(current.person);
        
        // Priority based expansion - prefer blood relations
        List<Relationship> sortedRelationships = relationships.stream()
            .sorted((r1, r2) -> {
                // Primary: blood relations first
                int priority1 = getRelationshipPriority(r1);
                int priority2 = getRelationshipPriority(r2);
                
                if (priority1 != priority2) {
                    return Integer.compare(priority1, priority2);
                }
                
                // Secondary: age proximity for generation logic
                Person p1 = getOtherPerson(r1, current.person);
                Person p2 = getOtherPerson(r2, current.person);
                return Integer.compare(
                    Math.abs(getPersonAge(p1) - getPersonAge(current.person)),
                    Math.abs(getPersonAge(p2) - getPersonAge(current.person))
                );
            })
            .limit(15) // Limit expansion to top 15 candidates for performance
            .toList();
        
        for (Relationship rel : sortedRelationships) {
            Person neighbor = getOtherPerson(rel, current.person);
            Long neighborId = neighbor.getId();
            
            if (!visited.containsKey(neighborId) && current.depth + 1 <= maxDepth) {
                PathNode newNode = new PathNode(neighbor, current, rel, current.depth + 1);
                visited.put(neighborId, newNode);
                queue.offer(newNode);
            }
        }
    }
    
    /**
     * Get relationship priority for search order (lower = higher priority)
     */
    private int getRelationshipPriority(Relationship rel) {
        return switch (rel.getType()) {
            case PARENT_CHILD -> 1;  // Highest priority
            case SIBLING -> 2;
            case SPOUSE -> 3;
            default -> 4;           // Lowest priority
        };
    }
    
    private List<Relationship> reconstructBidirectionalPath(PathNode forwardNode, PathNode backwardNode) {
        List<Relationship> path = new ArrayList<>();
        
        // Forward path'i ekle
        PathNode current = forwardNode;
        while (current.relationshipFromParent != null) {
            path.add(0, current.relationshipFromParent);
            current = current.parent;
        }
        
        // Backward path'i ekle (ters çevir)
        current = backwardNode.parent;
        while (current != null && current.relationshipFromParent != null) {
            path.add(current.relationshipFromParent);
            current = current.parent;
        }
        
        return path;
    }
    
    private List<Relationship> reconstructAStarPath(AStarNode node) {
        List<Relationship> path = new ArrayList<>();
        AStarNode current = node;
        
        while (current.relationshipFromParent != null) {
            path.add(0, current.relationshipFromParent);
            current = current.parent;
        }
        
        return path;
    }
    
    private List<Relationship> getRelationships(Person person) {
        // Graf cache'i kullan
        if (adjacencyList.containsKey(person.getId())) {
            return adjacencyList.get(person.getId()).stream()
                    .map(PersonNode::relationship)
                    .toList();
        }
        
        // Fallback: Mevcut repository metodlarını kullan
        List<Relationship> relationships = new ArrayList<>();
        relationships.addAll(relationshipRepository.findByPerson1AndIsActiveTrue(person));
        relationships.addAll(relationshipRepository.findByPerson2AndIsActiveTrue(person));
        
        return relationships;
    }
    
    private Person getOtherPerson(Relationship rel, Person person) {
        return rel.getPerson1().getId().equals(person.getId()) ? 
                rel.getPerson2() : rel.getPerson1();
    }
    
    private Set<Person> findAncestors(Person person, int maxDepth) {
        // BFS ile ancestors bul
        Set<Person> ancestors = new HashSet<>();
        Queue<Person> queue = new LinkedList<>();
        Set<Long> visited = new HashSet<>();
        Map<Long, Integer> depths = new HashMap<>();
        
        queue.offer(person);
        visited.add(person.getId());
        depths.put(person.getId(), 0);
        
        while (!queue.isEmpty()) {
            Person current = queue.poll();
            int currentDepth = depths.get(current.getId());
            
            if (currentDepth >= maxDepth) continue;
            
            for (Relationship rel : getRelationships(current)) {
                if (rel.getType() == RelationshipType.PARENT_CHILD && 
                    rel.getPerson2().getId().equals(current.getId())) {
                    
                    Person parent = rel.getPerson1();
                    if (!visited.contains(parent.getId())) {
                        ancestors.add(parent);
                        queue.offer(parent);
                        visited.add(parent.getId());
                        depths.put(parent.getId(), currentDepth + 1);
                    }
                }
            }
        }
        
        return ancestors;
    }
    
    private Person findClosestCommonAncestor(Person start, Person end, Set<Person> commonAncestors) {
        // En yakın ortak ancestor'u bul (BFS depth'e göre)
        return commonAncestors.stream()
                .min(Comparator.comparingInt(ancestor -> 
                        calculateDepthToAncestor(start, ancestor) + 
                        calculateDepthToAncestor(end, ancestor)))
                .orElse(null);
    }
    
    private int calculateDepthToAncestor(Person person, Person ancestor) {
        // BFS ile depth hesapla
        Queue<Person> queue = new LinkedList<>();
        Set<Long> visited = new HashSet<>();
        Map<Long, Integer> depths = new HashMap<>();
        
        queue.offer(person);
        visited.add(person.getId());
        depths.put(person.getId(), 0);
        
        while (!queue.isEmpty()) {
            Person current = queue.poll();
            if (current.getId().equals(ancestor.getId())) {
                return depths.get(current.getId());
            }
            
            int currentDepth = depths.get(current.getId());
            if (currentDepth >= 5) continue; // Max depth limiti
            
            for (Relationship rel : getRelationships(current)) {
                if (rel.getType() == RelationshipType.PARENT_CHILD && 
                    rel.getPerson2().getId().equals(current.getId())) {
                    
                    Person parent = rel.getPerson1();
                    if (!visited.contains(parent.getId())) {
                        queue.offer(parent);
                        visited.add(parent.getId());
                        depths.put(parent.getId(), currentDepth + 1);
                    }
                }
            }
        }
        
        return Integer.MAX_VALUE; // Bulunamadı
    }
    
    private List<Relationship> findPathToAncestor(Person person, Person ancestor) {
        // Ancestor'a giden yolu bul
        return findShortestPathBidirectional(person, ancestor, 5).orElse(null);
    }
    
    private List<Relationship> findPathFromAncestor(Person ancestor, Person person) {
        // Ancestor'dan gelen yolu bul (ters çevir)
        Optional<List<Relationship>> path = findShortestPathBidirectional(ancestor, person, 5);
        return path.orElse(null);
    }
    
    private Optional<List<Relationship>> findThroughSpouseConnections(Person start, Person end, int maxDepth) {
        // Spouse bağlantıları üzerinden yol bul
        for (Relationship rel : getRelationships(start)) {
            if (rel.getType() == RelationshipType.SPOUSE) {
                Person spouse = getOtherPerson(rel, start);
                Optional<List<Relationship>> pathFromSpouse = 
                        findShortestPathBidirectional(spouse, end, maxDepth - 1);
                
                if (pathFromSpouse.isPresent()) {
                    List<Relationship> fullPath = new ArrayList<>();
                    fullPath.add(rel);
                    fullPath.addAll(pathFromSpouse.get());
                    return Optional.of(fullPath);
                }
            }
        }
        
        return Optional.empty();
    }
    
    private int getPersonAge(Person person) {
        if (person.getBirthDate() != null) {
            return java.time.LocalDate.now().getYear() - person.getBirthDate().getYear();
        }
        return 50; // Default age
    }
    
    private double calculateNameSimilarity(Person p1, Person p2) {
        // Basit isim benzerliği hesaplama
        String name1 = (p1.getFirstName() + " " + p1.getLastName()).toLowerCase();
        String name2 = (p2.getFirstName() + " " + p2.getLastName()).toLowerCase();
        
        return levenshteinSimilarity(name1, name2);
    }
    
    private double levenshteinSimilarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - (double) distance / maxLen;
    }
    
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            dp[i - 1][j] + 1,
                            Math.min(
                                    dp[i][j - 1] + 1,
                                    dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                            )
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    // Node sınıfları
    private static class PathNode {
        Person person;
        PathNode parent;
        Relationship relationshipFromParent;
        int depth;
        
        PathNode(Person person, PathNode parent, Relationship relationshipFromParent, int depth) {
            this.person = person;
            this.parent = parent;
            this.relationshipFromParent = relationshipFromParent;
            this.depth = depth;
        }
    }
    
    private static class AStarNode {
        Person person;
        AStarNode parent;
        Relationship relationshipFromParent;
        double gCost; // Start'tan bu node'a maliyet
        double hCost; // Bu node'dan hedefe heuristic maliyet
        double fCost; // gCost + hCost
        
        AStarNode(Person person, AStarNode parent, Relationship relationshipFromParent, 
                 double gCost, double hCost) {
            this.person = person;
            this.parent = parent;
            this.relationshipFromParent = relationshipFromParent;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }
    }
    
    private record PersonNode(Person person, Relationship relationship) {}
} 