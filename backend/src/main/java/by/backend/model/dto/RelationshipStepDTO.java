package by.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.LocalDate;

@Data
@Builder(access = AccessLevel.PUBLIC)
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipStepDTO {
    private Long personId;
    private String personName;
    private String personGender;
    private Integer personBirthYear;
    private String relationshipToNextPerson;
    private boolean sourcePerson;
    private boolean targetPerson;
    private Long nextPersonId;
    private String nextPersonName;
    private String relationshipTypeName;
    private LocalDate relationshipStartDate;
    private LocalDate relationshipEndDate;
} 