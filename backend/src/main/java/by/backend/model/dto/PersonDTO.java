package by.backend.model.dto;

import by.backend.model.enums.Gender;
import java.time.LocalDate;

public record PersonDTO(
    Long id,
    String firstName,
    String lastName,
    LocalDate birthDate,
    LocalDate deathDate,
    Gender gender,
    PersonSummaryDTO mother,
    PersonSummaryDTO father,
    PersonSummaryDTO spouse
) {} 