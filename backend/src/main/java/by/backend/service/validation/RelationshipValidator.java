package by.backend.service.validation;

import by.backend.config.RelationshipProperties;
import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.RelationshipType;
import by.backend.repository.RelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelationshipValidator {

    private final RelationshipRepository relationshipRepository;
    private final MessageSource messageSource;
    private final RelationshipProperties relationshipProperties;

    public void validateRelationship(Person person1, Person person2, RelationshipType type, Locale locale) {
        if (person1.getId().equals(person2.getId())) {
            throw new IllegalArgumentException(getMessage("validation.error.self_relationship", locale, person1.getFirstName()));
        }

        if (type == RelationshipType.SPOUSE) {
            validateSpouseSpecificRules(person1, person2, locale);
        }

        if (type == RelationshipType.PARENT_CHILD) {
            validateParentChildSpecificRules(person1, person2, locale);
        }
    }

    private void validateSpouseSpecificRules(Person person1, Person person2, Locale locale) {
        // Cinsiyet uyumluluğu (aynı cinsiyetten evlilik kontrolü)
        if (person1.getGender() != null && person2.getGender() != null && person1.getGender().equals(person2.getGender())) {
            throw new IllegalArgumentException(getMessage("validation.error.spouse.same_gender", locale));
        }

        // Mevcut aktif eş kontrolü
        if (relationshipRepository.existsByPersonAndTypeAndIsActiveTrue(person1.getId(), RelationshipType.SPOUSE)) {
            throw new IllegalStateException(getMessage("validation.error.spouse.multiple_active", locale, person1.getFirstName()));
        }
        if (relationshipRepository.existsByPersonAndTypeAndIsActiveTrue(person2.getId(), RelationshipType.SPOUSE)) {
            throw new IllegalStateException(getMessage("validation.error.spouse.multiple_active", locale, person2.getFirstName()));
        }
    }

    private void validateParentChildSpecificRules(Person person1, Person person2, Locale locale) {
        // Ebeveynin çocuktan yaşlı olma durumu ve minimum ebeveyn yaşı
        if (person1.getBirthDate() != null && person2.getBirthDate() != null) {
            if (person1.getBirthDate().isAfter(person2.getBirthDate())) {
                throw new IllegalArgumentException(getMessage("validation.error.parent_child.parent_younger", locale));
            }

            LocalDate childsBirthDate = person2.getBirthDate();
            LocalDate parentBirthAtChildsBirth = person1.getBirthDate().plusYears(relationshipProperties.getMinParentAge());

            if (parentBirthAtChildsBirth.isAfter(childsBirthDate)) {
                log.warn("Ebeveyn-çocuk yaş farkı çok az ({} yıldan az): person1.id={}, person1.birthDate={}, person2.id={}, person2.birthDate={}",
                        relationshipProperties.getMinParentAge(), person1.getId(), person1.getBirthDate(), person2.getId(), person2.getBirthDate());
            }
        }

        // Döngüsel ilişki kontrolü (çocuğun, ebeveynin atalarından biri olmaması)
        Set<Person> ancestorsOfParent = getAncestorsForValidation(person1);
        if (ancestorsOfParent.contains(person2)) {
            throw new IllegalArgumentException(getMessage("validation.error.parent_child.cyclic.ancestor_is_child", locale, person2.getFirstName(), person1.getFirstName()));
        }
    }

    /**
     * Validasyon amacıyla bir kişinin atalarını getiren özel metot.
     * Bu metot, RelationshipServiceImpl'deki getAllAncestors'a doğrudan bağımlı olmamak için buradadır.
     */
    private Set<Person> getAncestorsForValidation(Person person) {
        Set<Person> ancestors = new HashSet<>();
        Queue<Person> queue = new LinkedList<>();
        queue.add(person);
        Set<Long> visited = new HashSet<>();
        visited.add(person.getId());

        while (!queue.isEmpty()) {
            Person current = queue.poll();
            // Sadece aktif PARENT_CHILD ilişkilerine bakılır.
            List<Relationship> parentRelationships = relationshipRepository.findByPerson2AndTypeAndIsActiveTrue(current, RelationshipType.PARENT_CHILD);
            for (Relationship rel : parentRelationships) {
                Person parent = rel.getPerson1();
                if (!visited.contains(parent.getId())) {
                    ancestors.add(parent);
                    queue.add(parent);
                    visited.add(parent.getId());
                }
            }
        }
        return ancestors;
    }

    private String getMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException _) {
            log.warn("Message key not found in RelationshipValidator: {} for locale {}", code, locale);
            String fallback = code;
            if (args != null && args.length > 0) {
                fallback += " (" + Arrays.stream(args).map(String::valueOf).collect(Collectors.joining(", ")) + ")";
            }
            return "[[VALIDATION_MSG_NOT_FOUND: " + fallback + "]] "; // Fallback daha belirgin hale getirildi
        }
    }
} 