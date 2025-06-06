package by.backend.model.dto;

import by.backend.model.enums.Gender;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PersonInfoDTO {
    private Long id;
    private String name;
    private Gender gender;
    private Integer birthYear;
    private Integer deathYear;
} 