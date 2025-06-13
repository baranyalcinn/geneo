package by.backend.model.entity;

import by.backend.model.enums.Gender;
import by.backend.model.enums.RelationshipType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "persons", indexes = {
    @Index(name = "idx_person_name", columnList = "first_name, last_name"),
    @Index(name = "idx_person_birth_date", columnList = "birth_date"),
    @Index(name = "idx_person_gender", columnList = "gender"),
    @Index(name = "idx_person_death_date", columnList = "death_date"),
    @Index(name = "idx_person_birth_year", columnList = "birth_year"), // For game queries
    @Index(name = "idx_person_full_name", columnList = "full_name"), // For search optimization
    @Index(name = "idx_person_active", columnList = "is_active") // For soft delete
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "personCache")
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"relationships1", "relationships2", "familyTrees"})
@ToString(exclude = {"relationships1", "relationships2", "familyTrees"})
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "Person.withRelationships",
        attributeNodes = {
            @NamedAttributeNode("relationships1"),
            @NamedAttributeNode("relationships2")
        }
    ),
    @NamedEntityGraph(
        name = "Person.minimal",
        attributeNodes = {}
    )
})
public class Person {
    
    public Person(String firstName, String lastName, LocalDate birthDate, Gender gender) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.fullName = firstName + " " + lastName;
        this.birthYear = birthDate != null ? birthDate.getYear() : null;
        this.isActive = true;
    }
    
    public Person(String firstName, String lastName, LocalDate birthDate, LocalDate deathDate, Gender gender) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
        this.gender = gender;
        this.fullName = firstName + " " + lastName;
        this.birthYear = birthDate != null ? birthDate.getYear() : null;
        this.deathYear = deathDate != null ? deathDate.getYear() : null;
        this.isActive = true;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    // Computed field for faster searches
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;
    
    @Column(name = "death_date")
    private LocalDate deathDate;

    // Denormalized fields for faster game queries
    @Column(name = "birth_year")
    private Integer birthYear;
    
    @Column(name = "death_year")
    private Integer deathYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    // Soft delete support
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Performance fields
    @Column(name = "relationship_count")
    private Integer relationshipCount = 0;

    @Column(name = "last_accessed")
    private LocalDate lastAccessed;

    @JsonIgnore
    @OneToMany(mappedBy = "person1", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    @Fetch(FetchMode.SUBSELECT)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Relationship> relationships1 = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "person2", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    @Fetch(FetchMode.SUBSELECT)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Relationship> relationships2 = new ArrayList<>();

    @JsonIgnore
    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<FamilyTree> familyTrees = new ArrayList<>();

    // Optimized methods with caching hints
    @Transient
    private List<Relationship> cachedAllRelationships;

    public List<Relationship> getAllRelationships() {
        if (cachedAllRelationships == null) {
            cachedAllRelationships = Stream.concat(relationships1.stream(), relationships2.stream())
                    .filter(Relationship::isActive)
                    .collect(Collectors.toList());
        }
        return cachedAllRelationships;
    }

    @Transient
    private List<Person> cachedParents;

    public List<Person> getParents() {
        if (cachedParents == null) {
            cachedParents = getAllRelationships().stream()
                    .filter(r -> r.getType() == RelationshipType.PARENT_CHILD && r.getPerson2().equals(this))
                    .map(Relationship::getPerson1)
                    .collect(Collectors.toList());
        }
        return cachedParents;
    }

    @Transient
    private List<Person> cachedChildren;

    public List<Person> getChildren() {
        if (cachedChildren == null) {
            cachedChildren = getAllRelationships().stream()
                    .filter(r -> r.getType() == RelationshipType.PARENT_CHILD && r.getPerson1().equals(this))
                    .map(Relationship::getPerson2)
                    .collect(Collectors.toList());
        }
        return cachedChildren;
    }

    @Transient
    private List<Person> cachedSiblings;

    public List<Person> getSiblings() {
        if (cachedSiblings == null) {
            cachedSiblings = getAllRelationships().stream()
                    .filter(r -> r.getType() == RelationshipType.SIBLING)
                    .map(r -> r.getPerson1().equals(this) ? r.getPerson2() : r.getPerson1())
                    .collect(Collectors.toList());
        }
        return cachedSiblings;
    }

    @Transient
    private Optional<Person> cachedSpouse;
    
    @Transient
    private boolean spouseCached = false;

    public Optional<Person> getSpouseOptional() {
        if (!spouseCached) {
            cachedSpouse = getAllRelationships().stream()
                    .filter(r -> r.getType() == RelationshipType.SPOUSE)
                    .<Person>map(r -> r.getPerson1().equals(this) ? r.getPerson2() : r.getPerson1())
                    .findFirst();
            spouseCached = true;
        }
        return cachedSpouse;
    }

    // Utility methods for performance
    public void invalidateCache() {
        cachedAllRelationships = null;
        cachedParents = null;
        cachedChildren = null;
        cachedSiblings = null;
        cachedSpouse = null;
        spouseCached = false;
    }

    public int calculateAge() {
        if (birthDate == null) return 0;
        LocalDate endDate = deathDate != null ? deathDate : LocalDate.now();
        return endDate.getYear() - birthDate.getYear();
    }

    public boolean isAlive() {
        return deathDate == null;
    }

    // Pre-persist and pre-update hooks
    @PrePersist
    @PreUpdate
    private void updateComputedFields() {
        if (firstName != null && lastName != null) {
            this.fullName = firstName + " " + lastName;
        }
        if (birthDate != null) {
            this.birthYear = birthDate.getYear();
        }
        if (deathDate != null) {
            this.deathYear = deathDate.getYear();
        }
        this.lastAccessed = LocalDate.now();
    }
}