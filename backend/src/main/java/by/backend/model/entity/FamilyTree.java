package by.backend.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "family_trees")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"rootMember", "members"})
public class FamilyTree {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Aile ağacı adı boş olamaz")
    private String name;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "root_member_id")
    private Person rootMember;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "family_tree_members",
        joinColumns = @JoinColumn(name = "tree_id"),
        inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    private Set<Person> members = new HashSet<>();

    public FamilyTree(String name) {
        this.name = name;
    }

    public FamilyTree(String name, Person rootMember) {
        this.name = name;
        this.rootMember = rootMember;
        this.members.add(rootMember);
    }

    public void addMember(Person person) {
        members.add(person);
        person.getFamilyTrees().add(this);
    }

    public void removeMember(Person person) {
        if (!person.equals(rootMember)) {
            members.remove(person);
            person.getFamilyTrees().remove(this);
        }
    }

    public void setRootMember(Person person) {
        if (!members.contains(person)) {
            addMember(person);
        }
        this.rootMember = person;
    }
    
    public Set<Person> getMembers() {
        return members;
    }
}