package by.backend.service.game;

import by.backend.exception.GameException;
import by.backend.model.dto.AnswerValidationResult;
import by.backend.model.dto.PersonSummaryDTO;
import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.dto.RelationshipPathDTO;
import by.backend.model.dto.RelationshipStepDTO;
import by.backend.model.entity.Person;
import by.backend.model.enums.RelationshipStatus;
import by.backend.repository.PersonRepository;
import by.backend.service.relationship.RelationshipService;
import by.backend.mapper.PersonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnswerValidatorServiceImpl implements AnswerValidatorService {

    private final PersonRepository personRepository;
    private final RelationshipService relationshipService;
    private final PersonMapper personMapper;
    private final MessageSource messageSource;

    @Override
    public AnswerValidationResult validateAnswer(String questionId, String userAnswer, Locale locale) {
        try {
            String[] ids = questionId.split("_");
            if (ids.length != 2) {
                throw new GameException("Invalid Question ID format: " + questionId);
            }

            long person1Id = Long.parseLong(ids[0]);
            long person2Id = Long.parseLong(ids[1]);
            
            Person p1 = personRepository.findById(person1Id)
                    .orElseThrow(() -> new GameException("Person 1 not found with id: " + person1Id));
            Person p2 = personRepository.findById(person2Id)
                    .orElseThrow(() -> new GameException("Person 2 not found with id: " + person2Id));
            
            PersonSummaryDTO p1Summary = personMapper.toSummaryDTO(p1);
            PersonSummaryDTO p2Summary = personMapper.toSummaryDTO(p2);

            RelationshipDescriptionResult result = relationshipService.findRelationshipDescription(p1Summary, p2Summary);

            if (result.getStatus() != RelationshipStatus.FOUND) {
                log.warn("Could not determine relationship for question ID: {}. Status: {}", questionId, result.getStatus());
                throw new GameException("Could not determine relationship for validation.");
            }

            String correctAnswerKey = result.getMessageKey();
            String correctAnswerText = getMessage(correctAnswerKey, locale);
            boolean isCorrect = normalizeAnswer(correctAnswerText).equalsIgnoreCase(normalizeAnswer(userAnswer));
            
            RelationshipPathDTO relationshipPath = null;
            if (isCorrect) {
                List<RelationshipStepDTO> pathSteps = result.getPath();
                relationshipPath = new RelationshipPathDTO();
                relationshipPath.setSteps(pathSteps);
                relationshipPath.setDescription(correctAnswerText);
            }
            
            String category = getRelationshipCategory(correctAnswerKey);

            return new AnswerValidationResult(isCorrect, correctAnswerText, category, relationshipPath);

        } catch (Exception e) {
            log.error("Error validating answer for questionId '{}': {}", questionId, e.getMessage(), e);
            throw new GameException(getMessage("game.error.could_not_determine_correct_answer", locale));
        }
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.trim().toLowerCase(Locale.ROOT);
    }

    private String getMessage(String code, Locale locale) {
        try {
            return messageSource.getMessage(code, null, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Translation not found for key: '{}', locale: {}. Using key as fallback.", code, locale);
            return code;
        }
    }
    
    private String getRelationshipCategory(String messageKey) {
        if (messageKey == null) return "undefined";
        String key = messageKey.toLowerCase(Locale.ROOT);
        if (key.startsWith("relationship.direct") || key.startsWith("relationship.reverse") || key.contains(".spouse")) return "direct";
        if (key.contains(".sibling")) return "siblings";
        if (key.contains("grandparent")) return "grandparent";
        if (key.contains("grandchild")) return "grandchild";
        if (key.contains("aunt") || key.contains("uncle")) return "aunt_uncle";
        if (key.contains("nephew") || key.contains("niece")) return "nephew_niece";
        if (key.contains("cousin")) return "cousin";
        if (key.contains("inlaw")) return "inlaw";
        if (key.contains("step")) return "step";
        if (key.contains("itself")) return "self";
        if (key.contains("not_found")) return "none";
        return "other";
    }
} 