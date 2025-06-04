package by.backend.service.description;

import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.entity.Person;
// import by.backend.model.dto.PersonSummaryDTO; // Person nesneleri alınacak

import java.util.Locale;

public interface RelationshipDescriptionResolver {
    RelationshipDescriptionResult resolveDescription(Person person1, Person person2, Locale locale);
} 