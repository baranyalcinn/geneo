package by.backend.model.entity;

import by.backend.model.enums.Gender;
import by.backend.model.enums.RelationshipType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "persons")
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"relationships1", "relationships2", "familyTrees"})
@ToString(exclude = {"relationships1", "relationships2", "familyTrees"})
public class Person {
    public Person(String firstName, String lastName, LocalDate birthDate, Gender gender) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
    }
    
    public Person(String firstName, String lastName, LocalDate birthDate, LocalDate deathDate, Gender gender) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
        this.gender = gender;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;
    
    @Column(name = "death_date")
    private LocalDate deathDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @JsonIgnore
    @OneToMany(mappedBy = "person1", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Relationship> relationships1 = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "person2", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Relationship> relationships2 = new ArrayList<>();

    @JsonIgnore
    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
    private List<FamilyTree> familyTrees = new ArrayList<>();

    public List<Relationship> getAllRelationships() {
        return Stream.concat(relationships1.stream(), relationships2.stream())
                .filter(Relationship::isActive)
                .collect(Collectors.toList());
    }

    public List<Person> getParents() {
        return getAllRelationships().stream()
                .filter(r -> r.getType() == RelationshipType.PARENT_CHILD && r.getPerson2().equals(this))
                .map(Relationship::getPerson1)
                .collect(Collectors.toList());
    }

    public List<Person> getChildren() {
        return getAllRelationships().stream()
                .filter(r -> r.getType() == RelationshipType.PARENT_CHILD && r.getPerson1().equals(this))
                .map(Relationship::getPerson2)
                .collect(Collectors.toList());
    }

    public List<Person> getSiblings() {
        return getAllRelationships().stream()
                .filter(r -> r.getType() == RelationshipType.SIBLING)
                .map(r -> r.getPerson1().equals(this) ? r.getPerson2() : r.getPerson1())
                .collect(Collectors.toList());
    }

    public Optional<Person> getSpouseOptional() {
        return getAllRelationships().stream()
                .filter(r -> r.getType() == RelationshipType.SPOUSE)
                .<Person>map(r -> r.getPerson1().equals(this) ? r.getPerson2() : r.getPerson1())
                .findFirst();
    }
}