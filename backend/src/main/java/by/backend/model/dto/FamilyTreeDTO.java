package by.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyTreeDTO {
    private Long id;
    private String name;
    private PersonDTO rootPerson;
    private List<PersonDTO> persons;
    private List<RelationshipDTO> relationships;
} 