package by.backend.service.description;

import by.backend.model.entity.Person;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Helper class for batch-fetching family connections to solve N+1 query problem
 * This class represents a comprehensive family network for a person
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyConnections {
    private Person spouse;
    private List<Person> parents;
    private List<Person> children;
    private List<Person> siblings;
    private List<Person> grandparents;
    private List<Person> grandchildren;
    private Set<Person> extendedFamily; // For complex relationship analysis
    
    /**
     * Check if this person has any family connections
     */
    public boolean hasAnyConnections() {
        return spouse != null || 
               (parents != null && !parents.isEmpty()) ||
               (children != null && !children.isEmpty()) ||
               (siblings != null && !siblings.isEmpty()) ||
               (grandparents != null && !grandparents.isEmpty()) ||
               (grandchildren != null && !grandchildren.isEmpty());
    }
    
    /**
     * Get total family member count for analysis
     */
    public int getTotalFamilySize() {
        int count = 0;
        if (spouse != null) count++;
        if (parents != null) count += parents.size();
        if (children != null) count += children.size();
        if (siblings != null) count += siblings.size();
        if (grandparents != null) count += grandparents.size();
        if (grandchildren != null) count += grandchildren.size();
        return count;
    }
    
    /**
     * Check if family network is complex (suitable for hard questions)
     */
    public boolean isComplexFamily() {
        return getTotalFamilySize() >= 5 || 
               (extendedFamily != null && extendedFamily.size() >= 8);
    }
} 