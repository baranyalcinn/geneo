package by.backend.model.entity;

import by.backend.model.enums.RelationshipType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "relationships", indexes = {
    @Index(name = "idx_relationship_person1", columnList = "person1_id"),
    @Index(name = "idx_relationship_person2", columnList = "person2_id"),
    @Index(name = "idx_relationship_type", columnList = "type"),
    @Index(name = "idx_relationship_active", columnList = "is_active"),
    @Index(name = "idx_relationship_persons", columnList = "person1_id, person2_id"),
    @Index(name = "idx_relationship_type_active", columnList = "type, is_active"),
    @Index(name = "idx_relationship_start_date", columnList = "start_date"),
    @Index(name = "idx_relationship_search", columnList = "person1_id, person2_id, type, is_active") // Composite for complex queries
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "relationshipCache")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(exclude = {"person1", "person2"})
@ToString(exclude = {"person1", "person2"})
@NamedQueries({
    @NamedQuery(
        name = "Relationship.findActiveByPersonId",
        query = "SELECT r FROM Relationship r WHERE (r.person1.id = :personId OR r.person2.id = :personId) AND r.isActive = true"
    ),
    @NamedQuery(
        name = "Relationship.findActiveByPersonIds",
        query = "SELECT r FROM Relationship r WHERE ((r.person1.id = :person1Id AND r.person2.id = :person2Id) OR (r.person1.id = :person2Id AND r.person2.id = :person1Id)) AND r.isActive = true"
    ),
    @NamedQuery(
        name = "Relationship.findByTypeAndActive",
        query = "SELECT r FROM Relationship r WHERE r.type = :type AND r.isActive = true"
    ),
    @NamedQuery(
        name = "Relationship.countActiveByPersonId",
        query = "SELECT COUNT(r) FROM Relationship r WHERE (r.person1.id = :personId OR r.person2.id = :personId) AND r.isActive = true"
    )
})
public class Relationship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY) // Changed to LAZY for better performance
    @JoinColumn(name = "person1_id", nullable = false)
    private Person person1;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY) // Changed to LAZY for better performance
    @JoinColumn(name = "person2_id", nullable = false)
    private Person person2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RelationshipType type;

    @Builder.Default
    @Column(name = "start_date")
    private LocalDate startDate = LocalDate.now();

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // Performance and audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // Relationship metadata for optimization
    @Column(name = "relationship_strength") // 0-10 scale for game algorithm
    private Integer relationshipStrength;

    @Column(name = "generation_difference") // Calculated field for performance
    private Integer generationDifference;

    @Column(name = "is_bidirectional", nullable = false)
    @Builder.Default
    private Boolean isBidirectional = false;

    // Validation and utility methods
    public boolean isValidRelationship() {
        return person1 != null && person2 != null && 
               !person1.equals(person2) && 
               type != null && isActive;
    }

    public Person getOtherPerson(Person person) {
        if (person1.equals(person)) {
            return person2;
        } else if (person2.equals(person)) {
            return person1;
        }
        return null;
    }

    public boolean involvePerson(Person person) {
        return person1.equals(person) || person2.equals(person);
    }

    public boolean isDirectFamily() {
        return type == RelationshipType.PARENT_CHILD || 
               type == RelationshipType.SPOUSE || 
               type == RelationshipType.SIBLING;
    }

    // Performance helper methods
    public String getRelationshipKey() {
        Long id1 = person1.getId();
        Long id2 = person2.getId();
        return (id1 < id2 ? id1 + "_" + id2 : id2 + "_" + id1) + "_" + type.name();
    }

    // Pre-persist and pre-update hooks for computed fields
    @PrePersist
    @PreUpdate
    private void updateComputedFields() {
        if (person1 != null && person2 != null) {
            // Calculate generation difference for optimization
            Integer person1BirthYear = person1.getBirthYear();
            Integer person2BirthYear = person2.getBirthYear();
            
            if (person1BirthYear != null && person2BirthYear != null) {
                generationDifference = Math.abs(person1BirthYear - person2BirthYear);
            }

            // Set relationship strength based on type
            relationshipStrength = calculateRelationshipStrength();
        }
    }

    private Integer calculateRelationshipStrength() {
        return switch (type) {
            case PARENT_CHILD -> 10;
            case SPOUSE -> 9;
            case SIBLING -> 8;
            case GRANDPARENT_GRANDCHILD -> 7;
            case UNCLE_AUNT_NEPHEW_NIECE -> 6;
            case COUSIN -> 5;
            default -> 3;
        };
    }

    // Soft delete method
    public void softDelete() {
        this.isActive = false;
        this.endDate = LocalDate.now();
    }

    public void reactivate() {
        this.isActive = true;
        this.endDate = null;
    }
} 