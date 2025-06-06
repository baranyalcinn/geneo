package by.backend.model.dto;

import by.backend.model.enums.Gender;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonInfoDTO {
    private Long id;
    private String name;
    private Gender gender;
    private Integer birthYear;
    private Integer deathYear;
} 