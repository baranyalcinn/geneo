package by.backend.model.dto;

import by.backend.model.enums.RelationshipType;
import java.time.LocalDate;

public record RelationshipDTO(
    Long id,
    PersonSummaryDTO person1,
    PersonSummaryDTO person2,
    RelationshipType type,
    LocalDate startDate,
    LocalDate endDate,
    boolean isActive
) {} 