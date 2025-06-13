package by.backend.model.dto;

import by.backend.model.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMemberDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private Integer birthYear;
    private Integer deathYear;
    private Gender gender;
    private Integer relationshipCount;
    private List<String> relationshipTypes;
    private Integer age;
    private Boolean isAlive;
    private String generationLevel;
    private Double relationshipStrength;
} 